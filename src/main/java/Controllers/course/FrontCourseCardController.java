package Controllers.course;

import Models.Course;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class FrontCourseCardController {

    @FXML private ImageView imgCourse;
    @FXML private Label lblPrice;
    @FXML private Label lblCategory;
    @FXML private Label lblTitle;
    @FXML private Label lblDescription;
    @FXML private Label lblLevel;
    @FXML private Label lblAiProb;
    @FXML private HBox aiBadge;
    @FXML private StackPane imageContainer;
    @FXML private javafx.scene.control.Button btnFavorite;
    
    private Course course;
    private boolean isLiked = false;

    // Simple static storage to remember likes during the application run
    private static final java.util.Set<Integer> likedCourses = new java.util.HashSet<>();

    public void setData(Course course) {
        this.course = course;
        
        // Initialize favorite state from memory
        isLiked = likedCourses.contains(course.getId());
        updateFavoriteUI();
        
        lblTitle.setText(course.getTitle());
        lblDescription.setText(course.getDescription());
        lblLevel.setText(course.getLevel());
        
        // Handle Category Styling
        lblCategory.getStyleClass().clear();
        lblCategory.getStyleClass().add("category-badge-base");
        
        String cat = course.getCategory().toLowerCase();
        if (cat.contains("dev") || cat.contains("program")) {
            lblCategory.getStyleClass().add("cat-dev");
        } else if (cat.contains("design") || cat.contains("ui") || cat.contains("ux")) {
            lblCategory.getStyleClass().add("cat-design");
        } else if (cat.contains("biz") || cat.contains("manage") || cat.contains("market")) {
            lblCategory.getStyleClass().add("cat-business");
        } else {
            lblCategory.getStyleClass().add("cat-default");
        }
        
        lblCategory.setText(course.getCategory() != null ? course.getCategory().toUpperCase() : "DÉVELOPPEMENT");
        
        // Handle Price
        if (course.getPrice() <= 0) {
            lblPrice.setText("GRATUIT");
            lblPrice.setStyle("-fx-background-color: #4ade80; -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 8; -fx-font-weight: 900; -fx-font-size: 10;");
        } else {
            lblPrice.setText(String.format("%.2f DT", course.getPrice()));
        }

        // Handle Image (Bulletproof Loading)
        if (course.getImage() != null && !course.getImage().isEmpty()) {
            Image loadedImage = Utils.AssetLoader.loadCourseImage(course.getImage());
            if (loadedImage != null) {
                imgCourse.setImage(loadedImage);
            } else {
                // Professional placeholder if file not found or wrong format
                URL placeholderRes = getClass().getResource("/FrontOffice/images/default-course.png");
                if (placeholderRes != null) imgCourse.setImage(new Image(placeholderRes.toExternalForm()));
            }
        } else {
            // Default placeholder if no image path exists at all
            URL placeholderRes = getClass().getResource("/FrontOffice/images/default-course.png");
            if (placeholderRes != null) imgCourse.setImage(new Image(placeholderRes.toExternalForm()));
        }

        // Simulate AI Probability if not exists
        int prob = 70 + new Random().nextInt(25);
        lblAiProb.setText(prob + "% SUCCÈS");
    }

    @FXML
    private void handleHoverIn() {
        // Handle visual hover effects if needed, though most are CSS-based
    }

    @FXML
    private void handleHoverOut() {
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/courseDetails.fxml"));
            Parent root = loader.load();
            
            FrontCourseDetailsController controller = loader.getController();
            controller.setCourse(course);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation vers détails : " + e.getMessage());
        }
    }

    @FXML
    private void handleFavoriteToggle() {
        isLiked = !isLiked;
        if (isLiked) {
            likedCourses.add(course.getId());
        } else {
            likedCourses.remove(course.getId());
        }
        updateFavoriteUI();
        
        // Simple scale effect for feedback
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), btnFavorite);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.3); st.setToY(1.3);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void updateFavoriteUI() {
        if (isLiked) {
            btnFavorite.setText("♥"); // Filled heart
            btnFavorite.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-padding: 0; -fx-cursor: hand; -fx-font-size: 22;");
        } else {
            btnFavorite.setText("♡"); // Empty heart
            btnFavorite.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-padding: 0; -fx-cursor: hand; -fx-font-size: 22;");
        }
    }
}
