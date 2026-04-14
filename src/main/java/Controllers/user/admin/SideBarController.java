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

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnCourses;
    @FXML
    private Button btnModules;
    @FXML
    private Button btnReclamations;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActive(btnDashboard);
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        setActive(btnDashboard);
        navigateTo(event, "/BackOffice/Admin/user/homeAdmin.fxml", "Tableau de Bord Admin");
    }

    @FXML
    private void handleCourses(ActionEvent event) {
        setActive(btnCourses);
        System.out.println("[Navigation] Cours non implemente.");
    }

    @FXML
    private void handleModules(ActionEvent event) {
        setActive(btnModules);
        System.out.println("[Navigation] Modules non implemente.");
    }

    @FXML
    private void handleReclamations(ActionEvent event) {
        setActive(btnReclamations);
        navigateTo(event, "/BackOffice/Admin/reclamation/manageReclamations.fxml", "Demandes des Membres");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        setActive(btnUsers);
        System.out.println("[Navigation] Utilisateurs non implemente.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Session.logout();
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    private void setActive(Button active) {
        Button[] all = {btnDashboard, btnCourses, btnModules, btnReclamations, btnUsers};
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
