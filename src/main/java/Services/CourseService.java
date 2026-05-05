package Services;

import Models.Course;
import Utils.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements Iservice<Course> {
    private Connection connection;

    public CourseService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Course course) throws SQLDataException {
        String sql = "INSERT INTO course (title, description, level, image, category, price, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, course.getTitle());
            ps.setString(2, course.getDescription());
            ps.setString(3, course.getLevel());
            ps.setString(4, course.getImage());
            ps.setString(5, course.getCategory());
            ps.setDouble(6, course.getPrice());
            ps.setTimestamp(7, Timestamp.valueOf(course.getCreatedAt() != null ? course.getCreatedAt() : java.time.LocalDateTime.now()));
            ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Course course) throws SQLDataException {
        // First delete all associated modules to avoid foreign key constraint issues
        ModuleService moduleService = new ModuleService();
        moduleService.supprimerParCours(course.getId());
        
        String sql = "DELETE FROM course WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, course.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Course course) throws SQLDataException {
        String sql = "UPDATE course SET title = ?, description = ?, level = ?, image = ?, category = ?, price = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, course.getTitle());
            ps.setString(2, course.getDescription());
            ps.setString(3, course.getLevel());
            ps.setString(4, course.getImage());
            ps.setString(5, course.getCategory());
            ps.setDouble(6, course.getPrice());
            ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setInt(8, course.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Course> recuperer() throws SQLDataException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM course";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Course c = new Course();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setLevel(rs.getString("level"));
                c.setImage(rs.getString("image"));
                c.setCategory(rs.getString("category"));
                c.setPrice(rs.getDouble("price"));
                c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                c.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                courses.add(c);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return courses;
    }

    public Course recupererParId(int id) {
        String sql = "SELECT * FROM course WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Course c = new Course();
                    c.setId(rs.getInt("id"));
                    c.setTitle(rs.getString("title"));
                    c.setDescription(rs.getString("description"));
                    c.setLevel(rs.getString("level"));
                    c.setImage(rs.getString("image"));
                    c.setCategory(rs.getString("category"));
                    c.setPrice(rs.getDouble("price"));
                    c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    c.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                    return c;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recupererParId: " + e.getMessage());
        }
        return null;
    }
}
