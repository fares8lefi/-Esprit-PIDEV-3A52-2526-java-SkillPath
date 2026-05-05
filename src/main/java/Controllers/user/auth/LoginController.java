package Controllers.user.auth;

import Models.User;
import Models.LoginResult;
import Services.SecurityService;
import Services.UserService;
import Utils.DatabaseConnection;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
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
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.application.Platform;
import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
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
    @FXML private Button verifyCaptchaBtn;
    @FXML private StackPane loadingContainer;
    @FXML private ProgressIndicator loadingIndicator;

    private Stage captchaStage;
    private boolean isCaptchaVerified = false;
    private String captchaToken = null;

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
        }
    }

    @FXML
    private void handleVerifyCaptcha(ActionEvent event) {
        if (siteKey == null || siteKey.isEmpty()) {
            showError("Erreur: Clé reCAPTCHA manquante (.env)");
            return;
        }

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setPageFill(javafx.scene.paint.Color.web("#0f172a")); // Dark background for the dialog

        try {
            String rawHtml = "";
            try (InputStream is = getClass().getResourceAsStream("/recaptcha.html")) {
                if (is != null) {
                    try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                        rawHtml = scanner.hasNext() ? scanner.next() : "";
                    }
                } else {
                    showError("Fichier recaptcha.html introuvable.");
                    return;
                }
            }

            final String html = rawHtml.replace("__SITE_KEY__", siteKey);

            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject) webView.getEngine().executeScript("window");
                    window.setMember("javaApp", this);
                }
            });

            // Start a local HTTP server for the WebView
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
                webView.getEngine().load("http://127.0.0.1:" + port + "/");
                
                // Stop the server when the stage closes
                webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.windowProperty().addListener((obsWindow, oldWindow, newWindow) -> {
                            if (newWindow != null) {
                                newWindow.setOnHidden(e -> server.stop(0));
                            }
                        });
                    }
                });

            } catch (Exception ex) {
                System.err.println("Erreur serveur HTTP local: " + ex.getMessage());
                webView.getEngine().loadContent(html);
            }

            captchaStage = new Stage();
            captchaStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            captchaStage.setTitle("Vérification de sécurité");
            captchaStage.setResizable(false);
            
            // Setting a reasonable size for the challenge
            Scene scene = new Scene(webView, 420, 600);
            captchaStage.setScene(scene);
            captchaStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'initialisation du reCAPTCHA");
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

        // ── Vérification reCAPTCHA ──────────────────────────────────────────
        if (!isCaptchaVerified || captchaToken == null) {
            showError(" Veuillez compléter le reCAPTCHA avant de vous connecter.");
            return;
        }
        
        if (!verifierCaptcha(captchaToken)) {
            showError(" La vérification reCAPTCHA a échoué. Veuillez réessayer.");
            isCaptchaVerified = false;
            captchaToken = null;
            verifyCaptchaBtn.setText("Effectuer la vérification");
            verifyCaptchaBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05);");
            verifyCaptchaBtn.setDisable(false);
            return;
        }
        // ────────────────────────────────────────────────────────────────────

        // Désactiver l'interface pendant la vérification
        loginBtn.setDisable(true);
        googleBtn.setDisable(true);
        emailField.setDisable(true);
        passwordField.setDisable(true);
        errorLabel.setVisible(false);
        
        // Afficher le loading indicator
        loadingContainer.setVisible(true);
        loadingContainer.setManaged(true);

        javafx.concurrent.Task<LoginResult> loginTask = new javafx.concurrent.Task<>() {
            @Override
            protected LoginResult call() throws Exception {
                String clientIp = java.net.InetAddress.getLocalHost().getHostAddress();
                java.sql.Connection conn = DatabaseConnection.getConnection();
                SecurityService securityService = new SecurityService();
                return securityService.login(email, password, clientIp, conn);
            }
        };

        loginTask.setOnSucceeded(e -> {
            // Masquer le loading indicator et réactiver les contrôles
            loadingContainer.setVisible(false);
            loadingContainer.setManaged(false);
            loginBtn.setDisable(false);
            googleBtn.setDisable(false);
            emailField.setDisable(false);
            passwordField.setDisable(false);
            
            LoginResult result = loginTask.getValue();

            if (result.isBlocked()) {
                showError(" ACCÈS BLOQUÉ : " + result.getMessage());
                errorLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-weight: bold; -fx-font-size: 12px;");
                return;
            }

            if (result.isSuccess()) {
                User user = result.getUser();
                Utils.Session.getInstance().login(user);
                System.out.println("✓ Connexion réussie : " + user.getUsername());
                
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    navigateTo(event, "/BackOffice/Admin/user/homeAdmin.fxml", "Tableau de Bord Admin");
                } else {
                    
                    navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "home user - SkillPath");
                }
            } else {
                showError(result.getMessage());
            }
        });

        loginTask.setOnFailed(e -> {
            // Masquer le loading indicator et réactiver les contrôles
            loadingContainer.setVisible(false);
            loadingContainer.setManaged(false);
            loginBtn.setDisable(false);
            googleBtn.setDisable(false);
            emailField.setDisable(false);
            passwordField.setDisable(false);
            // Réinitialiser le captcha après un échec
            isCaptchaVerified = false;
            captchaToken = null;
            verifyCaptchaBtn.setText("Effectuer la vérification");
            verifyCaptchaBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05);");
            verifyCaptchaBtn.setDisable(false);
            showError("Erreur de connexion au service de sécurité.");
            loginTask.getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        if (googleClientId == null || googleClientId.isEmpty() || googleClientSecret == null || googleClientSecret.isEmpty()) {
            showError("Configuration Google manquante dans le fichier .env");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Démarrer un serveur local pour recevoir le callback (Port fixe 54321)
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 54321), 0);
                int port = 54321;
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
                    // Tenter de récupérer l'utilisateur existant en base par son email
                    User user = userService.getUserByEmail(email);
                    
                    if (user == null) {
                        // Si l'utilisateur n'existe pas encore, on le crée
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setUsername(name != null ? name : email.split("@")[0]);
                        newUser.setPassword(UUID.randomUUID().toString()); // Mdp aléatoire non utilisé
                        newUser.setRole("client");
                        newUser.setStatus("active");
                        newUser.setVerified(true);
                        
                        try {
                            userService.ajouter(newUser);
                            // Après l'ajout, on le récupère pour avoir son ID généré en base
                            user = userService.getUserByEmail(email);
                        } catch (Exception ex) {
                            showError("Erreur lors de la création du compte Google.");
                            return;
                        }
                    }

                    if (user != null) {
                        Utils.Session.getInstance().login(user);
                        System.out.println("✓ Connexion Google réussie : " + user.getUsername());
                        if ("admin".equalsIgnoreCase(user.getRole())) {
                            navigateTo(event, "/BackOffice/Admin/user/homeAdmin.fxml", "Tableau de Bord Admin");
                        } else {
                            navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "home user - SkillPath");
                        }
                    } else {
                        showError("Impossible de finaliser la connexion Google.");
                    }
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

    // Méthodes appelées par le JavaScript
    public void onVerified(boolean verified) {
        Platform.runLater(() -> {
            System.out.println("reCAPTCHA vérifié via bridge");
        });
    }
    
    public void onTokenReceived(String token) {
        Platform.runLater(() -> {
            this.isCaptchaVerified = true;
            this.captchaToken = token;
            
            // Update button UI
            verifyCaptchaBtn.setText("Vérification réussie ✓");
            verifyCaptchaBtn.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-border-color: #10b981;");
            verifyCaptchaBtn.setDisable(true);
            errorLabel.setVisible(false);

            if (captchaStage != null && captchaStage.isShowing()) {
                captchaStage.close();
            }
        });
    }

    public void onChallengeStateChanged(boolean isChallengeVisible) {
        // Not really needed anymore since it's in a modal dialog of fixed size,
        // but we can leave it or just ignore it.
    }
}
