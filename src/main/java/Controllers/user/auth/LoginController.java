package Controllers.user.auth;

import Models.User;
import Services.UserService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Scanner;
import io.github.cdimascio.dotenv.Dotenv;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import javafx.scene.web.WebView;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private WebView captchaWebView;

    private final UserService userService = new UserService();
    private String siteKey;
    private String secretKey;
    
    // Rate Limiter: 3 tentatives max, blocage de 30 secondes
    private final Utils.RateLimiter rateLimiter = new Utils.RateLimiter(3, 30000);

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        siteKey = dotenv.get("key");
        secretKey = dotenv.get("secret");

        if (siteKey != null && captchaWebView != null) {
            try {
                InputStream is = getClass().getResourceAsStream("/recaptcha.html");
                if (is != null) {
                    Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                    String rawHtml = scanner.hasNext() ? scanner.next() : "";
                    final String html = rawHtml.replace("__SITE_KEY__", siteKey);
                    // Démarrer un mini-serveur HTTP local pour contourner la restriction de domaine
                    try {
                        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                        server.createContext("/", exchange -> {
                            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                            byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(200, bytes.length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(bytes);
                            os.close();
                        });
                        server.setExecutor(null);
                        server.start();
                        
                        int port = server.getAddress().getPort();
                        captchaWebView.getEngine().load("http://127.0.0.1:" + port + "/");
                        
                        // Assurez-vous d'arrêter le serveur quand la fenêtre se ferme si nécessaire
                        // mais pour le login, un port éphémère est généralement ok
                    } catch (Exception ex) {
                        System.err.println("Erreur serveur HTTP local: " + ex.getMessage());
                        // Fallback
                        captchaWebView.getEngine().loadContent(html);
                    }
                } else {
                    System.err.println("recaptcha.html introuvable !");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // Vérification du Rate Limiter
        if (rateLimiter.isLockedOut()) {
            showError("Trop de tentatives. Veuillez patienter " + rateLimiter.getSecondsLeft() + " secondes.");
            return;
        }

        // Vérification du reCAPTCHA
        if (captchaWebView != null && secretKey != null && !secretKey.isEmpty()) {
            try {
                // Vérifier d'abord si le captcha a été coché
                Object verifiedObj = captchaWebView.getEngine().executeScript("isCaptchaVerified()");
                boolean isVerified = verifiedObj instanceof Boolean ? (Boolean) verifiedObj : false;
                
                if (!isVerified) {
                    showError("Veuillez cocher la case 'Je ne suis pas un robot' pour continuer.");
                    return;
                }
                
                String token = (String) captchaWebView.getEngine().executeScript("getCaptchaResponse()");
                if (token == null || token.isEmpty()) {
                    showError("Erreur de communication avec reCAPTCHA. Veuillez réessayer.");
                    return;
                }
                if (!verifierCaptcha(token)) {
                    showError("La vérification reCAPTCHA a échoué. Veuillez réessayer.");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Erreur exécution script reCAPTCHA: " + e.getMessage());
                showError("Erreur lors de la vérification. Veuillez réessayer.");
            }
        }

        User user = userService.login(email, password);

        if (user != null) {
            // Réinitialiser les tentatives après un succès
            rateLimiter.recordSuccess();
            
            // Stockage dans la session
            Session.getInstance().login(user);
            System.out.println("Connexion réussie : " + user.getUsername());
            
            // Redirection selon le rôle
            if ("admin".equalsIgnoreCase(user.getRole())) {
                navigateTo(event, "/BackOffice/Admin/user/homeAdmin.fxml", "Tableau de Bord Admin");
            } else {
                navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "Accueil - SkillPath");
            }
        } else {
            rateLimiter.recordFailure();
            if (rateLimiter.isLockedOut()) {
                showError("Compte bloqué temporairement (30s) suite à plusieurs échecs.");
            } else {
                showError("Email ou mot de passe incorrect. Il vous reste " + rateLimiter.getAttemptsLeft() + " tentative(s).");
            }
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/auth/forgot_password.fxml", "Mot de passe oublié - SkillPath");
    }

    @FXML
    private void handleShowSignup(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/auth/signup.fxml", "Inscription - SkillPath");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation vers " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean verifierCaptcha(String token) {
        if (secretKey == null) return true;
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String parametres = "secret=" + secretKey + "&response=" + token;

        try {
            HttpURLConnection connection = (HttpURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(parametres.getBytes());
            os.flush();
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder reponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                reponse.append(inputLine);
            }
            in.close();

            return reponse.toString().contains("\"success\": true");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
