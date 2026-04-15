package Models;

import java.time.LocalDateTime;

public class Module {
    private int id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String level;
    private String image;
    private String type;
    private String content;
    private String document;
    private int courseId;
    private LocalDateTime scheduledAt;

    public Module() {
    }

    public Module(int id, String title, String description, LocalDateTime createdAt, LocalDateTime updatedAt, String level, String image, String type, String content, String document, int courseId, LocalDateTime scheduledAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.level = level;
        this.image = image;
        this.type = type;
        this.content = content;
        this.document = document;
        this.courseId = courseId;
        this.scheduledAt = scheduledAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    @Override
    public String toString() {
        return "Module{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", courseId=" + courseId +
                '}';
    }
}
