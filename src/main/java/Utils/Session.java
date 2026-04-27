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

    private final java.util.List<Models.Course> clickedCourses = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> categoryPreferences = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> levelPreferences = new java.util.HashMap<>();

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

    public java.util.List<Models.Course> getClickedCourses() { return clickedCourses; }
    public java.util.Map<String, Integer> getCategoryPreferences() { return categoryPreferences; }
    public java.util.Map<String, Integer> getLevelPreferences() { return levelPreferences; }

    public void logout() {
        this.currentUser = null;
        this.clickedCourses.clear();
        this.categoryPreferences.clear();
        this.levelPreferences.clear();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
