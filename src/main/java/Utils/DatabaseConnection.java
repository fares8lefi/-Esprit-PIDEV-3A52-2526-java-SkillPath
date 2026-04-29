package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

/**
 * Singleton de connexion MySQL compatible avec les UUID BINARY(16) de Symfony.
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private Properties properties = new Properties();

    private DatabaseConnection() {
        try (InputStream inputFromClasspath = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            InputStream input = inputFromClasspath;
            if (input == null) {
                java.io.File file = new java.io.File("src/main/resources/db.properties");
                if (file.exists()) {
                    input = new java.io.FileInputStream(file);
                }
            }
            
            if (input == null) {
                throw new RuntimeException("Désolé, impossible de trouver db.properties");
            }
            
            properties.load(input);
            if (input != inputFromClasspath) input.close(); // Fermer si on a ouvert un FileInputStream manuel
            Class.forName(properties.getProperty("db.driver"));
            this.connection = DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password")
            );
            System.out.println("--- Connexion sécurisée établie ---");
        } catch (IOException | ClassNotFoundException | SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            try {
                if (instance.connection == null || instance.connection.isClosed()) {
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                instance = new DatabaseConnection();
            }
        }
        return instance.connection;
    }

    public static byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
