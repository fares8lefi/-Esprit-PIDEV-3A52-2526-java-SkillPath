package Controllers.user.admin;

import Models.User;
import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.ResourceBundle;

public class ModifyUserController implements Initializable {

    @FXML private Label lblEmail;
    @FXML private TextField tfUsername;
    @FXML private ComboBox<String> comboRole;
    @FXML private ComboBox<String> comboStatus;
    @FXML private TextField tfDomaine;
    @FXML private TextField tfNiveau;
    @FXML private TextField tfStyle;
    
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private User currentUser;
    private final UserService userService = new UserService();
    private Runnable onUpdateSuccess;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les choix possibles
        comboRole.getItems().addAll("ROLE_USER", "ROLE_ADMIN", "ROLE_INSTRUCTEUR", "ROLE_ARCHITECTE");
        comboStatus.getItems().addAll("active", "inactive", "banned");
    }

    /**
     * Reçoit les données de l'utilisateur cliqué depuis GererUserController.
     */
    public void initData(User user, Runnable onUpdateSuccess) {
        this.currentUser = user;
        this.onUpdateSuccess = onUpdateSuccess;

        // Remplir les champs
        lblEmail.setText(user.getEmail());
        tfUsername.setText(user.getUsername());
        
        // Sélectionner les bonnes valeurs dans les ComboBox
        String currentRole = user.getRole();
        if(currentRole != null) {
            String roleUpper = currentRole.toUpperCase();
            if(!roleUpper.startsWith("ROLE_")) { roleUpper = "ROLE_" + roleUpper; }
            if (comboRole.getItems().contains(roleUpper)) {
                comboRole.setValue(roleUpper);
            } else {
                comboRole.setValue(currentRole);
            }
        }

        if (user.getStatus() != null) {
            comboStatus.setValue(user.getStatus().toLowerCase());
        }

        tfDomaine.setText(user.getDomaine());
        tfNiveau.setText(user.getNiveau());
        tfStyle.setText(user.getStyleDapprentissage());
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String newUsername = tfUsername.getText().trim();
        String newRole = comboRole.getValue();
        String newStatus = comboStatus.getValue();

        if (newUsername.isEmpty() || newRole == null || newStatus == null) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        // Mettre à jour l'objet
        currentUser.setUsername(newUsername);
        
        // Clean prefix if needed by DB, but here we keep "ROLE_" as is standardized
        currentUser.setRole(newRole);
        
        currentUser.setStatus(newStatus);
        currentUser.setDomaine(tfDomaine.getText().trim());
        currentUser.setNiveau(tfNiveau.getText().trim());
        currentUser.setStyleDapprentissage(tfStyle.getText().trim());

        try {
            userService.modifier(currentUser);
            
            // Rafraîchir la liste dans l'interface parente
            if (onUpdateSuccess != null) {
                onUpdateSuccess.run();
            }

            closeWindow(event);

        } catch (SQLDataException e) {
            showAlert("Erreur Modification", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
