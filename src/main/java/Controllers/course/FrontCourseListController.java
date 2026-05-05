package Controllers.course;

import Models.Course;
import Services.CourseService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class FrontCourseListController implements Initializable {

    @FXML private VBox courseContainer;
    @FXML private TextField txtSearch;
    @FXML private VBox categoryFilterContainer;
    @FXML private VBox levelFilterContainer;
    @FXML private Label lblResultCount;
    @FXML private VBox vboxNoResults;
    @FXML private Button btnClearSearch;
    @FXML private Button btnNewest;
    @FXML private Button btnPopular;
    @FXML private Button btnAZ;
    @FXML private Button btnZA;
    @FXML private VBox chatWindow;
    @FXML private VBox chatMessages;
    @FXML private TextField txtChatInput;
    @FXML private StackPane notifBadge;
    @FXML private Label lblNotifCount;

    private final CourseService courseService = new CourseService();
    private final Services.NotificationService notificationService = new Services.NotificationService();
    private final Services.ChatbotService chatbotService = new Services.ChatbotService();
    
    private List<Course> masterList = new ArrayList<>();
    private String selectedCategory = "";
    private String selectedLevel = "";
    private String currentSort = "NEWEST"; // Default sort

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Use runLater to ensure scene is fully loaded before performing potentially heavy data fetch
        Platform.runLater(() -> {
            loadData();
            setupFilters();
            
            // Initialize notifications
            Utils.Session session = Utils.Session.getInstance();
            if (session.isLoggedIn()) {
                Services.NotificationService notificationService = new Services.NotificationService();
                int count = notificationService.getUnreadCount(session.getCurrentUser().getId().toString());
                if (count > 0) {
                    lblNotifCount.setText(String.valueOf(count));
                    notifBadge.setVisible(true);
                }
            }
        });
        
        // Listeners for real-time search
        txtSearch.textProperty().addListener((obs, old, newVal) -> {
            btnClearSearch.setVisible(!newVal.isEmpty());
            applyFilters();
        });
    }

    private void loadData() {
        try {
            masterList = courseService.recuperer();
            if (masterList == null) masterList = new ArrayList<>();
            System.out.println("Data loaded: " + masterList.size() + " courses.");
            applyFilters();
        } catch (Exception e) {
            System.err.println("Erreur chargement données : " + e.getMessage());
        }
    }

    private void setupFilters() {
        // Categories
        Set<String> categories = masterList.stream()
                .map(Course::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());

        categoryFilterContainer.getChildren().clear();
        ToggleGroup catGroup = new ToggleGroup();
        
        RadioButton allCat = createFilterRadio("Toutes les catégories", "", catGroup, true, true);
        categoryFilterContainer.getChildren().add(allCat);

        for (String cat : categories) {
            categoryFilterContainer.getChildren().add(createFilterRadio(cat, cat, catGroup, true));
        }

        // Levels
        levelFilterContainer.getChildren().clear();
        ToggleGroup lvlGroup = new ToggleGroup();
        levelFilterContainer.getChildren().addAll(
            createFilterRadio("Tous les niveaux", "", lvlGroup, true, false),
            createFilterRadio("Débutant", "Débutant", lvlGroup, false),
            createFilterRadio("Intermédiaire", "Intermédiaire", lvlGroup, false),
            createFilterRadio("Avancé", "Avancé", lvlGroup, false)
        );
    }

    private RadioButton createFilterRadio(String label, String value, ToggleGroup group, boolean isCategory) {
        return createFilterRadio(label, value, group, false, isCategory);
    }

    private RadioButton createFilterRadio(String label, String value, ToggleGroup group, boolean selected, boolean isCategory) {
        RadioButton rb = new RadioButton(label);
        rb.setToggleGroup(group);
        rb.setSelected(selected);
        rb.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-cursor: hand;");
        rb.setPadding(new Insets(5, 0, 5, 0));
        
        rb.setOnAction(e -> {
            if (isCategory) {
                selectedCategory = value;
            } else {
                selectedLevel = value;
            }
            System.out.println("Filter Change: Cat=" + selectedCategory + ", Lvl=" + selectedLevel);
            applyFilters();
        });
        
        return rb;
    }

    private void applyFilters() {
        String search = txtSearch.getText().toLowerCase().trim();
        
        List<Course> filtered = masterList.stream()
                .filter(c -> {
                    if (search.isEmpty()) return true;
                    String t = (c.getTitle() != null) ? c.getTitle().toLowerCase() : "";
                    String d = (c.getDescription() != null) ? c.getDescription().toLowerCase() : "";
                    String cat = (c.getCategory() != null) ? c.getCategory().toLowerCase() : "";
                    return t.contains(search) || d.contains(search) || cat.contains(search);
                })
                .filter(c -> selectedCategory.isEmpty() || (c.getCategory() != null && selectedCategory.equalsIgnoreCase(c.getCategory())))
                .filter(c -> selectedLevel.isEmpty() || (c.getLevel() != null && selectedLevel.equalsIgnoreCase(c.getLevel())))
                .collect(Collectors.toList());

        // Apply Sorting
        switch (currentSort) {
            case "AZ" -> filtered.sort(Comparator.comparing(c -> (c.getTitle() != null ? c.getTitle() : "")));
            case "ZA" -> filtered.sort((c1, c2) -> (c2.getTitle() != null ? c2.getTitle() : "").compareTo(c1.getTitle() != null ? c1.getTitle() : ""));
            case "NEWEST" -> filtered.sort(Comparator.comparingInt(Course::getId).reversed());
            case "POPULAR" -> filtered.sort(Comparator.comparingDouble(Course::getPrice).reversed()); // Simulated popularity by price/value
        }

        // UI State Management
        vboxNoResults.setVisible(filtered.isEmpty());
        courseContainer.setVisible(!filtered.isEmpty());
        lblResultCount.setText(filtered.size() + " formations trouvées");
        
        displayCourses(filtered);
    }

    @FXML
    private void handleRecommendations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/recommendations.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleSortAZ() { setSort("AZ"); }
    @FXML private void handleSortZA() { setSort("ZA"); }
    @FXML private void handleSortNewest() { setSort("NEWEST"); }
    @FXML private void handleSortPopular() { setSort("POPULAR"); }

    private void setSort(String type) {
        this.currentSort = type;
        
        // Reset styles for all sort buttons
        btnAZ.getStyleClass().remove("btn-sort-active");
        btnZA.getStyleClass().remove("btn-sort-active");
        btnNewest.getStyleClass().remove("btn-sort-active");
        btnPopular.getStyleClass().remove("btn-sort-active");

        // Add active style to selected button
        switch(type) {
            case "AZ" -> btnAZ.getStyleClass().add("btn-sort-active");
            case "ZA" -> btnZA.getStyleClass().add("btn-sort-active");
            case "NEWEST" -> btnNewest.getStyleClass().add("btn-sort-active");
            case "POPULAR" -> btnPopular.getStyleClass().add("btn-sort-active");
        }
        
        applyFilters();
    }

    @FXML
    private void clearSearch() {
        txtSearch.clear();
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        txtSearch.clear();
        selectedCategory = "";
        selectedLevel = "";
        currentSort = "NEWEST";
        
        // Refresh filter UI
        setupFilters();
        setSort("NEWEST");
    }

    private void displayCourses(List<Course> courses) {
        courseContainer.getChildren().clear();
        int delay = 0;
        
        for (Course course : courses) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/courseCard.fxml"));
                Node card = loader.load();
                
                FrontCourseCardController controller = loader.getController();
                controller.setData(course);
                
                // Add to container first (ensures layout calculation)
                courseContainer.getChildren().add(card);
                
                // Professional Entrance Animation
                card.setOpacity(0.0);
                
                final Node cardNode = card;
                final int staggerDelay = delay;
                
                Platform.runLater(() -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(800), cardNode);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.setDelay(Duration.millis(staggerDelay));
                    
                    TranslateTransition tt = new TranslateTransition(Duration.millis(800), cardNode);
                    tt.setFromY(20);
                    tt.setToY(0);
                    tt.setDelay(Duration.millis(staggerDelay));
                    
                    ft.play();
                    tt.play();
                });
                
                delay += 100;
            } catch (IOException e) {
                System.err.println("Erreur chargement carte cours : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/home/homeUser.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblResultCount.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection accueil : " + e.getMessage());
        }
    }

    @FXML
    private void goToCourses() {
        loadData(); // Just refresh the data
    }

    @FXML
    private void goToEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/event/EventList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblResultCount.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection events : " + e.getMessage());
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Clic sur Déconnexion...");
        Session.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/auth/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Connexion - SkillPath");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection login : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showNotifications(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/home/notifPopUp.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Mes Notifications");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh badge
            Services.NotificationService notificationService = new Services.NotificationService();
            int count = notificationService.getUnreadCount(Session.getInstance().getCurrentUser().getId().toString());
            if (count <= 0) {
                notifBadge.setVisible(false);
            } else {
                lblNotifCount.setText(String.valueOf(count));
                notifBadge.setVisible(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleChat(ActionEvent event) {
        chatWindow.setVisible(!chatWindow.isVisible());
    }

    @FXML
    private void sendChatMessage() {
        String message = txtChatInput.getText().trim();
        if (message.isEmpty()) return;

        addMessageBubble(message, true);
        txtChatInput.clear();

        Label typingLabel = new Label("L'IA réfléchit...");
        typingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
        chatMessages.getChildren().add(typingLabel);

        chatbotService.askQuestion(message).thenAccept(response -> {
            Platform.runLater(() -> {
                chatMessages.getChildren().remove(typingLabel);
                addMessageBubble(response, false);
            });
        });
    }

    private void addMessageBubble(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(300);
        
        VBox bubble = new VBox(label);
        bubble.setPadding(new Insets(12, 20, 12, 20));
        
        if (isUser) {
            bubble.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 20 20 0 20;");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
            HBox container = new HBox(bubble);
            container.setAlignment(Pos.CENTER_RIGHT);
            chatMessages.getChildren().add(container);
        } else {
            bubble.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 20 20 20 0; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20 20 20 0;");
            label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14;");
            HBox container = new HBox(bubble);
            container.setAlignment(Pos.CENTER_LEFT);
            chatMessages.getChildren().add(container);
        }
        
        Platform.runLater(() -> {
            if (chatMessages.getParent().getParent() instanceof ScrollPane) {
                ((ScrollPane) chatMessages.getParent().getParent()).setVvalue(1.0);
            }
        });
    }
}
