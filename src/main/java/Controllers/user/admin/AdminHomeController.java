package Controllers.user.admin;

import Models.User;
import Services.UserService;
import Utils.Session;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.List;
import java.util.ResourceBundle;

public class AdminHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label urgentCountLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label moduleCountLabel;
    @FXML private Label reclamationCountLabel;
    @FXML private Label resultatCountLabel;
    @FXML private LineChart<String, Number> registrationChart;
    @FXML private PieChart rolesChart;
    @FXML private BarChart<String, Number> categoryChart;
    @FXML private VBox recentUsersContainer;
    @FXML private SideBarController sideBarController;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("dashboard");
        }
        // Affichage du nom de l'admin connecté via la Session Singleton
        User admin = Session.getInstance().getCurrentUser();
        if (admin != null && welcomeLabel != null) {
            welcomeLabel.setText("Bon retour, " + admin.getUsername() + ". Voici l'aperçu analytique de SkillPath.");
        }

        // Chargement des statistiques
        loadStats();
        loadCharts();
        loadRecentUsers();
    }

    private void loadStats() {
        try {
            List<User> users = userService.recuperer();
            if (userCountLabel != null)
                userCountLabel.setText(String.valueOf(users.size()));
        } catch (SQLDataException e) {
            System.out.println("Erreur chargement stats : " + e.getMessage());
        }

        // Valeurs par défaut pour les autres stats
        // (à remplacer par vos vrais services quand ils seront disponibles)
        if (urgentCountLabel != null) urgentCountLabel.setText("0");
        if (courseCountLabel != null) courseCountLabel.setText("—");
        if (moduleCountLabel != null) moduleCountLabel.setText("—");
        if (reclamationCountLabel != null) reclamationCountLabel.setText("0");
        if (resultatCountLabel != null) resultatCountLabel.setText("—");
    }

    private void loadCharts() {
        // Graphique de croissance (données exemple)
        if (registrationChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nouveaux utilisateurs");
            series.getData().add(new XYChart.Data<>("Lun", 5));
            series.getData().add(new XYChart.Data<>("Mar", 12));
            series.getData().add(new XYChart.Data<>("Mer", 8));
            series.getData().add(new XYChart.Data<>("Jeu", 20));
            series.getData().add(new XYChart.Data<>("Ven", 15));
            series.getData().add(new XYChart.Data<>("Sam", 7));
            series.getData().add(new XYChart.Data<>("Dim", 3));
            registrationChart.getData().add(series);
            registrationChart.setStyle("-fx-background-color: transparent;");
        }

        // Graphique Rôles (Pie)
        if (rolesChart != null) {
            rolesChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Étudiants", 85),
                new PieChart.Data("Admins", 15)
            ));
        }

        // Graphique Catégories (Bar)
        if (categoryChart != null) {
            XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
            barSeries.getData().add(new XYChart.Data<>("Dev Web", 24));
            barSeries.getData().add(new XYChart.Data<>("Data", 18));
            barSeries.getData().add(new XYChart.Data<>("Design", 12));
            barSeries.getData().add(new XYChart.Data<>("Mobile", 9));
            categoryChart.getData().add(barSeries);
        }
    }

    private void loadRecentUsers() {
        if (recentUsersContainer == null) return;

        try {
            List<User> users = userService.recuperer();
            int count = Math.min(5, users.size());

            for (int i = 0; i < count; i++) {
                User u = users.get(i);
                HBox row = createUserRow(u);
                recentUsersContainer.getChildren().add(row);
            }

            if (users.isEmpty()) {
                Label empty = new Label("Aucun utilisateur trouvé.");
                empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");
                recentUsersContainer.getChildren().add(empty);
            }
        } catch (SQLDataException e) {
            System.out.println("Erreur chargement utilisateurs : " + e.getMessage());
        }
    }

    private HBox createUserRow(User user) {
        HBox row = new HBox(15);
        row.setStyle("-fx-padding: 10 15; -fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar initial
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.setPrefSize(42, 42);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #d946ef); -fx-background-radius: 12;");
        String initial = (user.getUsername() != null && !user.getUsername().isEmpty())
                ? user.getUsername().substring(0, 1).toUpperCase() : "?";
        Label avatarLabel = new Label(initial);
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 900;");
        avatar.getChildren().add(avatarLabel);

        // Infos utilisateur
        VBox info = new VBox(3);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        info.getChildren().addAll(nameLabel, emailLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Badge rôle
        Label roleLabel = new Label(user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT");
        roleLabel.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #a78bfa; "
                + "-fx-font-size: 9; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 5;");

        row.getChildren().addAll(avatar, info, spacer, roleLabel);
        return row;
    }

    @FXML
    private void handleNouvelArchitecte(ActionEvent event) {
        navigateTo(event, "/BackOffice/Admin/user/ajouterArchitecte.fxml", "Ajouter un architecte - SkillPath");
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        Session.getInstance().logout();
        navigateTo(event, "/FrontOffice/user/auth/login.fxml", "Connexion - SkillPath");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
