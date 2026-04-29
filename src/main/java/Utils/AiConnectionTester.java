package Utils;

import Services.SecurityService;
import Models.LoginResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class AiConnectionTester {
    public static void main(String[] args) {
        System.out.println("=== AI Connection Test Starting ===");
        
        SecurityService securityService = new SecurityService();
        
        // We need a connection for the SecurityService to work, 
        // but for callFlaskPredict specifically, it might not need it if we mock it.
        // However, the login() method needs it.
        
        try {
            // Test specifically the Flask communication
            System.out.println("Pinging Flask ML Server at http://localhost:5000/predict...");
            
            // We can't call private methods directly, so let's use a dummy login attempt
            // We'll need a database connection though.
            
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Database connection OK.");
                
                LoginResult result = securityService.login("test@example.com", "wrong_password", "127.0.0.1", conn);
                
                System.out.println("\n--- Result from SecurityService ---");
                System.out.println("Success: " + result.isSuccess());
                System.out.println("Blocked: " + result.isBlocked());
                System.out.println("Risk Level: " + result.getRiskLevel());
                System.out.println("Message: " + result.getMessage());
                
                if (result.getRiskLevel() != null) {
                    System.out.println("\n[SUCCESS] AI Model connection and parsing verified!");
                } else {
                    System.out.println("\n[WARNING] AI Model returned null or unexpected result.");
                }
            } else {
                System.err.println("Could not establish database connection for test.");
            }
            
        } catch (Exception e) {
            System.err.println("Error during AI connection test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
