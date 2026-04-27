package Services;

import Models.Reclamation;
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

public class ReclamationService implements Iservice<Reclamation> {

    private final Connection connection;

    public ReclamationService() {
        this.connection = Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reclamation reclamation) throws SQLDataException {
        String sql = "INSERT INTO reclamation (sujet, description, statut, piece_jointe, user_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String filteredSujet = OllamaContentFilterService.censorBadWords(reclamation.getSujet());
            String filteredDescription = OllamaContentFilterService.censorBadWords(reclamation.getDescription());
            reclamation.setSujet(filteredSujet);
            reclamation.setDescription(filteredDescription);

            ps.setString(1, filteredSujet);
            ps.setString(2, filteredDescription);
            ps.setString(3, reclamation.getStatut());
            ps.setString(4, reclamation.getPieceJointe());
            ps.setBytes(5, reclamation.getUserIdBytes());
            ps.executeUpdate();
            System.out.println("Reclamation ajoutee avec succes.");
        } catch (RuntimeException e) {
            System.err.println("Erreur filtrage Ollama reclamation : " + e.getMessage());
            throw new SQLDataException("Filtrage IA impossible: " + e.getMessage());
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
            System.out.println("Reclamation supprimee avec succes.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression reclamation : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLDataException {
        String sql = "UPDATE reclamation SET sujet = ?, description = ?, statut = ?, piece_jointe = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String filteredSujet = OllamaContentFilterService.censorBadWords(reclamation.getSujet());
            String filteredDescription = OllamaContentFilterService.censorBadWords(reclamation.getDescription());
            reclamation.setSujet(filteredSujet);
            reclamation.setDescription(filteredDescription);

            ps.setString(1, filteredSujet);
            ps.setString(2, filteredDescription);
            ps.setString(3, reclamation.getStatut());
            ps.setString(4, reclamation.getPieceJointe());
            ps.setInt(5, reclamation.getId());
            ps.executeUpdate();
            System.out.println("Reclamation modifiee avec succes.");
        } catch (RuntimeException e) {
            System.err.println("Erreur filtrage Ollama reclamation : " + e.getMessage());
            throw new SQLDataException("Filtrage IA impossible: " + e.getMessage());
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

    public List<Reclamation> getAllReclamationsWithUsername(String usernameSearch, String statusFilter) throws SQLDataException {
        List<Reclamation> reclamations = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.sujet, r.description, r.statut, r.piece_jointe, r.user_id, u.username " +
                        "FROM reclamation r " +
                        "JOIN user u ON u.id = r.user_id "
        );

        boolean hasSearch = usernameSearch != null && !usernameSearch.isBlank();
        boolean hasStatusFilter = statusFilter != null && !statusFilter.isBlank();

        if (hasSearch || hasStatusFilter) {
            sql.append("WHERE ");
        }
        if (hasSearch) {
            sql.append("LOWER(u.username) LIKE ? ");
        }
        if (hasSearch && hasStatusFilter) {
            sql.append("AND ");
        }
        if (hasStatusFilter) {
            sql.append("LOWER(r.statut) = ? ");
        }

        sql.append("ORDER BY r.id DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (hasSearch) {
                ps.setString(paramIndex++, "%" + usernameSearch.trim().toLowerCase() + "%");
            }
            if (hasStatusFilter) {
                ps.setString(paramIndex, statusFilter.trim().toLowerCase());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setSujet(rs.getString("sujet"));
                r.setDescription(rs.getString("description"));
                r.setStatut(rs.getString("statut"));
                r.setPieceJointe(rs.getString("piece_jointe"));
                r.setUserIdBytes(rs.getBytes("user_id"));
                r.setUsername(rs.getString("username"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAllReclamationsWithUsername : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }

        return reclamations;
    }

    public void updateStatut(int reclamationId, String statut) throws SQLDataException {
        String sql = "UPDATE reclamation SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur updateStatut : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }

    public void deleteWithResponses(int reclamationId) throws SQLDataException {
        String deleteResponsesSql = "DELETE FROM reponse WHERE reclamation_id = ?";
        String deleteReclamationSql = "DELETE FROM reclamation WHERE id = ?";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psResp = connection.prepareStatement(deleteResponsesSql);
                 PreparedStatement psRecl = connection.prepareStatement(deleteReclamationSql)) {
                psResp.setInt(1, reclamationId);
                psResp.executeUpdate();

                psRecl.setInt(1, reclamationId);
                psRecl.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Erreur rollback deleteWithResponses : " + rollbackEx.getMessage());
            }
            System.err.println("Erreur deleteWithResponses : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur reset autoCommit : " + e.getMessage());
            }
        }
    }
}
