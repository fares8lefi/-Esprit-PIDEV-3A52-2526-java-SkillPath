package Controllers.user;

import Models.User;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import Models.Course;
import Services.CourseService;
import Controllers.course.FrontCourseCardController;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HomeUserController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private Label avatarInitial;
    @FXML private FlowPane courseContainer;
    @FXML private Button logoutBtn;
    @FXML private Label navCatalogue;

    private final CourseService courseService = new CourseService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupération de l'utilisateur connecté via le Singleton Session
        User currentUser = Session.getInstance().getCurrentUser();

        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            
            // Affichage de l'initiale
            if (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
                avatarInitial.setText(currentUser.getUsername().substring(0, 1).toUpperCase());
            }

            // Chargement des cours en arrière-plan
            Platform.runLater(this::loadFeaturedCourses);
        } else {
            // Si personne n'est connecté (accès direct sans login), on redirige vers le login
            System.out.println("Aucune session trouvée, redirection...");
        }
    }

    private void loadFeaturedCourses() {
        try {
            List<Course> allCourses = courseService.recuperer();
            // On affiche seulement les 3 premiers cours pour l'accueil
            List<Course> featured = allCourses.stream().limit(3).collect(Collectors.toList());
            
            courseContainer.getChildren().clear();
            for (Course course : featured) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/course/courseCard.fxml"));
                Node card = loader.load();
                
                FrontCourseCardController controller = loader.getController();
                controller.setData(course);
                
                courseContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement cours accueil : " + e.getMessage());
        }
    }

    @FXML
    private void handleCatalogue(MouseEvent event) {
        navigateTo(event, "/FrontOffice/course/courseList.fxml", "Catalogue des Formations - SkillPath");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // On vide la session (Singleton)
        Session.getInstance().logout();
        
        // Redirection vers la page de login
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    @FXML
    private void handleProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/user/home/profiluser.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Mon Profil - SkillPath");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
}
