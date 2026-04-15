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
        styleComboBox(comboFilterLevel);

        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        comboFilterLevel.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;");
                setOnMouseEntered(e -> setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 6;"));
                setOnMouseExited(e -> setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: transparent;"));
            }
        });
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
    private void addHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            node.setScaleX(1.02);
            node.setScaleY(1.02);
            node.setStyle(node.getStyle() + "-fx-border-color: rgba(139, 92, 246, 0.4);");
        });
        node.setOnMouseExited(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setStyle(node.getStyle().replace("-fx-border-color: rgba(139, 92, 246, 0.4);", ""));
        });
    }

    private Node createCourseCard(Course course) {
        VBox card = new VBox(0); // Zero spacing for the header strip
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(320); card.setMinWidth(320);
        card.setPadding(Insets.EMPTY); // Padding will be internal to secondary container
        card.setStyle("-fx-overflow: hidden; -fx-cursor: hand;");

        // 1. Visual Accent Header (Strip)
        String level = course.getLevel() != null ? course.getLevel() : "Débutant";
        String color;
        switch (level) {
            case "Débutant"      -> color = "#4ade80"; 
            case "Intermédiaire" -> color = "#fbbf24"; 
            case "Avancé"        -> color = "#f87171"; 
            default              -> color = "#94a3b8"; 
        }
        Pane accent = new Pane();
        accent.setPrefHeight(6);
        accent.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20 20 0 0;");

        // Internal Content Container
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // 2. Badge & Metadata Row
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);
        Label lblLevelBadge = new Label(level.toUpperCase());
        lblLevelBadge.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
                               "-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-weight: 900; " +
                               "-fx-font-size: 10; -fx-border-color: " + color + "44; -fx-border-radius: 6;");
        topRow.getChildren().add(lblLevelBadge);

        // 3. Title Section
        VBox titleArea = new VBox(5);
        Text txtTitle = new Text(course.getTitle());
        txtTitle.getStyleClass().add("gradient-text");
        txtTitle.setStyle("-fx-font-size: 22; -fx-font-weight: 900;");
        
        String catName = course.getCategory() == null || course.getCategory().isEmpty() ? "Général" : course.getCategory();
        Label lblCatMeta = new Label("📂 " + catName);
        lblCatMeta.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13;");
        titleArea.getChildren().addAll(txtTitle, lblCatMeta);

        // 4. Description Label
        Label lblDesc = new Label(course.getDescription() != null ? course.getDescription() : "Aucune description de cours.");
        lblDesc.setWrapText(true);
        lblDesc.setMinHeight(60); lblDesc.setMaxHeight(60);
        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-line-spacing: 2;");

        Separator sep = new Separator();
        sep.setOpacity(0.05);

        // 5. Footer
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        int modCount = 0;
        try { modCount = moduleService.countByCourse(course.getId()); } catch (Exception ignored) {}
        Label lblModsBadge = new Label("📚 " + modCount + " Chapitre(s)");
        lblModsBadge.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: #cbd5e1; " +
                              "-fx-background-radius: 8; -fx-padding: 6 12; -fx-font-weight: bold; -fx-font-size: 11; " +
                              "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 8;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(8);
        
        Button btnShow = createIconButton("👁", "#60a5fa");
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

        Button btnEdit = createIconButton("✏", "#4ade80");
        btnEdit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/course/editCourse.fxml"));
                Parent root = loader.load();
                EditCourseController controller = loader.getController();
                controller.setCourse(course);
                Stage stage = (Stage) btnEdit.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                showFlash("Erreur de navigation", false);
            }
        });

        Button btnDelete = createIconButton("🗑", "#f87171");
        btnDelete.setOnAction(e -> confirmDelete(course));
        
        actions.getChildren().addAll(btnShow, btnEdit, btnDelete);
        footer.getChildren().addAll(lblModsBadge, spacer, actions);

        content.getChildren().addAll(topRow, titleArea, lblDesc, sep, footer);
        card.getChildren().addAll(accent, content);
        
        addHoverEffect(card);
        card.setOnMouseClicked(e -> {
            if(e.getClickCount() == 2) btnShow.getOnAction().handle(new ActionEvent());
        });
        
        return card;
    }

    private Button createIconButton(String icon, String color) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: " + color + "; " +
                     "-fx-background-radius: 10; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand; -fx-font-size: 14;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("rgba(255,255,255,0.03)", "rgba(255,255,255,0.08)")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("rgba(255,255,255,0.08)", "rgba(255,255,255,0.03)")));
        return btn;
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
