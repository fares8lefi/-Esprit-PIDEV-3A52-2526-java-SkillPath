package Controllers.course;

import java.sql.SQLDataException;

import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
import Services.PDFService;
import Services.ProgressService;
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
    @FXML private HBox finishActionsBox;
    @FXML private Button topDownloadCertBtn;
    @FXML private HBox aiPredictionBox;
    @FXML private Label aiPredictionLabel;
    private final PDFService pdfService = new PDFService();
    
    @FXML private VBox chatWindow;
    @FXML private VBox chatMessages;
    @FXML private TextField txtChatInput;

    private Module currentModule;
    private Course currentCourse;
    private final CourseService courseService = new CourseService();
    private final ModuleService moduleService = new ModuleService();
    private final Services.ChatbotService chatbotService = new Services.ChatbotService();
    private final Services.PredictionService predictionService = new Services.PredictionService();
    private final Services.ProgressService progressService = new Services.ProgressService();
    private final Services.CertificateService certificateService = new Services.CertificateService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setData(Module module) {
        this.currentModule = module;
        this.currentCourse = courseService.recupererParId(module.getCourseId());
        
        updateUI();
        loadSidebar();
        updateNavigationButtons();
        loadAIPrediction();
    }

    private void loadAIPrediction() {
        if (Session.getInstance().getCurrentUser() != null && currentCourse != null) {
            aiPredictionLabel.setText("Calcul en cours...");
            // Trouver le nombre total de modules pour la prédiction
            try {
                List<Module> allModules = moduleService.getByCourse(currentCourse.getId());
                predictionService.predictSuccess(Session.getInstance().getCurrentUser(), currentCourse, allModules.size())
                    .thenAccept(score -> {
                        Platform.runLater(() -> {
                            if (score >= 0) {
                                aiPredictionLabel.setText(String.format("%.1f%% chances de réussite", score));
                                if (score > 75) {
                                    aiPredictionBox.setStyle("-fx-background-color: rgba(52, 211, 153, 0.1); -fx-background-radius: 8; -fx-padding: 8; -fx-border-color: rgba(52, 211, 153, 0.3);");
                                    aiPredictionLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 12;");
                                } else if (score < 40) {
                                    aiPredictionBox.setStyle("-fx-background-color: rgba(248, 113, 113, 0.1); -fx-background-radius: 8; -fx-padding: 8; -fx-border-color: rgba(248, 113, 113, 0.3);");
                                    aiPredictionLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 12;");
                                }
                            } else {
                                aiPredictionLabel.setText("Prédiction indisponible");
                            }
                        });
                    });
            } catch (SQLDataException e) {
                System.err.println("Erreur chargement modules pour prédiction : " + e.getMessage());
                aiPredictionLabel.setText("Prédiction indisponible");
            }
        } else {
            aiPredictionBox.setVisible(false);
            aiPredictionBox.setManaged(false);
        }
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
            
            Models.UserCourseProgress progressObj = null;
            if (Session.getInstance().getCurrentUser() != null) {
                progressObj = progressService.getProgress(Session.getInstance().getCurrentUser().getId(), currentCourse.getId());
            }
            int completedModules = progressObj != null ? progressObj.getCompletedModules() : 0;
            double progress = allModules.isEmpty() ? 0 : (double) completedModules / allModules.size();
            
            courseProgressBar.setProgress(progress);
            progressPercent.setText((int)(progress * 100) + "% Complété (" + completedModules + "/" + allModules.size() + ")");
            
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

            // Previous button
            if (currentIndex > 0) {
                Module prev = modules.get(currentIndex - 1);
                prevModuleBtn.setVisible(true);
                prevModuleBtn.setManaged(true);
                prevModuleTitle.setText(prev.getTitle());
                prevModuleBtn.setOnMouseClicked(e -> setData(prev));
            } else {
                prevModuleBtn.setVisible(false);
                prevModuleBtn.setManaged(false);
            }

            boolean isLastModule = (currentIndex == modules.size() - 1);
            boolean isCurrentModuleDone = Session.getInstance().getCurrentUser() != null &&
                    progressService.isModuleCompleted(Session.getInstance().getCurrentUser().getId(), currentModule.getId());

            // 1. Bouton Téléchargement (Haut) - Visible si certificat obtenu
            boolean hasCert = Session.getInstance().getCurrentUser() != null &&
                    certificateService.hasCertificate(Session.getInstance().getCurrentUser().getId(), currentCourse.getId());
            topDownloadCertBtn.setVisible(hasCert);
            topDownloadCertBtn.setManaged(hasCert);

            // 2. Bouton Suivant (Bas) - Toujours visible si ce n'est pas le dernier module
            if (currentIndex < modules.size() - 1) {
                Module next = modules.get(currentIndex + 1);
                nextModuleBtn.setVisible(true);
                nextModuleBtn.setManaged(true);
                nextModuleTitle.setText(next.getTitle());
                nextModuleBtn.setOnMouseClicked(e -> setData(next));
                
                // On montre le bouton "Marquer terminé" si CE module n'est pas encore fait
                if (!isCurrentModuleDone) {
                    finishActionsBox.setVisible(true);
                    finishActionsBox.setManaged(true);
                    finishCourseBtn.setVisible(true);
                    finishCourseBtn.setManaged(true);
                    finishCourseBtn.setText("Marquer terminé et continuer →");
                    nextModuleForNavigation = next;
                } else {
                    finishActionsBox.setVisible(false);
                    finishActionsBox.setManaged(false);
                }
            } else {
                nextModuleBtn.setVisible(false);
                nextModuleBtn.setManaged(false);
                
                // Sur le dernier module, si pas encore fait, montrer "Terminer le cours"
                if (!isCurrentModuleDone) {
                    finishActionsBox.setVisible(true);
                    finishActionsBox.setManaged(true);
                    finishCourseBtn.setVisible(true);
                    finishCourseBtn.setManaged(true);
                    finishCourseBtn.setText("Terminer le cours ✓");
                } else {
                    finishActionsBox.setVisible(false);
                    finishActionsBox.setManaged(false);
                }
            }

        } catch (SQLDataException e) {
            System.err.println("Erreur updateNavigationButtons: " + e.getMessage());
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

    // Stores the next module to navigate to after marking current as done
    private Module nextModuleForNavigation = null;

    @FXML
    private void handleFinishCourse() {
        if (Session.getInstance().getCurrentUser() == null) {
            System.err.println("Utilisateur non connecté !");
            return;
        }

        try {
            List<Module> allModules = moduleService.getByCourse(currentCourse.getId());
            progressService.incrementProgress(
                    Session.getInstance().getCurrentUser().getId(),
                    currentCourse.getId(),
                    currentModule.getId(),
                    allModules.size());

            Models.UserCourseProgress progressObj = progressService.getProgress(
                    Session.getInstance().getCurrentUser().getId(), currentCourse.getId());

            if (progressObj != null && progressObj.isCompleted()) {
                // Course 100% done
                certificateService.generateCertificate(
                        Session.getInstance().getCurrentUser().getId(), currentCourse.getId());

                loadSidebar();
                updateNavigationButtons();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Félicitations !");
                alert.setHeaderText("🏆 Cours terminé à 100%");
                alert.setContentText("Vous avez terminé \"" + currentCourse.getTitle() +
                        "\" et obtenu votre certificat !");
                alert.show();

            } else if (nextModuleForNavigation != null) {
                // Mid-course: mark done then advance to next module
                loadSidebar();
                setData(nextModuleForNavigation);
                nextModuleForNavigation = null;

            } else {
                loadSidebar();
                updateNavigationButtons();
                System.out.println("Module marqué comme terminé !");
            }

        } catch (SQLDataException e) {
            System.err.println("Erreur lors de la finalisation du module : " + e.getMessage());
        }
    }

    @FXML
    private void handleDownloadCertificate() {
        if (Session.getInstance().getCurrentUser() == null || currentCourse == null) return;

        String path = pdfService.generateCertificate(Session.getInstance().getCurrentUser(), currentCourse);

        if (path != null) {
            try {
                java.io.File file = new java.io.File(path);
                if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText("Fichier introuvable");
                    alert.setContentText("Le certificat n'a pas pu être ouvert.");
                    alert.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Échec de génération");
            alert.setContentText("Une erreur est survenue lors de la création du PDF.");
            alert.show();
        }
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
