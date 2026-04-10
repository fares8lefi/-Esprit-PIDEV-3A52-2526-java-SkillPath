package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private   final String URl = "jdbc:mysql://localhost:3306/skillpathdb";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
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
            //
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URl, USERNAME, PASSWORD);
            System.out.println("--- Connexion réussie à la base de données ! ---");
        } catch (ClassNotFoundException e) {
            System.out.println("ERREUR : Driver MySQL non trouvé. Vérifiez que le JAR est dans le classpath.");
        } catch (SQLException e) {
            System.out.println("ERREUR de connexion SQL : " + e.getMessage());
            System.out.println("Vérifiez : 1. MySQL est lancé, 2. La base 'skillpathdb' existe, 3. User/Pass corrects.");
        }
    }
}
