package com.syncsphere.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String googleId;
    private String authProvider;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private boolean online;
    private String customStatus;


    public User() {
        this.role = "USER";
        this.status = "OFFLINE";
        this.authProvider = "LOCAL";
    }

    public User(Long id, String username, String passwordHash, String email, String googleId, String authProvider,
                String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.googleId = googleId;
        this.authProvider = (authProvider == null || authProvider.isBlank()) ? "LOCAL" : authProvider;
        this.role = (role == null || role.isBlank()) ? "USER" : role;
        this.status = (status == null || status.isBlank()) ? "OFFLINE" : status;
        this.createdAt = createdAt;
        this.online = "ONLINE".equalsIgnoreCase(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.online = "ONLINE".equalsIgnoreCase(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
        this.status = online ? "ONLINE" : "OFFLINE";
    }

    public String getCustomStatus() {
        return customStatus;
    }

    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }

    public boolean isModerator() {

        return "MODERATOR".equalsIgnoreCase(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
