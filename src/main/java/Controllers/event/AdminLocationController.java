package Controllers.event;

import Controllers.user.admin.SideBarController;
import Models.Location;
import Services.LocationService;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.ResourceBundle;

public class AdminLocationController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;
    @FXML private Label lblCount;
    @FXML private SideBarController sideBarController;
    @FXML private TextField txtSearch;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;

    private final LocationService locationService = new LocationService();
    private ObservableList<Location> masterList = FXCollections.observableArrayList();
    private FilteredList<Location> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("locations");
        }
        setupFilters();
        loadData();
    }

    private void setupFilters() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void loadData() {
        try {
            masterList.setAll(locationService.recuperer());
            filteredList = new FilteredList<>(masterList, c -> true);
            renderCards();
        } catch (SQLDataException e) {
            showFlash("Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private void renderCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        
        if (filteredList == null || filteredList.isEmpty()) {
            emptyState.setVisible(true); emptyState.setManaged(true);
            lblCount.setText("0 lieu trouvé");
            return;
        }

        emptyState.setVisible(false); emptyState.setManaged(false);

        for (Location loc : filteredList) {
            cardsContainer.getChildren().add(createLocationCard(loc));
        }
        
        lblCount.setText(filteredList.size() + " lieu" + (filteredList.size() > 1 ? "x" : "") + " trouvé" + (filteredList.size() > 1 ? "s" : ""));
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

    private Node createLocationCard(Location loc) {
        VBox card = new VBox(0); 
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(320); card.setMinWidth(320);
        card.setPadding(Insets.EMPTY); 
        card.setStyle("-fx-overflow: hidden; -fx-cursor: hand;");

        Pane accent = new Pane();
        accent.setPrefHeight(6);
        accent.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 20 20 0 0;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Text txtTitle = new Text(loc.getName());
        txtTitle.getStyleClass().add("gradient-text");
        txtTitle.setStyle("-fx-font-size: 22; -fx-font-weight: 900;");

        Label lblBuilding = new Label("🏢 " + loc.getBuilding());
        lblBuilding.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13;");
        
        Label lblRoom = new Label("🚪 Salle: " + loc.getRoomNumber());
        lblRoom.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");

        Label lblCapacity = new Label("👥 Capacité: " + loc.getMaxCapacity());
        lblCapacity.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");

        Separator sep = new Separator();
        sep.setOpacity(0.05);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        
        Button btnEdit = createIconButton("✏", "#4ade80");
        btnEdit.setOnAction(e -> openEditDialog(loc));

        Button btnDelete = createIconButton("🗑", "#f87171");
        btnDelete.setOnAction(e -> confirmDelete(loc));
        
        actions.getChildren().addAll(btnEdit, btnDelete);
        footer.getChildren().addAll(spacer, actions);

        content.getChildren().addAll(txtTitle, lblBuilding, lblRoom, lblCapacity, sep, footer);
        card.getChildren().addAll(accent, content);
        
        addHoverEffect(card);
        card.setOnMouseClicked(e -> {
            if(e.getClickCount() == 2) btnEdit.getOnAction().handle(new ActionEvent());
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

        filteredList.setPredicate(loc -> {
            boolean matchSearch = search.isEmpty() ||
                    (loc.getName() != null && loc.getName().toLowerCase().contains(search)) ||
                    (loc.getBuilding() != null && loc.getBuilding().toLowerCase().contains(search));
            return matchSearch;
        });
        renderCards();
    }

    @FXML private void handleRefresh(ActionEvent event) { loadData(); showFlash("Données synchronisées", true); }
    @FXML private void handleReset(ActionEvent event) { txtSearch.clear(); }
    @FXML private void handleAdd(ActionEvent event) { openEditDialog(null); }

    public void openEditDialog(Location location) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(location == null ? "NOUVEAU LIEU" : "MODIFIER LIEU");
        
        VBox layout = new VBox(20);
        layout.setStyle("-fx-padding: 40; -fx-background-color: #0f172a; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1;");

        Label titleHeader = new Label(location == null ? "AJOUTER UN LIEU" : "ÉDITER LE LIEU");
        titleHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 800;");

        TextField fName = createEliteField(location != null ? location.getName() : "", "Nom du lieu...");
        TextField fBuilding = createEliteField(location != null ? location.getBuilding() : "", "Bâtiment...");
        TextField fRoom = createEliteField(location != null ? location.getRoomNumber() : "", "Salle...");
        TextField fCapacity = createEliteField(location != null ? String.valueOf(location.getMaxCapacity()) : "", "Capacité maximale...");

        TextField fImage = createEliteField(location != null && location.getImage() != null ? location.getImage() : "", "URL de l'image...");
        HBox.setHgrow(fImage, Priority.ALWAYS);
        Button btnUpload = new Button("📁");
        btnUpload.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 10; -fx-cursor: hand;");
        btnUpload.setOnAction(ev -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fileChooser.showOpenDialog(dialog);
            if (file != null) {
                fImage.setText(file.toURI().toString());
            }
        });
        HBox imageBox = new HBox(10, fImage, btnUpload);

        Button btnSave = new Button(location == null ? "CRÉER LE LIEU" : "METTRE À JOUR");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #1E88E5, #43A047); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
        
        btnSave.setOnAction(e -> {
            if (fName.getText().isBlank() || fCapacity.getText().isBlank()) return;
            
            int cap = 0;
            try { cap = Integer.parseInt(fCapacity.getText()); } catch (Exception ignored) { }
            
            Location l = location != null ? location : new Location();
            l.setName(fName.getText()); 
            l.setBuilding(fBuilding.getText()); 
            l.setRoomNumber(fRoom.getText()); 
            l.setMaxCapacity(cap);
            l.setImage(fImage.getText());
            
            try {
                if (location == null) locationService.ajouter(l); else locationService.modifier(l);
                dialog.close(); loadData();
                showFlash("Lieu mis à jour", true);
            } catch (Exception ex) { showFlash("Erreur", false); }
        });

        layout.getChildren().addAll(titleHeader, fName, fBuilding, fRoom, fCapacity, imageBox, btnSave);
        Scene scene = new Scene(layout, 400, 550);
        try {
            scene.getStylesheets().add(getClass().getResource("/FrontOffice/user/auth/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public void confirmDelete(Location location) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; -fx-border-color: rgba(248,113,113,0.3); -fx-border-width: 2; -fx-background-radius: 20; -fx-border-radius: 20;");

        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 50; -fx-text-fill: #f87171;");

        VBox textContent = new VBox(10);
        textContent.setAlignment(Pos.CENTER);
        Label title = new Label("CONFIRMATION");
        title.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 900; -fx-letter-spacing: 2; -fx-font-size: 14;");
        
        Label message = new Label("Voulez-vous vraiment supprimer\n« " + location.getName() + " » ?");
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
                locationService.supprimer(location);
                loadData();
                showFlash("Lieu supprimé définitivement", true);
            } catch (Exception ex) {
                showFlash("Impossible de supprimer ce lieu.", false);
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
        if(flashLabel == null || flashBox == null) return;
        flashLabel.setText(message);
        flashBox.setStyle("-fx-background-color: " + (success ? "rgba(74,222,128,0.1)" : "rgba(248,113,113,0.1)") + "; -fx-border-color: " + (success ? "#4ade80" : "#f87171") + "; -fx-border-radius: 10;");
        flashLabel.setStyle("-fx-text-fill: " + (success ? "#4ade80" : "#f87171") + "; -fx-font-weight: bold;");
        flashBox.setVisible(true); flashBox.setManaged(true);
        new Thread(() -> { try { Thread.sleep(3000); } catch (Exception ignored) {} javafx.application.Platform.runLater(() -> { flashBox.setVisible(false); flashBox.setManaged(false); }); }).start();
    }
}
