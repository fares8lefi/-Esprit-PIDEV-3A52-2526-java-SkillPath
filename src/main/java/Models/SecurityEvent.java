package Models;

import java.sql.Timestamp;

public class SecurityEvent {
    private long id;
    private String ip;
    private String username;
    private double score;
    private String action;
    private String riskLevel;
    private Timestamp createdAt;

    public SecurityEvent() {}

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
