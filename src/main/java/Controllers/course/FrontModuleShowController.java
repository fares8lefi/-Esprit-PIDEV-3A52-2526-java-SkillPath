package Controllers.course;

import java.sql.SQLDataException;

import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
import Services.AIService;
import Services.CertificateService;
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
import javafx.scene.text.TextFlow;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
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
    @FXML private VBox aiPredictionBox;
    @FXML private Label aiPredictionLabel;
    @FXML private StackPane predictionCircleContainer;
    @FXML private Label predictionStatusLabel;
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
            predictionStatusLabel.setText("Analyse...");
            try {
                List<Module> allModules = moduleService.getByCourse(currentCourse.getId());
                predictionService.predictSuccess(Session.getInstance().getCurrentUser(), currentCourse, allModules.size())
                    .thenAccept(score -> {
                        Platform.runLater(() -> {
                            if (score >= 0) {
                                animatePredictionCircle(score);
                            } else {
                                aiPredictionLabel.setText("--%");
                                predictionStatusLabel.setText("Indisponible");
                            }
                        });
                    });
            } catch (SQLDataException e) {
                System.err.println("Erreur prédiction : " + e.getMessage());
                predictionStatusLabel.setText("Erreur");
            }
        } else {
            aiPredictionBox.setVisible(false);
            aiPredictionBox.setManaged(false);
        }
    }

    private void animatePredictionCircle(double targetScore) {
        // Nettoyer les anciens arcs
        predictionCircleContainer.getChildren().removeIf(node -> node instanceof Arc);

        // Créer l'arc de progression
        Arc arc = new Arc(0, 0, 40, 40, 90, 0);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStrokeWidth(8);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);

        // Détermination des couleurs et du statut
        Color startColor;
        Color endColor;
        String status;
        String borderColor;

        if (targetScore < 40) {
            startColor = Color.web("#f87171"); // Rouge clair
            endColor = Color.web("#ef4444");   // Rouge foncé
            status = "Faible";
            borderColor = "rgba(239, 68, 68, 0.4)";
        } else if (targetScore < 70) {
            startColor = Color.web("#fbbf24"); // Orange/Ambre
            endColor = Color.web("#f59e0b");
            status = "Moyen";
            borderColor = "rgba(245, 158, 11, 0.4)";
        } else {
            startColor = Color.web("#34d399"); // Émeraude/Vert
            endColor = Color.web("#10b981");
            status = "Élevé";
            borderColor = "rgba(16, 185, 129, 0.4)";
        }

        // Appliquer le dégradé
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, startColor),
                new Stop(1, endColor));
        arc.setStroke(gradient);

        // Ajouter l'arc au conteneur (en dessous du texte)
        predictionCircleContainer.getChildren().add(0, arc);
        
        // Mettre à jour les textes
        predictionStatusLabel.setText(status);
        predictionStatusLabel.setTextFill(endColor);
        aiPredictionBox.setStyle(aiPredictionBox.getStyle() + "; -fx-border-color: " + borderColor + ";");

        // Animation de l'arc (le cercle fait 360°, donc score * 3.6)
        Timeline timeline = new Timeline();
        KeyFrame kf = new KeyFrame(Duration.millis(1500), 
            new KeyValue(arc.lengthProperty(), -targetScore * 3.6)
        );
        timeline.getKeyFrames().add(kf);
        timeline.play();

        // Animation du compteur textuel
        Timeline counter = new Timeline();
        int steps = (int) targetScore;
        for (int i = 0; i <= steps; i++) {
            final int val = i;
            counter.getKeyFrames().add(new KeyFrame(Duration.millis(i * (1500.0/steps)), e -> {
                aiPredictionLabel.setText(val + "%");
            }));
        }
        counter.play();
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

            Models.UserCourseProgress progressObj = null;
            if (Session.getInstance().getCurrentUser() != null) {
                progressObj = progressService.getProgress(Session.getInstance().getCurrentUser().getId(), currentCourse.getId());
            }

            // 1. Bouton Téléchargement (Haut) - Visible si certificat obtenu
            boolean hasCert = Session.getInstance().getCurrentUser() != null &&
                    certificateService.hasCertificate(Session.getInstance().getCurrentUser().getId(), currentCourse.getId());
            topDownloadCertBtn.setVisible(hasCert);
            topDownloadCertBtn.setManaged(hasCert);

            // 2. Bouton Suivant (Bas)
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
                
                // Sur le dernier module
                // On montre le bouton si le module n'est pas fait OU si le cours n'est pas à 100% (pour corriger les erreurs)
                int completedModules = (progressObj != null) ? progressObj.getCompletedModules() : 0;
                boolean courseNotFinished = (completedModules < modules.size());

                if (!isCurrentModuleDone || courseNotFinished) {
                    finishActionsBox.setVisible(true);
                    finishActionsBox.setManaged(true);
                    finishCourseBtn.setVisible(true);
                    finishCourseBtn.setManaged(true);
                    finishCourseBtn.setText(isCurrentModuleDone ? "Actualiser la progression ✓" : "Terminer le cours ✓");
                } else {
                    // Cours vraiment fini et à 100%
                    finishActionsBox.setVisible(true);
                    finishActionsBox.setManaged(true);
                    finishCourseBtn.setVisible(true);
                    finishCourseBtn.setManaged(true);
                    finishCourseBtn.setText("Certificat Obtenu 🏆");
                    finishCourseBtn.setDisable(false); // On peut cliquer pour voir le certificat
                    finishCourseBtn.setOnAction(e -> handleDownloadCertificate());
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

            // On rafraîchit TOUT immédiatement
            loadSidebar();
            updateNavigationButtons();
            loadAIPrediction();

            if (progressObj != null && progressObj.isCompleted()) {
                // Course 100% done
                certificateService.generateCertificate(
                        Session.getInstance().getCurrentUser().getId(), currentCourse.getId());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Félicitations !");
                alert.setHeaderText("🏆 Cours terminé à 100%");
                alert.setContentText("Vous avez terminé \"" + currentCourse.getTitle() +
                        "\" et obtenu votre certificat !");
                alert.show();

            } else if (nextModuleForNavigation != null) {
                // Mid-course: advance to next module
                setData(nextModuleForNavigation);
                nextModuleForNavigation = null;
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

    @FXML private StackPane summaryOverlay;
    @FXML private TextFlow summaryTextFlow;

    @FXML
    private void handleSummarize() {
        if (currentModule == null || currentModule.getContent() == null || currentModule.getContent().isEmpty()) {
            return;
        }

        // Animation de chargement visuelle (Optionnel : on pourrait ajouter un spinner)
        summaryTextFlow.getChildren().clear();
        javafx.scene.text.Text loadingText = new javafx.scene.text.Text("🧠 L'IA analyse le contenu...");
        loadingText.setFill(javafx.scene.paint.Color.web("#94a3b8"));
        summaryTextFlow.getChildren().add(loadingText);
        
        summaryOverlay.setVisible(true);
        summaryOverlay.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), summaryOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        AIService aiService = new AIService();
        aiService.summarizeContent(currentModule.getContent()).thenAccept(summary -> {
            Platform.runLater(() -> {
                summaryTextFlow.getChildren().clear();
                
                // On formate un peu le texte pour le rendre plus beau
                String[] lines = summary.split("\n");
                for (String line : lines) {
                    javafx.scene.text.Text textNode = new javafx.scene.text.Text(line + "\n");
                    textNode.setFill(javafx.scene.paint.Color.WHITE);
                    textNode.setStyle("-fx-font-size: 15;");
                    
                    if (line.startsWith("**") || line.startsWith("#")) {
                        textNode.setStyle("-fx-font-weight: bold; -fx-font-size: 18;");
                        textNode.setFill(javafx.scene.paint.Color.web("#10b981"));
                    } else if (line.trim().startsWith("*") || line.trim().startsWith("-")) {
                        textNode.setFill(javafx.scene.paint.Color.web("#3b82f6"));
                    }
                    
                    summaryTextFlow.getChildren().add(textNode);
                }
            });
        });
    }

    @FXML
    private void closeSummary() {
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), summaryOverlay);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> summaryOverlay.setVisible(false));
        ft.play();
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
