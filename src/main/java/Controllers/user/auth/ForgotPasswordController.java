package Controllers.user.auth;

import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private VBox alertBox;
    @FXML private Label alertLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email.");
            return;
        }

        if (!userService.emailExists(email)) {
            showError("Aucun compte n'est associé à cette adresse email.");
            return;
        }

        boolean success = userService.requestPasswordReset(email);
        if (success) {
            goToResetPassword(event, email);
        } else {
            showError("Une erreur est survenue lors de l'envoi du code. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    private void goToResetPassword(ActionEvent event, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/auth/reset_password.fxml"));
            Parent root = loader.load();
            
            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Réinitialisation - SkillPath");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page de réinitialisation.");
        }
    }

    private void showError(String message) {
        alertBox.setVisible(true);
        alertBox.setManaged(true);
        alertBox.setStyle("-fx-background-color: rgba(244,63,94,0.12); -fx-border-color: rgba(244,63,94,0.25); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px;");
        alertLabel.setText(message);
        alertLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 13px; -fx-font-weight: bold;");
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
