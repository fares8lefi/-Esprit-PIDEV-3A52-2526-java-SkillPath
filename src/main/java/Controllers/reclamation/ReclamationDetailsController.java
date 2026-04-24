package Controllers.reclamation;

import Models.Reclamation;
import Models.Reponse;
import Services.ReclamationService;
import Services.ReponseService;
import Utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import Utils.VoiceRecognitionService;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLDataException;
import java.util.List;

public class ReclamationDetailsController {

    @FXML
    private Label lblSujet;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblStatut;
    @FXML
    private Label lblPieceJointe;
    @FXML
    private Button btnOpenPieceJointe;
    @FXML
    private VBox chatContainer;
    @FXML
    private TextField txtReponse;
    @FXML
    private ComboBox<String> comboVoiceLang;
    @FXML
    private ScrollPane chatScrollPane;

    private Reclamation currentReclamation;
    private ReponseService reponseService;
    private ReclamationService reclamationService;

    public void initialize() {
        reponseService = new ReponseService();
        reclamationService = new ReclamationService();
        if (comboVoiceLang != null) {
            comboVoiceLang.setItems(FXCollections.observableArrayList("Français", "English", "Tounsi"));
            comboVoiceLang.getSelectionModel().select("Français");
        }
    }

    public void initData(Reclamation reclamation) {
        this.currentReclamation = reclamation;
        lblSujet.setText(reclamation.getSujet());
        lblDescription.setText(reclamation.getDescription());
        lblStatut.setText(reclamation.getStatut());
        configurePieceJointe(reclamation.getPieceJointe());
        
        loadResponses();
    }

    private void loadResponses() {
        chatContainer.getChildren().clear();
        try {
            List<Reponse> reponses = reponseService.getReponsesByReclamation(currentReclamation.getId());
            for (Reponse rep : reponses) {
                addResponseToChat(rep);
            }
            scrollToBottom();
        } catch (SQLDataException e) {
            System.err.println("Erreur chargement reponses : " + e.getMessage());
        }
    }

    private void addResponseToChat(Reponse rep) {
        HBox messageBox = new HBox();
        messageBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        
        Label messageLabel = new Label(rep.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400); // Prevent bubble from being too wide

        // Determine if this response is from the current user or someone else (e.g. Admin)
        boolean isMine = false;
        if (Session.isLoggedIn() && rep.getUserIdBytes() != null) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(rep.getUserIdBytes());
            long high = bb.getLong();
            long low = bb.getLong();
            java.util.UUID responderUIID = new java.util.UUID(high, low);
            if (responderUIID.equals(Session.getCurrentUser().getId())) {
                isMine = true;
            }
        }

        if (isMine) {
            messageLabel.getStyleClass().add("chat-bubble-me");
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageLabel.getStyleClass().add("chat-bubble-other");
            messageBox.setAlignment(Pos.CENTER_LEFT);
            // Prefix admin labels if needed
            Label senderLabel = new Label("Support");
            senderLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
            VBox msgWrapper = new VBox(2, senderLabel, messageLabel);
            messageBox.getChildren().add(msgWrapper);
            chatContainer.getChildren().add(messageBox);
            return;
        }

        messageBox.getChildren().add(messageLabel);
        chatContainer.getChildren().add(messageBox);
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
                String currentText = txtReponse.getText();
                txtReponse.setText(currentText.isEmpty() ? text : currentText + " " + text);
            }
        });
    }

    @FXML
    void sendReponse(ActionEvent event) {
        String msg = txtReponse.getText().trim();
        if (msg.isEmpty()) return;

        if (!Session.isLoggedIn() || Session.getCurrentUser().getId() == null) {
            System.err.println("User not logged in!");
            return;
        }

        try {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
            bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
            bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
            byte[] userIdBytes = bb.array();

            Reponse reponse = new Reponse(msg, currentReclamation.getId(), userIdBytes);
            reponseService.ajouter(reponse);

            // Change status to "En attente" since user replied
            if (!"En attente".equalsIgnoreCase(currentReclamation.getStatut())) {
                reclamationService.updateStatut(currentReclamation.getId(), "En attente");
                currentReclamation.setStatut("En attente");
                Platform.runLater(() -> lblStatut.setText("En attente"));
            }

            txtReponse.clear();
            addResponseToChat(reponse);
            scrollToBottom();

        } catch (SQLDataException e) {
            System.err.println("Erreur envoi reponse : " + e.getMessage());
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/reclamation/UserReclamations.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openPieceJointe(ActionEvent event) {
        if (currentReclamation == null || currentReclamation.getPieceJointe() == null || currentReclamation.getPieceJointe().isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Piece jointe", "Aucune piece jointe disponible.");
            return;
        }

        try {
            Path attachmentPath = Path.of(currentReclamation.getPieceJointe());
            File file = attachmentPath.toFile();
            if (!file.exists()) {
                showAlert(Alert.AlertType.ERROR, "Piece jointe", "Le fichier n'existe plus: " + attachmentPath);
                return;
            }

            if (!Desktop.isDesktopSupported()) {
                showAlert(Alert.AlertType.ERROR, "Piece jointe", "Ouverture de fichier non supportee sur cette machine.");
                return;
            }

            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Piece jointe", "Impossible d'ouvrir la piece jointe: " + e.getMessage());
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void configurePieceJointe(String pieceJointePath) {
        if (pieceJointePath == null || pieceJointePath.isBlank()) {
            lblPieceJointe.setText("Aucune piece jointe");
            btnOpenPieceJointe.setDisable(true);
            btnOpenPieceJointe.setVisible(false);
            btnOpenPieceJointe.setManaged(false);
            return;
        }

        String fileName;
        try {
            fileName = Path.of(pieceJointePath).getFileName().toString();
        } catch (Exception e) {
            fileName = pieceJointePath;
        }

        lblPieceJointe.setText(fileName);
        btnOpenPieceJointe.setDisable(false);
        btnOpenPieceJointe.setVisible(true);
        btnOpenPieceJointe.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
