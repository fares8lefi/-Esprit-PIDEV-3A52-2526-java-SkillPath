package Services.evaluation;

import Models.evaluation.Resultat;
import Utils.Database;
import Utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResultatService {
    private Connection cnx;

    public ResultatService() {
        cnx = Database.getInstance().getConnection();
    }

    public void ajouter(Resultat resultat) throws SQLDataException {
        if (cnx == null) {
            System.err.println("Database connection is null in ResultatService.ajouter!");
            return;
        }
        String query = "INSERT INTO resultat (score, note_max, date_passage, id_quiz, id_user) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, resultat.getScore());
            ps.setInt(2, resultat.getNote_max());
            ps.setTimestamp(3, resultat.getDate_passage());
            ps.setInt(4, resultat.getId_quiz());
            ps.setBytes(5, uuidToBytes(resultat.getId_user()));
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
        // Ignore parameter and use current user from session
        if (Session.getInstance().getCurrentUser() == null) {
            System.err.println("No user logged in!");
            return list;
        }
        UUID userId = Session.getInstance().getCurrentUser().getId();
        String query = "SELECT * FROM resultat WHERE id_user = ? ORDER BY date_passage DESC";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setBytes(1, uuidToBytes(userId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Resultat(
                        rs.getInt("id_resultat"),
                        rs.getInt("score"),
                        rs.getInt("note_max"),
                        rs.getTimestamp("date_passage"),
                        rs.getInt("id_quiz"),
                        bytesToUUID(rs.getBytes("id_user"))));
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return list;
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
            buffer[i + 8] = (byte) (lsb >>> 8 * (7 - i));
        }
        return buffer;
    }

    private UUID bytesToUUID(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return null;
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
            lsb = (lsb << 8) | (bytes[i + 8] & 0xff);
        }
        return new UUID(msb, lsb);
    }
}
