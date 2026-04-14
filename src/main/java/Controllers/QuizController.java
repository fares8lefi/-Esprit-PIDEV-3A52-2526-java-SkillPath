package Controllers;

import Models.CourseDTO;
import Models.Quiz;
import Services.QuizService;
import Utils.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.Timestamp;

public class QuizController {

    @FXML
    private FlowPane quizGrid;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextArea txtDescription;
    @FXML
    private TextField txtDuree;
    @FXML
    private TextField txtNoteMax;
    @FXML
    private ComboBox<CourseDTO> comboCourse;

    private QuizService quizService;
    private ObservableList<Quiz> quizList;

    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblError;

    @FXML
    public void initialize() {
        quizService = new QuizService();
        quizList = FXCollections.observableArrayList();

        loadCourses();
        loadQuizzes();
    }

    private void loadCourses() {
        ObservableList<CourseDTO> courses = FXCollections.observableArrayList();
        String sql = "SELECT id, title FROM course";
        try {
            Connection cnx = Database.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                courses.add(new CourseDTO(rs.getInt("id"), rs.getString("title")));
            }
            comboCourse.setItems(courses);
        } catch (Exception e) {
            System.out.println("Erreur chargement des cours (essai avec titre) : " + e.getMessage());
            try {
                courses.clear();
                sql = "SELECT id, titre as title FROM course";
                Connection cnx = Database.getInstance().getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    courses.add(new CourseDTO(rs.getInt("id"), rs.getString("title")));
                }
                comboCourse.setItems(courses);
            } catch (Exception e2) {
                System.out.println("Erreur fallback chargement des cours : " + e2.getMessage());
            }
        }
    }

    private void updateSubtitle() {
        int count = quizList.size();
        String text = count + " évaluation" + (count > 1 ? "s" : "") + " orchestrée" + (count > 1 ? "s" : "");
        if (lblSubtitle != null) {
            lblSubtitle.setText(text);
        }
    }

    private void loadQuizzes() {
        try {
            quizList.setAll(quizService.recuperer());
            refreshGrid();
            updateSubtitle();
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des quizzes: " + e.getMessage());
        }
    }

    private void refreshGrid() {
        quizGrid.getChildren().clear();
        for (Quiz q : quizList) {
            quizGrid.getChildren().add(createBackOfficeCard(q));
        }
    }

    private VBox selectedHoverCard = null;
    private Quiz selectedQuiz = null;

    private VBox createBackOfficeCard(Quiz quiz) {
        VBox card = new VBox();
        card.getStyleClass().add("quiz-card");
        card.setSpacing(10);
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label title = new Label(quiz.getTitre());
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: white; -fx-font-size: 15px;");
        title.setWrapText(true);

        Label desc = new Label(quiz.getDescription());
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxHeight(40);

        HBox badges = new HBox(8);
        Label pts = new Label(quiz.getNote_max() + " PTS");
        pts.getStyleClass().addAll("badge", "badge-amber");
        Label dur = new Label(quiz.getDuree() + " M");
        dur.getStyleClass().addAll("badge", "badge-blue");
        badges.getChildren().addAll(pts, dur);

        card.getChildren().addAll(title, desc, badges);

        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> {
            if (selectedHoverCard != null) {
                selectedHoverCard.setStyle(
                        "-fx-border-color: rgba(255,255,255,0.05); -fx-background-color: rgba(255,255,255,0.03);");
            }
            selectedHoverCard = card;
            selectedQuiz = quiz;
            card.setStyle("-fx-border-color: #3b82f6; -fx-background-color: rgba(59, 130, 246, 0.1);");
            populateFields(quiz);
        });

        return card;
    }

    private void populateFields(Quiz quiz) {
        txtTitre.setText(quiz.getTitre());
        txtDescription.setText(quiz.getDescription());
        txtDuree.setText(String.valueOf(quiz.getDuree()));
        txtNoteMax.setText(String.valueOf(quiz.getNote_max()));

        comboCourse.getSelectionModel().clearSelection();
        if (quiz.getCourse_id() != null) {
            for (CourseDTO c : comboCourse.getItems()) {
                if (c.getId() == quiz.getCourse_id()) {
                    comboCourse.setValue(c);
                    break;
                }
            }
        }
    }

    @FXML
    void handleAdd(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            Quiz quiz = new Quiz(
                    txtTitre.getText(),
                    txtDescription.getText(),
                    Integer.parseInt(txtDuree.getText()),
                    Integer.parseInt(txtNoteMax.getText()),
                    new Timestamp(System.currentTimeMillis()),
                    comboCourse.getValue() != null ? comboCourse.getValue().getId() : null);
            quizService.ajouter(quiz);
            loadQuizzes();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz ajouté avec succès !");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur d'ajout", e.getMessage());
        }
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        if (selectedQuiz == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection Requise", "Veuillez sélectionner un quiz à modifier.");
            return;
        }

        if (!validateInput()) {
            return;
        }

        try {
            selectedQuiz.setTitre(txtTitre.getText());
            selectedQuiz.setDescription(txtDescription.getText());
            selectedQuiz.setDuree(Integer.parseInt(txtDuree.getText()));
            selectedQuiz.setNote_max(Integer.parseInt(txtNoteMax.getText()));
            selectedQuiz.setCourse_id(comboCourse.getValue() != null ? comboCourse.getValue().getId() : null);

            quizService.modifier(selectedQuiz);
            loadQuizzes();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz mis à jour avec succès !");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de modification", e.getMessage());
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedQuiz != null) {
            try {
                quizService.supprimer(selectedQuiz);
                loadQuizzes();
                clearFields();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz supprimé avec succès !");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de suppression", e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un quiz à supprimer.");
        }
    }

    @FXML
    void handleManageQuestions(ActionEvent event) {
        if (selectedQuiz != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/QuestionManagement.fxml"));
                Parent root = loader.load();

                QuestionController controller = loader.getController();
                controller.setQuiz(selectedQuiz);

                Stage stage = (Stage) quizGrid.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un quiz pour gérer ses questions.");
        }
    }

    @FXML
    void handleClear(ActionEvent event) {
        clearFields();
    }

    private void clearFields() {
        txtTitre.clear();
        txtDescription.clear();
        txtDuree.clear();
        txtNoteMax.clear();
        comboCourse.getSelectionModel().clearSelection();
        selectedQuiz = null;
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
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        }
    }

    private boolean validateInput() {
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Le titre du quiz est obligatoire.");
            return false;
        }
        if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "La description est obligatoire.");
            return false;
        }
        if (comboCourse.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "Veuillez sélectionner un cours associé.");
            return false;
        }

        try {
            int duree = Integer.parseInt(txtDuree.getText().trim());
            if (duree <= 0) {
                showAlert(Alert.AlertType.WARNING, "Erreur de saisie",
                        "La durée doit être un entier positif (en minutes).");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Erreur de format", "La durée doit être un nombre valide.");
            return false;
        }

        try {
            int note = Integer.parseInt(txtNoteMax.getText().trim());
            if (note <= 0) {
                showAlert(Alert.AlertType.WARNING, "Erreur de saisie", "La note maximale doit être un entier positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Erreur de format", "La note maximale doit être un nombre valide.");
            return false;
        }

        return true;
    }
}
