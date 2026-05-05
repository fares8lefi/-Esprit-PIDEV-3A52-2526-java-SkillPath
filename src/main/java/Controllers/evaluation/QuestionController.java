package Controllers.evaluation;

import Models.evaluation.Question;
import Models.evaluation.Quiz;
import Services.evaluation.QuestionService;
import Services.evaluation.AIGeneratorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.sql.SQLDataException;

public class QuestionController {

    @FXML
    private FlowPane questionGrid;

    private VBox selectedHoverCard = null;
    private Question selectedQuestion = null;

    @FXML
    private TextArea txtEnonce;
    @FXML
    private TextField txtChoixA;
    @FXML
    private TextField txtChoixB;
    @FXML
    private TextField txtChoixC;
    @FXML
    private TextField txtChoixD;
    @FXML
    private ComboBox<String> comboBonneReponse;
    @FXML
    private TextField txtPoints;
    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblError;

    private QuestionService questionService;
    private AIGeneratorService aiService;
    private ObservableList<Question> questionList;
    private Quiz currentQuiz;

    public void setQuiz(Quiz quiz) {
        this.currentQuiz = quiz;
        loadQuestions();
    }

    @FXML
    public void initialize() {
        questionService = new QuestionService();
        aiService = new AIGeneratorService();
        questionList = FXCollections.observableArrayList();

        // Init Options for correct answer selection
        comboBonneReponse.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
    }

    private void loadQuestions() {
        if (currentQuiz != null) {
            try {
                questionList.setAll(questionService.recupererParQuiz(currentQuiz.getId_quiz()));
                refreshGrid();
                updateSubtitle();
            } catch (SQLDataException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void refreshGrid() {
        questionGrid.getChildren().clear();
        for (Question q : questionList) {
            questionGrid.getChildren().add(createQuestionCard(q));
        }
    }

    private void updateSubtitle() {
        int count = questionList.size();
        lblSubtitle.setText(count + " question" + (count > 1 ? "s" : "") + " configurée" + (count > 1 ? "s" : ""));
    }

    private VBox createQuestionCard(Question q) {
        VBox card = new VBox();
        card.getStyleClass().add("quiz-card");
        card.setSpacing(10);
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setAlignment(Pos.TOP_LEFT);

        Label enonce = new Label(q.getEnonce());
        enonce.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        enonce.setWrapText(true);
        enonce.setMaxHeight(40);

        HBox badges = new HBox(8);
        Label resp = new Label("Réponse: " + q.getBonne_reponse());
        resp.getStyleClass().addAll("badge", "badge-purple");
        Label pts = new Label(q.getPoints() + " PTS");
        pts.getStyleClass().addAll("badge", "badge-amber");
        badges.getChildren().addAll(resp, pts);

        card.getChildren().addAll(enonce, badges);

        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> {
            if (selectedHoverCard != null) {
                selectedHoverCard.setStyle(
                        "-fx-border-color: rgba(255,255,255,0.05); -fx-background-color: rgba(255,255,255,0.03);");
            }
            selectedHoverCard = card;
            selectedQuestion = q;
            card.setStyle("-fx-border-color: #3b82f6; -fx-background-color: rgba(59, 130, 246, 0.1);");
            populateFields(q);
        });

        return card;
    }

    private void populateFields(Question q) {
        txtEnonce.setText(q.getEnonce());
        txtChoixA.setText(q.getChoix_a());
        txtChoixB.setText(q.getChoix_b());
        txtChoixC.setText(q.getChoix_c());
        txtChoixD.setText(q.getChoix_d());
        comboBonneReponse.setValue(q.getBonne_reponse());
        txtPoints.setText(String.valueOf(q.getPoints()));
    }

    @FXML
    void handleAdd(ActionEvent event) {
        if (currentQuiz == null) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Veuillez d'abord sélectionner un quiz !");
            return;
        }
        if (!validateInput()) {
            return;
        }

        try {
            Question q = new Question(
                    txtEnonce.getText(),
                    txtChoixA.getText(),
                    txtChoixB.getText(),
                    txtChoixC.getText(),
                    txtChoixD.getText(),
                    comboBonneReponse.getValue(),
                    Integer.parseInt(txtPoints.getText()),
                    currentQuiz.getId_quiz());
            questionService.ajouter(q);
            loadQuestions();
            clearFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        if (selectedQuestion == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection Requise", "Veuillez sélectionner une question à modifier.");
            return;
        }
        if (!validateInput()) {
            return;
        }

        try {
            selectedQuestion.setEnonce(txtEnonce.getText());
            selectedQuestion.setChoix_a(txtChoixA.getText());
            selectedQuestion.setChoix_b(txtChoixB.getText());
            selectedQuestion.setChoix_c(txtChoixC.getText());
            selectedQuestion.setChoix_d(txtChoixD.getText());
            selectedQuestion.setBonne_reponse(comboBonneReponse.getValue());
            selectedQuestion.setPoints(Integer.parseInt(txtPoints.getText()));
            questionService.modifier(selectedQuestion);
            loadQuestions();
            clearFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedQuestion != null) {
            try {
                questionService.supprimer(selectedQuestion);
                loadQuestions();
                clearFields();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    @FXML
    void handleClear(ActionEvent event) {
        clearFields();
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/BackOffice/evaluation/QuizManagement.fxml"));
            Stage stage = (Stage) questionGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleGenerateAI(ActionEvent event) {
        if (currentQuiz == null) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Veuillez d'abord sélectionner un quiz !");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Génération en cours", "Veuillez patienter pendant la génération...");

        new Thread(() -> {
            try {
                String subject = currentQuiz.getTitre();
                java.util.List<Question> generated = aiService.generateQuestions(subject, currentQuiz.getId_quiz());
                
                javafx.application.Platform.runLater(() -> {
                    if (generated.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Génération échouée. Veuillez vérifier votre clé API dans le .env.");
                    } else {
                        for (Question q : generated) {
                            try {
                                questionService.ajouter(q);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        loadQuestions();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", generated.size() + " questions générées avec l'IA !");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    private void clearFields() {
        txtEnonce.clear();
        txtChoixA.clear();
        txtChoixB.clear();
        txtChoixC.clear();
        txtChoixD.clear();
        comboBonneReponse.setValue(null);
        txtPoints.clear();
        selectedQuestion = null;
        if (selectedHoverCard != null) {
            selectedHoverCard.setStyle(
                    "-fx-border-color: rgba(255,255,255,0.05); -fx-background-color: rgba(255,255,255,0.03);");
            selectedHoverCard = null;
        }
        if (lblError != null) {
            lblError.setVisible(false);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        if (type == Alert.AlertType.ERROR || type == Alert.AlertType.WARNING) {
            if (lblError != null) {
                lblError.setText(msg);
                lblError.setVisible(true);
            }
        } else {
            if (lblError != null)
                lblError.setVisible(false);
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.show();
        }
    }

    private boolean validateInput() {
        if (txtEnonce.getText() == null || txtEnonce.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "L'énoncé de la question est obligatoire.");
            return false;
        }
        if (txtChoixA.getText() == null || txtChoixA.getText().trim().isEmpty() ||
                txtChoixB.getText() == null || txtChoixB.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez renseigner au minimum les choix A et B.");
            return false;
        }
        if (comboBonneReponse.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez sélectionner la bonne réponse.");
            return false;
        }
        if (txtPoints.getText() == null || txtPoints.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez attribuer des points à cette question.");
            return false;
        }
        try {
            int pts = Integer.parseInt(txtPoints.getText().trim());
            if (pts <= 0) {
                showAlert(Alert.AlertType.WARNING, "Erreur de saisie",
                        "Les points doivent être un entier strictement positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Erreur de format", "Les points doivent être un nombre entier valide.");
            return false;
        }
        return true;
    }
}
