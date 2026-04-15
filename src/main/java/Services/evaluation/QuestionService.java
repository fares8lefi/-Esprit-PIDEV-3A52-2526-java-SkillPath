package Services.evaluation;

import Models.evaluation.Question;
import Services.Iservice;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements Iservice<Question> {
    private Connection connection;

    public QuestionService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Question q) throws SQLDataException {
        String sql = "INSERT INTO question (enonce, choix_a, choix_b, choix_c, choix_d, bonne_reponse, points, id_quiz) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getEnonce());
            ps.setString(2, q.getChoix_a());
            ps.setString(3, q.getChoix_b());
            ps.setString(4, q.getChoix_c());
            ps.setString(5, q.getChoix_d());
            ps.setString(6, q.getBonne_reponse());
            ps.setInt(7, q.getPoints());
            ps.setInt(8, q.getId_quiz());
            ps.executeUpdate();

            // Retrieve the Auto Increment key id
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    q.setId_question(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Question q) throws SQLDataException {
        String sql = "UPDATE question SET enonce=?, choix_a=?, choix_b=?, choix_c=?, choix_d=?, bonne_reponse=?, points=?, id_quiz=? WHERE id_question=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, q.getEnonce());
            ps.setString(2, q.getChoix_a());
            ps.setString(3, q.getChoix_b());
            ps.setString(4, q.getChoix_c());
            ps.setString(5, q.getChoix_d());
            ps.setString(6, q.getBonne_reponse());
            ps.setInt(7, q.getPoints());
            ps.setInt(8, q.getId_quiz());
            ps.setInt(9, q.getId_question());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Question q) throws SQLDataException {
        String sql = "DELETE FROM question WHERE id_question=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, q.getId_question());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Question> recuperer() throws SQLDataException {
        return new ArrayList<>(); // You could return all, but we usually want `recupererParQuiz`
    }

    public List<Question> recupererParQuiz(int id_quiz) throws SQLDataException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE id_quiz = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id_quiz);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setId_question(rs.getInt("id_question"));
                q.setEnonce(rs.getString("enonce"));
                q.setChoix_a(rs.getString("choix_a"));
                q.setChoix_b(rs.getString("choix_b"));
                q.setChoix_c(rs.getString("choix_c"));
                q.setChoix_d(rs.getString("choix_d"));
                q.setBonne_reponse(rs.getString("bonne_reponse"));
                q.setPoints(rs.getInt("points"));
                q.setId_quiz(rs.getInt("id_quiz"));
                list.add(q);
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return list;
    }
}
