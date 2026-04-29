package Controllers.user.auth;

import Models.BlockedIp;
import Models.SecurityEvent;
import Utils.DatabaseConnection;
import Controllers.user.admin.SideBarController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecurityDashboardController {

    @FXML private TableView<BlockedIp> blockedTable;
    @FXML private TableColumn<BlockedIp, String> colIpBlocked;
    @FXML private TableColumn<BlockedIp, String> colReason;
    @FXML private TableColumn<BlockedIp, Double> colScoreBlocked;
    @FXML private TableColumn<BlockedIp, Timestamp> colDateBlocked;

    @FXML private TableView<SecurityEvent> eventsTable;
    @FXML private TableColumn<SecurityEvent, String> colIpEvent;
    @FXML private TableColumn<SecurityEvent, String> colUserEvent;
    @FXML private TableColumn<SecurityEvent, String> colAction;
    @FXML private TableColumn<SecurityEvent, String> colRisk;
    @FXML private TableColumn<SecurityEvent, Timestamp> colDateEvent;

    @FXML private Label totalBlockedLabel;
    @FXML private Label avgScoreLabel;
    @FXML private Label topThreatLabel;

    @FXML private SideBarController sideBarController;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private Timeline autoRefresh;

    @FXML
    public void initialize() {
        // Marquer le bouton Shield Center comme actif dans la sidebar
        if (sideBarController != null) {
            sideBarController.setSelected("security");
        }
        setupColumns();
        refreshData();

        // Auto-refresh toutes les 30 secondes
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    private void setupColumns() {
        colIpBlocked.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colScoreBlocked.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDateBlocked.setCellValueFactory(new PropertyValueFactory<>("blockedAt"));

        colIpEvent.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colUserEvent.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colRisk.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colDateEvent.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    @FXML
    public void refreshData() {
        loadBlockedIps();
        loadSecurityEvents();
        updateStats();
    }

    private void loadBlockedIps() {
        List<BlockedIp> list = new ArrayList<>();
        String sql = "SELECT * FROM blocked_ips WHERE is_active = TRUE ORDER BY blocked_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                BlockedIp b = new BlockedIp();
                b.setIp(rs.getString("ip"));
                b.setReason(rs.getString("reason"));
                b.setScore(rs.getDouble("score"));
                b.setBlockedAt(rs.getTimestamp("blocked_at"));
                list.add(b);
            }
            blockedTable.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSecurityEvents() {
        List<SecurityEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM security_events ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                SecurityEvent s = new SecurityEvent();
                s.setIp(rs.getString("ip"));
                s.setUsername(rs.getString("username"));
                s.setAction(rs.getString("action"));
                s.setRiskLevel(rs.getString("risk_level"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(s);
            }
            eventsTable.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total bloqué aujourd'hui
            String sqlCount = "SELECT COUNT(*) FROM blocked_ips WHERE DATE(blocked_at) = CURDATE()";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlCount)) {
                if (rs.next()) totalBlockedLabel.setText(String.valueOf(rs.getInt(1)));
            }

            // Score moyen
            String sqlAvg = "SELECT AVG(score) FROM security_events";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlAvg)) {
                if (rs.next()) avgScoreLabel.setText(String.format("%.2f", rs.getDouble(1)));
            }

            // Top IP menaçante
            String sqlTop = "SELECT ip FROM security_events WHERE risk_level = 'CRITICAL' GROUP BY ip ORDER BY COUNT(*) DESC LIMIT 1";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlTop)) {
                if (rs.next()) topThreatLabel.setText(rs.getString(1));
                else topThreatLabel.setText("Aucune");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUnblock() {
        BlockedIp selected = blockedTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // 1. Désactiver en base locale
        String sql = "UPDATE blocked_ips SET is_active = FALSE WHERE ip = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selected.getIp());
            ps.executeUpdate();
            
            // 2. Notifier Flask
            notifyFlaskUnblock(selected.getIp());
            
            refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void notifyFlaskUnblock(String ip) {
        try {
            String json = "{\"ip\":\"" + ip + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:5000/unblock"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Erreur unblock Flask : " + e.getMessage());
        }
    }

    public void stopAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
    }
}
