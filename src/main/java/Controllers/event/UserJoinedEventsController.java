package Controllers.event;

import Models.Event;
import Services.EventService;
import Services.UserJoinedEventService;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserJoinedEventsController {

    @FXML private FlowPane flowPaneEvents;

    private EventService eventService;
    private UserJoinedEventService joinedService;

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
        joinedService = new UserJoinedEventService();
        loadJoinedEvents();
    }

    private void loadJoinedEvents() {
        try {
            UUID userId = getCurrentUserId();
            List<Integer> joinedIds = joinedService.getJoinedEventIds(userId);
            List<Event> allEvents = eventService.recuperer();
            
            List<Event> myTickets = allEvents.stream()
                    .filter(e -> joinedIds.contains(e.getId()))
                    .collect(Collectors.toList());

            flowPaneEvents.getChildren().clear();

            if (myTickets.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setStyle("-fx-alignment: center; -fx-padding: 60;");
                Label icon = new Label("🎟️");
                icon.setStyle("-fx-font-size: 48;");
                Label msg = new Label("Vous n'êtes inscrit à aucun événement.");
                msg.setStyle("-fx-font-style: italic; -fx-text-fill: #94a3b8; -fx-font-size: 16;");
                empty.getChildren().addAll(icon, msg);
                flowPaneEvents.getChildren().add(empty);
                return;
            }

            for (Event event : myTickets) {
                flowPaneEvents.getChildren().add(createTicketCard(event));
            }

        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    private VBox createTicketCard(Event event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 12; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12; -fx-background-color: rgba(16, 185, 129, 0.05);");
        card.setPrefWidth(220);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(180);
        imgView.setFitHeight(120);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(180, 120);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgView.setClip(clip);
        
        if (event.getImage() != null && !event.getImage().isEmpty()) {
            try {
                imgView.setImage(new Image(event.getImage(), true));
            } catch (Exception e) {}
        }

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        title.setWrapText(true);

        Label date = new Label("📅 " + event.getEventDate().toString());
        date.setStyle("-fx-text-fill: #94a3b8;");
        
        Label badge = new Label("✅ Inscrit");
        badge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; -fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11; -fx-font-weight: bold;");

        Button btnDetails = new Button("Voir Détails");
        btnDetails.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openDetails(event));

        Button btnLeave = new Button("Se désinscrire");
        btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.12); -fx-text-fill: #f87171; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: rgba(248,113,113,0.3); -fx-border-radius: 8;");
        btnLeave.setMaxWidth(Double.MAX_VALUE);
        btnLeave.setOnMouseEntered(e -> btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.25); -fx-text-fill: #f87171; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: rgba(248,113,113,0.5); -fx-border-radius: 8;"));
        btnLeave.setOnMouseExited(e -> btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.12); -fx-text-fill: #f87171; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: rgba(248,113,113,0.3); -fx-border-radius: 8;"));
        btnLeave.setOnAction(e -> confirmLeave(event));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgView, badge, title, date, spacer, btnDetails, btnLeave);
        return card;
    }

    private void confirmLeave(Event event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; -fx-border-color: rgba(248,113,113,0.4); -fx-border-width: 2; -fx-background-radius: 20; -fx-border-radius: 20;");
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPrefWidth(380);

        Label icon = new Label("🎟️");
        icon.setStyle("-fx-font-size: 44;");

        Label titleLbl = new Label("SE DÉSINSCRIRE ?");
        titleLbl.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 900; -fx-font-size: 16; -fx-letter-spacing: 1;");

        Label msg = new Label("Voulez-vous annuler votre inscription à\n« " + event.getTitle() + " » ?");
        msg.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14; -fx-text-alignment: center;");
        msg.setWrapText(true);

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(15);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button btnCancel = new Button("ANNULER");
        btnCancel.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #94a3b8; -fx-font-weight: 900; -fx-padding: 12 28; -fx-background-radius: 10; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnConfirm = new Button("OUI, ME DÉSINSCRIRE");
        btnConfirm.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 12 20; -fx-background-radius: 10; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            try {
                joinedService.leaveEvent(getCurrentUserId(), event.getId());
                dialog.close();
                loadJoinedEvents(); 
            } catch (SQLDataException ex) {
                ex.printStackTrace();
                dialog.close();
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(icon, titleLbl, msg, actions);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
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
            
            loadJoinedEvents(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
