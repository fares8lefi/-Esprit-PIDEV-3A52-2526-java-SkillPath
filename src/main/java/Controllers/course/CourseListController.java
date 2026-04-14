package Controllers.course;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Services.CourseService;
import Services.ModuleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.ResourceBundle;

public class CourseListController implements Initializable {

    // ── UI Containers ──
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;
    @FXML private Label lblCount;
    @FXML private SideBarController sideBarController;

    // ── Controls ──
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboFilterLevel;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;

    // ── Services ──
    private final CourseService courseService = new CourseService();
    private final ModuleService moduleService = new ModuleService();

    // ── Data ──
    private ObservableList<Course> masterList = FXCollections.observableArrayList();
    private FilteredList<Course> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("courses");
        }
        setupFilters();
        loadData();
    }

    private void setupFilters() {
        comboFilterLevel.setItems(FXCollections.observableArrayList("Tous les niveaux", "Débutant", "Intermédiaire", "Avancé"));
        comboFilterLevel.setValue("Tous les niveaux");

        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        comboFilterLevel.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void loadData() {
        try {
            masterList.setAll(courseService.recuperer());
            filteredList = new FilteredList<>(masterList, c -> true);
            renderCards();
        } catch (SQLDataException e) {
            showFlash("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        
        if (filteredList == null || filteredList.isEmpty()) {
            emptyState.setVisible(true); emptyState.setManaged(true);
            lblCount.setText("0 cours trouvé");
            return;
        }

        emptyState.setVisible(false); emptyState.setManaged(false);

        for (Course course : filteredList) {
            cardsContainer.getChildren().add(createCourseCard(course));
        }
        
        lblCount.setText(filteredList.size() + " formation" + (filteredList.size() > 1 ? "s" : "") + " trouvée" + (filteredList.size() > 1 ? "s" : ""));
    }

    /**
     * Programmatically creates a premium course card in Java code.
     * satisfying the "All-in-One" requirement.
     */
    private Node createCourseCard(Course course) {
        VBox card = new VBox(15);
        card.getStyleClass().add("glass-card");
        card.setPrefWidth(320);
        card.setPadding(new Insets(20));

        // 1. Level Badge Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        Label lblLevel = new Label(course.getLevel() != null ? course.getLevel() : "Débutant");
        
        String color, glow;
        switch (lblLevel.getText()) {
            case "Débutant"      -> { color = "#4ade80"; glow = "rgba(74, 222, 128, 0.1)"; }
            case "Intermédiaire" -> { color = "#fbbf24"; glow = "rgba(251, 191, 36, 0.1)"; }
            case "Avancé"        -> { color = "#f87171"; glow = "rgba(248, 113, 113, 0.1)"; }
            default              -> { color = "#94a3b8"; glow = "rgba(148, 163, 184, 0.1)"; }
        }
        lblLevel.setStyle("-fx-background-color: " + glow + "; -fx-text-fill: " + color + "; " +
                          "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold; " +
                          "-fx-font-size: 11; -fx-border-color: " + color + "33; -fx-border-radius: 20;");
        header.getChildren().add(lblLevel);

        // 2. Title & Category
        VBox meta = new VBox(8);
        Text txtTitle = new Text(course.getTitle());
        txtTitle.getStyleClass().add("gradient-text");
        txtTitle.setStyle("-fx-font-size: 20; -fx-font-weight: 800;");
        
        Label lblCat = new Label(course.getCategory() == null || course.getCategory().isEmpty() ? "Général" : course.getCategory());
        lblCat.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13;");
        meta.getChildren().addAll(txtTitle, lblCat);

        // 3. Description Label
        Label lblDesc = new Label(course.getDescription() != null ? course.getDescription() : "Aucune description.");
        lblDesc.setWrapText(true);
        lblDesc.setMinHeight(60); lblDesc.setMaxHeight(60);
        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-line-spacing: 2;");

        Separator sep = new Separator();
        sep.setOpacity(0.1);

        // 4. Footer
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        int modCount = 0;
        try { modCount = moduleService.countByCourse(course.getId()); } catch (Exception ignored) {}
        Label lblMods = new Label(modCount + " Chapitre(s)");
        lblMods.setStyle("-fx-background-color: rgba(30, 136, 229, 0.1); -fx-text-fill: #3b82f6; " +
                         "-fx-background-radius: 10; -fx-padding: 4 10; -fx-font-weight: bold; -fx-font-size: 11;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(8);
        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #4ade80; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> openEditDialog(course));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #f87171; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> confirmDelete(course));
        
        actions.getChildren().addAll(btnEdit, btnDelete);
        footer.getChildren().addAll(lblMods, spacer, actions);

        card.getChildren().addAll(header, meta, lblDesc, sep, footer);
        return card;
    }

    private void applyFilters() {
        if (filteredList == null) return;
        String search = txtSearch.getText().toLowerCase().trim();
        String level = comboFilterLevel.getValue();

        filteredList.setPredicate(course -> {
            boolean matchSearch = search.isEmpty() ||
                    (course.getTitle() != null && course.getTitle().toLowerCase().contains(search)) ||
                    (course.getDescription() != null && course.getDescription().toLowerCase().contains(search));
            boolean matchLevel = level.equals("Tous les niveaux") || (course.getLevel() != null && course.getLevel().equals(level));
            return matchSearch && matchLevel;
        });
        renderCards();
    }

    @FXML private void handleRefresh(ActionEvent event) { loadData(); showFlash("Données synchronisées", true); }
    @FXML private void handleReset(ActionEvent event) { txtSearch.clear(); comboFilterLevel.setValue("Tous les niveaux"); }
    @FXML private void handleAdd(ActionEvent event) { openEditDialog(null); }

    public void openEditDialog(Course course) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(course == null ? "NOUVELLE FORMATION" : "MODIFIER FORMATION");
        
        VBox layout = new VBox(20);
        layout.setStyle("-fx-padding: 40; -fx-background-color: #0f172a; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1;");

        Label titleHeader = new Label(course == null ? "AJOUTER UN COURS" : "ÉDITER LE COURS");
        titleHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 800;");

        TextField fTitle = createEliteField(course != null ? course.getTitle() : "", "Nom du cours...");
        TextArea fDesc = new TextArea(course != null ? course.getDescription() : "");
        fDesc.setPromptText("Description détaillée..."); fDesc.setStyle(fieldStyle() + "-fx-pref-height: 120;");

        ComboBox<String> fLevel = new ComboBox<>(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
        fLevel.setValue(course != null ? course.getLevel() : "Débutant");
        fLevel.setMaxWidth(Double.MAX_VALUE); fLevel.setStyle(fieldStyle());

        TextField fCategory = createEliteField(course != null ? course.getCategory() : "", "Catégorie...");

        Button btnSave = new Button(course == null ? "CRÉER LE CONTENU" : "METTRE À JOUR");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #1E88E5, #43A047); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
        
        btnSave.setOnAction(e -> {
            if (fTitle.getText().isBlank()) return;
            Course c = course != null ? course : new Course();
            c.setTitle(fTitle.getText()); c.setDescription(fDesc.getText()); c.setLevel(fLevel.getValue()); c.setCategory(fCategory.getText());
            try {
                if (course == null) courseService.ajouter(c); else courseService.modifier(c);
                dialog.close(); loadData();
                showFlash("Catalogue mis à jour", true);
            } catch (Exception ex) { showFlash("Erreur", false); }
        });

        layout.getChildren().addAll(titleHeader, fTitle, fDesc, fLevel, fCategory, btnSave);
        dialog.setScene(new Scene(layout, 500, 650));
        dialog.showAndWait();
    }

    public void confirmDelete(Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("ALERTE"); alert.setHeaderText("Supprimer «" + course.getTitle() + "» ?");
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    courseService.supprimer(course); loadData();
                    showFlash("Contenu supprimé", true);
                } catch (Exception e) { showFlash("Erreur", false); }
            }
        });
    }

    private TextField createEliteField(String val, String pr) { TextField tf = new TextField(val); tf.setPromptText(pr); tf.setStyle(fieldStyle()); return tf; }
    private String fieldStyle() { return "-fx-background-color: rgba(15, 23, 42, 0.4); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-text-fill: white; -fx-padding: 12;"; }

    private void showFlash(String message, boolean success) {
        flashLabel.setText(message);
        flashBox.setStyle("-fx-background-color: " + (success ? "rgba(74,222,128,0.1)" : "rgba(248,113,113,0.1)") + "; -fx-border-color: " + (success ? "#4ade80" : "#f87171") + "; -fx-border-radius: 10;");
        flashLabel.setStyle("-fx-text-fill: " + (success ? "#4ade80" : "#f87171") + "; -fx-font-weight: bold;");
        flashBox.setVisible(true); flashBox.setManaged(true);
        new Thread(() -> { try { Thread.sleep(3000); } catch (Exception ignored) {} javafx.application.Platform.runLater(() -> { flashBox.setVisible(false); flashBox.setManaged(false); }); }).start();
    }
}
