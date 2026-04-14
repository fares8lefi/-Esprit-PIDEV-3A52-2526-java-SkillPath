package Controllers.course;

import Models.Course;
import Services.CourseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.sql.SQLDataException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class CourseListController implements Initializable {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colTitle;
    @FXML private TableColumn<Course, String> colLevel;
    @FXML private TableColumn<Course, String> colCategory;
    @FXML private TableColumn<Course, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<Course, Void> colActions;

    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> comboLevel;
    @FXML private TextField txtCategory;
    @FXML private TextField txtImage;

    private CourseService courseService = new CourseService();
    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private Course selectedCourse = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
        setupForm();
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        setupActionsColumn();

        courseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Course, Void>, TableCell<Course, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Course, Void> call(final TableColumn<Course, Void> param) {
                return new TableCell<>() {
                    private final Button btnDelete = new Button("Supprimer");
                    {
                        btnDelete.getStyleClass().add("btn-logout"); // Using existing style for danger
                        btnDelete.setStyle("-fx-padding: 5 10; -fx-font-size: 11;");
                        btnDelete.setOnAction((ActionEvent event) -> {
                            Course data = getTableView().getItems().get(getIndex());
                            handleDelete(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnDelete);
                        }
                    }
                };
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void setupForm() {
        comboLevel.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
    }

    private void loadData() {
        try {
            courseList.clear();
            courseList.addAll(courseService.recuperer());
            courseTable.setItems(courseList);
        } catch (SQLDataException e) {
            showAlert("Erreur", "Impossible de charger les cours : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void fillForm(Course course) {
        selectedCourse = course;
        txtTitle.setText(course.getTitle());
        txtDescription.setText(course.getDescription());
        comboLevel.setValue(course.getLevel());
        txtCategory.setText(course.getCategory());
        txtImage.setText(course.getImage());
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInputs()) return;

        Course course = (selectedCourse == null) ? new Course() : selectedCourse;
        course.setTitle(txtTitle.getText());
        course.setDescription(txtDescription.getText());
        course.setLevel(comboLevel.getValue());
        course.setCategory(txtCategory.getText());
        course.setImage(txtImage.getText());

        try {
            if (selectedCourse == null) {
                courseService.ajouter(course);
                showAlert("Succès", "Cours ajouté avec succès !", Alert.AlertType.INFORMATION);
            } else {
                courseService.modifier(course);
                showAlert("Succès", "Cours modifié avec succès !", Alert.AlertType.INFORMATION);
            }
            handleClear(null);
            loadData();
        } catch (SQLDataException e) {
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedCourse = null;
        txtTitle.clear();
        txtDescription.clear();
        comboLevel.setValue(null);
        txtCategory.clear();
        txtImage.clear();
        courseTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadData();
    }

    private void handleDelete(Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce cours ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    courseService.supprimer(course);
                    loadData();
                    handleClear(null);
                } catch (SQLDataException e) {
                    showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private boolean validateInputs() {
        if (txtTitle.getText().isEmpty() || comboLevel.getValue() == null) {
            showAlert("Champs manquants", "Le titre et le niveau sont obligatoires.", Alert.AlertType.WARNING);
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
