package Utils;

import Models.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class to manage the current user session.
 */
public class Session {
    private static Session instance;
    private User currentUser;
    private String temporaryBanMessage;

    private final List<Models.Course> clickedCourses = new ArrayList<>();
    private final Map<String, Integer> categoryPreferences = new HashMap<>();
    private final Map<String, Integer> levelPreferences = new HashMap<>();

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

    // --- Singleton Instance Methods ---

    public void loginInstance(User user) {
        this.currentUser = user;
    }

    public void trackClick(Models.Course course) {
        if (!clickedCourses.contains(course)) {
            clickedCourses.add(course);
        }
        
        // Track category
        String cat = course.getCategory();
        categoryPreferences.put(cat, categoryPreferences.getOrDefault(cat, 0) + 1);
        
        // Track level
        String level = course.getLevel();
        levelPreferences.put(level, levelPreferences.getOrDefault(level, 0) + 1);
    }

    public List<Models.Course> getClickedCourses() { return clickedCourses; }
    public Map<String, Integer> getCategoryPreferences() { return categoryPreferences; }
    public Map<String, Integer> getLevelPreferences() { return levelPreferences; }

    public void logoutInstance() {
        this.currentUser = null;
        this.clickedCourses.clear();
        this.categoryPreferences.clear();
        this.levelPreferences.clear();
    }

    // --- Static Convenience Methods (for backward compatibility and ease of use) ---

    public static User getCurrentUser() {
        return getInstance().currentUser;
    }

    public static boolean isLoggedIn() {
        User user = getInstance().currentUser;
        return user != null && !"inactive".equalsIgnoreCase(user.getStatus());
    }

    public static void login(User user) {
        getInstance().loginInstance(user);
    }

    public static void logout() {
        getInstance().logoutInstance();
    }

    public static void logoutKeepTemporaryBanMessage() {
        getInstance().currentUser = null;
    }

    public static void setTemporaryBanMessage(String message) {
        getInstance().temporaryBanMessage = message;
    }

    public static String consumeTemporaryBanMessage() {
        String message = getInstance().temporaryBanMessage;
        getInstance().temporaryBanMessage = null;
        return message;
    }
}
