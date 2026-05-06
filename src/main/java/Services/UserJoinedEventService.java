package Services;

import Models.UserJoinedEvent;
import Utils.Database;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.LinkedHashMap;

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

    /** Returns the seat number for a user+event registration, or null if none. */
    public Integer getSeatNumber(UUID userId, int eventId) {
        String sql = "SELECT seat_number FROM user_joined_events WHERE user_id = ? AND event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, uuidToBytes(userId));
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int seat = rs.getInt("seat_number");
                return rs.wasNull() ? null : seat;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Analytics Methods ───────────────────────────────────────────────────

    /** Total number of registrations across all events. */
    public int getTotalParticipants() {
        String sql = "SELECT COUNT(*) FROM user_joined_events";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Map of eventId → participant count for every event that has registrations. */
    public Map<Integer, Integer> getParticipantCountPerEvent() {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT event_id, COUNT(*) AS cnt FROM user_joined_events GROUP BY event_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getInt("event_id"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Returns the top-N most popular events as a list of int[]{eventId, participantCount},
     * ordered by count descending.
     */
    public List<int[]> getTopEvents(int limit) {
        List<int[]> result = new ArrayList<>();
        String sql = "SELECT event_id, COUNT(*) AS cnt FROM user_joined_events " +
                     "GROUP BY event_id ORDER BY cnt DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new int[]{rs.getInt("event_id"), rs.getInt("cnt")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns registrations grouped by month (last 12 months).
     * Key: "YYYY-MM", Value: registration count.
     */
    public Map<String, Integer> getRegistrationsByMonth() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(e.event_date, '%Y-%m') AS month, COUNT(uje.event_id) AS cnt " +
                     "FROM event e LEFT JOIN user_joined_events uje ON e.id = uje.event_id " +
                     "WHERE e.event_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                     "GROUP BY month ORDER BY month ASC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("month"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}
