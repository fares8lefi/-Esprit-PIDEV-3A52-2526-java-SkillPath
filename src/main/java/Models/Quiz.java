package Models;

import java.sql.Timestamp;

public class Quiz {
    private int id_quiz;
    private String titre;
    private String description;
    private int duree;
    private int note_max;
    private Timestamp date_creation;
    private Integer course_id;

    public Quiz() {
    }

    public Quiz(int id_quiz, String titre, String description, int duree, int note_max, Timestamp date_creation,
            Integer course_id) {
        this.id_quiz = id_quiz;
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.note_max = note_max;
        this.date_creation = date_creation;
        this.course_id = course_id;
    }

    public Quiz(String titre, String description, int duree, int note_max, Timestamp date_creation, Integer course_id) {
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.note_max = note_max;
        this.date_creation = date_creation;
        this.course_id = course_id;
    }

    public int getId_quiz() {
        return id_quiz;
    }

    public void setId_quiz(int id_quiz) {
        this.id_quiz = id_quiz;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public int getNote_max() {
        return note_max;
    }

    public void setNote_max(int note_max) {
        this.note_max = note_max;
    }

    public Timestamp getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(Timestamp date_creation) {
        this.date_creation = date_creation;
    }

    public Integer getCourse_id() {
        return course_id;
    }

    public void setCourse_id(Integer course_id) {
        this.course_id = course_id;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id_quiz=" + id_quiz +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", duree=" + duree +
                ", note_max=" + note_max +
                ", date_creation=" + date_creation +
                ", course_id=" + course_id +
                '}';
    }
}
