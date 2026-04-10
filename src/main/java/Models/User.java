package Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {

    private UUID id;
    private String email;
    private String username;
    private String status;
    private String role;
    private String password;
    private boolean isVerified;
    private String verificationCode;
    private LocalDateTime createdAt;
    private String domaine;
    private String styleDapprentissage;
    private String niveau;



    // ================= Constructeur =================
    public User() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.isVerified = false;
    }

    // ================= Getters & Setters =================

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public String getStyleDapprentissage() {
        return styleDapprentissage;
    }

    public void setStyleDapprentissage(String styleDapprentissage) {
        this.styleDapprentissage = styleDapprentissage;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }


    // ================= Méthodes utiles =================

    public List<String> getRoles() {
        List<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");

        if ("admin".equalsIgnoreCase(this.role)) {
            roles.add("ROLE_ADMIN");
        }

        return roles;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}