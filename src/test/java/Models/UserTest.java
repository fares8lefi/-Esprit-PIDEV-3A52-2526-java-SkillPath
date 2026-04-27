package Models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de la classe User")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        // Initialise un nouvel utilisateur avant chaque test
        user = new User();
    }

    @Test
    @DisplayName("Créer un nouvel utilisateur - par défaut non vérifié")
    void testUserCreationDefaultNotVerified() {
        assertFalse(user.isVerified());
        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Vérifier que l'ID est unique")
    void testUserIdIsUnique() {
        User user2 = new User();
        assertNotEquals(user.getId(), user2.getId());
    }

    @Test
    @DisplayName("Tester setEmail et getEmail")
    void testSetAndGetEmail() {
        String email = "test@example.com";
        user.setEmail(email);
        assertEquals(email, user.getEmail());
    }

    @Test
    @DisplayName("Tester setUsername et getUsername")
    void testSetAndGetUsername() {
        String username = "testuser";
        user.setUsername(username);
        assertEquals(username, user.getUsername());
    }

    @Test
    @DisplayName("Tester setPassword et getPassword")
    void testSetAndGetPassword() {
        String password = "password123";
        user.setPassword(password);
        assertEquals(password, user.getPassword());
    }

    @Test
    @DisplayName("Tester setRole et getRole")
    void testSetAndGetRole() {
        String role = "INSTRUCTOR";
        user.setRole(role);
        assertEquals(role, user.getRole());
    }

    @Test
    @DisplayName("Tester setStatus et getStatus")
    void testSetAndGetStatus() {
        String status = "ACTIVE";
        user.setStatus(status);
        assertEquals(status, user.getStatus());
    }

    @Test
    @DisplayName("Tester setVerified et isVerified")
    void testSetAndIsVerified() {
        assertFalse(user.isVerified());
        user.setVerified(true);
        assertTrue(user.isVerified());
    }

    @Test
    @DisplayName("Tester setDomaine et getDomaine")
    void testSetAndGetDomaine() {
        String domaine = "Informatique";
        user.setDomaine(domaine);
        assertEquals(domaine, user.getDomaine());
    }

    @Test
    @DisplayName("Tester setStyleDapprentissage et getStyleDapprentissage")
    void testSetAndGetStyleApprentissage() {
        String style = "Visuel";
        user.setStyleDapprentissage(style);
        assertEquals(style, user.getStyleDapprentissage());
    }

    @Test
    @DisplayName("Tester setVerificationCode et getVerificationCode")
    void testSetAndGetVerificationCode() {
        String code = "123456";
        user.setVerificationCode(code);
        assertEquals(code, user.getVerificationCode());
    }

    @Test
    @DisplayName("Remplir tous les champs de l'utilisateur")
    void testUserWithAllFields() {
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setEmail("john@example.com");
        user.setUsername("john_doe");
        user.setPassword("securePassword123");
        user.setRole("INSTRUCTOR");
        user.setStatus("ACTIVE");
        user.setVerified(true);
        user.setDomaine("Science");
        user.setStyleDapprentissage("Pratique");
        user.setNiveau("Avancé");

        assertEquals(testId, user.getId());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("john_doe", user.getUsername());
        assertEquals("securePassword123", user.getPassword());
        assertEquals("INSTRUCTOR", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(user.isVerified());
        assertEquals("Science", user.getDomaine());
        assertEquals("Pratique", user.getStyleDapprentissage());
        assertEquals("Avancé", user.getNiveau());
    }
}
