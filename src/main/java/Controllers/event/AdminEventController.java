package Controllers.event;

import Controllers.user.admin.SideBarController;
import Models.Event;
import Models.Location;
import Services.EventService;
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
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class AdminEventController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;
    @FXML private Label lblCount;
    @FXML private SideBarController sideBarController;
    @FXML private TextField txtSearch;
    @FXML private HBox flashBox;
    @FXML private Label flashLabel;

    private final EventService eventService = new EventService();
    private final LocationService locationService = new LocationService();
    
    private ObservableList<Event> masterList = FXCollections.observableArrayList();
    private FilteredList<Event> filteredList;
    private List<Location> allLocations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("events");
        }
        setupFilters();
        loadLocations();
        loadData();
    }

    private void setupFilters() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void loadLocations() {
        try {
            allLocations = locationService.recuperer();
        } catch (SQLDataException e) {
            System.err.println("Failed to load locations: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            masterList.setAll(eventService.recuperer());
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
            lblCount.setText("0 événement trouvé");
            return;
        }

        emptyState.setVisible(false); emptyState.setManaged(false);

        for (Event event : filteredList) {
            cardsContainer.getChildren().add(createEventCard(event));
        }
        
        lblCount.setText(filteredList.size() + " événement" + (filteredList.size() > 1 ? "s" : "") + " trouvé" + (filteredList.size() > 1 ? "s" : ""));
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

    private Node createEventCard(Event event) {
        VBox card = new VBox(0); 
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(320); card.setMinWidth(320);
        card.setPadding(Insets.EMPTY); 
        card.setStyle("-fx-overflow: hidden; -fx-cursor: hand;");

        Pane accent = new Pane();
        accent.setPrefHeight(6);
        accent.setStyle("-fx-background-color: #8b5cf6; -fx-background-radius: 20 20 0 0;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Text txtTitle = new Text(event.getTitle());
        txtTitle.getStyleClass().add("gradient-text");
        txtTitle.setStyle("-fx-font-size: 22; -fx-font-weight: 900;");

        Label lblDate = new Label("📅 " + event.getEventDate());
        lblDate.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13;");
        
        Label lblTime = new Label("⏰ " + event.getStartTime() + " - " + event.getEndTime());
        lblTime.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");

        String locName = "Lieu Inconnu";
        if (allLocations != null) {
            for (Location loc : allLocations) {
                if (loc.getId() == event.getLocationId()) {
                    locName = loc.getName();
                    break;
                }
            }
        }
        Label lblLocation = new Label("📍 " + locName);
        lblLocation.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");

        Separator sep = new Separator();
        sep.setOpacity(0.05);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        
        Button btnEdit = createIconButton("✏", "#4ade80");
        btnEdit.setOnAction(e -> openEditDialog(event));

        Button btnDelete = createIconButton("🗑", "#f87171");
        btnDelete.setOnAction(e -> confirmDelete(event));
        
        actions.getChildren().addAll(btnEdit, btnDelete);
        footer.getChildren().addAll(spacer, actions);

        content.getChildren().addAll(txtTitle, lblDate, lblTime, lblLocation, sep, footer);
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

        filteredList.setPredicate(event -> {
            boolean matchSearch = search.isEmpty() ||
                    (event.getTitle() != null && event.getTitle().toLowerCase().contains(search)) ||
                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(search));
            return matchSearch;
        });
        renderCards();
    }

    @FXML private void handleRefresh(ActionEvent event) { loadData(); showFlash("Données synchronisées", true); }
    @FXML private void handleReset(ActionEvent event) { txtSearch.clear(); }
    @FXML private void handleAdd(ActionEvent event) { openEditDialog(null); }

    public void openEditDialog(Event event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(event == null ? "NOUVEL ÉVÉNEMENT" : "MODIFIER ÉVÉNEMENT");
        
        VBox layout = new VBox(15);
        layout.setStyle("-fx-padding: 40; -fx-background-color: #0f172a; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1;");

        Label titleHeader = new Label(event == null ? "AJOUTER UN ÉVÉNEMENT" : "ÉDITER L'ÉVÉNEMENT");
        titleHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 800;");

        TextField fTitle = createEliteField(event != null ? event.getTitle() : "", "Titre de l'événement...");
        TextArea fDesc = new TextArea(event != null ? event.getDescription() : "");
        fDesc.setPromptText("Description...");
        fDesc.setStyle("-fx-control-inner-background: #1e293b; -fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-pref-height: 80;");

        DatePicker dpDate = new DatePicker(event != null ? event.getEventDate() : null);
        dpDate.setPromptText("Date...");
        dpDate.setMaxWidth(Double.MAX_VALUE);
        dpDate.setStyle(fieldStyle());

        ObservableList<String> times = FXCollections.observableArrayList();
        for(int h=8; h<=22; h++) {
            for(int m=0; m<60; m+=15) {
                times.add(String.format("%02d:%02d", h, m));
            }
        }
        
        ComboBox<String> fStart = new ComboBox<>(times);
        fStart.setEditable(true);
        fStart.setPromptText("Heure début...");
        fStart.setValue(event != null ? event.getStartTime().toString() : "08:00");
        fStart.setMaxWidth(Double.MAX_VALUE);
        fStart.setStyle(fieldStyle());

        ComboBox<String> fEnd = new ComboBox<>(times);
        fEnd.setEditable(true);
        fEnd.setPromptText("Heure fin...");
        fEnd.setValue(event != null ? event.getEndTime().toString() : "10:00");
        fEnd.setMaxWidth(Double.MAX_VALUE);
        fEnd.setStyle(fieldStyle());

        HBox timeBox = new HBox(15, fStart, fEnd);

        ComboBox<Location> cbLocation = new ComboBox<>();
        if (allLocations != null) {
            cbLocation.setItems(FXCollections.observableArrayList(allLocations));
            cbLocation.setConverter(new StringConverter<Location>() {
                @Override public String toString(Location object) { return object == null ? "" : object.getName(); }
                @Override public Location fromString(String string) { return null; }
            });
            if (event != null) {
                for (Location l : allLocations) {
                    if (l.getId() == event.getLocationId()) {
                        cbLocation.setValue(l);
                        break;
                    }
                }
            }
        }
        cbLocation.setMaxWidth(Double.MAX_VALUE);
        cbLocation.setStyle(fieldStyle());
        cbLocation.setPromptText("Sélectionner un lieu...");

        TextField fImage = createEliteField(event != null && event.getImage() != null ? event.getImage() : "", "URL de l'image (optionnel)...");
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

        Button btnSave = new Button(event == null ? "CRÉER L'ÉVÉNEMENT" : "METTRE À JOUR");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #1E88E5, #43A047); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
        
        btnSave.setOnAction(e -> {
            if (fTitle.getText().isBlank() || dpDate.getValue() == null || fStart.getValue() == null || fEnd.getValue() == null || cbLocation.getValue() == null) {
                showFlash("Veuillez remplir tous les champs", false);
                return;
            }
            
            try {
                LocalTime tStart = LocalTime.parse(fStart.getValue().toString());
                LocalTime tEnd = LocalTime.parse(fEnd.getValue().toString());
                
                Event evt = event != null ? event : new Event();
                evt.setTitle(fTitle.getText());
                evt.setDescription(fDesc.getText());
                evt.setEventDate(dpDate.getValue());
                evt.setStartTime(tStart);
                evt.setEndTime(tEnd);
                evt.setLocationId(cbLocation.getValue().getId());
                evt.setImage(fImage.getText());
                
                if (event == null) {
                    evt.setAverageRating(0.0);
                    eventService.ajouter(evt);
                } else {
                    eventService.modifier(evt);
                }
                
                dialog.close(); 
                loadData();
                showFlash("Événement mis à jour", true);
            } catch (Exception ex) { 
                showFlash("Erreur de format d'heure (essayez HH:mm)", false); 
            }
        });

        layout.getChildren().addAll(titleHeader, fTitle, fDesc, dpDate, timeBox, cbLocation, imageBox, btnSave);
        Scene scene = new Scene(layout, 450, 750);
        try {
            scene.getStylesheets().add(getClass().getResource("/FrontOffice/user/auth/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public void confirmDelete(Event event) {
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
        
        Label message = new Label("Voulez-vous vraiment supprimer\n« " + event.getTitle() + " » ?");
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
                eventService.supprimer(event);
                loadData();
                showFlash("Événement supprimé définitivement", true);
            } catch (Exception ex) {
                showFlash("Impossible de supprimer cet événement.", false);
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
