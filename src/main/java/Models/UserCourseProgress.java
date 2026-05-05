package Models;

import java.util.UUID;

public class UserCourseProgress {
    private int id;
    private UUID userId;
    private int courseId;
    private int completedModules;
    private boolean isCompleted;

    public UserCourseProgress() {
    }

    public UserCourseProgress(int id, UUID userId, int courseId, int completedModules, boolean isCompleted) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.completedModules = completedModules;
        this.isCompleted = isCompleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public int getCompletedModules() {
        return completedModules;
    }

    public void setCompletedModules(int completedModules) {
        this.completedModules = completedModules;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
