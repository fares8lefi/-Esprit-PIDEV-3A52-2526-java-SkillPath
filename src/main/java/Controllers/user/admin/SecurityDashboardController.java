package Controllers.user.admin;

import Models.BlockedIp;
import Models.SecurityEvent;
import Utils.DatabaseConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur du tableau de bord de sécurité pour les administrateurs.
 * Affiche les IPs bloquées, les événements de sécurité et permet la gestion des blocages.
 */
public class SecurityDashboardController {

    @FXML private TableView<BlockedIp> blockedIpsTable;
    @FXML private TableColumn<BlockedIp, String> ipColumn;
    @FXML private TableColumn<BlockedIp, String> reasonColumn;
    @FXML private TableColumn<BlockedIp, Double> scoreColumn;
    @FXML private TableColumn<BlockedIp, String> blockedAtColumn;
    @FXML private TableColumn<BlockedIp, String> actionColumn;

    @FXML private TableView<SecurityEvent> eventsTable;
    @FXML private TableColumn<SecurityEvent, String> eventIpColumn;
    @FXML private TableColumn<SecurityEvent, String> eventUsernameColumn;
    @FXML private TableColumn<SecurityEvent, Double> eventScoreColumn;
    @FXML private TableColumn<SecurityEvent, String> eventActionColumn;
    @FXML private TableColumn<SecurityEvent, String> eventRiskColumn;
    @FXML private TableColumn<SecurityEvent, String> eventDateColumn;

    @FXML private Label totalBlockedLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label topThreatLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button refreshButton;

    private static final String FLASK_URL = "http://localhost:5000";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Timeline autoRefreshTimeline;
    private Connection dbConnection;

    @FXML
    public void initialize() {
        // Initialiser les connexions
        try {
            this.dbConnection = DatabaseConnection.getConnection();
        } catch (Exception e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
        }

        // Configurer les colonnes du tableau des IPs bloquées
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        blockedAtColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        formatTimestamp(cellData.getValue().getBlockedAt())));
        
        // Colonne d'actions avec bouton de déblocage
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
        actionColumn.setCellFactory(param -> new TableCell<BlockedIp, String>() {
            private final Button unblockBtn = new Button("Débloquer");
            {
                unblockBtn.setStyle("-fx-padding: 5px 10px; -fx-font-size: 11px; -fx-text-fill: white; " +
                        "-fx-background-color: #ef4444; -fx-border-radius: 4px; -fx-cursor: hand;");
                unblockBtn.setOnAction(event -> {
                    String ip = getItem();
                    if (ip != null) {
                        handleUnblockIp(ip);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : unblockBtn);
            }
        });

        // Configurer les colonnes du tableau des événements
        eventIpColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
        eventUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        eventScoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        eventActionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        eventRiskColumn.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        eventDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        formatTimestamp(cellData.getValue().getCreatedAt())));

        // Charger les données initiales
        refreshData();

        // Configuration du refresh automatique toutes les 30 secondes
        setupAutoRefresh();
    }

    /**
     * Configure le refresh automatique toutes les 30 secondes.
     */
    private void setupAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(30), event -> {
                    Platform.runLater(this::refreshData);
                })
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    /**
     * Rafraîchir tous les données du tableau de bord.
     */
    @FXML
    private void refreshData() {
        new Thread(() -> {
            try {
                // Charger les IPs bloquées
                ObservableList<BlockedIp> blockedIps = loadBlockedIps();
                // Charger les événements de sécurité
                ObservableList<SecurityEvent> events = loadSecurityEvents();
                // Calculer les statistiques
                Map<String, Object> stats = calculateStatistics(blockedIps, events);

                // Mettre à jour l'UI sur le FX Thread
                Platform.runLater(() -> {
                    blockedIpsTable.setItems(blockedIps);
                    eventsTable.setItems(events);
                    updateStatistics(stats);
                    updateLastRefreshTime();
                    loadingIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                });
            } catch (Exception e) {
                System.err.println("Erreur lors du refresh : " + e.getMessage());
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                    showAlert("Erreur", "Erreur lors du chargement des données : " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Charger les IPs bloquées depuis la base de données et le serveur Flask.
     */
    private ObservableList<BlockedIp> loadBlockedIps() throws SQLException {
        ObservableList<BlockedIp> blockedIps = FXCollections.observableArrayList();

        // 1. Charger depuis MySQL
        String sql = "SELECT id, ip, reason, score, blocked_at, expires_at, is_active " +
                "FROM blocked_ips WHERE is_active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) " +
                "ORDER BY blocked_at DESC";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                BlockedIp ip = new BlockedIp();
                ip.setId(rs.getInt("id"));
                ip.setIp(rs.getString("ip"));
                ip.setReason(rs.getString("reason"));
                ip.setScore(rs.getDouble("score"));
                ip.setBlockedAt(rs.getTimestamp("blocked_at"));
                ip.setExpiresAt(rs.getTimestamp("expires_at"));
                ip.setIsActive(rs.getBoolean("is_active"));
                blockedIps.add(ip);
            }
        }

        // 2. Enrichir avec les données du serveur Flask si disponible
        try {
            Map<String, Object> flaskData = callFlaskApi("/blocked", "GET", null);
            if (flaskData != null && (boolean) flaskData.getOrDefault("available", false)) {
                // Les données Flask pourraient enrichir les informations si nécessaire
                System.out.println("Données Flask chargées : " + flaskData.size() + " entrées");
            }
        } catch (Exception e) {
            System.out.println("Serveur Flask injoignable, utilisant les données locales seulement.");
        }

        return blockedIps;
    }

    /**
     * Charger les 50 derniers événements de sécurité.
     */
    private ObservableList<SecurityEvent> loadSecurityEvents() throws SQLException {
        ObservableList<SecurityEvent> events = FXCollections.observableArrayList();

        String sql = "SELECT id, ip, username, score, action, risk_level, created_at " +
                "FROM security_events ORDER BY created_at DESC LIMIT 50";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SecurityEvent event = new SecurityEvent();
                event.setId(rs.getLong("id"));
                event.setIp(rs.getString("ip"));
                event.setUsername(rs.getString("username"));
                event.setScore(rs.getDouble("score"));
                event.setAction(rs.getString("action"));
                event.setRiskLevel(rs.getString("risk_level"));
                event.setCreatedAt(rs.getTimestamp("created_at"));
                events.add(event);
            }
        }

        return events;
    }

    /**
     * Calculer les statistiques du tableau de bord.
     */
    private Map<String, Object> calculateStatistics(
            ObservableList<BlockedIp> blockedIps,
            ObservableList<SecurityEvent> events) throws SQLException {

        Map<String, Object> stats = new HashMap<>();

        // Total IPs bloquées
        stats.put("totalBlocked", blockedIps.size());

        // Score moyen
        double avgScore = blockedIps.stream()
                .mapToDouble(BlockedIp::getScore)
                .average()
                .orElse(0.0);
        stats.put("averageScore", String.format("%.2f", avgScore));

        // Top IP menaçante (score le plus élevé)
        String topThreat = blockedIps.stream()
                .max(Comparator.comparingDouble(BlockedIp::getScore))
                .map(BlockedIp::getIp)
                .orElse("N/A");
        stats.put("topThreat", topThreat);

        // Événements bloqués aujourd'hui
        String sqlTodayBlocked = "SELECT COUNT(*) FROM security_events " +
                "WHERE action = 'BLOCKED' AND DATE(created_at) = CURDATE()";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlTodayBlocked)) {
            if (rs.next()) {
                stats.put("blockedToday", rs.getInt(1));
            }
        }

        return stats;
    }

    /**
     * Mettre à jour les labels de statistiques.
     */
    private void updateStatistics(Map<String, Object> stats) {
        totalBlockedLabel.setText(String.valueOf(stats.get("totalBlocked")));
        averageScoreLabel.setText(String.valueOf(stats.get("averageScore")));
        topThreatLabel.setText(String.valueOf(stats.get("topThreat")));
    }

    /**
     * Débloquer une IP spécifique.
     */
    private void handleUnblockIp(String ip) {
        // Confirmation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Débloquer l'IP");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir débloquer l'IP " + ip + " ?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    // 1. Appeler l'API Flask pour débloquer
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("ip", ip);
                    callFlaskApi("/unblock", "POST", payload);

                    // 2. Mettre à jour en base locale
                    String sql = "UPDATE blocked_ips SET is_active = FALSE WHERE ip = ?";
                    try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                        ps.setString(1, ip);
                        ps.executeUpdate();
                    }

                    // 3. Rafraîchir l'affichage
                    Platform.runLater(() -> {
                        showAlert("Succès", "L'IP " + ip + " a été débloquée avec succès.");
                        refreshData();
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            showAlert("Erreur", "Erreur lors du déblocage : " + e.getMessage())
                    );
                }
            }).start();
        }
    }

    /**
     * Appeler l'API Flask.
     */
    private Map<String, Object> callFlaskApi(String endpoint, String method, Map<String, Object> payload) throws Exception {
        String url = FLASK_URL + endpoint;

        if ("GET".equals(method)) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            }
        } else if ("POST".equals(method) && payload != null) {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            }
        }
        return null;
    }

    /**
     * Formater un Timestamp pour l'affichage.
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /**
     * Mettre à jour l'heure du dernier refresh.
     */
    private void updateLastRefreshTime() {
        LocalDateTime now = LocalDateTime.now();
        lastUpdateLabel.setText("Dernière mise à jour : " +
                now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Afficher une alerte.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Bouton de refresh manuel.
     */
    @FXML
    private void onRefreshButtonClicked() {
        loadingIndicator.setVisible(true);
        refreshButton.setDisable(true);
        refreshData();
    }

    /**
     * Arrêter le refresh automatique si le contrôleur est fermé.
     */
    public void onDestroy() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }
}
