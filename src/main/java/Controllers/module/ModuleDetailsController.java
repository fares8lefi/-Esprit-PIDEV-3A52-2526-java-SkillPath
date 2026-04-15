package Controllers.module;

import Controllers.user.admin.SideBarController;
import Models.Course;
import Models.Module;
import Services.CourseService;
import Services.ModuleService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ModuleDetailsController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblTypeTag;
    @FXML private Label lblIdBadge;
    @FXML private Label lblDescription;
    @FXML private Label lblContent;
    @FXML private Label lblCourseName;
    @FXML private Label lblScheduledAt;
    @FXML private Hyperlink linkDocument;
    @FXML private VBox bannerContainer;
    @FXML private SideBarController sideBarController;

    private final ModuleService moduleService = new ModuleService();
    private final CourseService courseService = new CourseService();
    private Module currentModule;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("modules");
        }
    }

    public void setModule(Module module) {
        this.currentModule = module;
        
        lblTitle.setText(module.getTitle());
        lblTypeTag.setText(module.getType() != null ? module.getType().toUpperCase() : "MODULE");
        lblIdBadge.setText("ID: #" + module.getId());
        lblDescription.setText(module.getDescription() != null ? module.getDescription() : "Pas de description.");
        lblContent.setText(module.getContent() != null ? module.getContent() : "Contenu non disponible.");
        
        if (module.getScheduledAt() != null) {
            lblScheduledAt.setText(module.getScheduledAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")));
        } else {
            lblScheduledAt.setText("Non planifié");
        }

        if (module.getDocument() != null && !module.getDocument().isEmpty()) {
            linkDocument.setText(module.getDocument());
            linkDocument.setVisible(true);
        } else {
            linkDocument.setText("Aucun document joint");
            linkDocument.setDisable(true);
        }

        updateBannerStyle(module.getType());

        loadCourseName(module.getCourseId());
    }

    private void updateBannerStyle(String type) {
        String color1, color2;
        switch (type != null ? type : "") {
            case "Leçon"  -> { color1 = "#1E88E5"; color2 = "#1565C0"; } // Blue
            case "Quiz"   -> { color1 = "#fbbf24"; color2 = "#d97706"; } // Yellow/Orange
            case "TP"     -> { color1 = "#06B6D4"; color2 = "#0891B2"; } // Cyan
            case "Examen" -> { color1 = "#f43f5e"; color2 = "#be123c"; } // Rose/Red
            default       -> { color1 = "#475569"; color2 = "#1e293b"; } // Slate
        }
        bannerContainer.setStyle("-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2 + "); " +
                                 "-fx-background-radius: 24; -fx-padding: 40;");
    }

    private void loadCourseName(int courseId) {
        try {
            Course c = courseService.recuperer().stream()
                    .filter(course -> course.getId() == courseId)
                    .findFirst().orElse(null);
            if (c != null) {
                lblCourseName.setText(c.getTitle());
            } else {
                lblCourseName.setText("ID: " + courseId);
            }
        } catch (SQLDataException e) {
            lblCourseName.setText("ID: " + courseId);
        }
    }

    @FXML
    private void handleOpenDocument(ActionEvent event) {
        if (currentModule.getDocument() != null && !currentModule.getDocument().isEmpty()) {
            System.out.println("Ouverture du document: " + currentModule.getDocument());
            // Logic to open URL in browser could be added here
        }
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/module/editModule.fxml"));
            Parent root = loader.load();
            EditModuleController emc = loader.getController();
            emc.setModule(currentModule);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/BackOffice/module/moduleList.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
