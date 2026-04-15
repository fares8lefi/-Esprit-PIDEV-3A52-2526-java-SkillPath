package Controllers.module;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Models.Module;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.List;
import java.util.ResourceBundle;

public class ModuleListController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboFilterCourse;
    @FXML private ComboBox<String> comboFilterType;
    @FXML private FlowPane cardsContainer;
    @FXML private VBox flashBox;
    @FXML private Label flashLabel;
    @FXML private SideBarController sideBarController;

    private final ModuleService moduleService = new ModuleService();
    private final CourseService courseService = new CourseService();
    private ObservableList<Module> masterList = FXCollections.observableArrayList();
    private FilteredList<Module> filteredList;
    private List<Course> allCourses;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("modules");
        }
        setupFilters();
        loadData();
    }

    private void setupFilters() {
        try {
            allCourses = courseService.recuperer();
            ObservableList<String> courseNames = FXCollections.observableArrayList("Tous les cours");
            allCourses.forEach(c -> courseNames.add(c.getTitle()));
            comboFilterCourse.setItems(courseNames);
            comboFilterCourse.setValue("Tous les cours");
        } catch (SQLDataException e) {
            System.err.println("Erreur chargement cours pour filtres");
        }

        comboFilterType.setItems(FXCollections.observableArrayList("Tous les types", "Leçon", "Examen", "Quiz", "TP"));
        comboFilterType.setValue("Tous les types");

        // Force white text on selected item displayed in the ComboBox button
        styleComboBox(comboFilterCourse);
        styleComboBox(comboFilterType);

        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        comboFilterCourse.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        comboFilterType.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void styleComboBox(ComboBox<String> combo) {
        // Style the button cell (the displayed selected value)
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        // Style each item cell in the dropdown
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
            masterList.setAll(moduleService.recuperer());
            filteredList = new FilteredList<>(masterList, m -> true);
            filteredList.addListener((javafx.collections.ListChangeListener<Module>) c -> renderCards());
            renderCards();
        } catch (SQLDataException e) {
            showFlash("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private void applyFilters() {
        if (filteredList == null) return;

        String search = txtSearch.getText().toLowerCase().trim();
        String typeFilter = comboFilterType.getValue();
        String courseFilter = comboFilterCourse.getValue();

        filteredList.setPredicate(module -> {
            boolean matchSearch = search.isEmpty() || 
                                 module.getTitle().toLowerCase().contains(search) || 
                                 (module.getDescription() != null && module.getDescription().toLowerCase().contains(search));
            
            boolean matchType = typeFilter.equals("Tous les types") || module.getType().equals(typeFilter);
            
            boolean matchCourse = courseFilter.equals("Tous les cours");
            if (!matchCourse) {
                // Find course name by ID
                String courseName = allCourses.stream()
                        .filter(c -> c.getId() == module.getCourseId())
                        .map(Course::getTitle)
                        .findFirst().orElse("");
                matchCourse = courseName.equals(courseFilter);
            }

            return matchSearch && matchType && matchCourse;
        });
        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        for (Module module : filteredList) {
            cardsContainer.getChildren().add(createModuleCard(module));
        }
    }

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

    private Node createModuleCard(Module module) {
        VBox card = new VBox(0); // Zero spacing for the header strip
        card.getStyleClass().add("glass-card");
        card.setPrefWidth(320); card.setMaxWidth(320); card.setMinWidth(320);
        card.setPadding(Insets.EMPTY); 
        card.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;");

        // 1. Visual Accent Header (Strip)
        String type = module.getType() != null ? module.getType() : "Leçon";
        String color;
        switch (type) {
            case "Leçon"  -> color = "#60a5fa"; 
            case "Quiz"   -> color = "#fbbf24"; 
            case "TP"     -> color = "#22d3ee"; 
            case "Examen" -> color = "#f87171"; 
            default       -> color = "#94a3b8"; 
        }
        Pane accent = new Pane();
        accent.setPrefHeight(10); // Thicker strip for more impact
        accent.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20 20 0 0;");

        // Internal Content Container
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // 2. Badge Row (Contained)
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);
        Label lblTypeBadge = new Label(type.toUpperCase());
        lblTypeBadge.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
                              "-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-weight: 900; " +
                              "-fx-font-size: 10; -fx-border-color: " + color + "44; -fx-border-radius: 6;");
        topRow.getChildren().add(lblTypeBadge);

        // 3. Title Section
        VBox titleArea = new VBox(8);
        Text txtTitle = new Text(module.getTitle());
        // Use the accent color for the title to match the "Course" aesthetic
        txtTitle.setStyle("-fx-fill: " + color + "; -fx-font-size: 20; -fx-font-weight: 900; " +
                          "-fx-effect: dropshadow(gaussian, " + color + "33, 10, 0, 0, 0);");
        txtTitle.setWrappingWidth(280); 
        
        String courseName = allCourses.stream()
                .filter(c -> c.getId() == module.getCourseId())
                .map(Course::getTitle)
                .findFirst().orElse("Sans cours");
        Label lblCourseMeta = new Label("📂 " + courseName);
        lblCourseMeta.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12;");
        titleArea.getChildren().addAll(txtTitle, lblCourseMeta);

        // 4. Description Label
        Label lblDesc = new Label(module.getDescription() != null ? module.getDescription() : "Aucune description fournie.");
        lblDesc.setWrapText(true);
        lblDesc.setMinHeight(45); lblDesc.setMaxHeight(45);
        lblDesc.setPrefWidth(280);
        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-line-spacing: 1.5;");

        Separator sep = new Separator();
        sep.setOpacity(0.05);

        // 5. Footer (Actions & Meta)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        String dateStr = module.getScheduledAt() != null ? module.getScheduledAt().toLocalDate().toString() : "Libre";
        Label lblDateBadge = new Label("🗓 " + dateStr);
        lblDateBadge.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: #cbd5e1; " +
                              "-fx-background-radius: 8; -fx-padding: 6 10; -fx-font-weight: bold; -fx-font-size: 10; " +
                              "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 8;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnShow = createIconButton("👁", "#60a5fa");
        btnShow.setOnAction(e -> navigateToDetails(module));

        Button btnEdit = createIconButton("✏", "#4ade80");
        btnEdit.setOnAction(e -> navigateToEdit(module));

        Button btnDelete = createIconButton("🗑", "#f87171");
        btnDelete.setOnAction(e -> confirmDelete(module));
        
        actions.getChildren().addAll(btnShow, btnEdit, btnDelete);
        footer.getChildren().addAll(lblDateBadge, spacer, actions);

        content.getChildren().addAll(topRow, titleArea, lblDesc, sep, footer);
        card.getChildren().addAll(accent, content);
        
        addHoverEffect(card);
        card.setOnMouseClicked(e -> { if(e.getClickCount() == 2) navigateToDetails(module); });
        
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

    @FXML private void handleRefresh(ActionEvent event) { loadData(); showFlash("Modules synchronisés", true); }
    @FXML private void handleReset(ActionEvent event) { txtSearch.clear(); comboFilterCourse.setValue("Tous les cours"); comboFilterType.setValue("Tous les types"); }
    
    @FXML private void handleAdd(ActionEvent event) { navigateTo("/BackOffice/module/addModule.fxml"); }

    private void navigateToDetails(Module module) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/module/moduleDetails.fxml"));
            Parent root = loader.load();
            ModuleDetailsController controller = loader.getController();
            controller.setModule(module);
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showFlash("Impossible d'ouvrir les détails", false);
        }
    }

    private void navigateToEdit(Module module) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/module/editModule.fxml"));
            Parent root = loader.load();
            EditModuleController emc = loader.getController();
            emc.setModule(module);
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showFlash("Impossible d'ouvrir la modification", false);
        }
    }

    private void confirmDelete(Module module) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; -fx-border-color: rgba(248,113,113,0.3); -fx-border-width: 2; -fx-background-radius: 20; -fx-border-radius: 20;");

        Label icon = new Label("🗑"); icon.setStyle("-fx-font-size: 50; -fx-text-fill: #f87171;");
        Label msg = new Label("Supprimer ce module ?\n« " + module.getTitle() + " »");
        msg.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold; -fx-text-alignment: center;");
        
        HBox actions = new HBox(15); actions.setAlignment(Pos.CENTER);
        Button cnl = new Button("ANNULER"); cnl.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-padding: 10 25; -fx-background-radius: 10;");
        cnl.setOnAction(e -> dialog.close());
        Button ok = new Button("SUPPRIMER"); ok.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-padding: 10 25; -fx-background-radius: 10;");
        ok.setOnAction(e -> {
            try { moduleService.supprimer(module); loadData(); showFlash("Module supprimé", true); }
            catch (Exception ex) { showFlash("Erreur suppression", false); }
            finally { dialog.close(); }
        });
        actions.getChildren().addAll(cnl, ok);
        root.getChildren().addAll(icon, msg, actions);
        dialog.setScene(new Scene(root)); dialog.getScene().setFill(null); dialog.showAndWait();
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { showFlash("Navigation impossible", false); }
    }

    private void showFlash(String message, boolean success) {
        flashLabel.setText(message);
        flashBox.setStyle("-fx-background-color: " + (success ? "rgba(74,222,128,0.1)" : "rgba(248,113,113,0.1)") + "; -fx-border-color: " + (success ? "#4ade80" : "#f87171") + "; -fx-border-radius: 10;");
        flashLabel.setStyle("-fx-text-fill: " + (success ? "#4ade80" : "#f87171") + "; -fx-font-weight: bold;");
        flashBox.setVisible(true); flashBox.setManaged(true);
        new Thread(() -> { try { Thread.sleep(3000); } catch (Exception ignored) {} javafx.application.Platform.runLater(() -> { flashBox.setVisible(false); flashBox.setManaged(false); }); }).start();
    }
}
