package Controllers.event;

import Models.Event;
import Services.EventService;
import Services.UserFavouriteEventService;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserFavouriteEventsController {

    @FXML private FlowPane flowPaneEvents;

    private EventService eventService;
    private UserFavouriteEventService favService;

    private UUID getCurrentUserId() {
        if (Session.getInstance().isLoggedIn()) {
            return Session.getInstance().getCurrentUser().getId();
        }
        String hex = "019cb4228d417e87a229dbd3a7e7b872";
        long high = Long.parseUnsignedLong(hex.substring(0, 16), 16);
        long low = Long.parseUnsignedLong(hex.substring(16, 32), 16);
        return new UUID(high, low);
    }

    @FXML
    public void initialize() {
        eventService = new EventService();
        favService = new UserFavouriteEventService();
        loadFavourites();
    }

    private void loadFavourites() {
        try {
            UUID userId = getCurrentUserId();
            List<Integer> favIds = favService.getFavouriteEventIds(userId);
            List<Event> allEvents = eventService.recuperer();
            
            List<Event> myFavs = allEvents.stream()
                    .filter(e -> favIds.contains(e.getId()))
                    .collect(Collectors.toList());

            flowPaneEvents.getChildren().clear();

            if (myFavs.isEmpty()) {
                Label noFav = new Label("You haven't added any favourite events yet.");
                noFav.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                flowPaneEvents.getChildren().add(noFav);
                return;
            }

            for (Event event : myFavs) {
                flowPaneEvents.getChildren().add(createEventCard(event));
            }

        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private VBox createEventCard(Event event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 12; -fx-border-color: rgba(239, 68, 68, 0.3); -fx-border-radius: 12; -fx-background-color: rgba(239, 68, 68, 0.05);");
        card.setPrefWidth(220);

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        title.setWrapText(true);

        Label date = new Label("📅 " + event.getEventDate().toString());
        date.setStyle("-fx-text-fill: #94a3b8;");
        
        Button btnDetails = new Button("Voir Détails");
        btnDetails.getStyleClass().add("btn-primary");
        btnDetails.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openDetails(event));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(title, date, spacer, btnDetails);
        return card;
    }

    private void openDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/event/EventDetails.fxml"));
            Parent root = loader.load();
            
            UserEventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Event Details");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Reload favourites in case user unfavourited
            loadFavourites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
