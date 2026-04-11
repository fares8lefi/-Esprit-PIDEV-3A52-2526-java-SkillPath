package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {

    @FXML private Button registerBtn;
    @FXML private Button loginBtn;
    @FXML private Button startAdventureBtn;

    @FXML
    public void goToSignup() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/FrontOffice/user/auth/signup.fxml")
            );
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur redirection signup : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/FrontOffice/user/auth/login.fxml")
            );
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Erreur redirection login : " + e.getMessage());
        }
    }
}
