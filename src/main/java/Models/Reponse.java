package Models;

public class Reponse {

    private int id;
    private String message;
    private int reclamationId;
    private byte[] userIdBytes; // To link who sent the response via BINARY(16)

    // ================= Constructeurs =================
    public Reponse() {
    }

    public Reponse(String message, int reclamationId, byte[] userIdBytes) {
        this.message = message;
        this.reclamationId = reclamationId;
        this.userIdBytes = userIdBytes;
    }

    // ================= Getters & Setters =================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public byte[] getUserIdBytes() {
        return userIdBytes;
    }

    public void setUserIdBytes(byte[] userIdBytes) {
        this.userIdBytes = userIdBytes;
    }

    @Override
    public String toString() {
        return "Reponse{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", reclamationId=" + reclamationId +
                '}';
    }
}
