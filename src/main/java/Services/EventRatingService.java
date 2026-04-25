package Services;

import Models.EventRating;
import Utils.Database;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventRatingService implements Iservice<EventRating> {
    private Connection connection;

    public EventRatingService() {
        connection = Database.getInstance().getConnection();
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    @Override
    public void ajouter(EventRating rating) throws SQLDataException {
        // Upsert logic if the user already rated
        String sql = "INSERT INTO event_rating (score, event_id, user_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE score = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rating.getScore());
            ps.setInt(2, rating.getEventId());
            ps.setBytes(3, uuidToBytes(rating.getUserId()));
            ps.setInt(4, rating.getScore());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(EventRating rating) throws SQLDataException {
        String sql = "DELETE FROM event_rating WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rating.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(EventRating rating) throws SQLDataException {
        String sql = "UPDATE event_rating SET score = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rating.getScore());
            ps.setInt(2, rating.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<EventRating> recuperer() throws SQLDataException {
        List<EventRating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM event_rating";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                EventRating r = new EventRating();
                r.setId(rs.getInt("id"));
                r.setScore(rs.getInt("score"));
                r.setEventId(rs.getInt("event_id"));
                r.setUserId(bytesToUuid(rs.getBytes("user_id")));
                ratings.add(r);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return ratings;
    }

    public int getUserRatingForEvent(int eventId, UUID userId) {
        String sql = "SELECT score FROM event_rating WHERE event_id = ? AND user_id = ?";
        try(PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setBytes(2, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getInt("score");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // Not rated
    }
}
