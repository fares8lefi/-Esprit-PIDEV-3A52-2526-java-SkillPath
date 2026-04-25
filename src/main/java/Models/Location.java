package Models;

public class Location {
    private int id;
    private String name;
    private String building;
    private String roomNumber;
    private int maxCapacity;
    private String image;

    public Location() {}

    public Location(int id, String name, String building, String roomNumber, int maxCapacity, String image) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.roomNumber = roomNumber;
        this.maxCapacity = maxCapacity;
        this.image = image;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return name + " (" + building + " - " + roomNumber + ")";
    }
}
