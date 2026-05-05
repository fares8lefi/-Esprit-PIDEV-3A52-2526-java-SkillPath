package Services;

import Models.User;
import Utils.Session;
import com.github.f4b6a3.uuid.UuidCreator;
import io.github.cdimascio.dotenv.Dotenv;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class UserService implements Iservice<User> {

    private Connection connection;
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final Map<String, Long> TEMPORARY_DEACTIVATIONS = new ConcurrentHashMap<>();
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

  
    public boolean emailExists(String email) {
        if (connection == null) {
            System.err.println("Erreur : Connexion à la base de données absente.");
            return false;
        }
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
        String sql = "UPDATE users SET verification_code = ? WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, email);
            ps.executeUpdate();
            
            // On peut chercher le nom d'utilisateur pour personnaliser l'email
            String username = "Collaborateur";
            String findUser = "SELECT username FROM users WHERE email = ?";
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
        String sql = "SELECT verification_code FROM users WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedCode = rs.getString("verification_code");
                if (storedCode != null && storedCode.equals(code)) {
                    // Sécurité : Hachage du nouveau mot de passe
                    String hashed = hashPassword(newPassword);
                    
                    // On met à jour le mot de passe et on invalide le code utilisé
                    String updateSql = "UPDATE users SET password = ?, verification_code = NULL WHERE email = ?";
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
        if (connection == null) {
            throw new SQLDataException("Connexion à la base de données indisponible. Veuillez vérifier votre configuration.");
        }
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
    
    public void ajouterUserParAdmin(User user) throws SQLDataException {
        String sql = "INSERT INTO users (id, email, username, password, status, role, is_verified, verification_code, created_at) " +
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
        String sql = "SELECT * FROM users WHERE email = ? AND is_verified = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                boolean isVerified = rs.getBoolean("is_verified");
                String storedHash = rs.getString("password");
                
                System.out.println("Compte trouvé pour " + email + ". Vérifié: " + isVerified);

                if (!isVerified) {
                    System.err.println("ERREUR : Le compte '" + email + "' n'est pas vérifié (is_verified = 0 dans la base).");
                    return null;
                }

                if (verifyPassword(password, storedHash)) {
                    String status = rs.getString("status");
                    if (!"active".equalsIgnoreCase(status)) {
                        System.out.println("Compte desactive pour : " + email);
                        return null;
                    }

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
                    user.setStatus(status);
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

    public void deactivateClientTemporarily(byte[] userIdBytes, int seconds) {
        if (userIdBytes == null || userIdBytes.length == 0 || seconds <= 0) {
            return;
        }

        String userKey = Base64.getEncoder().encodeToString(userIdBytes);
        long deactivateUntil = System.currentTimeMillis() + (seconds * 1000L);
        TEMPORARY_DEACTIVATIONS.put(userKey, deactivateUntil);

        updateClientStatus(userIdBytes, "inactive");
        notifyCurrentUserAboutTemporaryBanAndLogout(userIdBytes, seconds);
        System.out.println("Compte client desactive temporairement pendant " + seconds + " secondes.");

        new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
                Long latestUntil = TEMPORARY_DEACTIVATIONS.get(userKey);
                if (latestUntil != null && latestUntil <= deactivateUntil) {
                    updateClientStatus(userIdBytes, "active");
                    TEMPORARY_DEACTIVATIONS.remove(userKey);
                    System.out.println("Compte client reactive apres suspension temporaire.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void updateClientStatus(byte[] userIdBytes, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ? AND LOWER(role) <> 'admin'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setBytes(2, userIdBytes);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.err.println("Aucun compte trouve pour appliquer le statut: " + status);
            }
        } catch (SQLException e) {
            System.err.println("Erreur updateClientStatus : " + e.getMessage());
        }
    }

    private void notifyCurrentUserAboutTemporaryBanAndLogout(byte[] userIdBytes, int seconds) {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getId() == null) {
            return;
        }

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(Session.getCurrentUser().getId().getMostSignificantBits());
        bb.putLong(Session.getCurrentUser().getId().getLeastSignificantBits());
        if (java.util.Arrays.equals(bb.array(), userIdBytes)) {
            Session.setTemporaryBanMessage("Votre compte a ete desactive pendant " + seconds
                    + " secondes car votre message contient des mots inappropries.");
            Session.logoutKeepTemporaryBanMessage();
        }
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
        String sql = "UPDATE users SET username = ?, status = ?, role = ?, domaine = ?, niveau = ?, style_dapprentissage = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getStatus());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getDomaine());
            ps.setString(5, user.getNiveau());
            ps.setString(6, user.getStyleDapprentissage());
            ps.setString(7, user.getEmail());
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
                u.setVerified(rs.getBoolean("is_verified"));
                
                Timestamp ts = rs.getTimestamp("created_at");
                if(ts != null) u.setCreatedAt(ts.toLocalDateTime());
                
                u.setDomaine(rs.getString("domaine"));
                u.setStyleDapprentissage(rs.getString("style_dapprentissage"));
                u.setNiveau(rs.getString("niveau"));
                
                list.add(u);
            }
        } catch (SQLException e) {
            System.out.println("Erreur recuperer : " + e.getMessage());
        }
        return list;
    }
   public List<User> serchByName(User user) throws SQLDataException {
    String sql = "SELECT * FROM users WHERE username LIKE ?";
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
            u.setVerified(rs.getBoolean("is_verified"));
            
            Timestamp ts = rs.getTimestamp("created_at");
            if(ts != null) u.setCreatedAt(ts.toLocalDateTime());
            
            u.setDomaine(rs.getString("domaine"));
            u.setStyleDapprentissage(rs.getString("style_dapprentissage"));
            u.setNiveau(rs.getString("niveau"));
            
            list.add(u);
        }
    } catch (SQLException e) {
        System.out.println("Erreur searchByName : " + e.getMessage());
    }
    return list;
}
public List<User> getClientList() throws SQLDataException {
    String sql = "SELECT * FROM users WHERE role LIKE ?";
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


    public void updatePassword(String email, String newPassword) throws SQLDataException {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public boolean checkCurrentPassword(String email, String plainPassword) {
        String sql = "SELECT password FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                return verifyPassword(plainPassword, storedHash);
            }
        } catch (SQLException e) {
            System.out.println("Erreur checkCurrentPassword : " + e.getMessage());
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════
    //   STATISTIQUES AVANCÉES — Dashboard Admin
    // ═══════════════════════════════════════════════════════

    /** Nombre d'utilisateurs avec status = 'active' */
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'active'";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur countActive : " + e.getMessage());
        }
        return 0;
    }

    /** Nombre d'utilisateurs non vérifiés (is_verified = false) */
    public int countUnverified() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_verified = false";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur countUnverified : " + e.getMessage());
        }
        return 0;
    }

    /** Nombre d'utilisateurs inscrits dans les 7 derniers jours */
    public int countNewThisWeek() {
        String sql = "SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL 7 DAY";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur countNewThisWeek : " + e.getMessage());
        }
        return 0;
    }

    /** Répartition par rôle : {role -> count} */
    public java.util.Map<String, Integer> countByRole() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT COALESCE(role, 'inconnu') as role, COUNT(*) as cnt FROM users GROUP BY role ORDER BY cnt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("role"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur countByRole : " + e.getMessage());
        }
        return map;
    }

    /** Répartition par statut : {status -> count} */
    public java.util.Map<String, Integer> countByStatus() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT COALESCE(status, 'inconnu') as status, COUNT(*) as cnt FROM users GROUP BY status ORDER BY cnt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("status"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur countByStatus : " + e.getMessage());
        }
        return map;
    }

    /**
     * Inscriptions par jour sur les N derniers jours.
     * Retourne une map ordonnée {date (dd/MM) -> count}
     */
    public java.util.Map<String, Integer> getRegistrationsByDay(int days) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE(created_at) as day, COUNT(*) as cnt " +
                     "FROM users " +
                     "WHERE created_at >= NOW() - INTERVAL ? DAY " +
                     "GROUP BY DATE(created_at) " +
                     "ORDER BY day ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
            while (rs.next()) {
                java.sql.Date d = rs.getDate("day");
                String label = d.toLocalDate().format(fmt);
                map.put(label, rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getRegistrationsByDay : " + e.getMessage());
        }
        return map;
    }
}

