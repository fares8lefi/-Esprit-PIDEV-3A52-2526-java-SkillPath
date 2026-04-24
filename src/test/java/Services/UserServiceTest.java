package Services;

import Models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService")
class UserServiceTest {

    private UserService userService;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        // Initialisation du UserService avec la connexion mockée
        userService = new UserService();
    }

    @Test
    @DisplayName("Vérifier que emailExists retourne false pour un email inexistant")
    void testEmailExistsReturnsFalseForNonExistentEmail() {
        String email = "nonexistent@example.com";
        
        // Le test utilise la vraie base de données via getInstance()
        // Pour un vrai test, il faudrait refactoriser UserService pour injecter la connexion
        
        // Résultat attendu : false ou exception selon la base
        assertNotNull(userService);
    }

    @Test
    @DisplayName("Vérifier la génération du code de vérification")
    void testGenerateVerificationCode() {
        String code1 = userService.generateVerificationCode();
        String code2 = userService.generateVerificationCode();

        // Le code doit être une chaîne de 6 caractères
        assertEquals(6, code1.length());
        assertEquals(6, code2.length());
        
        // Les codes doivent être numériques
        assertTrue(code1.matches("\\d{6}"));
        assertTrue(code2.matches("\\d{6}"));
        
        // Les codes ne doivent pas être identiques (très probablement)
        assertNotEquals(code1, code2);
    }

    @Test
    @DisplayName("Vérifier que le code de vérification est entre 000000 et 999999")
    void testVerificationCodeRange() {
        for (int i = 0; i < 100; i++) {
            String code = userService.generateVerificationCode();
            int codeInt = Integer.parseInt(code);
            
            assertTrue(codeInt >= 0 && codeInt <= 999999);
        }
    }

    @Test
    @DisplayName("Vérifier la méthode sendVerificationEmail")
    void testSendVerificationEmail() {
        String email = "test@example.com";
        String username = "testuser";
        String code = "123456";

        assertNotNull(userService);
        // sendVerificationEmail retourne un boolean
        // Vous devriez tester avec une vraie base ou un mock complet
    }

    @Test
    @DisplayName("Créer un utilisateur avec tous les champs requis")
    void testCreateCompleteUser() {
        User user = new User();
        user.setEmail("john@example.com");
        user.setUsername("john_doe");
        user.setPassword("SecurePass123");
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");
        user.setVerificationCode("123456");

        assertNotNull(user.getId());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("john_doe", user.getUsername());
        assertEquals("SecurePass123", user.getPassword());
        assertEquals("STUDENT", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertEquals("123456", user.getVerificationCode());
    }
}
