package Controllers.event;

import Models.Event;
import Services.EventService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.stream.Collectors;

public class UserEventListController {

    @FXML private TextField tfSearchName;
    @FXML private ComboBox<String> cbMinRating;
    @FXML private FlowPane flowPaneEvents;
    @FXML private Button btnFavourites;

    private EventService eventService;
    private List<Event> allEvents;

    @FXML
    public void initialize() {
        eventService = new EventService();
        cbMinRating.getItems().addAll("All", "1+ Stars", "2+ Stars", "3+ Stars", "4+ Stars", "5 Stars");
        cbMinRating.getSelectionModel().selectFirst();
        
        loadEvents();
        
        // Listeners for live filtering
        tfSearchName.textProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        cbMinRating.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
    }

    private void loadEvents() {
        try {
            allEvents = eventService.recuperer();
            filterAndDisplay();
        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private void filterAndDisplay() {
        String search = tfSearchName.getText().toLowerCase();
        int minRating = 0;
        String selectedRating = cbMinRating.getValue();
        if (selectedRating != null && !selectedRating.equals("All")) {
            minRating = Integer.parseInt(selectedRating.substring(0, 1));
        }

        final int finalMinRating = minRating;
        List<Event> filtered = allEvents.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(search))
                .filter(e -> e.getAverageRating() >= finalMinRating)
                .collect(Collectors.toList());

        flowPaneEvents.getChildren().clear();

        for (Event event : filtered) {
            flowPaneEvents.getChildren().add(createEventCard(event));
        }
    }

    private VBox createEventCard(Event event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        card.setPrefWidth(220);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(180);
        imgView.setFitHeight(120);
        imgView.setPreserveRatio(false); // Stretch to fill
        Rectangle clip = new Rectangle(180, 120);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgView.setClip(clip);
        
        if (event.getImage() != null && !event.getImage().trim().isEmpty()) {
            try {
                imgView.setImage(new Image(event.getImage(), true));
            } catch (Exception e) {
                System.err.println("Could not load image: " + event.getImage());
            }
        }

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        title.setWrapText(true);

        Label date = new Label("📅 " + event.getEventDate().toString());
        date.setStyle("-fx-text-fill: #94a3b8;");
        
        Label rating = new Label(String.format("%.1f ★ Rating", event.getAverageRating()));
        rating.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 11px; -fx-background-color: rgba(252, 211, 77, 0.1); -fx-padding: 3 8; -fx-background-radius: 5;");
        
        Button btnDetails = new Button("Voir Détails");
        btnDetails.getStyleClass().add("btn-primary");
        btnDetails.setStyle("-fx-font-size: 12px; -fx-padding: 8 15;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openDetails(event));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgView, rating, title, date, spacer, btnDetails);
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
            
            // Reload on close (in case rating changed)
            loadEvents();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleShowFavourites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/event/FavouriteEvents.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("My Favourite Events");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleShowJoined() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/event/JoinedEvents.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("My Tickets");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/user/home/homeUser.fxml"));
            Stage stage = (Stage) tfSearchName.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection accueil : " + e.getMessage());
        }
    }

    @FXML
    public void goToCourses() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/course/courseList.fxml"));
            Stage stage = (Stage) tfSearchName.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection cours : " + e.getMessage());
        }
    }
}
