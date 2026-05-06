package Controllers.evaluation;

import Models.evaluation.Quiz;
import Models.evaluation.Resultat;
import Services.evaluation.QuizService;
import Services.evaluation.ResultatService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
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
        
        // Set up listener to refresh when window is shown
        Platform.runLater(() -> {
            if (resultsGrid.getScene() != null && resultsGrid.getScene().getWindow() != null) {
                resultsGrid.getScene().getWindow().setOnShown(event -> refreshHistory());
            }
        });
    }

    public void refreshHistory() {
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

        Button btnExport = new Button("📄 Exporter PDF");
        btnExport.setStyle("-fx-background-color: transparent; -fx-border-color: #3b82f6; -fx-border-radius: 5; -fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-font-weight: bold;");
        btnExport.setOnAction(e -> exportToPDF(r, title));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(dateLabel, spacer, btnExport);
        bottomBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        cardBody.getChildren().addAll(titleLabel, scoreLabel, bottomBar);
        cardContainer.getChildren().addAll(topColorLine, cardBody);

        return cardContainer;
    }

    private void exportToPDF(Resultat r, String title) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le résultat en PDF");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Bilan_" + title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf");
        
        java.io.File file = fileChooser.showSaveDialog(resultsGrid.getScene().getWindow());
        if (file != null) {
            try {
                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                document.open();
                
                // Définition des polices
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 28, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(30, 58, 138));
                com.itextpdf.text.Font subtitleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.DARK_GRAY);
                com.itextpdf.text.Font tableHeaderFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
                com.itextpdf.text.Font tableCellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.BLACK);
                com.itextpdf.text.Font appreciationFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(15, 118, 110));

                double percentage = (r.getNote_max() > 0) ? (double) r.getScore() / r.getNote_max() : 0;
                boolean isSuccess = percentage >= 0.5;

                // Titre principal
                com.itextpdf.text.Paragraph mainTitle = new com.itextpdf.text.Paragraph(isSuccess ? "CERTIFICAT DE REUSSITE" : "BILAN D'EVALUATION", titleFont);
                mainTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(mainTitle);
                
                com.itextpdf.text.Paragraph subTitle = new com.itextpdf.text.Paragraph("Plateforme SkillPath - Bilan Officiel", subtitleFont);
                subTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(subTitle);
                
                document.add(new com.itextpdf.text.Paragraph("\n"));

                // Ligne de séparation
                com.itextpdf.text.pdf.draw.LineSeparator ls = new com.itextpdf.text.pdf.draw.LineSeparator();
                ls.setLineColor(new com.itextpdf.text.BaseColor(200, 200, 200));
                document.add(new com.itextpdf.text.Chunk(ls));
                document.add(new com.itextpdf.text.Paragraph("\n\n"));

                // Création du tableau de détails
                com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(2);
                table.setWidthPercentage(90);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(20f);
                
                // En-têtes du tableau
                com.itextpdf.text.pdf.PdfPCell cell1 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Information", tableHeaderFont));
                com.itextpdf.text.pdf.PdfPCell cell2 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Details", tableHeaderFont));
                com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(59, 130, 246);
                cell1.setBackgroundColor(headerBg);
                cell2.setBackgroundColor(headerBg);
                cell1.setPadding(10f); cell2.setPadding(10f);
                table.addCell(cell1); table.addCell(cell2);

                String dateStrFull = new java.text.SimpleDateFormat("dd/MM/yyyy 'a' HH:mm").format(r.getDate_passage());
                int percentInt = (int)(percentage * 100);

                String[][] data = {
                    {"Sujet / Quiz", title},
                    {"Date de passage", dateStrFull},
                    {"Points obtenus", r.getScore() + " / " + r.getNote_max()},
                    {"Pourcentage de reussite", percentInt + " %"},
                    {"Statut Global", isSuccess ? "VALIDE" : "ECHOUE"}
                };

                for(String[] row : data) {
                    com.itextpdf.text.pdf.PdfPCell c1 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(row[0], tableCellFont));
                    com.itextpdf.text.pdf.PdfPCell c2 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(row[1], tableCellFont));
                    c1.setPadding(10f); c2.setPadding(10f);
                    table.addCell(c1); table.addCell(c2);
                }

                document.add(table);

                // Appréciation
                String appreciation;
                if (percentage >= 0.8) appreciation = "Exceptionnel ! Vous maitrisez parfaitement ce sujet. Continuez ainsi !";
                else if (percentage >= 0.6) appreciation = "Bon travail ! Vous avez acquis de bonnes bases, quelques revisions et ce sera parfait.";
                else if (percentage >= 0.4) appreciation = "Moyen. Vous avez encore besoin de revoir quelques concepts importants.";
                else appreciation = "Insuffisant. Il est vivement conseille de repasser ce cours et de retenter votre chance.";
                
                com.itextpdf.text.Paragraph appPara = new com.itextpdf.text.Paragraph("Appreciation du Systeme :\n" + appreciation, appreciationFont);
                appPara.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(appPara);

                // Footer
                document.add(new com.itextpdf.text.Paragraph("\n\n\n\n\n\n"));
                com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph("Document genere automatiquement par SkillPath", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.GRAY));
                footer.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                document.add(footer);
                
                document.close();
                
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Exportation Reussie");
                alert.setHeaderText(null);
                alert.setContentText("Votre bilan PDF a ete genere avec un design ameliore !");
                alert.showAndWait();
                
            } catch (Exception e) {
                e.printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Echec de l'exportation");
                alert.setContentText("Impossible de creer le fichier PDF :\n" + e.getMessage());
                alert.showAndWait();
            }
        }
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
