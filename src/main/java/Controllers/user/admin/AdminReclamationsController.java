package Controllers.user.admin;

import Models.Reclamation;
import Models.Reponse;
import Models.User;
import Services.ReclamationService;
import Services.ReponseService;
import Utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.SQLDataException;
import java.util.List;

public class AdminReclamationsController {

    @FXML
    private TextField searchUsernameField;
    @FXML
    private ComboBox<String> sortStatusCombo;
    @FXML
    private VBox reclamationCardsContainer;
    @FXML
    private Label selectedReclamationLabel;
    @FXML
    private Label selectedDescriptionLabel;
    @FXML
    private Label selectedPieceJointeLabel;
    @FXML
    private TextArea adminResponseArea;
    @FXML
    private Button replyButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button openAttachmentButton;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ReponseService reponseService = new ReponseService();
    private Reclamation selectedReclamation;

    @FXML
    public void initialize() {
        sortStatusCombo.setItems(FXCollections.observableArrayList("En attente", "Traitee"));
        sortStatusCombo.getSelectionModel().select("En attente");
        clearSelectionDetails();
        loadReclamations();
    }

    @FXML
    private void handleSearch() {
        loadReclamations();
    }

    @FXML
    private void handleResetSearch() {
        searchUsernameField.clear();
        sortStatusCombo.getSelectionModel().select("En attente");
        loadReclamations();
    }

    @FXML
    private void handleSortChange() {
        loadReclamations();
    }

    @FXML
    private void handleReply() {
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "Reclamation", "Selectionnez une reclamation a traiter.");
            return;
        }

        String message = adminResponseArea.getText().trim();
        if (message.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Reponse", "Le message de reponse est obligatoire.");
            return;
        }

        User admin = Session.getCurrentUser();
        if (admin == null || admin.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Session", "Session admin invalide.");
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(admin.getId().getMostSignificantBits());
            bb.putLong(admin.getId().getLeastSignificantBits());

            Reponse reponse = new Reponse(message, selectedReclamation.getId(), bb.array());
            reponseService.ajouter(reponse);
            reclamationService.updateStatut(selectedReclamation.getId(), "Traitee");

            adminResponseArea.clear();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Reponse envoyee et statut mis a jour.");
            loadReclamations();
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer la reponse: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "Reclamation", "Selectionnez une reclamation a supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la reclamation #" + selectedReclamation.getId());
        confirm.setContentText("Cette action supprimera aussi les reponses associees.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                try {
                    reclamationService.deleteWithResponses(selectedReclamation.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succes", "Reclamation supprimee.");
                    loadReclamations();
                } catch (SQLDataException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleOpenAttachment() {
        if (selectedReclamation == null || selectedReclamation.getPieceJointe() == null || selectedReclamation.getPieceJointe().isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Piece jointe", "Aucune piece jointe disponible.");
            return;
        }

        try {
            Path attachmentPath = Path.of(selectedReclamation.getPieceJointe());
            File file = attachmentPath.toFile();
            if (!file.exists()) {
                showAlert(Alert.AlertType.ERROR, "Piece jointe", "Fichier introuvable: " + attachmentPath);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                showAlert(Alert.AlertType.ERROR, "Piece jointe", "Ouverture de fichier non supportee.");
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Piece jointe", "Impossible d'ouvrir le fichier: " + e.getMessage());
        }
    }

    private void loadReclamations() {
        String search = searchUsernameField.getText();
        String statusFilter = sortStatusCombo.getValue();

        try {
            List<Reclamation> reclamations = reclamationService.getAllReclamationsWithUsername(search, statusFilter);
            renderCards(reclamations);
            clearSelectionDetails();
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Chargement des reclamations impossible: " + e.getMessage());
        }
    }

    private void renderCards(List<Reclamation> reclamations) {
        reclamationCardsContainer.getChildren().clear();

        if (reclamations == null || reclamations.isEmpty()) {
            Label empty = new Label("Aucune reclamation trouvee.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
            reclamationCardsContainer.getChildren().add(empty);
            return;
        }

        for (Reclamation reclamation : reclamations) {
            VBox card = buildCard(reclamation);
            reclamationCardsContainer.getChildren().add(card);
        }
    }

    private VBox buildCard(Reclamation reclamation) {
        Label idLabel = new Label("#" + reclamation.getId());
        idLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: 900;");

        Label userLabel = new Label(safe(reclamation.getUsername()));
        userLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 700;");

        Label sujetLabel = new Label(safe(reclamation.getSujet()));
        sujetLabel.setWrapText(true);
        sujetLabel.setStyle("-fx-text-fill: #e2e8f0;");

        Label statutLabel = new Label(safe(reclamation.getStatut()));
        statutLabel.setStyle("-fx-background-color: rgba(139,92,246,0.2); -fx-text-fill: #c4b5fd; -fx-padding: 4 10; -fx-background-radius: 10;");

        Button selectBtn = new Button("Selectionner");
        selectBtn.getStyleClass().add("btn-secondary");
        selectBtn.setOnAction(e -> setSelectedReclamation(reclamation));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(10, idLabel, userLabel, spacer, statutLabel, selectBtn);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(8, topRow, sujetLabel);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;");
        return card;
    }

    private void setSelectedReclamation(Reclamation reclamation) {
        selectedReclamation = reclamation;
        if (reclamation == null) {
            clearSelectionDetails();
            return;
        }

        selectedReclamationLabel.setText("#" + reclamation.getId() + " - " + safe(reclamation.getUsername()) + " - " + safe(reclamation.getSujet()));
        selectedDescriptionLabel.setText(safe(reclamation.getDescription()));

        String pieceJointe = reclamation.getPieceJointe();
        if (pieceJointe == null || pieceJointe.isBlank()) {
            selectedPieceJointeLabel.setText("Aucune");
            openAttachmentButton.setDisable(true);
        } else {
            try {
                selectedPieceJointeLabel.setText(Path.of(pieceJointe).getFileName().toString());
            } catch (Exception e) {
                selectedPieceJointeLabel.setText(pieceJointe);
            }
            openAttachmentButton.setDisable(false);
        }

        replyButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    private void clearSelectionDetails() {
        selectedReclamation = null;
        selectedReclamationLabel.setText("Aucune reclamation selectionnee");
        selectedDescriptionLabel.setText("-");
        selectedPieceJointeLabel.setText("-");
        adminResponseArea.clear();
        replyButton.setDisable(true);
        deleteButton.setDisable(true);
        openAttachmentButton.setDisable(true);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
