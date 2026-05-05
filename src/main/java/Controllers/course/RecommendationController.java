package Controllers.course;

import Models.Course;
import Services.RecommendationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RecommendationController implements Initializable {

    @FXML private Label lblDetectedProfile;
    @FXML private Label lblBudget;
    @FXML private Slider budgetSlider;
    @FXML private VBox recommendationsContainer;

    private final RecommendationService recommendationService = new RecommendationService();
    private List<Course> allRecommended;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String cat = recommendationService.getPreferredCategory();
        String level = recommendationService.getPreferredLevel();
        
        if (cat != null) {
            lblDetectedProfile.setText(cat + " (" + (level != null ? level : "Tous niveaux") + ")");
        } else {
            lblDetectedProfile.setText("Aucune donnée (cliquez sur des cours)");
        }

        allRecommended = recommendationService.getRecommendations();
        
        budgetSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblBudget.setText(String.format("%.0f DT", newVal.doubleValue()));
            filterByBudget(newVal.doubleValue());
        });

        displayCourses(allRecommended);
    }

    private void filterByBudget(double maxPrice) {
        List<Course> filtered = allRecommended.stream()
                .filter(c -> c.getPrice() <= maxPrice)
                .collect(Collectors.toList());
        displayCourses(filtered);
    }

    private void displayCourses(List<Course> courses) {
        recommendationsContainer.getChildren().clear();
        try {
            for (Course c : courses) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/courseCard.fxml"));
                Node card = loader.load();
                FrontCourseCardController controller = loader.getController();
                controller.setData(c);
                
                // Enhancement: Add a "Match" badge if the level is perfect
                String preferredLevel = recommendationService.getPreferredLevel();
                if (c.getLevel().equalsIgnoreCase(preferredLevel)) {
                    javafx.scene.control.Label badge = new javafx.scene.control.Label("✨ MATCH PARFAIT");
                    badge.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
                    ((javafx.scene.layout.Pane) card).getChildren().add(badge);
                }
                
                recommendationsContainer.getChildren().add(card);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToHome(ActionEvent event) {
        navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "Accueil - SkillPath");
    }

    @FXML
    private void goToCourseList(ActionEvent event) {
        navigateTo(event, "/FrontOffice/course/courseList.fxml", "Cours - SkillPath");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
