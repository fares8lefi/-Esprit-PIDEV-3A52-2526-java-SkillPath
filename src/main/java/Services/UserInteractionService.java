package Services;

import Utils.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserInteractionService {
    private Connection connection;

    public UserInteractionService() {
        connection = Database.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        // user_id changed to VARCHAR(36) to support UUID
        String sql = "CREATE TABLE IF NOT EXISTS user_interactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id VARCHAR(36) NOT NULL, " +
                "course_id INT NOT NULL, " +
                "category VARCHAR(255), " +
                "level VARCHAR(255), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur creation table user_interactions : " + e.getMessage());
        }
    }

    public void recordInteraction(UUID userId, int courseId, String category, String level) {
        String sql = "INSERT INTO user_interactions (user_id, course_id, category, level) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setInt(2, courseId);
            ps.setString(3, category);
            ps.setString(4, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur recordInteraction : " + e.getMessage());
        }
    }

    public List<Integer> getClickedCourseIds(UUID userId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT DISTINCT course_id FROM user_interactions WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public String getMostPreferredCategory(UUID userId) {
        String sql = "SELECT category, COUNT(*) as count FROM user_interactions WHERE user_id = ? GROUP BY category ORDER BY count DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("category");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMostPreferredLevel(UUID userId) {
        String sql = "SELECT level, COUNT(*) as count FROM user_interactions WHERE user_id = ? GROUP BY level ORDER BY count DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("level");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
