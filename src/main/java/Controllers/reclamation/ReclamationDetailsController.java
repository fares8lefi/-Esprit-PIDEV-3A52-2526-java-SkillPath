package Controllers.reclamation;

import Models.Reclamation;
import Models.Reponse;
import Models.User;
import Services.ReponseService;
import Services.ReclamationService;
import Utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLDataException;
import java.util.Arrays;
import java.util.List;

public class ReclamationDetailsController {

    @FXML private Label lblSujet;
    @FXML private Label lblDescription;
    @FXML private Label lblStatut;
    @FXML private Label lblPieceJointe;
    @FXML private Button btnOpenPieceJointe;
    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField txtReponse;

    private final ReponseService reponseService = new ReponseService();
    private Reclamation reclamation;

    public void setReclamation(Reclamation r) {
        this.reclamation = r;
        lblSujet.setText(r.getSujet());
        lblDescription.setText(r.getDescription());
        lblStatut.setText(r.getStatut());
        
        // Style du statut
        if (r.getStatut().equalsIgnoreCase("Traitee")) {
            lblStatut.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 20;");
        } else {
            lblStatut.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 20;");
        }

        if (r.getPieceJointe() != null && !r.getPieceJointe().isEmpty()) {
            lblPieceJointe.setText(new File(r.getPieceJointe()).getName());
            btnOpenPieceJointe.setVisible(true);
            btnOpenPieceJointe.setManaged(true);
        }

        loadReponses();
    }

    private void loadReponses() {
        if (reclamation == null) return;
        
        try {
            List<Reponse> list = reponseService.getReponsesByReclamation(reclamation.getId());
            chatContainer.getChildren().clear();
            
            for (Reponse resp : list) {
                chatContainer.getChildren().add(createResponseBubble(resp));
            }
            
            // Scroll to bottom
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private VBox createResponseBubble(Reponse resp) {
        boolean isMine = false;
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null && resp.getUserIdBytes() != null) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(currentUser.getId().getMostSignificantBits());
            bb.putLong(currentUser.getId().getLeastSignificantBits());
            isMine = Arrays.equals(bb.array(), resp.getUserIdBytes());
        }

        VBox bubble = new VBox(5);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        
        Label author = new Label(isMine ? "Moi" : "Support SkillPath");
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 10; -fx-text-fill: #94a3b8;");

        Label msg = new Label(resp.getMessage());
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: white;");

        bubble.getChildren().addAll(author, msg);
        
        if (isMine) {
            bubble.setStyle("-fx-background-color: #8b5cf6; -fx-background-radius: 15 15 0 15;");
            VBox.setMargin(bubble, new Insets(0, 0, 0, 150));
            bubble.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15 15 15 0;");
            VBox.setMargin(bubble, new Insets(0, 150, 0, 0));
            bubble.setAlignment(Pos.CENTER_LEFT);
        }
        
        return bubble;
    }

    @FXML
    private void sendReponse(ActionEvent event) {
        String message = txtReponse.getText().trim();
        if (message.isEmpty() || reclamation == null) return;

        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(currentUser.getId().getMostSignificantBits());
            bb.putLong(currentUser.getId().getLeastSignificantBits());

            Reponse reponse = new Reponse(message, reclamation.getId(), bb.array());
            reponseService.ajouter(reponse);
            
            txtReponse.clear();
            loadReponses();
            
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openPieceJointe(ActionEvent event) {
        if (reclamation != null && reclamation.getPieceJointe() != null) {
            try {
                Desktop.getDesktop().open(new File(reclamation.getPieceJointe()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/reclamation/UserReclamations.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Mes Réclamations");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
