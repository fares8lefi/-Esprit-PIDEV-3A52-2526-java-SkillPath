package Controllers.module;

import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLDataException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class ModuleListController implements Initializable {

    @FXML private TableView<Module> moduleTable;
    @FXML private TableColumn<Module, String> colTitle;
    @FXML private TableColumn<Module, String> colType;
    @FXML private TableColumn<Module, Integer> colCourse;
    @FXML private TableColumn<Module, LocalDateTime> colScheduledAt;
    @FXML private TableColumn<Module, Void> colActions;

    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Course> comboCourse;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContent;
    @FXML private TextField txtDocument;
    @FXML private DatePicker dateScheduled;

    private ModuleService moduleService = new ModuleService();
    private CourseService courseService = new CourseService();
    private ObservableList<Module> moduleList = FXCollections.observableArrayList();
    private ObservableList<Course> courseChoices = FXCollections.observableArrayList();
    private Module selectedModule = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadInitialData();
        setupForm();
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        colScheduledAt.setCellValueFactory(new PropertyValueFactory<>("scheduledAt"));

        setupActionsColumn();

        moduleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Supprimer");
            {
                btnDelete.getStyleClass().add("btn-logout");
                btnDelete.setStyle("-fx-padding: 5 10; -fx-font-size: 11;");
                btnDelete.setOnAction(event -> {
                    Module data = getTableView().getItems().get(getIndex());
                    handleDelete(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnDelete);
            }
        });
    }

    private void setupForm() {
        comboType.setItems(FXCollections.observableArrayList("Leçon", "Examen", "Quiz", "TP"));
        
        comboCourse.setConverter(new StringConverter<Course>() {
            @Override
            public String toString(Course course) {
                return (course == null) ? "" : course.getTitle();
            }
            @Override
            public Course fromString(String string) {
                return null;
            }
        });
    }

    private void loadInitialData() {
        try {
            // Load modules
            moduleList.clear();
            moduleList.addAll(moduleService.recuperer());
            moduleTable.setItems(moduleList);

            // Load courses for combo
            courseChoices.clear();
            courseChoices.addAll(courseService.recuperer());
            comboCourse.setItems(courseChoices);
        } catch (SQLDataException e) {
            showAlert("Erreur", "Impossible de charger les données : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void fillForm(Module module) {
        selectedModule = module;
        txtTitle.setText(module.getTitle());
        txtDescription.setText(module.getDescription());
        txtContent.setText(module.getContent());
        txtDocument.setText(module.getDocument());
        comboType.setValue(module.getType());
        
        if (module.getScheduledAt() != null) {
            dateScheduled.setValue(module.getScheduledAt().toLocalDate());
        } else {
            dateScheduled.setValue(null);
        }

        // Find and select the course
        courseChoices.stream()
                .filter(c -> c.getId() == module.getCourseId())
                .findFirst()
                .ifPresent(comboCourse::setValue);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInputs()) return;

        Module module = (selectedModule == null) ? new Module() : selectedModule;
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
            if (selectedModule == null) {
                moduleService.ajouter(module);
                showAlert("Succès", "Module ajouté avec succès !", Alert.AlertType.INFORMATION);
            } else {
                moduleService.modifier(module);
                showAlert("Succès", "Module modifié avec succès !", Alert.AlertType.INFORMATION);
            }
            handleClear(null);
            handleRefresh(null);
        } catch (SQLDataException e) {
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedModule = null;
        txtTitle.clear();
        txtDescription.clear();
        txtContent.clear();
        txtDocument.clear();
        comboType.setValue(null);
        comboCourse.setValue(null);
        dateScheduled.setValue(null);
        moduleTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadInitialData();
    }

    private void handleDelete(Module module) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce module ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    moduleService.supprimer(module);
                    handleRefresh(null);
                    handleClear(null);
                } catch (SQLDataException e) {
                    showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private boolean validateInputs() {
        if (txtTitle.getText().isEmpty() || comboCourse.getValue() == null) {
            showAlert("Champs manquants", "Le titre et le cours parent sont obligatoires.", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
