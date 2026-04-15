package Controllers.course;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Services.CourseService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EditCourseController implements Initializable {

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> comboLevel;
    @FXML private TextField txtCategory;
    @FXML private TextArea txtDescription;
    @FXML private Label lblCourseTitleHeader;
    @FXML private Label lblIdBadge;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblUpdatedAt;
    @FXML private HBox updatedAtBox;
    @FXML private Label lblImageName;
    @FXML private Label lblCurrentImage;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;
    @FXML private SideBarController sideBarController;

    private final CourseService courseService = new CourseService();
    private Course currentCourse;
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("courses");
        }
        setupLevels();
    }

    private void setupLevels() {
        comboLevel.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
        styleComboBox(comboLevel);
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;");
                setOnMouseEntered(e -> setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 6;"));
                setOnMouseExited(e -> setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;"));
            }
        });
    }

    /**
     * Injected by the previous controller to load the course data.
     */
    public void setCourse(Course course) {
        this.currentCourse = course;
        
        lblIdBadge.setText("ID: #" + course.getId());
        lblCourseTitleHeader.setText(course.getTitle());
        txtTitle.setText(course.getTitle());
        comboLevel.setValue(course.getLevel());
        txtCategory.setText(course.getCategory());
        txtDescription.setText(course.getDescription());
        
        // Simuler l'image actuelle
        lblCurrentImage.setText("Image actuelle : " + (course.getImage() != null ? course.getImage() : "Aucune"));
        
        // Formater les dates (Supposant que Course a getCreatedAt()/getUpdatedAt() de type Timestamp ou LocalDateTime)
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        // lblCreatedAt.setText(course.getCreatedAt().format(formatter));
        // if (course.getUpdatedAt() != null) {
        //     lblUpdatedAt.setText(course.getUpdatedAt().format(formatter));
        //     updatedAtBox.setVisible(true);
        // } else {
        //     updatedAtBox.setVisible(false);
        // }
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Remplacer l'image du cours");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        selectedImageFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedImageFile != null) {
            lblImageName.setText(selectedImageFile.getName());
            lblImageName.setStyle("-fx-text-fill: #4ade80;");
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (isInputValid()) {
            currentCourse.setTitle(txtTitle.getText());
            currentCourse.setLevel(comboLevel.getValue());
            currentCourse.setCategory(txtCategory.getText());
            currentCourse.setDescription(txtDescription.getText());

            try {
                courseService.modifier(currentCourse);
                showFlash("Modifications enregistrées !", true);
                
                // Navigate back after a short delay
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> navigateToList(event));
                }).start();
                
            } catch (Exception e) {
                showFlash("Erreur lors de la mise à jour : " + e.getMessage(), false);
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateToList(event);
    }

    private boolean isInputValid() {
        if (txtTitle.getText().isBlank()) {
            showFlash("Le titre est obligatoire", false);
            return false;
        }
        return true;
    }

    private void navigateToList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/courseList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
        }
    }

    private void showFlash(String message, boolean success) {
        flashLabel.setText(message);
        flashBox.setStyle("-fx-background-color: " + (success ? "rgba(74,222,128,0.1)" : "rgba(248,113,113,0.1)") + "; " +
                         "-fx-border-color: " + (success ? "#4ade80" : "#f87171") + "; -fx-border-radius: 10;");
        flashLabel.setStyle("-fx-text-fill: " + (success ? "#4ade80" : "#f87171") + "; -fx-font-weight: bold;");
        flashBox.setVisible(true);
        flashBox.setManaged(true);
    }
}
