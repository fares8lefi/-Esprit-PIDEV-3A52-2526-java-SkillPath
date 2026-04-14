package Controllers.user.reclamation;

import Models.Reclamation;
import Services.ReclamationService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;

public class AddReclamationController {

    @FXML
    private TextField txtSujet;

    @FXML
    private TextArea txtDescription;

    private ReclamationService reclamationService;

    public void initialize() {
        reclamationService = new ReclamationService();
    }

    @FXML
    void submitReclamation(ActionEvent event) {
        String sujet = txtSujet.getText().trim();
        String description = txtDescription.getText().trim();

        if (sujet.isEmpty() || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de Saisie", "Veuillez remplir tous les champs obligatoires.");
            return;
        }

        if (!Session.isLoggedIn() || Session.getCurrentUser().getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur Session", "Vous devez être connecté pour soumettre une réclamation.");
            return;
        }

        try {
            // Convert UUID to byte[]
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
            bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
            bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
            byte[] userIdBytes = bb.array();

            Reclamation reclamation = new Reclamation(sujet, description, null, userIdBytes);
            reclamationService.ajouter(reclamation);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre réclamation a été soumise avec succès.");
            navigateToReclamationsList(event);

        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données", "Impossible d'enregistrer la réclamation: " + e.getMessage());
        }
    }

    @FXML
    void cancel(ActionEvent event) {
        navigateToReclamationsList(event);
    }

    private void navigateToReclamationsList(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/reclamation/UserReclamations.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
