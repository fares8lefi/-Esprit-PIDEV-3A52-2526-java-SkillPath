package Services;

import Models.Certificate;
import Utils.Database;

import java.sql.*;
import java.util.UUID;

public class CertificateService {
    private final Connection connection;

    public CertificateService() {
        this.connection = Database.getInstance().getConnection();
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS certificates (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BINARY(16) NOT NULL, " +
                "course_id INT NOT NULL, " +
                "certificate_uid VARCHAR(50) UNIQUE, " +
                "issue_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            
            // Mise à jour de la table si la colonne n'existe pas encore
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN certificate_uid VARCHAR(50) UNIQUE AFTER course_id");
            } catch (SQLException ignored) {} // Déjà présent
        } catch (SQLException e) {
            System.err.println("Erreur création table certificates: " + e.getMessage());
        }
    }

    public boolean hasCertificate(UUID userId, int courseId) {
        String sql = "SELECT id FROM certificates WHERE user_id = ? AND course_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erreur hasCertificate: " + e.getMessage());
        }
        return false;
    }

    public void generateCertificate(UUID userId, int courseId) {
        if (hasCertificate(userId, courseId)) return; 

        String uid = "SKP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO certificates (user_id, course_id, certificate_uid) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, courseId);
            stmt.setString(3, uid);
            stmt.executeUpdate();
            System.out.println("Certificat généré avec succès en BDD avec l'UID : " + uid);
        } catch (SQLException e) {
            System.err.println("Erreur generateCertificate: " + e.getMessage());
        }
    }

    public String getCertificateUID(UUID userId, int courseId) {
        String sql = "SELECT certificate_uid FROM certificates WHERE user_id = ? AND course_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("certificate_uid");
            }
        } catch (SQLException e) {
            System.err.println("Erreur getCertificateUID: " + e.getMessage());
        }
        return "N/A";
    }

    public int countCertificates(UUID userId) {
        String sql = "SELECT COUNT(*) FROM certificates WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, UUIDToBytes(userId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur countCertificates: " + e.getMessage());
        }
        return 0;
    }

    private byte[] UUIDToBytes(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (mostSigBits >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (leastSigBits >>> (8 * (7 - i)));
        }
        return bytes;
    }
}
