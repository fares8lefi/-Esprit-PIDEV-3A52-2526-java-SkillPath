package Models;

import java.sql.Timestamp;

public class BlockedIp {
    private int id;
    private String ip;
    private String reason;
    private double score;
    private Timestamp blockedAt;
    private Timestamp expiresAt;
    private boolean isActive;

    public BlockedIp() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public Timestamp getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Timestamp blockedAt) { this.blockedAt = blockedAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
}
