package Models;

/**
 * Contient le résultat d'une tentative de connexion traitée par le système de sécurité.
 */
public class LoginResult {
    private final boolean success;
    private final User user;
    private final boolean blocked;
    private final String riskLevel;
    private final String message;

    public LoginResult(boolean success, User user, boolean blocked, String riskLevel, String message) {
        this.success = success;
        this.user = user;
        this.blocked = blocked;
        this.riskLevel = riskLevel;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public User getUser() { return user; }
    public boolean isBlocked() { return blocked; }
    public String getRiskLevel() { return riskLevel; }
    public String getMessage() { return message; }
}
