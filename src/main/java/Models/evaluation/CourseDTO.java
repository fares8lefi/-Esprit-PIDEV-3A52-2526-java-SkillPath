package Models.evaluation;

public class CourseDTO {
    private int id;
    private String title;

    public CourseDTO(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
