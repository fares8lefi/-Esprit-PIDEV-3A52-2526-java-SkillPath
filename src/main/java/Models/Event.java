package Models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String image;
    private int locationId;
    private Location location;
    private double averageRating;

    public Event() {}

    public Event(int id, String title, String description, LocalDate eventDate, LocalTime startTime, LocalTime endTime, String image, int locationId, double averageRating) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.image = image;
        this.locationId = locationId;
        this.averageRating = averageRating;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    @Override
    public String toString() {
        return title;
    }
}
