package Models;

import java.util.UUID;

public class UserJoinedEvent {
    private UUID userId;
    private int eventId;
    private Integer seatNumber;

    public UserJoinedEvent() {}

    public UserJoinedEvent(UUID userId, int eventId, Integer seatNumber) {
        this.userId = userId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
