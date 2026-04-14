package Controllers.user.admin;

import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur de la barre latérale admin.
 * Gère la navigation entre les modules et la déconnexion.
 * Chargé via fx:include dans homeAdmin.fxml.
 */
public class SideBarController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnCourses;
    @FXML private Button btnModules;
    @FXML private Button btnReclamations;
    @FXML private Button btnUsers;
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Appliquer le style actif sur le bouton Dashboard par défaut
        setActive(btnDashboard);
    }

    // ─────────────────────────────────────────────────────────────
    // Handlers navigation
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleDashboard(ActionEvent event) {
        setActive(btnDashboard);
        navigateTo(event, "/BackOffice/Admin/user/homeAdmin.fxml", "Tableau de Bord Admin");
    }

    @FXML
    private void handleCourses(ActionEvent event) {
        setActive(btnCourses);
        navigateTo(event, "/BackOffice/course/courseList.fxml", "Système de Cours");
    }

    @FXML
    private void handleModules(ActionEvent event) {
        setActive(btnModules);
        navigateTo(event, "/BackOffice/module/moduleList.fxml", "Unités d'Apprentissage");
    }

    @FXML
    private void handleReclamations(ActionEvent event) {
        setActive(btnReclamations);
        // navigateTo(event, "/BackOffice/reclamation/reclamationList.fxml", "Demandes des Membres");
        System.out.println("[Navigation] → Demandes des Membres (à implémenter)");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        setActive(btnUsers);
        // navigateTo(event, "/BackOffice/user/userList.fxml", "Annuaire des Utilisateurs");
        System.out.println("[Navigation] → Annuaire des Utilisateurs (à implémenter)");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Session.logout();
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Met en surbrillance le bouton actif et retire le style actif des autres.
     */
    private void setActive(Button active) {
        Button[] all = {btnDashboard, btnCourses, btnModules, btnReclamations, btnUsers};
        for (Button btn : all) {
            if (btn == null) continue;
            btn.getStyleClass().remove("nav-item-active");
            if (!btn.getStyleClass().contains("nav-item")) {
                btn.getStyleClass().add("nav-item");
            }
        }
        if (active != null && !active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    /**
     * Charge un FXML et remplace la scène courante.
     */
    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setMaximized(stage.isMaximized());
            stage.show();
        } catch (IOException e) {
            System.err.println("[SideBarController] Erreur navigation → " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
