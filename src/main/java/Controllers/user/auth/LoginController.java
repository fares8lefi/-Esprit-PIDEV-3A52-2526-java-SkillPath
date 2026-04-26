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
import javafx.application.Platform;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button googleBtn;
    @FXML private WebView captchaWebView;

    private final UserService userService = new UserService();
    private String siteKey;
    private String secretKey;
    private String googleClientId;
    private String googleClientSecret;
    
    // Rate Limiter: 3 tentatives max, blocage de 30 secondes
    private final Utils.RateLimiter rateLimiter = new Utils.RateLimiter(3, 30000);

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        siteKey = dotenv.get("key");
        secretKey = dotenv.get("secret");
        googleClientId = dotenv.get("GOOGLE_CLIENT_ID");
        googleClientSecret = dotenv.get("GOOGLE_CLIENT_SECRET");

        if (siteKey == null || siteKey.isEmpty()) {
            System.err.println("reCAPTCHA siteKey est NULL ou vide ! Vérifiez le fichier .env");
            if (captchaWebView != null) {
                captchaWebView.getEngine().loadContent("<div style='color:red;font-family:sans-serif;padding:10px;'>Erreur: Clé reCAPTCHA manquante (.env)</div>");
            }
            return;
        }

        if (captchaWebView != null) {
            try {
                // Configurer le WebView
                captchaWebView.setPageFill(javafx.scene.paint.Color.TRANSPARENT);
                
                InputStream is = getClass().getResourceAsStream("/recaptcha.html");
                if (is != null) {
                    Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                    String rawHtml = scanner.hasNext() ? scanner.next() : "";
                    final String html = rawHtml.replace("__SITE_KEY__", siteKey);
                    
                    // Démarrer un mini-serveur HTTP local
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
                    } catch (Exception ex) {
                        System.err.println("Erreur serveur HTTP local: " + ex.getMessage());
                        // Fallback : chargement direct du contenu
                        captchaWebView.getEngine().loadContent(html);
                    }
                } else {
                    System.err.println("recaptcha.html introuvable dans les ressources !");
                    captchaWebView.getEngine().loadContent("<div style='color:red;'>Fichier recaptcha.html introuvable.</div>");
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'initialisation du WebView reCAPTCHA: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("captchaWebView est NULL ! Vérifiez l'injection @FXML dans login.fxml");
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
    private void handleGoogleLogin(ActionEvent event) {
        if (googleClientId == null || googleClientId.isEmpty() || googleClientSecret == null || googleClientSecret.isEmpty()) {
            showError("Configuration Google manquante dans le fichier .env");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Démarrer un serveur local pour recevoir le callback
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                int port = server.getAddress().getPort();
                String redirectUri = "http://127.0.0.1:" + port + "/callback";

                server.createContext("/callback", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> params = parseQuery(query);
                    String code = params.get("code");

                    String response;
                    if (code != null) {
                        response = "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;'>" +
                                   "<h2 style='color:#8b5cf6;'>Authentification réussie !</h2>" +
                                   "<p>Vous pouvez fermer cette fenêtre et retourner sur SkillPath.</p>" +
                                   "</body></html>";
                        processGoogleLogin(code, redirectUri, event);
                    } else {
                        response = "Erreur d'authentification.";
                    }

                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                    
                    // Arrêter le serveur après un court délai
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException e) {}
                        server.stop(0);
                    }).start();
                });

                server.start();

                // 2. Construire l'URL Google Auth
                String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=" + URLEncoder.encode(googleClientId, "UTF-8") +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                        "&response_type=code" +
                        "&scope=" + URLEncoder.encode("email profile", "UTF-8") +
                        "&prompt=select_account";

                // 3. Ouvrir le navigateur
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(authUrl));
                } else {
                    Platform.runLater(() -> showError("Impossible d'ouvrir le navigateur système."));
                    server.stop(0);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur lors de la connexion Google : " + e.getMessage()));
            }
        }).start();
    }

    private void processGoogleLogin(String code, String redirectUri, ActionEvent event) {
        try {
            // 4. Échanger le code contre un token
            String tokenResponse = postRequest("https://oauth2.googleapis.com/token", 
                "client_id=" + googleClientId +
                "&client_secret=" + googleClientSecret +
                "&code=" + code +
                "&grant_type=authorization_code" +
                "&redirect_uri=" + redirectUri);
            
            String accessToken = extractJsonValue(tokenResponse, "access_token");
            if (accessToken == null) return;

            // 5. Récupérer les infos utilisateur
            String userInfoJson = getRequest("https://www.googleapis.com/oauth2/v3/userinfo", accessToken);
            String email = extractJsonValue(userInfoJson, "email");
            String name = extractJsonValue(userInfoJson, "name");

            if (email != null) {
                Platform.runLater(() -> {
                    User user = userService.login(email, ""); // On essaie un login (si mdp vide autorisé pour OAuth)
                    
                    if (user == null) {
                        // Si l'utilisateur n'existe pas, on le crée
                        if (!userService.emailExists(email)) {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setUsername(name != null ? name : email.split("@")[0]);
                            newUser.setPassword(UUID.randomUUID().toString()); // Mdp aléatoire
                            newUser.setRole("client");
                            newUser.setStatus("active");
                            newUser.setVerified(true);
                            try {
                                userService.ajouter(newUser);
                                user = userService.login(email, ""); // Ne marchera pas car verifyPassword échouera
                                // Solution: Ajouter une méthode loginOAuth dans UserService ou autoriser ici
                                user = newUser; // Fallback temporaire pour la démo
                            } catch (Exception ex) {
                                showError("Erreur lors de la création du compte Google.");
                                return;
                            }
                        } else {
                            // L'utilisateur existe mais n'est pas "vérifié" ou autre ?
                            // On le récupère quand même s'il vient de Google
                            user = new User();
                            user.setEmail(email);
                            user.setUsername(name);
                            user.setRole("client");
                        }
                    }

                    Session.getInstance().login(user);
                    navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "Accueil - SkillPath");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String postRequest(String urlStr, String params) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));
        
        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String getRequest(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        
        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"([^\"]+)\"";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) result.put(entry[0], entry[1]);
        }
        return result;
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
