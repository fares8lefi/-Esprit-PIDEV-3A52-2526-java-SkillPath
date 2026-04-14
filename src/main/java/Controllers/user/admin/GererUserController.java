package Controllers.user.admin;

import Models.User;
import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la vue de gestion des utilisateurs (Annuaire).
 */
public class GererUserController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label lblTotalUsers;
    @FXML private FlowPane usersContainer;
    @FXML private ComboBox<String> comboTri;

    private final UserService userService = new UserService();
    private List<User> currentUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboTri.getItems().addAll("Nom (A-Z)", "Nom (Z-A)", "Plus Récent", "Plus Ancien", "Rôle");
        comboTri.getSelectionModel().select("Nom (A-Z)");
        loadUsers(null);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String query = searchField.getText();
        loadUsers(query);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadUsers(null);
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        navigateTo(event, "/BackOffice/Admin/user/ajouterArchitecte.fxml", "Créer un Utilisateur - SkillPath");
    }

    @FXML
    private void handleSort(ActionEvent event) {
        if (currentUsers != null && !currentUsers.isEmpty()) {
            sortUsers();
        }
    }

    private void loadUsers(String query) {
        usersContainer.getChildren().clear();
        try {
            if (query != null && !query.trim().isEmpty()) {
                User fake = new User();
                fake.setUsername(query.trim());
                currentUsers = userService.serchByName(fake);
            } else {
                currentUsers = userService.recuperer();
            }

            lblTotalUsers.setText("Total: " + currentUsers.size() + " utilisateur" + (currentUsers.size() > 1 ? "s" : ""));
            
            sortUsers();
        } catch (SQLDataException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    private void sortUsers() {
        if (currentUsers == null) return;
        
        String tri = comboTri.getValue();
        if (tri == null) tri = "Nom (A-Z)";
        
        switch (tri) {
            case "Nom (Z-A)":
                currentUsers.sort((u1, u2) -> {
                    String n1 = u1.getUsername() == null ? "" : u1.getUsername();
                    String n2 = u2.getUsername() == null ? "" : u2.getUsername();
                    return n2.compareToIgnoreCase(n1);
                });
                break;
            case "Plus Récent":
                currentUsers.sort((u1, u2) -> {
                    if (u1.getCreatedAt() == null) return 1;
                    if (u2.getCreatedAt() == null) return -1;
                    return u2.getCreatedAt().compareTo(u1.getCreatedAt());
                });
                break;
            case "Plus Ancien":
                currentUsers.sort((u1, u2) -> {
                    if (u1.getCreatedAt() == null) return 1;
                    if (u2.getCreatedAt() == null) return -1;
                    return u1.getCreatedAt().compareTo(u2.getCreatedAt());
                });
                break;
            case "Rôle":
                currentUsers.sort((u1, u2) -> {
                    String r1 = u1.getRole() == null ? "" : u1.getRole();
                    String r2 = u2.getRole() == null ? "" : u2.getRole();
                    return r1.compareToIgnoreCase(r2);
                });
                break;
            case "Nom (A-Z)":
            default:
                currentUsers.sort((u1, u2) -> {
                    String n1 = u1.getUsername() == null ? "" : u1.getUsername();
                    String n2 = u2.getUsername() == null ? "" : u2.getUsername();
                    return n1.compareToIgnoreCase(n2);
                });
                break;
        }
        
        // Re-render
        usersContainer.getChildren().clear();
        for (User user : currentUsers) {
            usersContainer.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(User user) {
        VBox card = new VBox(12);
        card.setPrefWidth(290);
        card.getStyleClass().add("glass-card");
        
        // Base styling for the card
        String defaultStyle = "-fx-padding: 30 25; -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1; -fx-background-color: rgba(30, 41, 59, 0.8);";
        String hoverStyle = "-fx-padding: 30 25; -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(139, 92, 246, 0.4); -fx-border-width: 1; -fx-background-color: rgba(30, 41, 59, 0.95); -fx-cursor: hand;";
        
        card.setStyle(defaultStyle);
        card.setAlignment(Pos.TOP_CENTER);
        
        // Hover effects on the card
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        // Initials Avatar with DropShadow
        StackPane avatar = new StackPane();
        
        // Determine gradient based on role
        String gradient = "linear-gradient(to bottom right, #f59e0b, #ea580c)";
        String roleStr = user.getRole() != null ? user.getRole().toUpperCase() : "ÉTUDIANT";
        if(roleStr.startsWith("ROLE_")) { roleStr = roleStr.substring(5); }
        
        if ("ADMIN".equals(roleStr) || "ARCHITECTE".equals(roleStr)) {
            gradient = "linear-gradient(to bottom right, #8b5cf6, #d946ef)"; // Purple/Pink
        } else if ("INSTRUCTEUR".equals(roleStr) || "TEACHER".equals(roleStr)) {
            gradient = "linear-gradient(to bottom right, #3b82f6, #0ea5e9)"; // Blue
        }
        
        avatar.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 35; -fx-min-width: 70; -fx-min-height: 70; -fx-max-width: 70; -fx-max-height: 70;");
        
        // Add a subtle shadow to the avatar
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.4));
        shadow.setRadius(10);
        shadow.setOffsetY(4);
        avatar.setEffect(shadow);

        String initialStr = "?";
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            initialStr = user.getUsername().substring(0, 1).toUpperCase();
        }
        Label initial = new Label(initialStr);
        initial.setStyle("-fx-font-size: 26; -fx-font-weight: 900; -fx-text-fill: white; -fx-font-family: 'Segoe UI', sans-serif;");
        avatar.getChildren().add(initial);

        // Name and Email Container
        VBox nameBox = new VBox(4);
        nameBox.setAlignment(Pos.CENTER);
        
        HBox nameLine = new HBox(5);
        nameLine.setAlignment(Pos.CENTER);
        
        Label lblName = new Label(user.getUsername() != null ? user.getUsername() : "Inconnu");
        lblName.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: white;");
        nameLine.getChildren().add(lblName);
        
        if (user.isVerified()) {
            Label lblVerified = new Label("✓");
            // Tooltip for verified
            Tooltip tooltip = new Tooltip("Compte Vérifié");
            Tooltip.install(lblVerified, tooltip);
            lblVerified.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: 900; -fx-font-size: 16;");
            nameLine.getChildren().add(lblVerified);
        }
        
        Label lblEmail = new Label(user.getEmail() != null ? user.getEmail() : "N/A");
        lblEmail.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
        lblEmail.setWrapText(true);
        lblEmail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        nameBox.getChildren().addAll(nameLine, lblEmail);

        // Member since 
        Label lblDate = new Label("Membre depuis: " + (user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : "N/A"));
        lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b; -fx-font-style: italic;");

        // Role & Status Badges
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER);
        
        Label roleBadge = new Label(roleStr);
        if ("ADMIN".equals(roleStr) || "ARCHITECTE".equals(roleStr)) {
            roleBadge.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #c084fc; -fx-padding: 5 12; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: 900; -fx-tracking: 1;");
        } else {
            roleBadge.setStyle("-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #fbbf24; -fx-padding: 5 12; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: 900; -fx-tracking: 1;");
        }

        String statusStr = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
        Label statusBadge = new Label("● " + statusStr);
        if ("ACTIVE".equalsIgnoreCase(statusStr)) {
            statusBadge.setStyle("-fx-background-color: rgba(34,197,94,0.15); -fx-text-fill: #4ade80; -fx-padding: 5 12; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: 900; -fx-tracking: 1;");
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; -fx-padding: 5 12; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: 900; -fx-tracking: 1;");
        }
        badges.getChildren().addAll(roleBadge, statusBadge);

        // Extra Info Tags (Domaine, Niveau, Style)
        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(5);
        tagsPane.setVgap(5);
        tagsPane.setAlignment(Pos.CENTER);
        
        if (user.getDomaine() != null && !user.getDomaine().trim().isEmpty()) {
            Label tagDomaine = new Label(user.getDomaine());
            tagDomaine.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #cbd5e1; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6;");
            tagsPane.getChildren().add(tagDomaine);
        }
        if (user.getNiveau() != null && !user.getNiveau().trim().isEmpty()) {
            Label tagNiveau = new Label(user.getNiveau());
            tagNiveau.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #cbd5e1; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6;");
            tagsPane.getChildren().add(tagNiveau);
        }
        if (user.getStyleDapprentissage() != null && !user.getStyleDapprentissage().trim().isEmpty()) {
            Label tagStyle = new Label(user.getStyleDapprentissage());
            tagStyle.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #cbd5e1; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6;");
            tagsPane.getChildren().add(tagStyle);
        }

        // Actions (Modifier & Supprimer)
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setStyle("-fx-padding: 20 0 0 0; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1 0 0 0; -fx-margin-top: 5;");
        
        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;");
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;"));
        btnEdit.setOnAction(e -> openEditModal(user));

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(239,68,68,0.2); -fx-border-radius: 12; -fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-border-color: rgba(239,68,68,0.4); -fx-border-radius: 12; -fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(239,68,68,0.2); -fx-border-radius: 12; -fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;"));
        btnDelete.setOnAction(e -> deleteUser(user));
        
        actions.getChildren().addAll(btnEdit, btnDelete);

        // Adding an empty region for spacing if needed, but spacing is handled by VBox
        card.getChildren().addAll(avatar, nameBox, lblDate, badges, tagsPane, actions);
        return card;
    }

    private void deleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de Suppression");
        confirmation.setHeaderText("Supprimer " + user.getUsername() + " ?");
        confirmation.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimer(user);
                loadUsers(searchField.getText()); // Refresh with same search query if any
            } catch (SQLDataException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private void openEditModal(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BackOffice/Admin/user/ModifyUser.fxml"));
            Parent root = loader.load();

            ModifyUserController controller = loader.getController();
            controller.initData(user, () -> loadUsers(searchField.getText()));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
            e.printStackTrace();
        }
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
            showAlert(Alert.AlertType.ERROR, "Navigation", "Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
