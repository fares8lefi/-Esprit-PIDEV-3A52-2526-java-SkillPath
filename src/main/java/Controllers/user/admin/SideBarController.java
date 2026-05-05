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

public class SideBarController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnCourses;
    @FXML private Button btnModules;
    @FXML private Button btnReclamations;
    @FXML private Button btnEvaluation;
    @FXML private Button btnUsers;
    @FXML private Button btnSecurity;
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Le style par défaut est géré par l'injection dans le contrôleur parent
        // ou via setSelected() après le chargement.
    }

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
        navigateTo(event, "/BackOffice/Admin/reclamation/manageReclamations.fxml", "Demandes des Membres");
    }

    @FXML
    private void handleEvaluation(ActionEvent event) {
        setActive(btnEvaluation);
        navigateTo(event, "/BackOffice/evaluation/QuizManagement.fxml", "Centre d'Évaluation - SkillPath");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        setActive(btnUsers);
        navigateTo(event, "/BackOffice/Admin/user/gererUser.fxml", "Annuaire des Membres - Administrateur");
    }

    @FXML
    private void handleSecurity(ActionEvent event) {
        setActive(btnSecurity);
        navigateTo(event, "/BackOffice/Admin/user/security_dashboard.fxml", "Shield Center - Sécurité");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Session.getInstance().logout();
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Permet aux contrôleurs parents de définir quel bouton est actif.
     */
    public void setSelected(String pageName) {
        switch (pageName.toLowerCase()) {
            case "dashboard"    -> setActive(btnDashboard);
            case "courses"      -> setActive(btnCourses);
            case "modules"      -> setActive(btnModules);
            case "reclamations" -> setActive(btnReclamations);
            case "evaluation"   -> setActive(btnEvaluation);
            case "users"        -> setActive(btnUsers);
            case "security"     -> setActive(btnSecurity);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private void setActive(Button active) {
        Button[] all = {btnDashboard, btnCourses, btnModules, btnReclamations, btnEvaluation, btnUsers, btnSecurity};
        for (Button btn : all) {
            if (btn == null) {
                continue;
            }
            btn.getStyleClass().remove("nav-item-active");
            if (!btn.getStyleClass().contains("nav-item")) {
                btn.getStyleClass().add("nav-item");
            }
        }
        if (active != null && !active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

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
            System.err.println("[SideBarController] Erreur navigation vers " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
