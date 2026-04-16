package Controllers.user.auth;

import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ResetPasswordController {

    @FXML private Label emailLabel;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private VBox alertBox;
    @FXML private Label alertLabel;

    private String email;
    private final UserService userService = new UserService();

    public void setEmail(String email) {
        this.email = email;
        if (emailLabel != null) {
            emailLabel.setText("Un code a été envoyé à : " + email);
        }
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (newPassword.length() < 8) {
            showError("Le mot de passe doit contenir au moins 8 caractères.");
            return;
        }

        boolean success = userService.finalizePasswordReset(email, code, newPassword);
        if (success) {
            showSuccess("Votre mot de passe a été réinitialisé avec succès !");
            // Optionnel : redirection automatique vers login après 2 secondes
        } else {
            showError("Code invalide ou erreur lors de la réinitialisation.");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    private void showError(String message) {
        alertBox.setVisible(true);
        alertBox.setManaged(true);
        alertBox.setStyle("-fx-background-color: rgba(244,63,94,0.12); -fx-border-color: rgba(244,63,94,0.25); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px;");
        alertLabel.setText(message);
        alertLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        alertBox.setVisible(true);
        alertBox.setManaged(true);
        alertBox.setStyle("-fx-background-color: rgba(16,185,129,0.12); -fx-border-color: rgba(16,185,129,0.25); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px;");
        alertLabel.setText(message);
        alertLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-font-weight: bold;");
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
            e.printStackTrace();
        }
    }
}
