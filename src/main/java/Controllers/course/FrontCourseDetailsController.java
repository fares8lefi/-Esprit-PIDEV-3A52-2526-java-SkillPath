package Controllers.course;

import Models.Course;
import Models.Module;
import Services.ModuleService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import Models.evaluation.Quiz;
import Services.evaluation.QuizService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;

public class FrontCourseDetailsController {

    @FXML private Label lblTitle;
    @FXML private Label lblCategory;
    @FXML private Label lblLevel;
    @FXML private Label lblDescription;
    @FXML private Label lblPrice;
    @FXML private Label lblModuleInfo;
    @FXML private ImageView imgCourse;
    @FXML private VBox modulesContainer;
    @FXML private VBox quizzesSection;
    @FXML private VBox quizzesContainer;
    
    @FXML private VBox chatWindow;
    @FXML private VBox chatMessages;
    @FXML private TextField txtChatInput;

    private final ModuleService moduleService = new ModuleService();
    private final QuizService quizService = new QuizService();
    private Course currentCourse;

    public void setCourse(Course course) {
        this.currentCourse = course;
        
        lblTitle.setText(course.getTitle());
        lblCategory.setText(course.getCategory() != null ? course.getCategory().toUpperCase() : "DÉVELOPPEMENT");
        lblLevel.setText(course.getLevel());
        lblDescription.setText(course.getDescription());
        
        if (course.getPrice() <= 0) {
            lblPrice.setText("GRATUIT");
        } else {
            lblPrice.setText(String.format("%.2f DT", course.getPrice()));
        }

        // Handle Image (Bulletproof Loading)
        if (course.getImage() != null && !course.getImage().isEmpty()) {
            Image loadedImage = Utils.AssetLoader.loadCourseImage(course.getImage());
            if (loadedImage != null) {
                imgCourse.setImage(loadedImage);
            }
        }

        loadModules();
        loadQuizzes();
    }

    private void loadModules() {
        try {
            List<Module> modules = moduleService.getByCourse(currentCourse.getId());
            lblModuleInfo.setText(modules.size() + " modules exclusifs");
            
            modulesContainer.getChildren().clear();
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                modulesContainer.getChildren().add(createModuleRow(i + 1, m.getTitle()));
            }
        } catch (java.sql.SQLDataException e) {
            System.err.println("Erreur chargement modules : " + e.getMessage());
        }
    }

    private Node createModuleRow(int index, String title) {
        VBox row = new VBox(5);
        row.getStyleClass().add("glass-card-elite");
        row.setStyle("-fx-padding: 20 25; -fx-background-radius: 15; -fx-cursor: hand;");
        
        Label lblIndex = new Label("MODULE " + index);
        lblIndex.setStyle("-fx-text-fill: #6366f1; -fx-font-weight: 950; -fx-font-size: 10; -fx-letter-spacing: 1.5;");
        
        Label lblModuleTitle = new Label(title);
        lblModuleTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
        
        row.getChildren().addAll(lblIndex, lblModuleTitle);
        return row;
    }

    private void loadQuizzes() {
        if (quizzesSection == null || quizzesContainer == null) return;
        
        List<Quiz> quizzes = quizService.getByCourse(currentCourse.getId());
        if (quizzes != null && !quizzes.isEmpty()) {
            quizzesSection.setVisible(true);
            quizzesSection.setManaged(true);
            quizzesContainer.getChildren().clear();
            
            for (Quiz q : quizzes) {
                quizzesContainer.getChildren().add(createQuizRow(q));
            }
        } else {
            quizzesSection.setVisible(false);
            quizzesSection.setManaged(false);
        }
    }

    private Node createQuizRow(Quiz quiz) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("glass-card-elite");
        row.setStyle("-fx-padding: 20 25; -fx-background-radius: 15; -fx-cursor: hand;");
        
        // Icon
        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: rgba(16,185,129,0.1); -fx-background-radius: 10; -fx-min-width: 45; -fx-min-height: 45;");
        Label iconLabel = new Label("📝");
        iconLabel.setStyle("-fx-font-size: 20;");
        iconBox.getChildren().add(iconLabel);
        
        // Info
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label lblTitle = new Label(quiz.getTitre());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
        
        HBox detailsBox = new HBox(15);
        detailsBox.setAlignment(Pos.CENTER_LEFT);
        
        Label lblDuration = new Label("⏳ " + quiz.getDuree() + " min");
        lblDuration.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        
        Label lblScore = new Label("🎯 Max " + quiz.getNote_max() + " pts");
        lblScore.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        
        detailsBox.getChildren().addAll(lblDuration, lblScore);
        infoBox.getChildren().addAll(lblTitle, detailsBox);
        
        // Start Button
        javafx.scene.control.Button btnStart = new javafx.scene.control.Button("Démarrer");
        btnStart.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnStart.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/evaluation/QuizPass.fxml"));
                Parent root = loader.load();
                
                Controllers.evaluation.QuizPassController passController = loader.getController();
                passController.setQuiz(quiz);
                
                Stage stage = (Stage) btnStart.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        
        row.getChildren().addAll(iconBox, infoBox, btnStart);
        return row;
    }

    @FXML
    private void toggleChat() {
        chatWindow.setVisible(!chatWindow.isVisible());
        if (chatWindow.isVisible() && chatMessages.getChildren().isEmpty()) {
            addChatMessage("Bonjour ! En quoi puis-je vous éclairer sur ce cours en particulier ?", false);
        }
    }

    @FXML
    private void sendChatMessage() {
        String text = txtChatInput.getText().trim();
        if (!text.isEmpty()) {
            addChatMessage(text, true);
            txtChatInput.clear();
            Platform.runLater(() -> {
                try { Thread.sleep(800); } catch (Exception ignored) {}
                addChatMessage("C'est une excellente question sur " + currentCourse.getTitle() + ". Nos mentors reviendront vers vous avec plus de détails, mais sachez que ce cours est l'un des plus complets sur SkillPath.", false);
            });
        }
    }

    private void addChatMessage(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(300);
        label.getStyleClass().add(isUser ? "chat-bubble-user" : "chat-bubble-ai");
        HBox box = new HBox(label);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessages.getChildren().add(box);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/courseList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    private void handleBackClick(MouseEvent event) {
        handleBack(new ActionEvent(event.getSource(), null));
    }

    @FXML
    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/home/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblTitle.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur redirection accueil : " + e.getMessage());
        }
    }
}
