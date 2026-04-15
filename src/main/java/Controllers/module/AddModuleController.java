package Controllers.module;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
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
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class AddModuleController implements Initializable {

    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Course> comboCourse;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContent;
    @FXML private TextField txtDocument;
    @FXML private DatePicker dateScheduled;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;
    @FXML private SideBarController sideBarController;

    private final ModuleService moduleService = new ModuleService();
    private final CourseService courseService = new CourseService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("modules");
        }
        setupCombos();
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        txtTitle.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateField(txtTitle);
        });
        comboCourse.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateField(comboCourse);
        });
        comboType.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateField(comboType);
        });
    }

    private void validateField(Control field) {
        boolean isValid = true;
        if (field instanceof TextField tf) isValid = !tf.getText().isEmpty();
        else if (field instanceof ComboBox<?> cb) isValid = cb.getValue() != null;

        if (isValid) {
            field.setStyle("-fx-border-color: rgba(255, 255, 255, 0.1);");
        } else {
            field.setStyle("-fx-border-color: #f43f5e;");
        }
    }

    private void setupCombos() {
        comboType.setItems(FXCollections.observableArrayList("Leçon", "Examen", "Quiz", "TP"));
        
        try {
            comboCourse.setItems(FXCollections.observableArrayList(courseService.recuperer()));
            comboCourse.setConverter(new StringConverter<Course>() {
                @Override public String toString(Course c) { return c == null ? "" : c.getTitle(); }
                @Override public Course fromString(String s) { return null; }
            });
        } catch (SQLDataException e) {
            showFlash("Erreur chargement cours", false);
        }

        // Force white readable text for selected value in each ComboBox
        styleCourseCombo(comboCourse);
        styleTypeCombo(comboType);
    }

    private void styleCourseCombo(ComboBox<Course> combo) {
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle());
                setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle());
                setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;");
                setOnMouseEntered(e -> setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 6;"));
                setOnMouseExited(e -> setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;"));
            }
        });
    }

    private void styleTypeCombo(ComboBox<String> combo) {
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
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
    private void handleSave(ActionEvent event) {
        if (!validateInputs()) return;

        Module module = new Module();
        module.setTitle(txtTitle.getText());
        module.setDescription(txtDescription.getText());
        module.setType(comboType.getValue());
        module.setContent(txtContent.getText());
        module.setDocument(txtDocument.getText());
        module.setCourseId(comboCourse.getValue().getId());
        
        if (dateScheduled.getValue() != null) {
            module.setScheduledAt(LocalDateTime.of(dateScheduled.getValue(), LocalTime.MIDNIGHT));
        }

        try {
            moduleService.ajouter(module);
            showFlash("Module créé avec succès !", true);
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> handleBack(event));
            }).start();
        } catch (SQLDataException e) {
            showFlash("Erreur: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleBrowseDocument(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.pptx", "*.txt"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            txtDocument.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/BackOffice/module/moduleList.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        boolean ok = true;
        if (txtTitle.getText().isEmpty()) { validateField(txtTitle); ok = false; }
        if (comboCourse.getValue() == null) { validateField(comboCourse); ok = false; }
        if (comboType.getValue() == null) { validateField(comboType); ok = false; }

        if (!ok) {
            showFlash("Veuillez remplir les champs obligatoires (*)", false);
        }
        return ok;
    }

    private void showFlash(String message, boolean success) {
        flashLabel.setText(message);
        flashBox.setStyle("-fx-background-color: " + (success ? "rgba(74,222,128,0.1)" : "rgba(248,113,113,0.1)") + "; " +
                           "-fx-border-color: " + (success ? "#4ade80" : "#f87171") + "; -fx-border-radius: 10;");
        flashLabel.setStyle("-fx-text-fill: " + (success ? "#4ade80" : "#f87171") + "; -fx-font-weight: bold;");
        flashBox.setVisible(true); flashBox.setManaged(true);
    }
}
