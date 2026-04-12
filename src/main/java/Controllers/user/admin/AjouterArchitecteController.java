package Controllers.user.admin;

import Models.User;
import Services.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'interface de création d'architecte par l'admin.
 */
public class AjouterArchitecteController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button btnSubmit;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser ComboBox Rôles
        roleComboBox.setItems(FXCollections.observableArrayList("étudiant", "admin", "architecte"));
        roleComboBox.setValue("architecte");
    }

    /**
     * Gère la création de l'architecte.
     */
    @FXML
    private void handleCreate(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        // 1. Validation basique
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champs requis", "Veuillez remplir tous les champs !");
            return;
        }

        // 2. Création de l'utilisateur
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(role);
        newUser.setStatus("active");

        try {
            userService.ajouterUserParAdmin(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "L'utilisateur " + username + " a été créé!");
            handleBack(event);
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de créer : " + e.getMessage());
        }
    }

    /**
     * Retourne à la page précédente.
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // New path after the user moved things
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/Admin/user/homeAdmin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Tableau de Bord Admin - SkillPath");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection → " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
