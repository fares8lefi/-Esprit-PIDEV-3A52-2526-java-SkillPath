package Controllers.evaluation;

import Models.evaluation.Quiz;
import Services.evaluation.QuestionService;
import Services.evaluation.QuizService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.sql.SQLDataException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QuizFrontOfficeController {

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> comboSort;
    @FXML
    private FlowPane quizGrid;

    private QuizService quizService;
    private QuestionService questionService;
    private ObservableList<Quiz> allQuizzes;

    @FXML
    public void initialize() {
        quizService = new QuizService();
        questionService = new QuestionService();
        allQuizzes = FXCollections.observableArrayList();

        // Init Sorting Options
        comboSort.setItems(FXCollections.observableArrayList(
                "Durée ↑ (Courte)",
                "Durée ↓ (Longue)",
                "Titre A-Z",
                "Titre Z-A"));

        // Load data
        loadQuizzesFromDB();

        // Setup Listeners for real-time search & sort
        txtSearch.textProperty().addListener((obs, oldV, newV) -> filterAndDisplay());
        comboSort.valueProperty().addListener((obs, oldV, newV) -> filterAndDisplay());
    }

    private void loadQuizzesFromDB() {
        try {
            allQuizzes.setAll(quizService.recuperer());
            filterAndDisplay();
        } catch (SQLDataException e) {
            System.err.println("Erreur chargement: " + e.getMessage());
        }
    }

    private void filterAndDisplay() {
        String searchText = txtSearch.getText().toLowerCase();
        String sortCriteria = comboSort.getValue();

        List<Quiz> filtered = allQuizzes.stream()
                .filter(q -> q.getTitre().toLowerCase().contains(searchText) ||
                        q.getDescription().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        if (sortCriteria != null) {
            switch (sortCriteria) {
                case "Durée ↑ (Courte)":
                    filtered.sort(Comparator.comparingInt(Quiz::getDuree));
                    break;
                case "Durée ↓ (Longue)":
                    filtered.sort((q1, q2) -> Integer.compare(q2.getDuree(), q1.getDuree()));
                    break;
                case "Titre A-Z":
                    filtered.sort((q1, q2) -> q1.getTitre().compareToIgnoreCase(q2.getTitre()));
                    break;
                case "Titre Z-A":
                    filtered.sort((q1, q2) -> q2.getTitre().compareToIgnoreCase(q1.getTitre()));
                    break;
            }
        }

        quizGrid.getChildren().clear();

        if (filtered.isEmpty()) {
            Label noQuizLabel = new Label("Aucun quiz disponible pour le moment.\nRevenez bientôt !");
            noQuizLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: bold;");
            noQuizLabel.setAlignment(Pos.CENTER);
            quizGrid.getChildren().add(noQuizLabel);
            return;
        }

        // Generate Cards
        for (Quiz quiz : filtered) {
            quizGrid.getChildren().add(createQuizCard(quiz));
        }
    }

    private VBox createQuizCard(Quiz quiz) {
        // Main Container
        VBox cardContainer = new VBox();
        cardContainer.setPrefWidth(350);
        cardContainer.setMaxWidth(350);

        // Top colored line
        HBox topColorLine = new HBox();
        topColorLine.setPrefHeight(8);
        topColorLine.getStyleClass().add("card-header-line");

        // The card body
        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("quiz-card");
        cardBody.setSpacing(15);

        // Header: Title and Question count
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(10);
        Label titleLabel = new Label(quiz.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);

        // Fetch question count
        int qCount = 0;
        try {
            qCount = questionService.recupererParQuiz(quiz.getId_quiz()).size();
        } catch (Exception ignored) {
        }

        Label qCountBadge = new Label(qCount + " Q");
        qCountBadge.getStyleClass().add("badge-purple");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        headerBox.getChildren().addAll(titleLabel, spacer, qCountBadge);

        // Description
        Label descLabel = new Label(quiz.getDescription());
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60); // Imitate line-clamp

        // Stats (Duration & Points)
        HBox statsBox = new HBox();
        statsBox.setSpacing(20);
        Label durationLabel = new Label("⏳ " + quiz.getDuree() + " min");
        durationLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        Label pointsLabel = new Label("⭐ " + quiz.getNote_max() + " pts");
        pointsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        statsBox.getChildren().addAll(durationLabel, pointsLabel);

        cardBody.getChildren().addAll(headerBox, descLabel, statsBox);
        cardContainer.getChildren().addAll(topColorLine, cardBody);

        // Hover Effect
        cardContainer.setStyle("-fx-cursor: hand;");
        cardContainer.setOnMouseEntered(e -> cardBody.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08);"));
        cardContainer.setOnMouseExited(e -> cardBody.setStyle("-fx-background-color: transparent;")); // fallback

        // Navigation Logic
        cardContainer.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/evaluation/QuizPass.fxml"));
                Parent root = loader.load();

                QuizPassController passController = loader.getController();
                passController.setQuiz(quiz);

                Stage stage = (Stage) quizGrid.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return cardContainer;
    }

    @FXML
    void goToHistory(javafx.scene.input.MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/evaluation/QuizHistory.fxml"));
            Stage stage = (Stage) txtSearch.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
