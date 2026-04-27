package Controllers.reclamation;

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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import Utils.VoiceRecognitionService;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLDataException;
import java.util.UUID;

public class AddReclamationController {

    @FXML
    private TextField txtSujet;

    @FXML
    private ComboBox<String> comboVoiceLang;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Button btnSubmit;

    private ReclamationService reclamationService;
    private File selectedFile;
    private Reclamation reclamationToEdit;
    private String currentPieceJointePath;
    private static final Path UPLOAD_DIR = Path.of("uploads", "reclamations");

    public void initialize() {
        reclamationService = new ReclamationService();
        if (selectedFileLabel != null) {
            selectedFileLabel.setText("Aucun fichier choisi");
        }
        if (comboVoiceLang != null) {
            comboVoiceLang.setItems(FXCollections.observableArrayList("Français", "English", "Tounsi"));
            comboVoiceLang.getSelectionModel().select("Français");
        }
    }

    public void initForEdit(Reclamation reclamation) {
        if (reclamation == null) {
            return;
        }

        this.reclamationToEdit = reclamation;
        this.currentPieceJointePath = reclamation.getPieceJointe();
        this.selectedFile = null;

        txtSujet.setText(reclamation.getSujet());
        txtDescription.setText(reclamation.getDescription());

        if (btnSubmit != null) {
            btnSubmit.setText("Mettre a jour");
        }
        updateSelectedFileLabel(currentPieceJointePath);
    }

    @FXML
    void chooseAttachment(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une piece jointe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File chosen = fileChooser.showOpenDialog(stage);
        if (chosen != null) {
            selectedFile = chosen;
            if (selectedFileLabel != null) {
                selectedFileLabel.setText(chosen.getName());
            }
        }
    }

    @FXML
    void startVoiceRecognition(ActionEvent event) {
        String selected = comboVoiceLang.getValue();
        String langCode = "fr-FR";
        if ("English".equals(selected)) {
            langCode = "en-US";
        } else if ("Tounsi".equals(selected)) {
            langCode = "ar-TN"; 
        }

        VoiceRecognitionService.startRecognition(langCode, text -> {
            if (text != null && !text.isBlank()) {
                String currentText = txtDescription.getText();
                txtDescription.setText(currentText.isEmpty() ? text : currentText + " " + text);
            }
        });
    }

    @FXML
    void submitReclamation(ActionEvent event) {
        String sujet = txtSujet.getText().trim();
        String description = txtDescription.getText().trim();

        if (sujet.isEmpty() || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez remplir tous les champs obligatoires.");
            return;
        }

        if (!Session.isLoggedIn() || Session.getCurrentUser() == null || Session.getCurrentUser().getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur session", "Vous devez etre connecte pour soumettre une reclamation.");
            return;
        }

        try {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
            bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
            bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
            byte[] userIdBytes = bb.array();

            String pieceJointePath = currentPieceJointePath;
            if (selectedFile != null) {
                pieceJointePath = storeAttachment(selectedFile);
            }

            if (reclamationToEdit != null) {
                Reclamation updated = new Reclamation();
                updated.setId(reclamationToEdit.getId());
                updated.setSujet(sujet);
                updated.setDescription(description);
                updated.setStatut(resolveStatutForUpdate(reclamationToEdit.getStatut()));
                updated.setPieceJointe(pieceJointePath);
                updated.setUserIdBytes(userIdBytes);
                reclamationService.modifier(updated);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Votre reclamation a ete modifiee avec succes.");
            } else {
                Reclamation reclamation = new Reclamation(sujet, description, pieceJointePath, userIdBytes);
                reclamationService.ajouter(reclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Votre reclamation a ete soumise avec succes.");
            }

            navigateToReclamationsList(event);
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", "Impossible d'enregistrer la reclamation: " + e.getMessage());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur fichier", "Impossible d'enregistrer la piece jointe: " + e.getMessage());
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

    private String storeAttachment(File sourceFile) throws IOException {
        Files.createDirectories(UPLOAD_DIR);

        String originalName = sourceFile.getName();
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 ? originalName.substring(dot) : "";
        String storedFileName = UUID.randomUUID() + extension;

        Path destination = UPLOAD_DIR.resolve(storedFileName);
        Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toString();
    }

    private String resolveStatutForUpdate(String statut) {
        if (statut == null || statut.isBlank()) {
            return "En attente";
        }
        return statut;
    }

    private void updateSelectedFileLabel(String pieceJointePath) {
        if (selectedFileLabel == null) {
            return;
        }

        if (pieceJointePath == null || pieceJointePath.isBlank()) {
            selectedFileLabel.setText("Aucun fichier choisi");
            return;
        }

        try {
            selectedFileLabel.setText(Path.of(pieceJointePath).getFileName().toString());
        } catch (Exception e) {
            selectedFileLabel.setText(pieceJointePath);
        }
    }
}
