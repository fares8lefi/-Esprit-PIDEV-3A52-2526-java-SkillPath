package Services;

import Models.Reclamation;
import Utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements Iservice<Reclamation> {

    private final Connection connection;

    public ReclamationService() {
        this.connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reclamation reclamation) throws SQLDataException {
        String sql = "INSERT INTO reclamation (sujet, description, statut, piece_jointe, user_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reclamation.getSujet());
            ps.setString(2, reclamation.getDescription());
            ps.setString(3, reclamation.getStatut());
            ps.setString(4, reclamation.getPieceJointe());
            ps.setBytes(5, reclamation.getUserIdBytes());
            ps.executeUpdate();
            System.out.println("Reclamation ajoutée avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur ajout reclamation : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void supprimer(Reclamation reclamation) throws SQLDataException {
        String sql = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reclamation.getId());
            ps.executeUpdate();
            System.out.println("Reclamation supprimée avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression reclamation : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLDataException {
        String sql = "UPDATE reclamation SET sujet = ?, description = ?, statut = ?, piece_jointe = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reclamation.getSujet());
            ps.setString(2, reclamation.getDescription());
            ps.setString(3, reclamation.getStatut());
            ps.setString(4, reclamation.getPieceJointe());
            ps.setInt(5, reclamation.getId());
            ps.executeUpdate();
            System.out.println("Reclamation modifiée avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur modification reclamation : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Reclamation> recuperer() throws SQLDataException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setSujet(rs.getString("sujet"));
                r.setDescription(rs.getString("description"));
                r.setStatut(rs.getString("statut"));
                r.setPieceJointe(rs.getString("piece_jointe"));
                r.setUserIdBytes(rs.getBytes("user_id"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation reclamations : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
        return reclamations;
    }

    public List<Reclamation> getReclamationsByUser(byte[] userIdBytes) throws SQLDataException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, userIdBytes);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setSujet(rs.getString("sujet"));
                r.setDescription(rs.getString("description"));
                r.setStatut(rs.getString("statut"));
                r.setPieceJointe(rs.getString("piece_jointe"));
                r.setUserIdBytes(rs.getBytes("user_id"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur recuperation reclamations par utilisateur : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
        return reclamations;
    }
}
