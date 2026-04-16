package Controllers.reclamation;

import Models.Reclamation;
import Models.User;
import Services.ReclamationService;
import Utils.Session;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLDataException;
import java.util.List;

public class UserReclamationsController {

    @FXML private VBox reclamationCardsContainer;

    private final ReclamationService reclamationService = new ReclamationService();

    @FXML
    public void initialize() {
        loadUserReclamations();
    }

    private void loadUserReclamations() {
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(currentUser.getId().getMostSignificantBits());
            bb.putLong(currentUser.getId().getLeastSignificantBits());

            List<Reclamation> list = reclamationService.getReclamationsByUser(bb.array());
            renderReclamations(list);
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private void renderReclamations(List<Reclamation> list) {
        reclamationCardsContainer.getChildren().clear();
        
        if (list.isEmpty()) {
            Label placeholder = new Label("Vous n'avez soumis aucune réclamation.");
            placeholder.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            reclamationCardsContainer.getChildren().add(placeholder);
            return;
        }

        for (Reclamation r : list) {
            reclamationCardsContainer.getChildren().add(createReclamationCard(r));
        }
    }

    private VBox createReclamationCard(Reclamation r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("reclamation-card-client"); // CSS class
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + r.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b5cf6;");

        Label sujetLabel = new Label(r.getSujet());
        sujetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(r.getStatut());
        String statusStyle = r.getStatut().equalsIgnoreCase("Traitee") 
                ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 20;"
                : "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 20;";
        statusLabel.setStyle(statusStyle);

        header.getChildren().addAll(idLabel, sujetLabel, spacer, statusLabel);

        Label descLabel = new Label(r.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);
        descLabel.setStyle("-fx-text-fill: #94a3b8;");

        Button btnDetails = new Button("Voir Détails");
        btnDetails.getStyleClass().add("btn-primary");
        btnDetails.setStyle("-fx-cursor: hand;");
        btnDetails.setOnAction(e -> openDetails(r, (Stage) btnDetails.getScene().getWindow()));

        HBox footer = new HBox(btnDetails);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, descLabel, footer);
        return card;
    }

    private void openDetails(Reclamation r, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/reclamation/ReclamationDetails.fxml"));
            Parent root = loader.load();
            
            ReclamationDetailsController controller = loader.getController();
            controller.setReclamation(r);
            
            stage.setTitle("Détails Réclamation #" + r.getId());
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddReclamation(ActionEvent event) {
        navigateTo(event, "/FrontOffice/reclamation/AddReclamation.fxml", "Nouvelle Réclamation");
    }

    @FXML
    private void goBackHome(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "SkillPath - Accueil");
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
}
