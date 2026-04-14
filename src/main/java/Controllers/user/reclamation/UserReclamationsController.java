package Controllers.user.reclamation;

import Models.Reclamation;
import Services.ReclamationService;
import Utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;

public class UserReclamationsController {

    @FXML
    private TableView<Reclamation> reclamationTable;
    @FXML
    private TableColumn<Reclamation, Integer> colId;
    @FXML
    private TableColumn<Reclamation, String> colSujet;
    @FXML
    private TableColumn<Reclamation, String> colStatut;
    @FXML
    private TableColumn<Reclamation, Reclamation> colAction;

    private ReclamationService reclamationService;

    public void initialize() {
        reclamationService = new ReclamationService();
        setupTable();
        loadReclamations();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Action Column: Add a "Voir Details" button
        colAction.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colAction.setCellFactory(param -> new TableCell<Reclamation, Reclamation>() {
            private final Button btn = new Button("Détails");

            {
                btn.getStyleClass().add("btn-secondary");
                btn.setOnAction(event -> openDetails(getItem(), event));
            }

            @Override
            protected void updateItem(Reclamation reclamation, boolean empty) {
                super.updateItem(reclamation, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
    }

    private void loadReclamations() {
        if (!Session.isLoggedIn() || Session.getCurrentUser().getId() == null) {
            System.err.println("Aucun utilisateur connecté ou UUID manquant !");
            return;
        }

        try {
            // Get user bytes from Session User
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
            bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
            bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
            byte[] userIdBytes = bb.array();

            List<Reclamation> myReclamations = reclamationService.getReclamationsByUser(userIdBytes);
            ObservableList<Reclamation> observableList = FXCollections.observableArrayList(myReclamations);
            reclamationTable.setItems(observableList);
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openAddReclamation(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/reclamation/AddReclamation.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDetails(Reclamation reclamation, ActionEvent event) {
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
}
