package Controllers.course;

import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
import Utils.AssetLoader;
import Utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class FrontModuleShowController implements Initializable {

    @FXML private Label courseTitleSidebar;
    @FXML private Label progressPercent;
    @FXML private ProgressBar courseProgressBar;
    @FXML private VBox modulesContainer;
    
    @FXML private Label breadcrumbCourse;
    @FXML private Label breadcrumbModule;
    
    @FXML private VBox videoContainer;
    @FXML private ImageView moduleImageView;
    @FXML private Label moduleTypeBadge;
    @FXML private Label moduleTitle;
    @FXML private Label moduleMeta;
    @FXML private Label moduleDescription;
    @FXML private Label moduleContent;
    
    @FXML private VBox docReaderSection;
    @FXML private Label fileNameLabel;
    @FXML private Label fileExtLabel;
    
    @FXML private VBox prevModuleBtn;
    @FXML private Label prevModuleTitle;
    @FXML private VBox nextModuleBtn;
    @FXML private Label nextModuleTitle;
    @FXML private Button finishCourseBtn;
    
    @FXML private VBox chatWindow;
    @FXML private VBox chatMessages;
    @FXML private TextField txtChatInput;

    private Module currentModule;
    private Course currentCourse;
    private final CourseService courseService = new CourseService();
    private final ModuleService moduleService = new ModuleService();
    private final Services.ChatbotService chatbotService = new Services.ChatbotService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setData(Module module) {
        this.currentModule = module;
        this.currentCourse = courseService.recupererParId(module.getCourseId());
        
        updateUI();
        loadSidebar();
        updateNavigationButtons();
    }

    private void updateUI() {
        if (currentModule == null) return;

        moduleTitle.setText(currentModule.getTitle());
        breadcrumbModule.setText(currentModule.getTitle());
        
        if (currentCourse != null) {
            courseTitleSidebar.setText(currentCourse.getTitle());
            breadcrumbCourse.setText(currentCourse.getTitle());
        }

        moduleTypeBadge.setText(currentModule.getType() != null ? currentModule.getType().toUpperCase() : "MODULE");
        
        String dateStr = currentModule.getCreatedAt() != null 
            ? currentModule.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) 
            : "N/A";
        
        String level = currentModule.getLevel() != null ? currentModule.getLevel() : (currentCourse != null ? currentCourse.getLevel() : "Tous niveaux");
        moduleMeta.setText(level + " • Ajouté le " + dateStr);
        
        moduleDescription.setText(currentModule.getDescription() != null ? currentModule.getDescription() : "");
        moduleContent.setText(currentModule.getContent() != null ? currentModule.getContent() : "Pas de contenu textuel pour ce module.");

        if (currentModule.getImage() != null) {
            Image img = AssetLoader.loadCourseImage(currentModule.getImage());
            if (img != null) moduleImageView.setImage(img);
        }

        videoContainer.setVisible("video".equalsIgnoreCase(currentModule.getType()));
        videoContainer.setManaged("video".equalsIgnoreCase(currentModule.getType()));

        if (currentModule.getDocument() != null && !currentModule.getDocument().isEmpty()) {
            docReaderSection.setVisible(true);
            docReaderSection.setManaged(true);
            fileNameLabel.setText(currentModule.getDocument());
            String ext = "";
            if (currentModule.getDocument().contains(".")) {
                ext = currentModule.getDocument().substring(currentModule.getDocument().lastIndexOf(".") + 1).toUpperCase();
            }
            fileExtLabel.setText(ext);
        } else {
            docReaderSection.setVisible(false);
            docReaderSection.setManaged(false);
        }
    }

    private void loadSidebar() {
        if (currentCourse == null) return;
        
        try {
            List<Module> allModules = moduleService.getByCourse(currentCourse.getId());
            modulesContainer.getChildren().clear();
            
            int index = 1;
            for (Module m : allModules) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/moduleNavItem.fxml"));
                Node item = loader.load();
                
                Label idxLabel = (Label) item.lookup("#indexLabel");
                Label titleLabel = (Label) item.lookup("#titleLabel");
                Label typeLabel = (Label) item.lookup("#typeLabel");
                VBox wrapper = (VBox) item.lookup("#itemWrapper");
                
                idxLabel.setText(String.valueOf(index++));
                titleLabel.setText(m.getTitle());
                typeLabel.setText(m.getType().toUpperCase());
                
                if (m.getId() == currentModule.getId()) {
                    wrapper.getStyleClass().add("nav-item-active");
                }
                
                item.setOnMouseClicked(e -> setData(m));
                modulesContainer.getChildren().add(item);
            }
            
            double progress = allModules.isEmpty() ? 0 : (double) 1 / allModules.size();
            courseProgressBar.setProgress(progress);
            progressPercent.setText((int)(progress * 100) + "% Complété");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNavigationButtons() {
        try {
            List<Module> modules = moduleService.getByCourse(currentModule.getCourseId());
            int currentIndex = -1;
            for (int i = 0; i < modules.size(); i++) {
                if (modules.get(i).getId() == currentModule.getId()) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex > 0) {
                Module prev = modules.get(currentIndex - 1);
                prevModuleBtn.setVisible(true);
                prevModuleTitle.setText(prev.getTitle());
                prevModuleBtn.setOnMouseClicked(e -> setData(prev));
            } else {
                prevModuleBtn.setVisible(false);
            }
            
            if (currentIndex < modules.size() - 1) {
                Module next = modules.get(currentIndex + 1);
                nextModuleBtn.setVisible(true);
                nextModuleTitle.setText(next.getTitle());
                nextModuleBtn.setOnMouseClicked(e -> setData(next));
                finishCourseBtn.setVisible(false);
            } else {
                nextModuleBtn.setVisible(false);
                finishCourseBtn.setVisible(true);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenDocument() {
        if (currentModule == null || currentModule.getDocument() == null || currentModule.getDocument().isEmpty()) return;
        
        try {
            java.io.File file;
            if (currentModule.getDocument().contains(":\\")) {
                file = new java.io.File(currentModule.getDocument());
            } else {
                file = new java.io.File("uploads/modules/" + currentModule.getDocument());
            }
            
            if (file.exists()) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    System.err.println("Le bureau n'est pas supporté sur ce système.");
                }
            } else {
                System.err.println("Fichier non trouvé : " + file.getAbsolutePath());
            }
        } catch (java.io.IOException e) {
            System.err.println("Erreur ouverture document : " + e.getMessage());
        }
    }

    @FXML
    private void goToHome(javafx.scene.input.MouseEvent event) {
        navigateTo(event, "/FrontOffice/user/home/homeUser.fxml", "Accueil - SkillPath");
    }

    @FXML
    private void handleFinishCourse() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Félicitations !");
        alert.setHeaderText("Cours terminé");
        alert.setContentText("Vous avez terminé le cours " + currentCourse.getTitle());
        alert.show();
    }

    private void navigateTo(javafx.event.Event event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleChat(ActionEvent event) {
        chatWindow.setVisible(!chatWindow.isVisible());
    }

    @FXML
    private void sendChatMessage() {
        String message = txtChatInput.getText().trim();
        if (message.isEmpty()) return;

        addMessageBubble(message, true);
        txtChatInput.clear();

        Label typingLabel = new Label("L'IA réfléchit...");
        typingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
        chatMessages.getChildren().add(typingLabel);

        chatbotService.askQuestion(message).thenAccept(response -> {
            Platform.runLater(() -> {
                chatMessages.getChildren().remove(typingLabel);
                addMessageBubble(response, false);
            });
        });
    }

    private void addMessageBubble(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(300);
        
        VBox bubble = new VBox(label);
        bubble.setPadding(new Insets(12, 20, 12, 20));
        
        if (isUser) {
            bubble.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 20 20 0 20;");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
            HBox container = new HBox(bubble);
            container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            chatMessages.getChildren().add(container);
        } else {
            bubble.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 20 20 20 0; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20 20 20 0;");
            label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14;");
            HBox container = new HBox(bubble);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            chatMessages.getChildren().add(container);
        }
        
        Platform.runLater(() -> {
            if (chatMessages.getParent().getParent() instanceof ScrollPane) {
                ((ScrollPane) chatMessages.getParent().getParent()).setVvalue(1.0);
            }
        });
    }
}
