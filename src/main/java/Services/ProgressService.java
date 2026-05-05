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
        // 1. Enregistrer la complétion du module si pas déjà fait
        if (!isModuleCompleted(userId, moduleId)) {
            String sql = "INSERT INTO user_module_completion (user_id, module_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBytes(1, UUIDToBytes(userId));
                stmt.setInt(2, moduleId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur markModuleAsCompleted: " + e.getMessage());
            }
        }

        // 2. Recalculer et mettre à jour le progrès global du cours
        syncCourseProgress(userId, courseId, totalModules);
    }

    private void syncCourseProgress(UUID userId, int courseId, int totalModules) {
        // Compter le nombre réel de modules complétés pour ce cours
        String sqlCount = "SELECT COUNT(*) FROM user_module_completion umc " +
                         "JOIN module m ON umc.module_id = m.id " +
                         "WHERE umc.user_id = ? AND m.course_id = ?";
        
        int actualCount = 0;
        try (PreparedStatement stmt = connection.prepareStatement(sqlCount)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                actualCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur count completed modules: " + e.getMessage());
        }

        // Mettre à jour la table de progression globale
        UserCourseProgress progress = getProgress(userId, courseId);
        if (progress == null) {
            String sqlInsert = "INSERT INTO user_course_progress (user_id, course_id, completed_modules, is_completed) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sqlInsert)) {
                stmt.setBytes(1, UUIDToBytes(userId));
                stmt.setInt(2, courseId);
                stmt.setInt(3, actualCount);
                stmt.setBoolean(4, actualCount >= totalModules);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur insert progress: " + e.getMessage());
            }
        } else {
            String sqlUpdate = "UPDATE user_course_progress SET completed_modules = ?, is_completed = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlUpdate)) {
                stmt.setInt(1, actualCount);
                stmt.setBoolean(2, actualCount >= totalModules);
                stmt.setInt(3, progress.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur update progress: " + e.getMessage());
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
