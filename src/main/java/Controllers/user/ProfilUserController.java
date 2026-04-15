package Controllers.user;

import Models.User;
import Services.UserService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.ResourceBundle;

public class ProfilUserController implements Initializable {

    @FXML private Label usernameTitle;
    @FXML private Label roleLabel;
    @FXML private Label avatarInitial;
    
    @FXML private TextField tfUsername;
    @FXML private TextField tfEmail;
    @FXML private TextField tfDomaine;
    @FXML private TextField tfNiveau;
    @FXML private TextField tfStyle;
    
    @FXML private PasswordField tfCurrentPwd;
    @FXML private PasswordField tfNewPwd;
    @FXML private PasswordField tfConfirmPwd;

    private final UserService userService = new UserService();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserData();
        }
    }

    private void loadUserData() {
        usernameTitle.setText(currentUser.getUsername());
        roleLabel.setText(currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "ÉTUDIANT");
        if (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
            avatarInitial.setText(currentUser.getUsername().substring(0, 1).toUpperCase());
        }

        tfUsername.setText(currentUser.getUsername());
        tfEmail.setText(currentUser.getEmail());
        tfDomaine.setText(currentUser.getDomaine());
        tfNiveau.setText(currentUser.getNiveau());
        tfStyle.setText(currentUser.getStyleDapprentissage());
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        currentUser.setUsername(safeTrim(tfUsername.getText()));
        currentUser.setDomaine(safeTrim(tfDomaine.getText()));
        currentUser.setNiveau(safeTrim(tfNiveau.getText()));
        currentUser.setStyleDapprentissage(safeTrim(tfStyle.getText()));
       

        try {
            userService.modifier(currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès !");
            loadUserData(); // Rafraîchir l'interface
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le profil : " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        String current = tfCurrentPwd.getText();
        String next = tfNewPwd.getText();
        String confirm = tfConfirmPwd.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champs vides", "Veuillez remplir tous les champs de mot de passe.");
            return;
        }

        if (!next.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nouveau mot de passe et sa confirmation ne correspondent pas.");
            return;
        }

        // Vérifier le mot de passe actuel
        if (!userService.checkCurrentPassword(currentUser.getEmail(), current)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le mot de passe actuel est incorrect.");
            return;
        }

        try {
            userService.updatePassword(currentUser.getEmail(), next);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre mot de passe a été modifié avec succès !");
            
            // Effacer les champs
            tfCurrentPwd.clear();
            tfNewPwd.clear();
            tfConfirmPwd.clear();
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier le mot de passe : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "Tableau de Bord - SkillPath");
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
    private String safeTrim(String str) {
        return str == null ? "" : str.trim();
    }
}
