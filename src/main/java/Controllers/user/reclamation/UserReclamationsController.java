package Controllers.user.reclamation;

import Models.Reclamation;
import Services.ReclamationService;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import java.sql.SQLDataException;
import java.util.List;

public class UserReclamationsController {

    @FXML
    private VBox reclamationCardsContainer;

    private ReclamationService reclamationService;

    public void initialize() {
        reclamationService = new ReclamationService();
        loadReclamations();
    }

    private void loadReclamations() {
        if (!Session.isLoggedIn() || Session.getCurrentUser() == null || Session.getCurrentUser().getId() == null) {
            return;
        }

        try {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
            bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
            bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
            byte[] userIdBytes = bb.array();

            List<Reclamation> myReclamations = reclamationService.getReclamationsByUser(userIdBytes);
            renderCards(myReclamations);
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private void renderCards(List<Reclamation> reclamations) {
        reclamationCardsContainer.getChildren().clear();

        if (reclamations == null || reclamations.isEmpty()) {
            Label empty = new Label("Aucune reclamation pour le moment.");
            empty.getStyleClass().add("subtitle");
            reclamationCardsContainer.getChildren().add(empty);
            return;
        }

        for (Reclamation reclamation : reclamations) {
            reclamationCardsContainer.getChildren().add(buildCard(reclamation));
        }
    }

    private VBox buildCard(Reclamation reclamation) {
        Label idLabel = new Label("#" + reclamation.getId());
        idLabel.getStyleClass().add("reclamation-id");

        Label sujetLabel = new Label(reclamation.getSujet() == null ? "" : reclamation.getSujet());
        sujetLabel.getStyleClass().add("reclamation-subject");
        sujetLabel.setWrapText(true);

        Label statutLabel = new Label(reclamation.getStatut() == null ? "En attente" : reclamation.getStatut());
        statutLabel.getStyleClass().add("status-pill");
        if (statutLabel.getText().toLowerCase().contains("traitee")) {
            statutLabel.getStyleClass().add("status-traitee");
        } else {
            statutLabel.getStyleClass().add("status-attente");
        }

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-secondary");
        detailsBtn.setOnAction(event -> openDetails(reclamation, event));

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(event -> openEditReclamation(reclamation, event));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(10, idLabel, statutLabel, spacer, detailsBtn, editBtn);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(10, top, sujetLabel);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("reclamation-card");
        return card;
    }

    @FXML
    void openAddReclamation(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/reclamation/AddReclamation.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBackHome(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/user/home/homeUser.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDetails(Reclamation reclamation, javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/reclamation/ReclamationDetails.fxml"));
            Parent root = loader.load();

            ReclamationDetailsController controller = loader.getController();
            controller.initData(reclamation);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditReclamation(Reclamation reclamation, javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/reclamation/AddReclamation.fxml"));
            Parent root = loader.load();

            AddReclamationController controller = loader.getController();
            controller.initForEdit(reclamation);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
