package Controllers.course;

import Models.Course;
import Services.CourseService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private final CourseService courseService = new CourseService();
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
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(800), cardNode);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.setDelay(Duration.millis(staggerDelay));
                    
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(800), cardNode);
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
    private void toggleChat() {
        boolean isVisible = chatWindow.isVisible();
        chatWindow.setVisible(!isVisible);
        
        if (!isVisible) {
            FadeTransition ft = new FadeTransition(Duration.millis(300), chatWindow);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
            
            if (chatMessages.getChildren().isEmpty()) {
                addChatMessage("Bonjour ! Je suis votre assistant SkillPath. Comment puis-je vous aider aujourd'hui ?", false);
            }
        }
    }

    @FXML
    private void sendChatMessage() {
        String text = txtChatInput.getText().trim();
        if (text.isEmpty()) return;

        addChatMessage(text, true);
        txtChatInput.clear();

        // Simulate AI Response
        Platform.runLater(() -> {
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                Platform.runLater(() -> {
                    String response = getAIResponse(text);
                    addChatMessage(response, false);
                });
            }).start();
        });
    }

    private void addChatMessage(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(280);
        label.getStyleClass().add(isUser ? "chat-bubble-user" : "chat-bubble-ai");
        
        HBox box = new HBox(label);
        box.setAlignment(isUser ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        
        chatMessages.getChildren().add(box);
        
        // Simple entrance animation for bubble
        label.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(300), label);
        ft.setToValue(1);
        ft.play();
    }

    private String getAIResponse(String input) {
        input = input.toLowerCase();
        if (input.contains("java")) return "Le cours Java Expert est excellent pour maîtriser les Streams et le Multi-threading.";
        if (input.contains("prix") || input.contains("gratuit")) return "Certains de nos cours sont gratuits, d'autres sont à prix premium pour garantir la qualité.";
        if (input.contains("merci")) return "Je vous en prie ! N'hésitez pas si vous avez d'autres questions.";
        return "Je suis là pour vous aider à trouver la meilleure formation sur SkillPath !";
    }

    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/home/homeUser.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) lblResultCount.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection accueil : " + e.getMessage());
        }
    }

    @FXML
    private void goToCourses() {
        loadData(); // Just refresh the data
    }
}
