package Services;

import Models.Notification;
import Utils.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private Connection connection;

    public NotificationService() {
        connection = Database.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS notifications (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id VARCHAR(36) NOT NULL, " +
                "title VARCHAR(255) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur creation table notifications : " + e.getMessage());
        }
    }

    public void sendNotification(String userId, String title, String message) {
        String sql = "INSERT INTO notifications (user_id, title, message) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur sendNotification : " + e.getMessage());
        }
    }

    public List<Notification> getUnreadNotifications(String userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.setId(rs.getInt("id"));
                n.setUserId(rs.getString("user_id"));
                n.setTitle(rs.getString("title"));
                n.setMessage(rs.getString("message"));
                n.setRead(rs.getBoolean("is_read"));
                n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getUnreadCount(String userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void markAllAsRead(String userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
