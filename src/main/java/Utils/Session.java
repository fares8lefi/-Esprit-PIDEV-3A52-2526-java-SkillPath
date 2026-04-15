package Utils;

import Models.User;

/**
 * Singleton class to manage the current user session.
 */
public class Session {
    private static Session instance;
    private User currentUser;

    // Private constructor 
    private Session() {}

    /**
     * Gets the unique instance of the Session.
     * @return The unique Session instance.
     */
    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
