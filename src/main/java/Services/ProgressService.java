package Services;

import Models.UserCourseProgress;
import Utils.Database;

import java.sql.*;
import java.util.UUID;

public class ProgressService {
    private final Connection connection;

    public ProgressService() {
        this.connection = Database.getInstance().getConnection();
        initTables();
    }

    private void initTables() {
        // Table for course-level progress
        String sql1 = "CREATE TABLE IF NOT EXISTS user_course_progress (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BINARY(16) NOT NULL, " +
                "course_id INT NOT NULL, " +
                "completed_modules INT DEFAULT 0, " +
                "is_completed BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE" +
                ")";
        
        // Table for individual module completion tracking
        String sql2 = "CREATE TABLE IF NOT EXISTS user_module_completion (" +
                "user_id BINARY(16) NOT NULL, " +
                "module_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, module_id)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        } catch (SQLException e) {
            System.err.println("Erreur création tables progression: " + e.getMessage());
        }
    }

    public UserCourseProgress getProgress(UUID userId, int courseId) {
        String sql = "SELECT * FROM user_course_progress WHERE user_id = ? AND course_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new UserCourseProgress(
                        rs.getInt("id"),
                        userId,
                        rs.getInt("course_id"),
                        rs.getInt("completed_modules"),
                        rs.getBoolean("is_completed")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur getProgress: " + e.getMessage());
        }
        return null;
    }

    public boolean isModuleCompleted(UUID userId, int moduleId) {
        String sql = "SELECT 1 FROM user_module_completion WHERE user_id = ? AND module_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, moduleId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erreur isModuleCompleted: " + e.getMessage());
        }
        return false;
    }

    public void incrementProgress(UUID userId, int courseId, int moduleId, int totalModules) {
        // Check if module is already completed
        if (isModuleCompleted(userId, moduleId)) {
            System.out.println("Module déjà terminé, progression ignorée.");
            return;
        }

        // Mark module as completed
        String markModuleSql = "INSERT INTO user_module_completion (user_id, module_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(markModuleSql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, moduleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur markModuleSql: " + e.getMessage());
            return;
        }

        // Increment course progress
        UserCourseProgress progress = getProgress(userId, courseId);
        if (progress == null) {
            String sql = "INSERT INTO user_course_progress (user_id, course_id, completed_modules, is_completed) VALUES (?, ?, 1, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBytes(1, UUIDToBytes(userId));
                stmt.setInt(2, courseId);
                stmt.setBoolean(3, 1 >= totalModules);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur insert progress: " + e.getMessage());
            }
        } else {
            if (!progress.isCompleted()) {
                int newCount = progress.getCompletedModules() + 1;
                boolean completed = newCount >= totalModules;
                String sql = "UPDATE user_course_progress SET completed_modules = ?, is_completed = ? WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, newCount);
                    stmt.setBoolean(2, completed);
                    stmt.setInt(3, progress.getId());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    System.err.println("Erreur update progress: " + e.getMessage());
                }
            }
        }
    }

    // Convert UUID to binary(16) for MySQL
    private byte[] UUIDToBytes(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (mostSigBits >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (leastSigBits >>> (8 * (7 - i)));
        }
        return bytes;
    }
}
