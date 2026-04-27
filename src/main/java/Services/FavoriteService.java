package Services;

import Utils.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriteService {
    private Connection connection;

    public FavoriteService() {
        connection = Database.getInstance().getConnection();
    }

    private byte[] uuidToBytes(String uuidStr) {
        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public void addFavorite(String userId, int courseId) {
        String sql = "INSERT INTO user_course (user_id, course_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur addFavorite : " + e.getMessage());
        }
    }

    public void removeFavorite(String userId, int courseId) {
        String sql = "DELETE FROM user_course WHERE user_id = ? AND course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur removeFavorite : " + e.getMessage());
        }
    }

    public boolean isFavorite(String userId, int courseId) {
        String sql = "SELECT COUNT(*) FROM user_course WHERE user_id = ? AND course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
