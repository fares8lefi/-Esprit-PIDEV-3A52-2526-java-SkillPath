package Services;

import Models.User;
import com.github.f4b6a3.uuid.UuidCreator;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;

public class UserService implements Iservice<User> {

    private Connection connection;
    private static final Dotenv dotenv = Dotenv.load();
    public UserService() {
        connection = Utils.Database.getInstance().getConnection();
    }

    // ─── Hashage simple SHA-256 (substitut de BCrypt sans dépendance externe) ───
    private String hashPassword(String password) {
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

    // ─── Génère un code à 6 chiffres ───
    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // ─── Vérifie si un email existe déjà ───
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur emailExists : " + e.getMessage());
        }
        return false;
    }

    // ─── Envoi de l'email de vérification via JavaMail ───
    public boolean sendVerificationEmail(String toEmail, String username, String code) {
        String host = "smtp.gmail.com";
        // skill path email
        String from = dotenv.get("MAIL_USERNAME");
        //password : xckmtvtgnmxutbfm
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
            System.out.println("Email envoyé à " + toEmail);
            return true;
        } catch (jakarta.mail.MessagingException e) {
            System.out.println("Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    // ─── Inscription ───
    @Override
    public void ajouter(User user) throws SQLDataException {
        String sql = "INSERT INTO users (id, email, username, password, status, role, is_verified, verification_code, created_at) " +
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

    // ─── Vérification du code ───
    public boolean verifyCode(String email, String code) {
        String sql = "SELECT verification_code FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedCode = rs.getString("verification_code");
                if (storedCode != null && code.equals(storedCode)) {
                    // Activer le compte
                    String updateSql = "UPDATE users SET is_verified = true, status = 'active', verification_code = NULL WHERE email = ?";
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
        String sql = "SELECT * FROM users WHERE email = ? AND password = ? AND is_verified = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashPassword(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setEmail(rs.getString("email"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setStatus(rs.getString("status"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Erreur login : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void supprimer(User user) throws SQLDataException {
        String sql = "DELETE FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void modifier(User user) throws SQLDataException {
        String sql = "UPDATE users SET username = ?, status = ?, role = ? WHERE email = ?";
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
        String sql = "SELECT * FROM users";
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
}
