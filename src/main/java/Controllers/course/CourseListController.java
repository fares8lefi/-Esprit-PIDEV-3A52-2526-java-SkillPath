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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
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

        // Actions Row
        HBox actions = new HBox(8);
        
        Button btnShow = new Button("👁");
        btnShow.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #60a5fa; -fx-background-radius: 8; -fx-cursor: hand;");
        btnShow.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/courseDetails.fxml"));
                Parent root = loader.load();
                CourseDetailsController controller = loader.getController();
                controller.setCourse(course);
                Stage stage = (Stage) btnShow.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                showFlash("Erreur de navigation", false);
            }
        });

        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #4ade80; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/editCourse.fxml"));
                Parent root = loader.load();
                
                // Pass the course to the controller
                EditCourseController controller = loader.getController();
                controller.setCourse(course);
                
                Stage stage = (Stage) btnEdit.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                showFlash("Erreur de navigation", false);
            }
        });

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #f87171; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> confirmDelete(course));
        
        actions.getChildren().addAll(btnShow, btnEdit, btnDelete);
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

    @FXML 
    private void handleAdd(ActionEvent event) { 
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/addCourse.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showFlash("Erreur de navigation", false);
        }
    }

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
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; -fx-border-color: rgba(248,113,113,0.3); -fx-border-width: 2; -fx-background-radius: 20; -fx-border-radius: 20;");

        // Icon (Trash)
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 50; -fx-text-fill: #f87171;");

        VBox textContent = new VBox(10);
        textContent.setAlignment(Pos.CENTER);
        Label title = new Label("CONFIRMATION");
        title.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 900; -fx-letter-spacing: 2; -fx-font-size: 14;");
        
        Label message = new Label("Voulez-vous vraiment supprimer\n« " + course.getTitle() + " » ?");
        message.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-text-alignment: center;");
        message.setWrapText(true);
        textContent.getChildren().addAll(title, message);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        
        Button btnCancel = new Button("ANNULER");
        btnCancel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-font-weight: 900; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnConfirm = new Button("SUPPRIMER");
        btnConfirm.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            try {
                courseService.supprimer(course);
                loadData();
                showFlash("Contenu supprimé définitivement", true);
            } catch (Exception ex) {
                showFlash("Impossible de supprimer : ce cours contient peut-être déjà des modules.", false);
                ex.printStackTrace();
            } finally {
                dialog.close();
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(icon, textContent, actions);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
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
