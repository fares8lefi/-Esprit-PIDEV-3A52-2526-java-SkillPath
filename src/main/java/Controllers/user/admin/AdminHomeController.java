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
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminHomeController implements Initializable {

    // ── KPI Ligne 1 (existantes) ──────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label urgentCountLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label moduleCountLabel;
    @FXML private Label reclamationCountLabel;
    @FXML private Label resultatCountLabel;

    // ── KPI Ligne 2 (nouvelles) ────────────────────────────
    @FXML private Label activeUsersLabel;
    @FXML private Label unverifiedUsersLabel;
    @FXML private Label newThisWeekLabel;
    @FXML private Label activationRateLabel;
    @FXML private ProgressBar activationProgressBar;

    // ── Graphiques ─────────────────────────────────────────
    @FXML private LineChart<String, Number> registrationChart;
    @FXML private PieChart rolesChart;
    @FXML private BarChart<String, Number> categoryChart;
    @FXML private BarChart<String, Number> statusChart;

    // ── Utilisateurs récents ───────────────────────────────
    @FXML private VBox recentUsersContainer;

    @FXML private SideBarController sideBarController;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("dashboard");
        }

        User admin = Session.getInstance().getCurrentUser();
        if (admin != null && welcomeLabel != null) {
            welcomeLabel.setText("Bon retour, " + admin.getUsername() + ". Voici l'aperçu analytique de SkillPath.");
        }

        loadStats();
        loadCharts();
        loadRecentUsers();
    }

    // ══════════════════════════════════════════════════════
    //  STATS KPI
    // ══════════════════════════════════════════════════════
    private void loadStats() {
        try {
            List<User> users = userService.recuperer();
            int total = users.size();

            // ── Ligne 1 ──
            if (userCountLabel != null) userCountLabel.setText(String.valueOf(total));

            // ── Ligne 2 : stats avancées ──
            int active = userService.countActive();
            int unverified = userService.countUnverified();
            int newWeek = userService.countNewThisWeek();
            double rate = total > 0 ? (double) active / total : 0.0;

            if (activeUsersLabel != null)
                activeUsersLabel.setText(String.valueOf(active));

            if (unverifiedUsersLabel != null)
                unverifiedUsersLabel.setText(String.valueOf(unverified));

            if (newThisWeekLabel != null)
                newThisWeekLabel.setText("+" + newWeek);

            if (activationRateLabel != null)
                activationRateLabel.setText(String.format("%.0f%%", rate * 100));

            if (activationProgressBar != null)
                activationProgressBar.setProgress(rate);

        } catch (SQLDataException e) {
            System.out.println("Erreur chargement stats : " + e.getMessage());
        }

        // Valeurs par défaut pour les stats non encore branchées
        if (urgentCountLabel != null) urgentCountLabel.setText("0");
        if (courseCountLabel != null) courseCountLabel.setText("—");
        if (moduleCountLabel != null) moduleCountLabel.setText("—");
        if (reclamationCountLabel != null) reclamationCountLabel.setText("0");
        if (resultatCountLabel != null) resultatCountLabel.setText("—");
    }

    // ══════════════════════════════════════════════════════
    //  GRAPHIQUES
    // ══════════════════════════════════════════════════════
    private void loadCharts() {
        loadRegistrationChart();
        loadRolesChart();
        loadStatusChart();
    }

    /** Graphique de croissance — données réelles des 30 derniers jours */
    private void loadRegistrationChart() {
        if (registrationChart == null) return;
        registrationChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nouveaux utilisateurs");

        Map<String, Integer> data = userService.getRegistrationsByDay(30);

        if (data.isEmpty()) {
            // Fallback si aucune inscription dans les 30 derniers jours
            series.getData().add(new XYChart.Data<>("Aujourd'hui", 0));
        } else {
            data.forEach((day, count) ->
                    series.getData().add(new XYChart.Data<>(day, count)));
        }

        registrationChart.getData().add(series);
        registrationChart.setStyle("-fx-background-color: transparent;");
    }

    /** PieChart Rôles — données réelles depuis la DB */
    private void loadRolesChart() {
        if (rolesChart == null) return;
        rolesChart.getData().clear();

        Map<String, Integer> roles = userService.countByRole();

        if (roles.isEmpty()) {
            rolesChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Aucune donnée", 1)));
        } else {
            var pieData = FXCollections.<PieChart.Data>observableArrayList();
            roles.forEach((role, count) -> {
                String label = capitalizeRole(role);
                pieData.add(new PieChart.Data(label + " (" + count + ")", count));
            });
            rolesChart.setData(pieData);
        }
    }

    /** BarChart Statut des comptes — données réelles depuis la DB */
    private void loadStatusChart() {
        if (statusChart == null) return;
        statusChart.getData().clear();

        Map<String, Integer> statuses = userService.countByStatus();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Comptes");

        if (statuses.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucune donnée", 0));
        } else {
            statuses.forEach((status, count) ->
                    series.getData().add(new XYChart.Data<>(capitalizeStatus(status), count)));
        }

        statusChart.getData().add(series);
    }

    // ══════════════════════════════════════════════════════
    //  UTILISATEURS RÉCENTS
    // ══════════════════════════════════════════════════════
    private void loadRecentUsers() {
        if (recentUsersContainer == null) return;
        recentUsersContainer.getChildren().clear();

        try {
            List<User> users = userService.recuperer();
            int count = Math.min(5, users.size());

            for (int i = 0; i < count; i++) {
                recentUsersContainer.getChildren().add(createUserRow(users.get(i)));
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

        // Avatar initiale
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.setPrefSize(42, 42);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #d946ef); -fx-background-radius: 12;");
        String initial = (user.getUsername() != null && !user.getUsername().isEmpty())
                ? user.getUsername().substring(0, 1).toUpperCase() : "?";
        Label avatarLabel = new Label(initial);
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 900;");
        avatar.getChildren().add(avatarLabel);

        // Infos
        VBox info = new VBox(3);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        info.getChildren().addAll(nameLabel, emailLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Badge statut vérification
        boolean verified = user.isVerified();
        Label verifiedBadge = new Label(verified ? "✓ Vérifié" : "⚠ Non vérifié");
        verifiedBadge.setStyle(verified
                ? "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; -fx-font-size: 9; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 5;"
                : "-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; -fx-font-size: 9; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 5;");

        // Badge rôle
        Label roleLabel = new Label(user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT");
        roleLabel.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #a78bfa; "
                + "-fx-font-size: 9; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 5;");

        row.getChildren().addAll(avatar, info, spacer, verifiedBadge, roleLabel);
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════
    private String capitalizeRole(String role) {
        if (role == null || role.isBlank()) return "Inconnu";
        return switch (role.toLowerCase()) {
            case "admin", "ROLE_ADMIN" -> "Admins";
            case "student", "ROLE_USER" -> "Étudiants";
            case "instructor" -> "Instructeurs";
            default -> role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
        };
    }

    private String capitalizeStatus(String status) {
        if (status == null || status.isBlank()) return "Inconnu";
        return switch (status.toLowerCase()) {
            case "active" -> " Actif";
            case "inactive" -> " Inactif";
            case "pending" -> " En attente";
            case "banned" -> " Banni";
            default -> status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        };
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════
    @FXML
    private void handleNouvelArchitecte(ActionEvent event) {
        navigateTo(event, "/BackOffice/Admin/user/ajouterArchitecte.fxml", "Ajouter un utilisateur - SkillPath");
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
