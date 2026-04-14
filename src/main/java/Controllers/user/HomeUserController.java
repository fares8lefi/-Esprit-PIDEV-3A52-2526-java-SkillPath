package Controllers.user;

import Models.User;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeUserController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private Label avatarInitial;
    @FXML private FlowPane courseContainer;
    @FXML private Button logoutBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupération de l'utilisateur connecté via le Singleton Session
        User currentUser = Session.getCurrentUser();

        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            
            // Affichage de l'initiale
            if (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
                avatarInitial.setText(currentUser.getUsername().substring(0, 1).toUpperCase());
            }
        } else {
            // Si personne n'est connecté (accès direct sans login), on redirige vers le login
            System.out.println("Aucune session trouvée, redirection...");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // On vide la session (Singleton)
        Session.logout();
        
        // Redirection vers la page de login
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
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
    @FXML
    private void openAddReclamation(ActionEvent event) {
        navigateTo(event, "/FrontOffice/reclamation/AddReclamation.fxml", "Nouvelle Réclamation");
    }

    @FXML
    private void openMyReclamations(ActionEvent event) {
        navigateTo(event, "/FrontOffice/reclamation/UserReclamations.fxml", "Mes Réclamations");
    }
}
