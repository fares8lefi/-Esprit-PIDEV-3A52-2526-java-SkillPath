package Services.evaluation;

import Models.evaluation.Resultat;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultatService {
    private Connection cnx;

    public ResultatService() {
        cnx = Database.getInstance().getConnection();
    }

    public void ajouter(Resultat resultat) throws SQLDataException {
        String query = "INSERT INTO resultat (score, note_max, date_passage, id_quiz, id_etudiant) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, resultat.getScore());
            ps.setInt(2, resultat.getNote_max());
            ps.setTimestamp(3, resultat.getDate_passage());
            ps.setInt(4, resultat.getId_quiz());
            ps.setInt(5, resultat.getId_etudiant());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resultat.setId_resultat(generatedKeys.getInt(1));
                }
            }
            System.out.println("Résultat enregistré !");
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public List<Resultat> recupererParEtudiant(int idEtudiant) throws SQLDataException {
        List<Resultat> list = new ArrayList<>();
        String query = "SELECT * FROM resultat WHERE id_etudiant = ? ORDER BY date_passage DESC";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, idEtudiant);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Resultat(
                        rs.getInt("id_resultat"),
                        rs.getInt("score"),
                        rs.getInt("note_max"),
                        rs.getTimestamp("date_passage"),
                        rs.getInt("id_quiz"),
                        rs.getInt("id_etudiant")));
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return list;
    }
}
