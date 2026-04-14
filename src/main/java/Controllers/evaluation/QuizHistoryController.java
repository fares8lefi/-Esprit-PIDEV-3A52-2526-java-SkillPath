package Controllers.evaluation;

import Models.evaluation.Quiz;
import Models.evaluation.Resultat;
import Services.evaluation.QuizService;
import Services.evaluation.ResultatService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class QuizHistoryController {

    @FXML
    private FlowPane resultsGrid;

    private ResultatService resultatService = new ResultatService();
    private QuizService quizService = new QuizService();

    @FXML
    public void initialize() {
        loadHistory();
    }

    private void loadHistory() {
        try {
            // student = 1
            List<Resultat> list = resultatService.recupererParEtudiant(1);

            resultsGrid.getChildren().clear();

            if (list.isEmpty()) {
                Label noData = new Label("Aucun résultat pour l'instant. Passez un quiz !");
                noData.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
                resultsGrid.getChildren().add(noData);
                return;
            }

            for (Resultat r : list) {
                Quiz q = quizService.recupererParId(r.getId_quiz());
                String title = (q != null) ? q.getTitre() : "Quiz Inconnu";
                resultsGrid.getChildren().add(createResultCard(r, title));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createResultCard(Resultat r, String title) {
        VBox cardContainer = new VBox();
        cardContainer.setPrefWidth(350);
        cardContainer.setMaxWidth(350);

        HBox topColorLine = new HBox();
        topColorLine.setPrefHeight(8);
        topColorLine.getStyleClass().add("card-header-line");

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("quiz-card");
        cardBody.setSpacing(15);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white;");
        titleLabel.setWrapText(true);

        Label scoreLabel = new Label("Score : " + r.getScore() + " / " + r.getNote_max());
        double percentage = (r.getNote_max() > 0) ? (double) r.getScore() / r.getNote_max() : 0;

        if (percentage >= 0.7) {
            scoreLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else if (percentage >= 0.5) {
            scoreLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            scoreLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-font-weight: bold;");
        }

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getDate_passage());
        Label dateLabel = new Label("🗓 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        cardBody.getChildren().addAll(titleLabel, scoreLabel, dateLabel);
        cardContainer.getChildren().addAll(topColorLine, cardBody);

        return cardContainer;
    }

    @FXML
    public void goToFrontOffice(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontOffice/evaluation/QuizFrontOffice.fxml"));
            Stage stage = (Stage) resultsGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
