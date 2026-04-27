package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private final String URL = dotenv.get("DB_URL", "jdbc:mysql://localhost:3306/skillpath?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private final String USERNAME = dotenv.get("DB_USER", "root");
    private final String PASSWORD = dotenv.get("DB_PASS", "");
    private Connection connection;
    private  static Database instance ;

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private Database() {
        try {
            System.out.println("URL: " + URL);
            System.out.println("USERNAME: " + USERNAME);
            System.out.println("PASSWORD: " + PASSWORD);
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("--- Connexion réussie à la base de données ! ---");
        } catch (ClassNotFoundException e) {
            System.out.println("ERREUR : Driver MySQL non trouvé. Vérifiez que le JAR est dans le classpath.");
        } catch (SQLException e) {
            System.out.println("ERREUR de connexion SQL : " + e.getMessage());
            System.out.println("Vérifiez : 1. MySQL est lancé, 2. La base 'skillpathdb' existe, 3. User/Pass corrects.");
        }
    }
}
