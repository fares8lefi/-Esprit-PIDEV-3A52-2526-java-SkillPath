package Services;

import Models.UserFavouriteEvent;
import Utils.Database;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserFavouriteEventService {
    private Connection connection;

    public UserFavouriteEventService() {
        connection = Database.getInstance().getConnection();
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public void addFavourite(UserFavouriteEvent fav) throws SQLDataException {
        String sql = "INSERT IGNORE INTO user_favorite_events (user_id, event_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(fav.getUserId()));
            ps.setInt(2, fav.getEventId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public void removeFavourite(UUID userId, int eventId) throws SQLDataException {
        String sql = "DELETE FROM user_favorite_events WHERE user_id = ? AND event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, eventId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public boolean isFavourite(UUID userId, int eventId) {
        String sql = "SELECT 1 FROM user_favorite_events WHERE user_id = ? AND event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Integer> getFavouriteEventIds(UUID userId) {
        List<Integer> eventIds = new ArrayList<>();
        String sql = "SELECT event_id FROM user_favorite_events WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                eventIds.add(rs.getInt("event_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return eventIds;
    }
}
