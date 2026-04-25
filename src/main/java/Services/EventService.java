package Services;

import Models.Event;
import Models.Location;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService implements Iservice<Event> {
    private Connection connection;

    public EventService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Event event) throws SQLDataException {
        String sql = "INSERT INTO event (title, description, event_date, start_time, end_time, image, location_id, average_rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setDate(3, Date.valueOf(event.getEventDate()));
            ps.setTime(4, Time.valueOf(event.getStartTime()));
            ps.setTime(5, Time.valueOf(event.getEndTime()));
            ps.setString(6, event.getImage());
            if (event.getLocationId() > 0) {
                ps.setInt(7, event.getLocationId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setDouble(8, event.getAverageRating());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Event event) throws SQLDataException {
        String sql = "DELETE FROM event WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, event.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Event event) throws SQLDataException {
        String sql = "UPDATE event SET title = ?, description = ?, event_date = ?, start_time = ?, end_time = ?, image = ?, location_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setDate(3, Date.valueOf(event.getEventDate()));
            ps.setTime(4, Time.valueOf(event.getStartTime()));
            ps.setTime(5, Time.valueOf(event.getEndTime()));
            ps.setString(6, event.getImage());
            if (event.getLocationId() > 0) {
                ps.setInt(7, event.getLocationId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setInt(8, event.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Event> recuperer() throws SQLDataException {
        List<Event> events = new ArrayList<>();
        // Join with location to get location details
        String sql = "SELECT e.*, l.name as loc_name, l.building as loc_building, l.room_number as loc_room FROM event e LEFT JOIN location l ON e.location_id = l.id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setEventDate(rs.getDate("event_date").toLocalDate());
                e.setStartTime(rs.getTime("start_time").toLocalTime());
                e.setEndTime(rs.getTime("end_time").toLocalTime());
                e.setImage(rs.getString("image"));
                e.setLocationId(rs.getInt("location_id"));
                e.setAverageRating(rs.getDouble("average_rating"));

                if (e.getLocationId() > 0) {
                    Location loc = new Location();
                    loc.setId(e.getLocationId());
                    loc.setName(rs.getString("loc_name"));
                    loc.setBuilding(rs.getString("loc_building"));
                    loc.setRoomNumber(rs.getString("loc_room"));
                    e.setLocation(loc);
                }

                events.add(e);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return events;
    }

    public void updateAverageRating(int eventId) {
        String query = "SELECT AVG(score) FROM event_rating WHERE event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double avg = rs.getDouble(1);
                String updateQuery = "UPDATE event SET average_rating = ? WHERE id = ?";
                try(PreparedStatement psUpdate = connection.prepareStatement(updateQuery)) {
                    psUpdate.setDouble(1, avg);
                    psUpdate.setInt(2, eventId);
                    psUpdate.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
