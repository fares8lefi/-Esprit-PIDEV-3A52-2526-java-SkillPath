package Services;

import Models.Location;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationService implements Iservice<Location> {
    private Connection connection;

    public LocationService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Location location) throws SQLDataException {
        String sql = "INSERT INTO location (name, building, room_number, max_capacity, image) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getName());
            ps.setString(2, location.getBuilding());
            ps.setString(3, location.getRoomNumber());
            ps.setInt(4, location.getMaxCapacity());
            ps.setString(5, location.getImage());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Location location) throws SQLDataException {
        String sql = "DELETE FROM location WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, location.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Location location) throws SQLDataException {
        String sql = "UPDATE location SET name = ?, building = ?, room_number = ?, max_capacity = ?, image = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getName());
            ps.setString(2, location.getBuilding());
            ps.setString(3, location.getRoomNumber());
            ps.setInt(4, location.getMaxCapacity());
            ps.setString(5, location.getImage());
            ps.setInt(6, location.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Location> recuperer() throws SQLDataException {
        List<Location> locations = new ArrayList<>();
        String sql = "SELECT * FROM location";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Location loc = new Location();
                loc.setId(rs.getInt("id"));
                loc.setName(rs.getString("name"));
                loc.setBuilding(rs.getString("building"));
                loc.setRoomNumber(rs.getString("room_number"));
                loc.setMaxCapacity(rs.getInt("max_capacity"));
                loc.setImage(rs.getString("image"));
                locations.add(loc);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return locations;
    }
}
