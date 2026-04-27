package Services;

import Models.Module;
import Utils.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleService implements Iservice<Module> {
    private Connection connection;

    public ModuleService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Module module) throws SQLDataException {
        String sql = "INSERT INTO module (title, description, created_at, updated_at, level, image, type, content, document, course_id, scheduled_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, module.getTitle());
            ps.setString(2, module.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(module.getCreatedAt() != null ? module.getCreatedAt() : java.time.LocalDateTime.now()));
            ps.setTimestamp(4, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(5, module.getLevel());
            ps.setString(6, module.getImage());
            ps.setString(7, module.getType());
            ps.setString(8, module.getContent());
            ps.setString(9, module.getDocument());
            ps.setInt(10, module.getCourseId());
            ps.setTimestamp(11, module.getScheduledAt() != null ? Timestamp.valueOf(module.getScheduledAt()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Module module) throws SQLDataException {
        String sql = "DELETE FROM module WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, module.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Module module) throws SQLDataException {
        String sql = "UPDATE module SET title = ?, description = ?, updated_at = ?, level = ?, image = ?, type = ?, content = ?, document = ?, course_id = ?, scheduled_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, module.getTitle());
            ps.setString(2, module.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(4, module.getLevel());
            ps.setString(5, module.getImage());
            ps.setString(6, module.getType());
            ps.setString(7, module.getContent());
            ps.setString(8, module.getDocument());
            ps.setInt(9, module.getCourseId());
            ps.setTimestamp(10, module.getScheduledAt() != null ? Timestamp.valueOf(module.getScheduledAt()) : null);
            ps.setInt(11, module.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Module> recuperer() throws SQLDataException {
        List<Module> modules = new ArrayList<>();
        String sql = "SELECT * FROM module";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Module m = new Module();
                m.setId(rs.getInt("id"));
                m.setTitle(rs.getString("title"));
                m.setDescription(rs.getString("description"));
                m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                m.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                m.setLevel(rs.getString("level"));
                m.setImage(rs.getString("image"));
                m.setType(rs.getString("type"));
                m.setContent(rs.getString("content"));
                m.setDocument(rs.getString("document"));
                m.setCourseId(rs.getInt("course_id"));
                m.setScheduledAt(rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toLocalDateTime() : null);
                modules.add(m);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return modules;
    }

    /**
     * Returns the number of modules belonging to the given course.
     */
    public int countByCourse(int courseId) throws SQLDataException {
        String sql = "SELECT COUNT(*) FROM module WHERE course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return 0;
    }

    /**
     * Returns all modules belonging to the given course.
     */
    public List<Module> getByCourse(int courseId) throws SQLDataException {
        List<Module> modules = new ArrayList<>();
        String sql = "SELECT * FROM module WHERE course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Module m = new Module();
                    m.setId(rs.getInt("id"));
                    m.setTitle(rs.getString("title"));
                    m.setDescription(rs.getString("description"));
                    m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    m.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                    m.setLevel(rs.getString("level"));
                    m.setImage(rs.getString("image"));
                    m.setType(rs.getString("type"));
                    m.setContent(rs.getString("content"));
                    m.setDocument(rs.getString("document"));
                    m.setCourseId(rs.getInt("course_id"));
                    m.setScheduledAt(rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toLocalDateTime() : null);
                    modules.add(m);
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return modules;
    }

    /**
     * Deletes all modules associated with a specific course.
     */
    public void supprimerParCours(int courseId) throws SQLDataException {
        String sql = "DELETE FROM module WHERE course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }
}
