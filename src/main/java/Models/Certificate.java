package Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Certificate {
    private int id;
    private UUID userId;
    private int courseId;
    private LocalDateTime issueDate;

    public Certificate() {
    }

    public Certificate(int id, UUID userId, int courseId, LocalDateTime issueDate) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.issueDate = issueDate;
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

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }
}
