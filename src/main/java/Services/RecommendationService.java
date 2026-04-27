package Services;

import Models.Course;
import Utils.Session;
import java.sql.SQLDataException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {
    private final CourseService courseService = new CourseService();

    private final UserInteractionService interactionService = new UserInteractionService();

    /**
     * Gets the most preferred category based on session or DB.
     */
    public String getPreferredCategory() {
        Map<String, Integer> prefs = Session.getInstance().getCategoryPreferences();
        if (prefs.isEmpty() && Session.getInstance().isLoggedIn()) {
            return interactionService.getMostPreferredCategory(Session.getInstance().getCurrentUser().getId());
        }
        if (prefs.isEmpty()) return null;
        return Collections.max(prefs.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * Gets the most preferred level based on session or DB.
     */
    public String getPreferredLevel() {
        Map<String, Integer> prefs = Session.getInstance().getLevelPreferences();
        if (prefs.isEmpty() && Session.getInstance().isLoggedIn()) {
            return interactionService.getMostPreferredLevel(Session.getInstance().getCurrentUser().getId());
        }
        if (prefs.isEmpty()) return null;
        return Collections.max(prefs.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * Recommends courses based on session or DB behavior.
     */
    public List<Course> getRecommendations() {
        try {
            List<Course> allCourses = courseService.recuperer();
            List<Course> clicked = new ArrayList<>(Session.getInstance().getClickedCourses());
            
            if (Session.getInstance().isLoggedIn()) {
                java.util.UUID userId = Session.getInstance().getCurrentUser().getId();
                List<Integer> clickedIds = interactionService.getClickedCourseIds(userId);
                // Add DB clicked courses to the exclusion list
                for (Course c : allCourses) {
                    if (clickedIds.contains(c.getId()) && !clicked.contains(c)) {
                        clicked.add(c);
                    }
                }
            }

            String preferredCat = getPreferredCategory();
            String preferredLevel = getPreferredLevel();

            if (preferredCat == null) {
                return allCourses.stream().limit(6).collect(Collectors.toList());
            }

            // STRICT FILTERING: Only show courses that match the preferred category
            String finalPreferredCat = preferredCat.trim();
            return allCourses.stream()
                .filter(c -> c.getCategory() != null && c.getCategory().trim().equalsIgnoreCase(finalPreferredCat)) // STRICT CATEGORY MATCH
                .sorted((c1, c2) -> {
                    int score1 = 0;
                    int score2 = 0;

                    // Level matching is now the primary sorting factor
                    if (c1.getLevel().equalsIgnoreCase(preferredLevel)) score1 += 100;
                    if (c2.getLevel().equalsIgnoreCase(preferredLevel)) score2 += 100;
                    
                    // Bonus score if the user HAS already clicked on it (it's an interest)
                    if (clicked.contains(c1)) score1 += 50;
                    if (clicked.contains(c2)) score2 += 50;

                    return Integer.compare(score2, score1); // Descending
                })
                .limit(10)
                .collect(Collectors.toList());

        } catch (SQLDataException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
