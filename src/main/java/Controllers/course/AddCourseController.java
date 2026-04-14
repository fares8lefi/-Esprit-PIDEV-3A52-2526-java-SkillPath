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
import java.util.ResourceBundle;

public class AddCourseController implements Initializable {

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> comboLevel;
    @FXML private TextField txtCategory;
    @FXML private TextArea txtDescription;
    @FXML private Label lblImageName;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;
    @FXML private SideBarController sideBarController;

    private final CourseService courseService = new CourseService();
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
        comboLevel.setValue("Débutant");
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner l'image du cours");
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
            Course course = new Course();
            course.setTitle(txtTitle.getText());
            course.setLevel(comboLevel.getValue());
            course.setCategory(txtCategory.getText());
            course.setDescription(txtDescription.getText());
            // Note: imageFile logic would involve copying the file to a static resource folder
            // For now, we simulate the database persistence.
            
            try {
                courseService.ajouter(course);
                showFlash("Formation créée avec succès !", true);
                
                // Navigate back after a short delay
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> navigateToList(event));
                }).start();
                
            } catch (Exception e) {
                showFlash("Erreur lors de la création : " + e.getMessage(), false);
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
