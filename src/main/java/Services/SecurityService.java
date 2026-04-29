package Services;

import Models.User;
import Models.LoginResult;
import Utils.DatabaseConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service de sécurité interceptant les tentatives de connexion et communiquant avec le serveur ML Flask.
 */
public class SecurityService {

    private static final String FLASK_URL = "http://localhost:5000/predict";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginResult login(String email, String password, String clientIp, Connection conn) {
        try {
            // 1. Vérifier si l'IP est bloquée en base locale
            if (isIpBlocked(clientIp, conn)) {
                return new LoginResult(false, null, true, "CRITICAL", "Accès bloqué - Contacter l'administrateur");
            }

            // 2. Tenter l'authentification
            User user = authenticate(email, password, conn);
            String status = (user != null) ? "SUCCESS" : "FAILED";

            // 3. Enregistrer le log dans login_logs
            byte[] userIdBytes = (user != null) ? DatabaseConnection.uuidToBytes(user.getId()) : null;
            saveLoginLog(userIdBytes, clientIp, email, status, conn);

            // 4. Préparer les données pour Flask
            Map<String, Object> metrics = calculateMetrics(clientIp, conn);
            Map<String, Object> payload = new HashMap<>(metrics);
            payload.put("ip", clientIp);
            payload.put("username", email);
            payload.put("status", status);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            payload.put("timestamp", sdf.format(new Date()));

            // 5. Appeler Flask (ML Prediction)
            Map<String, Object> prediction = callFlaskPredict(payload);

            if (prediction == null) {
                // Fallback local si Flask est DOWN
                return handleLocalFallback(clientIp, status, user, conn);
            }

            // 6. Traiter la réponse Flask
            String action = (String) prediction.getOrDefault("action", "ALLOW");
            String riskLevel = (String) prediction.getOrDefault("risk", "LOW");
            double score = ((Number) prediction.getOrDefault("score", 0.0)).doubleValue();
            boolean block = (boolean) prediction.getOrDefault("block", false);

            if (block || "BLOCK".equals(action)) {
                blockIp(clientIp, "ML Detection: " + riskLevel, score, riskLevel, conn);
                saveSecurityEvent(clientIp, email, score, "BLOCKED", riskLevel, conn);
                return new LoginResult(false, null, true, riskLevel, "Alerte de sécurité : accès bloqué");
            }

            if ("MONITOR".equals(action)) {
                saveSecurityEvent(clientIp, email, score, "MONITORED", riskLevel, conn);
            }

            if (user != null) {
                return new LoginResult(true, user, false, riskLevel, "Connexion réussie");
            } else {
                return new LoginResult(false, null, false, riskLevel, "Email ou mot de passe incorrect");
            }

        } catch (Exception e) {
            System.err.println("Erreur SecurityService : " + e.getMessage());
            return new LoginResult(false, null, false, "LOW", "Erreur technique lors de la connexion");
        }
    }

    private boolean isIpBlocked(String ip, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blocked_ips WHERE ip = ? AND is_active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private User authenticate(String email, String password, Connection conn) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                // Adaptation pour les hashs Symfony ($2y$ -> $2a$)
                String hash = storedHash;
                if (hash.startsWith("$2y$")) hash = "$2a$" + hash.substring(4);
                
                if (BCrypt.checkpw(password, hash)) {
                    User user = new User();
                    user.setId(DatabaseConnection.bytesToUuid(rs.getBytes("id")));
                    user.setEmail(rs.getString("email"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        }
        return null;
    }

    private void saveLoginLog(byte[] userId, String ip, String username, String status, Connection conn) throws SQLException {
        String sql = "INSERT INTO login_logs (user_id, ip, username, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBytes(1, userId);
            ps.setString(2, ip);
            ps.setString(3, username);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    private Map<String, Object> calculateMetrics(String ip, Connection conn) throws SQLException {
        Map<String, Object> metrics = new HashMap<>();
        
        // Attempts last 60s
        String sqlAttempts = "SELECT COUNT(*) FROM login_logs WHERE ip = ? AND created_at > (NOW() - INTERVAL 1 MINUTE)";
        try (PreparedStatement ps = conn.prepareStatement(sqlAttempts)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            metrics.put("attempts_last_60s", rs.next() ? rs.getInt(1) : 0);
        }

        // Distinct users last 60s
        String sqlUsers = "SELECT COUNT(DISTINCT username) FROM login_logs WHERE ip = ? AND created_at > (NOW() - INTERVAL 1 MINUTE)";
        try (PreparedStatement ps = conn.prepareStatement(sqlUsers)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            metrics.put("distinct_users_last_60s", rs.next() ? rs.getInt(1) : 0);
        }

        // Time between attempts (seconds)
        String sqlTime = "SELECT created_at FROM login_logs WHERE ip = ? ORDER BY created_at DESC LIMIT 1, 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlTime)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp last = rs.getTimestamp(1);
                long diff = (System.currentTimeMillis() - last.getTime()) / 1000;
                metrics.put("time_between_attempts", Math.max(0, diff));
            } else {
                metrics.put("time_between_attempts", 0);
            }
        }
        
        return metrics;
    }

    private Map<String, Object> callFlaskPredict(Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FLASK_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            }
        } catch (Exception e) {
            System.err.println("Serveur Flask injoignable : " + e.getMessage());
        }
        return null;
    }

    private LoginResult handleLocalFallback(String ip, String status, User user, Connection conn) throws SQLException {
        // Règle locale : 5 échecs en 60s = block 5 minutes
        String sql = "SELECT COUNT(*) FROM login_logs WHERE ip = ? AND status = 'FAILED' AND created_at > (NOW() - INTERVAL 1 MINUTE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) >= 5) {
                Timestamp expires = new Timestamp(System.currentTimeMillis() + (5 * 60 * 1000));
                blockIpWithExpiry(ip, "Local Fallback: Too many attempts", 0.9, "HIGH", expires, conn);
                return new LoginResult(false, null, true, "HIGH", "Trop de tentatives. Accès bloqué 5min.");
            }
        }
        
        if (user != null) return new LoginResult(true, user, false, "LOW", "Connexion réussie");
        return new LoginResult(false, null, false, "LOW", "Email ou mot de passe incorrect");
    }

    private void blockIp(String ip, String reason, double score, String risk, Connection conn) throws SQLException {
        String sql = "INSERT INTO blocked_ips (ip, reason, score) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE is_active = TRUE, reason = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setDouble(3, score);
            ps.setString(4, reason);
            ps.executeUpdate();
        }
    }

    private void blockIpWithExpiry(String ip, String reason, double score, String risk, Timestamp expires, Connection conn) throws SQLException {
        String sql = "INSERT INTO blocked_ips (ip, reason, score, expires_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE is_active = TRUE, expires_at = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setDouble(3, score);
            ps.setTimestamp(4, expires);
            ps.setTimestamp(5, expires);
            ps.executeUpdate();
        }
    }

    private void saveSecurityEvent(String ip, String username, double score, String action, String risk, Connection conn) throws SQLException {
        String sql = "INSERT INTO security_events (ip, username, score, action, risk_level) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, username);
            ps.setDouble(3, score);
            ps.setString(4, action);
            ps.setString(5, risk);
            ps.executeUpdate();
        }
    }
}
