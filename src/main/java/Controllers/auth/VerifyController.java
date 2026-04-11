package Controllers.auth;

import Services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class VerifyController {

    @FXML private TextField codeField;
    @FXML private Button verifyButton;
    @FXML private HBox alertBox;
    @FXML private Label alertLabel;
    @FXML private Label emailLabel;

    private String email;
    private final UserService userService = new UserService();

    public void setEmail(String email) {
        this.email = email;
        if (emailLabel != null) {
            emailLabel.setText("Un code a été envoyé à : " + email);
        }
    }

    @FXML
    public void initialize() {
        if (alertBox != null) alertBox.setVisible(false);
    }

    @FXML
    public void handleVerify() {
        hideAlert();
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            showError("Veuillez entrer le code de vérification.");
            return;
        }
        if (code.length() != 6 || !code.matches("\\d{6}")) {
            showError("Le code doit être composé de 6 chiffres.");
            return;
        }

        boolean success = userService.verifyCode(email, code);
        if (success) {
            // Rediriger vers login
            try {
                Parent root = FXMLLoader.load(
                    getClass().getResource("/FrontOffice/user/auth/login.fxml")
                );
                Stage stage = (Stage) verifyButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                showSuccess("Compte activé avec succès ! Vous pouvez vous connecter.");
            }
        } else {
            showError("Code incorrect. Veuillez réessayer.");
        }
    }

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
        }
    }

    private void hideAlert() {
        if (alertBox != null) {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }
    }
}
