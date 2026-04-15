package Models.evaluation;

import java.sql.Timestamp;

public class Resultat {
    private int id_resultat;
    private int score;
    private int note_max;
    private Timestamp date_passage;
    private int id_quiz;
    private int id_etudiant;

    public Resultat() {
    }

    public Resultat(int id_resultat, int score, int note_max, Timestamp date_passage, int id_quiz, int id_etudiant) {
        this.id_resultat = id_resultat;
        this.score = score;
        this.note_max = note_max;
        this.date_passage = date_passage;
        this.id_quiz = id_quiz;
        this.id_etudiant = id_etudiant;
    }

    public Resultat(int score, int note_max, Timestamp date_passage, int id_quiz, int id_etudiant) {
        this.score = score;
        this.note_max = note_max;
        this.date_passage = date_passage;
        this.id_quiz = id_quiz;
        this.id_etudiant = id_etudiant;
    }

    public int getId_resultat() {
        return id_resultat;
    }

    public void setId_resultat(int id_resultat) {
        this.id_resultat = id_resultat;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getNote_max() {
        return note_max;
    }

    public void setNote_max(int note_max) {
        this.note_max = note_max;
    }

    public Timestamp getDate_passage() {
        return date_passage;
    }

    public void setDate_passage(Timestamp date_passage) {
        this.date_passage = date_passage;
    }

    public int getId_quiz() {
        return id_quiz;
    }

    public void setId_quiz(int id_quiz) {
        this.id_quiz = id_quiz;
    }

    public int getId_etudiant() {
        return id_etudiant;
    }

    public void setId_etudiant(int id_etudiant) {
        this.id_etudiant = id_etudiant;
    }

    @Override
    public String toString() {
        return "Resultat{" +
                "id_resultat=" + id_resultat +
                ", score=" + score +
                ", note_max=" + note_max +
                ", date_passage=" + date_passage +
                ", id_quiz=" + id_quiz +
                ", id_etudiant=" + id_etudiant +
                '}';
    }
}
