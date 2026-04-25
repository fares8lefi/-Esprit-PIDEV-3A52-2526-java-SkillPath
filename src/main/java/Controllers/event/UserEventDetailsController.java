package Controllers.event;

import Models.Event;
import Models.EventRating;
import Models.UserFavouriteEvent;
import Services.EventRatingService;
import Services.EventService;
import Services.UserFavouriteEventService;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLDataException;
import java.util.UUID;

public class UserEventDetailsController {

    @FXML private Label lblTitle;
    @FXML private Label lblDescription;
    @FXML private Label lblDate;
    @FXML private Label lblTime;
    @FXML private Label lblLocation;
    @FXML private Label lblAverageRating;
    @FXML private ComboBox<Integer> cbMyRating;
    @FXML private Button btnFavourite;
    @FXML private Button btnClose;

    private Event event;
    private EventService eventService;
    private EventRatingService ratingService;
    private UserFavouriteEventService favService;

    // Hardcoded UUID for testing as requested: 0x019cb4228d417e87a229dbd3a7e7b872
    // If there is an active session, use it. Otherwise use the fallback.
    private UUID getCurrentUserId() {
        if (Session.getInstance().isLoggedIn()) {
            return Session.getInstance().getCurrentUser().getId();
        }
        // Fallback hex: 019cb4228d417e87a229dbd3a7e7b872
        // Convert hex to UUID
        String hex = "019cb4228d417e87a229dbd3a7e7b872";
        long high = Long.parseUnsignedLong(hex.substring(0, 16), 16);
        long low = Long.parseUnsignedLong(hex.substring(16, 32), 16);
        return new UUID(high, low);
    }

    @FXML
    public void initialize() {
        eventService = new EventService();
        ratingService = new EventRatingService();
        favService = new UserFavouriteEventService();

        cbMyRating.getItems().addAll(1, 2, 3, 4, 5);
    }

    public void setEvent(Event event) {
        this.event = event;
        lblTitle.setText(event.getTitle());
        lblDescription.setText(event.getDescription());
        lblDate.setText("Date: " + event.getEventDate().toString());
        lblTime.setText("Time: " + event.getStartTime() + " - " + event.getEndTime());
        
        if (event.getLocation() != null) {
            lblLocation.setText("Location: " + event.getLocation().toString());
        } else {
            lblLocation.setText("Location: TBD");
        }
        
        lblAverageRating.setText(String.format("Average Rating: %.1f", event.getAverageRating()));

        // Load user specific data
        UUID userId = getCurrentUserId();
        
        int myScore = ratingService.getUserRatingForEvent(event.getId(), userId);
        if (myScore > 0) {
            cbMyRating.getSelectionModel().select(Integer.valueOf(myScore));
        }

        updateFavouriteButton(favService.isFavourite(userId, event.getId()));
    }

    private void updateFavouriteButton(boolean isFav) {
        if (isFav) {
            btnFavourite.setText("❤️ Remove from Favourites");
            btnFavourite.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #b71c1c;");
        } else {
            btnFavourite.setText("🤍 Add to Favourites");
            btnFavourite.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333333;");
        }
    }

    @FXML
    void handleRate() {
        if (event != null && cbMyRating.getValue() != null) {
            UUID userId = getCurrentUserId();
            EventRating rating = new EventRating(0, cbMyRating.getValue(), event.getId(), userId);
            try {
                ratingService.ajouter(rating);
                eventService.updateAverageRating(event.getId());
                
                // Alert success
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Rating saved successfully!");
                alert.show();
            } catch (SQLDataException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleToggleFavourite() {
        if (event != null) {
            UUID userId = getCurrentUserId();
            boolean isFav = favService.isFavourite(userId, event.getId());
            
            try {
                if (isFav) {
                    favService.removeFavourite(userId, event.getId());
                    updateFavouriteButton(false);
                } else {
                    favService.addFavourite(new UserFavouriteEvent(userId, event.getId()));
                    updateFavouriteButton(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage());
                alert.show();
            }
        }
    }

    @FXML
    void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}
