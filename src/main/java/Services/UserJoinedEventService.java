package Services;

import Models.UserJoinedEvent;
import Utils.Database;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserJoinedEventService {
    private Connection connection;

    public UserJoinedEventService() {
        connection = Database.getInstance().getConnection();
        ensureSeatColumnExists();
    }

    private void ensureSeatColumnExists() {
        // Automatically ensure the seat_number column exists dynamically to prevent crashes
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_joined_events' AND COLUMN_NAME = 'seat_number'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE user_joined_events ADD COLUMN seat_number INT NULL");
                System.out.println("Automated Migration: Added seat_number to user_joined_events");
            }
        } catch (SQLException e) {
            System.err.println("Database check failed: " + e.getMessage());
        }
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public void joinEvent(UserJoinedEvent joined) throws SQLDataException {
        String sql = "INSERT IGNORE INTO user_joined_events (user_id, event_id, seat_number) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(joined.getUserId()));
            ps.setInt(2, joined.getEventId());
            if (joined.getSeatNumber() != null) {
                ps.setInt(3, joined.getSeatNumber());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public void leaveEvent(UUID userId, int eventId) throws SQLDataException {
        String sql = "DELETE FROM user_joined_events WHERE user_id = ? AND event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, eventId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public boolean hasJoined(UUID userId, int eventId) {
        String sql = "SELECT 1 FROM user_joined_events WHERE user_id = ? AND event_id = ?";
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

    public int getParticipantCount(int eventId) {
        String sql = "SELECT COUNT(*) FROM user_joined_events WHERE event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public List<Integer> getOccupiedSeats(int eventId) {
        List<Integer> seats = new ArrayList<>();
        String sql = "SELECT seat_number FROM user_joined_events WHERE event_id = ? AND seat_number IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                seats.add(rs.getInt("seat_number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return seats;
    }

    public List<Integer> getJoinedEventIds(UUID userId) {
        List<Integer> eventIds = new ArrayList<>();
        String sql = "SELECT event_id FROM user_joined_events WHERE user_id = ?";
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
