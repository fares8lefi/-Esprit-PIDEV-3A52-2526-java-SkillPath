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
    @FXML private ComboBox<String> comboCategory;
    @FXML private TextArea txtDescription;
    @FXML private Label lblImageName;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;
    @FXML private SideBarController sideBarController;
    @FXML private Label lblErrorTitle;
    @FXML private Label lblErrorCategory;
    @FXML private Label lblErrorDescription;

    private final CourseService courseService = new CourseService();
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("courses");
        }
        setupLevels();
        setupCategories();
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        txtTitle.textProperty().addListener((obs, oldVal, newVal) -> validateField(txtTitle));
        comboCategory.valueProperty().addListener((obs, oldVal, newVal) -> validateField(comboCategory));
        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> validateField(txtDescription));
        
        // Also keep focus listeners to clear/validate on leave
        txtTitle.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateField(txtTitle); });
        comboCategory.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateField(comboCategory); });
        txtDescription.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateField(txtDescription); });
    }

    private void validateField(Control field) {
        boolean isValid = true;
        String errorMsg = "";
        Label targetLabel = null;

        if (field instanceof TextField tf) {
            String text = tf.getText().trim();
            if (tf == txtTitle) {
                targetLabel = lblErrorTitle;
                if (text.isEmpty()) { isValid = false; errorMsg = "Le titre est obligatoire"; }
                else if (text.length() < 3) { isValid = false; errorMsg = "Le titre doit faire au moins 3 caractères (" + text.length() + "/3)"; }
            }
        } else if (field instanceof ComboBox<?> cb) {
            String val = (String) cb.getValue();
            if (cb == comboCategory) {
                targetLabel = lblErrorCategory;
                if (val == null || val.isEmpty()) { isValid = false; errorMsg = "La catégorie est obligatoire"; }
            }
        } else if (field instanceof TextArea ta) {
            String text = ta.getText().trim();
            targetLabel = lblErrorDescription;
            if (text.isEmpty()) { isValid = false; errorMsg = "La description est obligatoire"; }
            else if (text.length() < 10) { isValid = false; errorMsg = "Minimum 10 caractères requis (" + text.length() + "/10)"; }
        }

        if (isValid) {
            field.setStyle("-fx-border-color: rgba(255, 255, 255, 0.1); -fx-background-color: rgba(255, 255, 255, 0.03); -fx-text-fill: white;");
            if (targetLabel != null) {
                targetLabel.setVisible(false);
                targetLabel.setManaged(false);
            }
        } else {
            field.setStyle("-fx-border-color: #f43f5e; -fx-background-color: rgba(244, 63, 94, 0.05); -fx-text-fill: white;");
            if (targetLabel != null) {
                targetLabel.setText(errorMsg);
                targetLabel.setVisible(true);
                targetLabel.setManaged(true);
            }
        }
    }

    private void setupLevels() {
        comboLevel.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
        comboLevel.setValue("Débutant");
        styleComboBox(comboLevel);
    }

    private void setupCategories() {
        comboCategory.setItems(FXCollections.observableArrayList("Développement", "Design", "Marketing", "Devops", "Businesses"));
        styleComboBox(comboCategory);
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
            course.setCategory(comboCategory.getValue());
            course.setDescription(txtDescription.getText());
            
            // Handle Image Saving (Bulletproof Path)
            if (selectedImageFile != null) {
                String fileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                File destinationDir = Utils.AssetLoader.getModulesUploadsDir();
                File destinationFile = new File(destinationDir, fileName);
                try {
                    java.nio.file.Files.copy(selectedImageFile.toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    course.setImage(fileName);
                } catch (IOException e) {
                    System.err.println("Erreur copie image : " + e.getMessage());
                }
            } else {
                course.setImage(""); // Or a default image
            }
            
            try {
                courseService.ajouter(course);
                showFlash("Formation créée avec succès !", true);
                
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
        boolean valid = true;
        
        if (txtTitle.getText().trim().length() < 3) {
            validateField(txtTitle);
            valid = false;
        }
        if (comboCategory.getValue() == null || comboCategory.getValue().isEmpty()) {
            validateField(comboCategory);
            valid = false;
        }
        if (txtDescription.getText().trim().length() < 10) {
            validateField(txtDescription);
            valid = false;
        }

        if (!valid) {
            showAlert("Données Invalides", "Veuillez corriger les erreurs suivantes :\n- Titre : min 3 caractères\n- Description : min 10 caractères\n- Catégorie : obligatoire");
            showFlash("Veuillez corriger les erreurs (Titre: min 3, Desc: min 10)", false);
        }
        
        return valid;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Elite CSS styling for the dialog would go here if using custom skins, 
        // but for now, we use the standard robust dialog.
        alert.showAndWait();
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
