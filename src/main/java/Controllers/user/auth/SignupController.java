package Controllers.user.auth;

import Models.User;
import Services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;

public class SignupController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox termsCheckbox;
    @FXML private Button submitButton;
    @FXML private Hyperlink loginLink;
    @FXML private HBox alertBox;
    @FXML private Label alertLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (alertBox != null) alertBox.setVisible(false);
    }

    @FXML
    public void handleSignup() {
        try {
            hideAlert();

            String username = usernameField.getText().trim();
            String email    = emailField.getText().trim();
            String password = passwordField.getText();

            // controller de saisie
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError("Veuillez remplir tous les champs.");
                return;
            }
            if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
                showError("L'adresse email n'est pas valide.");
                return;
            }
            if (password.length() < 8) {
                showError("Le mot de passe doit comporter au moins 8 caractères.");
                return;
            }
            if (!termsCheckbox.isSelected()) {
                showError("Veuillez accepter les conditions d'utilisation.");
                return;
            }
            if (userService.emailExists(email)) {
                showError("Un compte existe déjà avec cet email.");
                return;
            }

            // Générer un code de vérification
            String verificationCode = userService.generateVerificationCode();

            // Création de l'utilisateur
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setStatus("inactive");
            user.setRole("student");
            user.setVerified(false);
            user.setVerificationCode(verificationCode);

            try {
                userService.ajouter(user);
            } catch (SQLDataException e) {
                showError("Erreur lors de l'inscription : " + e.getMessage());
                return;
            }

            // Envoi de l'email de vérification et redirection
            submitButton.setDisable(true); // Désactiver le bouton pendant le traitement

            new Thread(() -> {
                try {
                    boolean emailSent = userService.sendVerificationEmail(email, username, verificationCode);
                    
                    javafx.application.Platform.runLater(() -> {
                        submitButton.setDisable(false);
                        if (!emailSent) {
                            showError("Compte créé, mais l'email de vérification n'a pas pu être envoyé.");
                        }

                        // Rediriger vers la page de vérification
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/FrontOffice/user/auth/verify.fxml")
                            );
                            Parent root = loader.load();

                            VerifyController verifyController = loader.getController();
                            verifyController.setEmail(email);

                            Stage stage = (Stage) submitButton.getScene().getWindow();
                            stage.setScene(new Scene(root));
                            stage.show();
                        } catch (IOException e) {
                            showSuccess("Compte créé ! Code envoyé à " + email);
                            System.err.println("Redirection verify.fxml : " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        submitButton.setDisable(false);
                        showError("Erreur lors de la finalisation : " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            showError("Une erreur inattendue est survenue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/FrontOffice/user/auth/login.fxml")
            );
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur navigation login : " + e.getMessage());
        }
    }

    //UI 
    private void showError(String message) {
        if (alertBox != null) {
            alertBox.setVisible(true);
            alertBox.setManaged(true);
            alertBox.setStyle(
                "-fx-background-color: rgba(244,63,94,0.12);" +
                "-fx-border-color: rgba(244,63,94,0.25);" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px;"
            );
            alertLabel.setText(message);
            alertLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            showAlert(Alert.AlertType.ERROR, message);
        }
    }

    private void showSuccess(String message) {
        if (alertBox != null) {
            alertBox.setVisible(true);
            alertBox.setManaged(true);
            alertBox.setStyle(
                "-fx-background-color: rgba(16,185,129,0.12);" +
                "-fx-border-color: rgba(16,185,129,0.25);" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px;"
            );
            alertLabel.setText(message);
            alertLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            showAlert(Alert.AlertType.INFORMATION, message);
        }
    }

    private void hideAlert() {
        if (alertBox != null) {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }
}
