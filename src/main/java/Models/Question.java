package Models;

public class Question {
    private int id_question;
    private String enonce;
    private String choix_a;
    private String choix_b;
    private String choix_c;
    private String choix_d;
    private String bonne_reponse;
    private int points;
    private int id_quiz;

    public Question() {
    }

    public Question(int id_question, String enonce, String choix_a, String choix_b, String choix_c, String choix_d,
            String bonne_reponse, int points, int id_quiz) {
        this.id_question = id_question;
        this.enonce = enonce;
        this.choix_a = choix_a;
        this.choix_b = choix_b;
        this.choix_c = choix_c;
        this.choix_d = choix_d;
        this.bonne_reponse = bonne_reponse;
        this.points = points;
        this.id_quiz = id_quiz;
    }

    public Question(String enonce, String choix_a, String choix_b, String choix_c, String choix_d, String bonne_reponse,
            int points, int id_quiz) {
        this.enonce = enonce;
        this.choix_a = choix_a;
        this.choix_b = choix_b;
        this.choix_c = choix_c;
        this.choix_d = choix_d;
        this.bonne_reponse = bonne_reponse;
        this.points = points;
        this.id_quiz = id_quiz;
    }

    public int getId_question() {
        return id_question;
    }

    public void setId_question(int id_question) {
        this.id_question = id_question;
    }

    public String getEnonce() {
        return enonce;
    }

    public void setEnonce(String enonce) {
        this.enonce = enonce;
    }

    public String getChoix_a() {
        return choix_a;
    }

    public void setChoix_a(String choix_a) {
        this.choix_a = choix_a;
    }

    public String getChoix_b() {
        return choix_b;
    }

    public void setChoix_b(String choix_b) {
        this.choix_b = choix_b;
    }

    public String getChoix_c() {
        return choix_c;
    }

    public void setChoix_c(String choix_c) {
        this.choix_c = choix_c;
    }

    public String getChoix_d() {
        return choix_d;
    }

    public void setChoix_d(String choix_d) {
        this.choix_d = choix_d;
    }

    public String getBonne_reponse() {
        return bonne_reponse;
    }

    public void setBonne_reponse(String bonne_reponse) {
        this.bonne_reponse = bonne_reponse;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getId_quiz() {
        return id_quiz;
    }

    public void setId_quiz(int id_quiz) {
        this.id_quiz = id_quiz;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id_question=" + id_question +
                ", enonce='" + enonce + '\'' +
                ", choix_a='" + choix_a + '\'' +
                ", choix_b='" + choix_b + '\'' +
                ", choix_c='" + choix_c + '\'' +
                ", choix_d='" + choix_d + '\'' +
                ", bonne_reponse='" + bonne_reponse + '\'' +
                ", points=" + points +
                ", id_quiz=" + id_quiz +
                '}';
    }
}
