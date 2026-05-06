package Services.evaluation;

import Models.evaluation.Quiz;
import Services.Iservice;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService implements Iservice<Quiz> {
    private Connection connection;

    public QuizService() {
        connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Quiz quiz) throws SQLDataException {
        String sql = "INSERT INTO quiz (titre, description, duree, note_max, date_creation, course_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz.getTitre());
            ps.setString(2, quiz.getDescription());
            ps.setInt(3, quiz.getDuree());
            ps.setInt(4, quiz.getNote_max());
            ps.setTimestamp(5, quiz.getDate_creation());
            if (quiz.getCourse_id() != null) {
                ps.setInt(6, quiz.getCourse_id());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();

            // Retrieving the auto-incremented ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    quiz.setId_quiz(generatedKeys.getInt(1));
                }
            }
            System.out.println("Quiz ajouté avec succès !");
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Quiz quiz) throws SQLDataException {
        // First delete questions and results associated with this quiz to satisfy
        // foreign key constraints
        String sqlQuestions = "DELETE FROM question WHERE quiz_id = ?";
        String sqlResults = "DELETE FROM resultat WHERE id_quiz = ?";
        String sqlQuiz = "DELETE FROM quiz WHERE id_quiz = ?";

        try {
            // It's better to use a transaction for multiple deletes
            connection.setAutoCommit(false);

            try (PreparedStatement psQ = connection.prepareStatement(sqlQuestions)) {
                psQ.setInt(1, quiz.getId_quiz());
                psQ.executeUpdate();
            }

            try (PreparedStatement psR = connection.prepareStatement(sqlResults)) {
                psR.setInt(1, quiz.getId_quiz());
                psR.executeUpdate();
            }

            try (PreparedStatement psQuiz = connection.prepareStatement(sqlQuiz)) {
                psQuiz.setInt(1, quiz.getId_quiz());
                psQuiz.executeUpdate();
            }

            connection.commit();
            System.out.println("Quiz et ses données associées supprimés avec succès !");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            throw new SQLDataException("Erreur de suppression : " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void modifier(Quiz quiz) throws SQLDataException {
        String sql = "UPDATE quiz SET titre=?, description=?, duree=?, note_max=?, date_creation=?, course_id=? WHERE id_quiz=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, quiz.getTitre());
            ps.setString(2, quiz.getDescription());
            ps.setInt(3, quiz.getDuree());
            ps.setInt(4, quiz.getNote_max());
            ps.setTimestamp(5, quiz.getDate_creation());
            if (quiz.getCourse_id() != null) {
                ps.setInt(6, quiz.getCourse_id());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, quiz.getId_quiz());
            ps.executeUpdate();
            System.out.println("Quiz modifié !");
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Quiz> recuperer() throws SQLDataException {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Quiz q = new Quiz();
                q.setId_quiz(rs.getInt("id_quiz"));
                q.setTitre(rs.getString("titre"));
                q.setDescription(rs.getString("description"));
                q.setDuree(rs.getInt("duree"));
                q.setNote_max(rs.getInt("note_max"));
                q.setDate_creation(rs.getTimestamp("date_creation"));

                int courseId = rs.getInt("course_id");
                if (rs.wasNull()) {
                    q.setCourse_id(null);
                } else {
                    q.setCourse_id(courseId);
                }
                list.add(q);
            }
        } catch (SQLException e) {
            System.out.println("Erreur recuperer : " + e.getMessage());
        }
        return list;
    }

    public Quiz recupererParId(int id) {
        String sql = "SELECT * FROM quiz WHERE id_quiz = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Quiz q = new Quiz();
                q.setId_quiz(rs.getInt("id_quiz"));
                q.setTitre(rs.getString("titre"));
                q.setDescription(rs.getString("description"));
                q.setDuree(rs.getInt("duree"));
                q.setNote_max(rs.getInt("note_max"));
                q.setDate_creation(rs.getTimestamp("date_creation"));
                int courseId = rs.getInt("course_id");
                q.setCourse_id(rs.wasNull() ? null : courseId);
                return q;
            }
        } catch (SQLException e) {
            System.out.println("Erreur recupererParId : " + e.getMessage());
        }
        return null;
    }

    public List<Quiz> getByCourse(int courseId) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz WHERE course_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Quiz q = new Quiz();
                    q.setId_quiz(rs.getInt("id_quiz"));
                    q.setTitre(rs.getString("titre"));
                    q.setDescription(rs.getString("description"));
                    q.setDuree(rs.getInt("duree"));
                    q.setNote_max(rs.getInt("note_max"));
                    q.setDate_creation(rs.getTimestamp("date_creation"));
                    q.setCourse_id(rs.getInt("course_id"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur getByCourse : " + e.getMessage());
        }
        return list;
    }
}
