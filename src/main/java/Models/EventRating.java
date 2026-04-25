package Models;

import java.util.UUID;

public class EventRating {
    private int id;
    private int score;
    private int eventId;
    private UUID userId;

    public EventRating() {}

    public EventRating(int id, int score, int eventId, UUID userId) {
        this.id = id;
        this.score = score;
        this.eventId = eventId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
