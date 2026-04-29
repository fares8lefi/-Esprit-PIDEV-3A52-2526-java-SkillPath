package Services;

import Models.Reponse;
import Utils.Database;
import Utils.OllamaContentFilterService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements Iservice<Reponse> {

    private static final int BAD_WORD_DEACTIVATION_SECONDS = 30;
    private final Connection connection;
    private final UserService userService;

    public ReponseService() {
        this.connection = Database.getInstance().getConnection();
        this.userService = new UserService();
    }

    @Override
    public void ajouter(Reponse reponse) throws SQLDataException {
        String sql = "INSERT INTO reponse (message, reclamation_id, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String filteredMessage = OllamaContentFilterService.censorBadWords(reponse.getMessage());
            suspendClientIfProfanityDetected(reponse.getUserIdBytes(), reponse.getMessage(), filteredMessage);
            reponse.setMessage(filteredMessage);

            ps.setString(1, filteredMessage);
            ps.setInt(2, reponse.getReclamationId());
            ps.setBytes(3, reponse.getUserIdBytes());
            ps.executeUpdate();
            System.out.println("Reponse ajoutee avec succes.");
        } catch (RuntimeException e) {
            System.err.println("Erreur filtrage reponse : " + e.getMessage());
            throw new SQLDataException("Filtrage impossible: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur ajout reponse : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Reponse reponse) throws SQLDataException {
        String sql = "DELETE FROM reponse WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reponse.getId());
            ps.executeUpdate();
            System.out.println("Reponse supprimee avec succes.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression reponse : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Reponse reponse) throws SQLDataException {
        String sql = "UPDATE reponse SET message = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String filteredMessage = OllamaContentFilterService.censorBadWords(reponse.getMessage());
            suspendClientIfProfanityDetected(reponse.getUserIdBytes(), reponse.getMessage(), filteredMessage);
            reponse.setMessage(filteredMessage);

            ps.setString(1, filteredMessage);
            ps.setInt(2, reponse.getId());
            ps.executeUpdate();
            System.out.println("Reponse modifiee avec succes.");
        } catch (RuntimeException e) {
            System.err.println("Erreur filtrage reponse : " + e.getMessage());
            throw new SQLDataException("Filtrage impossible: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur modification reponse : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Reponse> recuperer() throws SQLDataException {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setMessage(rs.getString("message"));
                r.setReclamationId(rs.getInt("reclamation_id"));
                r.setUserIdBytes(rs.getBytes("user_id"));
                reponses.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation reponses : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
        return reponses;
    }

    public List<Reponse> getReponsesByReclamation(int reclamationId) throws SQLDataException {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE reclamation_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setMessage(rs.getString("message"));
                r.setReclamationId(rs.getInt("reclamation_id"));
                r.setUserIdBytes(rs.getBytes("user_id"));
                reponses.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation reponses par reclamation : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
        return reponses;
    }

    private void suspendClientIfProfanityDetected(byte[] userIdBytes, String originalText, String filteredText) {
        if (originalText == null || filteredText == null || originalText.equals(filteredText)) {
            return;
        }
        userService.deactivateClientTemporarily(userIdBytes, BAD_WORD_DEACTIVATION_SECONDS);
    }
}
