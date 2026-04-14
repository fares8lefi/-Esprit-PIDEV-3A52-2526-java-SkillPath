package Controllers.course;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Models.Module;
import Services.ModuleService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CourseDetailsController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblCategoryHeader;
    @FXML private Label lblIdBadge;
    @FXML private Label lblDescription;
    @FXML private Label lblLevel;
    @FXML private Label lblCategory;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblModuleCount;
    @FXML private VBox modulesContainer;
    @FXML private ImageView imgCourse;
    @FXML private Label lblNoImage;
    @FXML private SideBarController sideBarController;

    private final ModuleService moduleService = new ModuleService();
    private Course currentCourse;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("courses");
        }
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        
        lblTitle.setText(course.getTitle());
        lblCategoryHeader.setText(course.getCategory() != null ? course.getCategory() : "Général");
        lblIdBadge.setText("ID: #" + course.getId());
        lblDescription.setText(course.getDescription() != null ? course.getDescription() : "Aucune description fournie.");
        lblLevel.setText(course.getLevel());
        lblCategory.setText(course.getCategory() != null ? course.getCategory() : "N/A");
        
        if (course.getCreatedAt() != null) {
            lblCreatedAt.setText(course.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        }

        // Image loading
        if (course.getImage() != null && !course.getImage().isEmpty()) {
            try {
                // Assuming images are in a specific public URL or local path
                // For now, placeholder logic
                lblNoImage.setVisible(false);
            } catch (Exception e) {
                lblNoImage.setVisible(true);
            }
        } else {
            lblNoImage.setVisible(true);
        }

        loadModules();
    }

    private void loadModules() {
        try {
            List<Module> modules = moduleService.getByCourse(currentCourse.getId());
            lblModuleCount.setText(modules.size() + " modules");
            
            modulesContainer.getChildren().clear();
            if (modules.isEmpty()) {
                Label empty = new Label("Aucun module associé pour le moment.");
                empty.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
                modulesContainer.getChildren().add(empty);
            } else {
                for (int i = 0; i < modules.size(); i++) {
                    modulesContainer.getChildren().add(createModuleRow(modules.get(i), i + 1));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement modules: " + e.getMessage());
        }
    }

    private HBox createModuleRow(Module module, int index) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;");
        
        // Index Badge
        Label lblIndex = new Label(String.valueOf(index));
        lblIndex.setAlignment(Pos.CENTER);
        lblIndex.setPrefSize(32, 32);
        lblIndex.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11;");
        
        // Info
        VBox info = new VBox(2);
        Label title = new Label(module.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        Label type = new Label(module.getType() != null ? module.getType().toUpperCase() : "CONTENT");
        type.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10; -fx-font-weight: 900;");
        info.getChildren().addAll(title, type);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Arrow Icon
        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: #64748b; -fx-font-size: 18;");
        
        row.getChildren().addAll(lblIndex, info, spacer, arrow);
        
        // Hover effects
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(139, 92, 246, 0.3); -fx-border-radius: 12; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;"));
        
        return row;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateTo(event, "/BackOffice/course/courseList.fxml");
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/editCourse.fxml"));
            Parent root = loader.load();
            EditCourseController controller = loader.getController();
            controller.setCourse(currentCourse);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddModule(ActionEvent event) {
        // Logique pour naviguer vers l'ajout de module avec le course_id pré-rempli
        System.out.println("Naviguer vers ajout module pour cours: " + currentCourse.getId());
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
