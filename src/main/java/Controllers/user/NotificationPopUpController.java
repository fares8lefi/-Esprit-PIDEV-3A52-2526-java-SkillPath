package Controllers.user;

import Models.Notification;
import Services.NotificationService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationPopUpController {

    @FXML private VBox notifContainer;
    private final NotificationService notificationService = new NotificationService();

    @FXML
    public void initialize() {
        loadNotifications();
    }

    private void loadNotifications() {
        String userId = Session.getInstance().getCurrentUser().getId().toString();
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);

        notifContainer.getChildren().clear();
        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("Aucune nouvelle notification.");
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            notifContainer.getChildren().add(emptyLabel);
        } else {
            for (Notification n : notifications) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/home/notifItem.fxml"));
                    VBox item = loader.load();
                    
                    Label lblTitle = (Label) item.lookup("#lblTitle");
                    Label lblMessage = (Label) item.lookup("#lblMessage");
                    Label lblDate = (Label) item.lookup("#lblDate");
                    
                    lblTitle.setText(n.getTitle());
                    lblMessage.setText(n.getMessage());
                    lblDate.setText(n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                    
                    notifContainer.getChildren().add(item);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void markAllAsRead(ActionEvent event) {
        String userId = Session.getInstance().getCurrentUser().getId().toString();
        notificationService.markAllAsRead(userId);
        loadNotifications();
        // Optionnel: On pourrait fermer la fenêtre aussi
    }

    @FXML
    private void closePopUp(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
