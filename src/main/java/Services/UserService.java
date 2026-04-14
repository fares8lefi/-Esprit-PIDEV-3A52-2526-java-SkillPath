package Services;

import Models.User;
import com.github.f4b6a3.uuid.UuidCreator;
import io.github.cdimascio.dotenv.Dotenv;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.Random;

public class UserService implements Iservice<User> {

    private Connection connection;
    private static final Dotenv dotenv = Dotenv.load();
    public UserService() {
        connection = Utils.Database.getInstance().getConnection();
    }

    // ─── Hachage BCrypt (pour les nouveaux utilisateurs Java) ───
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // ─── Hachage SHA-256 (compatibilité anciens comptes Java) ───
    private String hashPasswordSHA256(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    // ─── Détecte le type de hash et compare ───
    private boolean verifyPassword(String plainPassword, String storedHash) {
        if (storedHash == null) return false;

        if (storedHash.startsWith("$2y$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2a$")) {
            // Hash BCrypt (Symfony ou nouveaux comptes Java)
            String hash = storedHash;
            if (hash.startsWith("$2y$") || hash.startsWith("$2b$")) {
                hash = "$2a$" + hash.substring(4);
            }
            try {
                return BCrypt.checkpw(plainPassword, hash);
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur BCrypt : " + e.getMessage());
                return false;
            }
        } else if (storedHash.length() == 64) {
            // Hash SHA-256 (anciens comptes Java)
            System.out.println("⚠️ Hash SHA-256 détecté, utilisation du mode de compatibilité.");
            return hashPasswordSHA256(plainPassword).equals(storedHash);
        }

        System.out.println("⚠️ Format de hash inconnu !");
        return false;
    }

    // ─── Génère un code à 6 chiffres ───
    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // ─── Vérifie si un email existe déjà ───
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur emailExists : " + e.getMessage());
        }
        return false;
    }

    // ─── Envoi de l'email via JavaMail ───
    private boolean sendMail(String toEmail, String username, String code) {
        String host = "smtp.gmail.com";
        String from = dotenv.get("MAIL_USERNAME");
        String appPassword = dotenv.get("MAIL_PASSWORD");

        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        try {
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props,
                new jakarta.mail.Authenticator() {
                    protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new jakarta.mail.PasswordAuthentication(from, appPassword);
                    }
                });

            jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(from));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO,
                    jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject("Vérification de votre compte SkillPath");

            String htmlContent =
                "<h1 style='color:#8b5cf6;'>Bienvenue sur SkillPath!</h1>" +
                "<p>Bonjour <strong>" + username + "</strong>,</p>" +
                "<p>Votre code de vérification est :</p>" +
                "<h2 style='font-size:32px; color:#1e88e5; letter-spacing:8px;'>" + code + "</h2>" +
                "<p>Veuillez entrer ce code pour activer votre compte.</p>" +
                "<p style='color:#64748b; font-size:12px;'>Ce code expire dans 15 minutes.</p>";

            message.setContent(htmlContent, "text/html; charset=UTF-8");
            jakarta.mail.Transport.send(message);
            System.out.println("Email envoyé avec succès à " + toEmail);
            return true;
        } catch (jakarta.mail.MessagingException e) {
            System.out.println("Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    public boolean sendVerificationEmail(String toEmail, String username, String code) {
        return sendMail(toEmail, username, code);
    }

    public boolean resendEmail(String toEmail, String username, String code) {
        return sendMail(toEmail, username, code);
    }

    /**
     * Étape 1 : Demander une réinitialisation de mot de passe.
     * Génère un code, le stocke en base et l'envoie par email.
     */
    public boolean requestPasswordReset(String email) {
        if (!emailExists(email)) return false;

        String code = generateVerificationCode();
        String sql = "UPDATE user SET verification_code = ? WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, email);
            ps.executeUpdate();
            
            // On peut chercher le nom d'utilisateur pour personnaliser l'email
            String username = "Collaborateur";
            String findUser = "SELECT username FROM user WHERE email = ?";
            try (PreparedStatement ps2 = connection.prepareStatement(findUser)) {
                ps2.setString(1, email);
                ResultSet rs = ps2.executeQuery();
                if(rs.next()) username = rs.getString("username");
            }

            return sendMail(email, username, code);
        } catch (SQLException e) {
            System.err.println("Erreur requestPasswordReset : " + e.getMessage());
            return false;
        }
    }

    /**
     * Étape 2 : Finaliser la réinitialisation après vérification du code.
     * Vérifie le code, hache le nouveau mot de passe et l'enregistre.
     */
    public boolean finalizePasswordReset(String email, String code, String newPassword) {
        String sql = "SELECT verification_code FROM user WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedCode = rs.getString("verification_code");
                if (storedCode != null && storedCode.equals(code)) {
                    // Sécurité : Hachage du nouveau mot de passe
                    String hashed = hashPassword(newPassword);
                    
                    // On met à jour le mot de passe et on invalide le code utilisé
                    String updateSql = "UPDATE user SET password = ?, verification_code = NULL WHERE email = ?";
                    try (PreparedStatement ups = connection.prepareStatement(updateSql)) {
                        ups.setString(1, hashed);
                        ups.setString(2, email);
                        ups.executeUpdate();
                        System.out.println("Mot de passe mis à jour pour : " + email);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur finalizePasswordReset : " + e.getMessage());
        }
        return false;
    }
    // ─── Inscription ───
    @Override
    public void ajouter(User user) throws SQLDataException {
        String sql = "INSERT INTO user (id, email, username, password, status, role, is_verified, verification_code, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Génération de l'UUID v7 (Time-ordered)
            UUID uuid = UuidCreator.getTimeOrderedEpoch();
            
            // Conversion de l'UUID en byte[] (16 octets) pour le format BINARY(16) de Symfony
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            byte[] uuidBytes = bb.array();

            String hashedPassword = hashPassword(user.getPassword());
            
            ps.setBytes(1, uuidBytes);
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getUsername());
            ps.setString(4, hashedPassword);
            ps.setString(5, user.getStatus());
            ps.setString(6, user.getRole());
            ps.setBoolean(7, false);
            ps.setString(8, user.getVerificationCode());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            
            ps.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur ajouter : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }
    }
    
    public void ajouterUserParAdmin(User user) throws SQLDataException {
        String sql = "INSERT INTO user (id, email, username, password, status, role, is_verified, verification_code, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            UUID uuid = UuidCreator.getTimeOrderedEpoch();
            
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            byte[] uuidBytes = bb.array();
            
            String hashedPassword = hashPassword(user.getPassword());
            
            ps.setBytes(1, uuidBytes);
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getUsername());
            ps.setString(4, hashedPassword);
            ps.setString(5, "active");
            ps.setString(6, user.getRole());
            ps.setBoolean(7, true);
            ps.setNull(8, Types.VARCHAR);
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();
            System.out.println("Compte Admin ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur ajouterUserParAdmin : " + e.getMessage());
            throw new SQLDataException(e.getMessage());
        }          
    }


    // ─── Vérification du code ───
    public boolean verifyCode(String email, String code) {
        String sql = "SELECT verification_code FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedCode = rs.getString("verification_code");
                if (storedCode != null && code.equals(storedCode)) {
                    // Activer le compte
                    String updateSql = "UPDATE user SET is_verified = true, status = 'active', verification_code = NULL WHERE email = ?";
                    try (PreparedStatement ps2 = connection.prepareStatement(updateSql)) {
                        ps2.setString(1, email);
                        ps2.executeUpdate();
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur verifyCode : " + e.getMessage());
        }
        return false;
    }

    // ─── Connexion ───
    public User login(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ? AND is_verified = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                System.out.println("Hash trouvé en base : " + storedHash);

                if (verifyPassword(password, storedHash)) {
                    User user = new User();
                    
                    // Récupérer et convertir l'ID binaire (16 octets) en UUID
                    byte[] idBytes = rs.getBytes("id");
                    if (idBytes != null && idBytes.length == 16) {
                        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(idBytes);
                        long high = bb.getLong();
                        long low = bb.getLong();
                        user.setId(new UUID(high, low));
                    }
                    
                    user.setEmail(rs.getString("email"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    System.out.println("Connexion réussie pour : " + user.getUsername());
                    return user;
                } else {
                    System.out.println("Mot de passe incorrect pour : " + email);
                }
            } else {
                System.out.println("Aucun compte vérifié trouvé pour : " + email);
            }
        } catch (SQLException e) {
            System.out.println("Erreur login SQL : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void supprimer(User user) throws SQLDataException {
        String sql = "DELETE FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(User user) throws SQLDataException {
        String sql = "UPDATE user SET username = ?, status = ?, role = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getStatus());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<User> recuperer() throws SQLDataException {
        String sql = "SELECT * FROM user";
        List<User> list = new ArrayList<>();
        if (connection == null) return list;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User u = new User();
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setStatus(rs.getString("status"));
                list.add(u);
            }
        } catch (SQLException e) {
            System.out.println("Erreur recuperer : " + e.getMessage());
        }
        return list;
    }
   public List<User> serchByName(User user) throws SQLDataException {
    String sql = "SELECT * FROM user WHERE username LIKE ?";
    List<User> list = new ArrayList<>();
    if (connection == null) return list;
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, "%" + user.getUsername() + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            User u = new User();
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setRole(rs.getString("role"));
            u.setStatus(rs.getString("status"));
            list.add(u);
        }
    } catch (SQLException e) {
        System.out.println("Erreur searchByName : " + e.getMessage());
    }
    return list;
}
public List<User> getClientList() throws SQLDataException {
    String sql = "SELECT * FROM user WHERE role LIKE ?";
    List<User> list = new ArrayList<>();
    
    if (connection == null) return list;

    try {
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, "client");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User u = new User();
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setRole(rs.getString("role"));
            u.setStatus(rs.getString("status"));
            list.add(u);
        }

    } catch (SQLException e) {
        System.out.println("Erreur getClientList : " + e.getMessage());
    }

    return list; 
}



}

