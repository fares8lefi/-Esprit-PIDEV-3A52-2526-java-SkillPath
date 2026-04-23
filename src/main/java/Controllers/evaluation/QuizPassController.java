package Controllers.evaluation;

import Models.User;
import Models.evaluation.Question;
import Models.evaluation.Quiz;
import Services.evaluation.QuestionService;
import Services.evaluation.ResultatService;
import Services.evaluation.MailService;
import Services.evaluation.ScrapingService;
import Utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizPassController {

    @FXML
    private Label lblNavTitle;

    // Intro View
    @FXML
    private VBox viewIntro;
    @FXML
    private Label lblIntroTitle, lblIntroDesc, lblIntroQuestions, lblIntroTime;

    // Question View
    @FXML
    private VBox viewQuestion;
    @FXML
    private Label lblQuestionProgress, lblQuestionPoints, lblEnonce, lblTimer;
    
    private Timeline timeline;
    private int timeSeconds;
    @FXML
    private VBox optionsContainer;
    @FXML
    private Button btnNext;

    // Result View
    @FXML
    private VBox viewResult;
    @FXML
    private Label lblScore, lblFeedback;
    @FXML
    private VBox recommendationBox;
    @FXML
    private Label lblRecommendation;
    @FXML
    private Hyperlink linkRecommendation;

    private String recommendationUrl;

    private Quiz quiz;
    private List<Question> questions;
    private QuestionService questionService = new QuestionService();

    private int currentIndex = 0;
    private int currentScore = 0;
    private int totalPoints = 0;
    private ToggleGroup toggleGroup;

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        this.lblNavTitle.setText(quiz.getTitre());

        // Load questions
        try {
            this.questions = questionService.recupererParQuiz(quiz.getId_quiz());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Calculate total points
        for (Question q : questions) {
            totalPoints += q.getPoints();
        }

        // Setup Intro
        lblIntroTitle.setText(quiz.getTitre());
        lblIntroDesc.setText(quiz.getDescription());
        lblIntroQuestions.setText(questions.size() + " Questions");
        lblIntroTime.setText(quiz.getDuree() + " min");

        viewIntro.setVisible(true);
        viewQuestion.setVisible(false);
        viewResult.setVisible(false);
    }

    @FXML
    void startQuiz(ActionEvent event) {
        if (questions == null || questions.isEmpty()) {
            lblIntroDesc.setText("Aucune question n'est disponible pour ce quiz !");
            lblIntroDesc.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        viewIntro.setVisible(false);
        viewQuestion.setVisible(true);
        currentIndex = 0;
        currentScore = 0;

        // Setup timer
        timeSeconds = quiz.getDuree() * 60;
        updateTimerLabel();
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeSeconds--;
            updateTimerLabel();
            if (timeSeconds <= 0) {
                timeline.stop();
                showResult();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        showCurrentQuestion();
    }

    private void updateTimerLabel() {
        int minutes = timeSeconds / 60;
        int seconds = timeSeconds % 60;
        lblTimer.setText(String.format("⏳ %02d:%02d", minutes, seconds));
        if (timeSeconds <= 60) {
            lblTimer.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ef4444;"); // Red
        } else {
            lblTimer.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;"); // Yellow
        }
    }

    private void showCurrentQuestion() {
        Question q = questions.get(currentIndex);

        lblQuestionProgress.setText("Question " + (currentIndex + 1) + " / " + questions.size());
        lblQuestionPoints.setText(q.getPoints() + " pts");
        lblEnonce.setText(q.getEnonce());

        // Setup Radio Buttons
        optionsContainer.getChildren().clear();
        toggleGroup = new ToggleGroup();

        String[] options = { q.getChoix_a(), q.getChoix_b(), q.getChoix_c(), q.getChoix_d() };
        String[] labels = { "A) ", "B) ", "C) ", "D) " };

        boolean isLast = (currentIndex == questions.size() - 1);
        btnNext.setText(isLast ? "Terminer" : "Suivant →");
        btnNext.setDisable(true); // Must select an answer

        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && !options[i].trim().isEmpty()) {
                RadioButton rb = new RadioButton(labels[i] + options[i]);
                rb.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10; -fx-cursor: hand;");
                rb.setToggleGroup(toggleGroup);

                // Allow proceeding if selected
                rb.setOnAction(e -> btnNext.setDisable(false));
                optionsContainer.getChildren().add(rb);
            }
        }
    }

    @FXML
    void nextQuestion(ActionEvent event) {
        // Evaluate answer
        RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
        if (selected != null) {
            // Extraction de la lettre (A, B, C ou D) depuis le texte du bouton "A) Option..."
            String selectedLetter = selected.getText().substring(0, 1); 
            Question q = questions.get(currentIndex);
            
            // Vérifier si la lettre correspond à la bonne réponse stockée
            if (selectedLetter.equalsIgnoreCase(q.getBonne_reponse())) {
                currentScore += q.getPoints();
            }
        }

        currentIndex++;

        if (currentIndex < questions.size()) {
            showCurrentQuestion();
        } else {
            showResult();
        }
    }

    private void showResult() {
        if (timeline != null) {
            timeline.stop();
        }
        viewQuestion.setVisible(false);
        viewResult.setVisible(true);

        lblScore.setText(currentScore + " / " + totalPoints);

        // Save Resultat in DB (assuming user id = 1 for mock)
        ResultatService rs = new ResultatService();
        try {
            rs.ajouter(new Models.evaluation.Resultat(currentScore, totalPoints,
                    new java.sql.Timestamp(System.currentTimeMillis()), quiz.getId_quiz(), 1));
        } catch (Exception e) {
            System.err.println("Failed to save result: " + e.getMessage());
        }

        if (totalPoints == 0) {
            lblFeedback.setText("Quiz complété, mais aucun point n'était en jeu.");
            return;
        }

        double percentage = (double) currentScore / totalPoints;
        if (percentage >= 0.99) {
            lblFeedback.setText("Incroyable ! Un sans-faute ! 🏆");
            lblScore.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 64px; -fx-font-weight: 900;");
        } else if (percentage >= 0.7) {
            lblFeedback.setText("Très bon travail ! 👏");
            lblScore.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 64px; -fx-font-weight: 900;");
        } else if (percentage >= 0.5) {
            lblFeedback.setText("Pas mal, mais peut mieux faire. 👍");
            lblScore.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 64px; -fx-font-weight: 900;");
        } else {
            lblFeedback.setText("Il va falloir réviser un peu. 📚");
            lblScore.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 64px; -fx-font-weight: 900;");
        }

        // Send Email
        User currentUser = Session.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            String studentEmail = currentUser.getEmail();
            String studentName = currentUser.getUsername() != null ? currentUser.getUsername() : "Étudiant";
            String feedback = lblFeedback.getText();
            
            new Thread(() -> {
                MailService mailService = new MailService();
                mailService.sendQuizResultEmail(studentEmail, studentName, quiz.getTitre(), currentScore, totalPoints, feedback);
            }).start();
        } else {
            System.out.println("No user logged in or email missing, skipping email notification.");
        }

        // Web Scraping Recommendation if score <= 50%
        if (currentScore <= totalPoints / 2) {
            new Thread(() -> {
                ScrapingService scrapingService = new ScrapingService();
                String rawRecommendation = scrapingService.getRecommendation(quiz.getTitre());
                
                // Extraire le lien
                String url = "";
                Pattern pattern = Pattern.compile("https?://\\S+");
                Matcher matcher = pattern.matcher(rawRecommendation);
                if (matcher.find()) {
                    url = matcher.group();
                }
                
                final String finalUrl = url;
                final String finalText = rawRecommendation.split("\nLien")[0];
                
                Platform.runLater(() -> {
                    recommendationUrl = finalUrl;
                    lblRecommendation.setText(finalText);
                    recommendationBox.setVisible(true);
                    recommendationBox.setManaged(true);
                });
            }).start();
        }
    }

    @FXML
    void openRecommendation(ActionEvent event) {
        if (recommendationUrl != null && !recommendationUrl.isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI(recommendationUrl));
            } catch (IOException | URISyntaxException e) {
                System.err.println("Erreur ouverture lien : " + e.getMessage());
            }
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        if (timeline != null) {
            timeline.stop();
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/evaluation/QuizFrontOffice.fxml"));
            Stage stage = (Stage) viewIntro.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
