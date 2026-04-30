package Controllers.event;

import Models.Event;
import Models.UserJoinedEvent;
import Services.UserJoinedEventService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.UUID;

public class SeatSelectionController {

    @FXML private GridPane seatGrid;
    @FXML private Label lblEventTitle;

    private Event event;
    private UUID userId;
    private UserEventDetailsController parent;
    private UserJoinedEventService joinedService;

    public void initData(Event ev, UUID user, UserEventDetailsController parentCtrl) {
        this.event = ev;
        this.userId = user;
        this.parent = parentCtrl;
        this.joinedService = new UserJoinedEventService();

        lblEventTitle.setText("Sélectionnez votre place : " + ev.getTitle());
        buildSeatMap();
    }

    private void buildSeatMap() {
        seatGrid.getChildren().clear();
        seatGrid.setHgap(10);
        seatGrid.setVgap(10);
        seatGrid.setAlignment(Pos.CENTER);

        List<Integer> occupied = joinedService.getOccupiedSeats(event.getId());

        int cols = 10;
        int rows = 5; // 50 seats total as requested
        int seatNum = 1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button seat = new Button(String.valueOf(seatNum));
                seat.setPrefSize(45, 45);

                if (occupied.contains(seatNum)) {
                    // Seat is taken
                    seat.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-border-color: #7f1d1d; -fx-border-radius: 5; -fx-background-radius: 5;");
                    seat.setDisable(true);
                } else {
                    // Seat is available
                    seat.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #1d4ed8; -fx-border-radius: 5; -fx-background-radius: 5;");
                    final int selectedSeat = seatNum;
                    seat.setOnAction(e -> confirmSeatSelection(selectedSeat));
                }

                seatGrid.add(seat, c, r);
                seatNum++;
            }
        }
    }

    private void confirmSeatSelection(int seatNumber) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la réservation du siège N° " + seatNumber + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    joinedService.joinEvent(new UserJoinedEvent(userId, event.getId(), seatNumber));
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Siège réservé avec succès!");
                    success.show();
                    
                    if (parent != null) parent.setEvent(event); // refresh parent UI
                    
                    Stage stage = (Stage) seatGrid.getScene().getWindow();
                    stage.close();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Erreur de réservation: " + ex.getMessage());
                    err.show();
                }
            }
        });
    }
}
