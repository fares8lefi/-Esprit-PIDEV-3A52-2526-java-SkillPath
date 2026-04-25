package Models;

import java.util.UUID;

public class UserFavouriteEvent {
    private UUID userId;
    private int eventId;

    public UserFavouriteEvent() {}

    public UserFavouriteEvent(UUID userId, int eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
}
