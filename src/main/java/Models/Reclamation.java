package Models;

public class Reclamation {

    private int id;
    private String sujet;
    private String description;
    private String statut;
    private String pieceJointe;
    private byte[] userIdBytes; // To store BINARY(16) array in Java cleanly, or map it manually
    private String username;

    // ================= Constructeurs =================
    public Reclamation() {
        this.statut = "En attente"; // Default status
    }

    public Reclamation(String sujet, String description, String pieceJointe, byte[] userIdBytes) {
        this.sujet = sujet;
        this.description = description;
        this.pieceJointe = pieceJointe;
        this.userIdBytes = userIdBytes;
        this.statut = "En attente";
    }

    // ================= Getters & Setters =================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getPieceJointe() {
        return pieceJointe;
    }

    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }

    public byte[] getUserIdBytes() {
        return userIdBytes;
    }

    public void setUserIdBytes(byte[] userIdBytes) {
        this.userIdBytes = userIdBytes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", sujet='" + sujet + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
