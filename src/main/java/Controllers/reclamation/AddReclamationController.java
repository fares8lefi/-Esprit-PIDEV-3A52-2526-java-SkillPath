package Controllers.reclamation;

import Models.Reclamation;
import Models.User;
import Services.ReclamationService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLDataException;
import java.util.UUID;

public class AddReclamationController {

    @FXML private TextField txtSujet;
    @FXML private TextArea txtDescription;
    @FXML private Label selectedFileLabel;

    private final ReclamationService reclamationService = new ReclamationService();
    private File selectedFile;

    @FXML
    private void chooseAttachment(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une pièce jointe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf")
        );
        selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
        }
    }

    @FXML
    private void submitReclamation(ActionEvent event) {
        String sujet = txtSujet.getText().trim();
        String description = txtDescription.getText().trim();

        if (sujet.isEmpty() || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Le sujet et la description sont obligatoires.");
            return;
        }

        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté pour soumettre une réclamation.");
            return;
        }

        try {
            String pieceJointePath = null;
            if (selectedFile != null) {
                pieceJointePath = saveFile(selectedFile);
            }

            // Conversion UUID en bytes pour la compatibilité base de données
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(currentUser.getId().getMostSignificantBits());
            bb.putLong(currentUser.getId().getLeastSignificantBits());

            Reclamation reclamation = new Reclamation();
            reclamation.setSujet(sujet);
            reclamation.setDescription(description);
            reclamation.setStatut("En attente");
            reclamation.setPieceJointe(pieceJointePath);
            reclamation.setUserIdBytes(bb.array());

            reclamationService.ajouter(reclamation);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre réclamation a été soumise avec succès.");
            cancel(event);

        } catch (SQLDataException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue : " + e.getMessage());
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
        navigateTo(event, "/FrontOffice/reclamation/UserReclamations.fxml", "Mes Réclamations");
    }

    private String saveFile(File file) throws IOException {
        Path uploadDir = Paths.get("uploads/reclamations");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getName();
        Path targetPath = uploadDir.resolve(fileName);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toString();
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
