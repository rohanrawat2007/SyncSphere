package com.syncsphere.service;

import com.syncsphere.model.User;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<Long, User> activeSessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void login(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("A valid user is required to login.");
        }
        user.setStatus("ONLINE");
        user.setOnline(true);
        activeSessions.put(user.getId(), user);
    }

    public void logout(User user) {
        if (user == null) {
            return;
        }
        activeSessions.remove(user.getId());
        if (user.getId() != null) {
            user.setStatus("OFFLINE");
            user.setOnline(false);
        }
    }

    public User getCurrentUser(Long userId) {
        return activeSessions.get(userId);
    }

    public boolean isLoggedIn(Long userId) {
        return activeSessions.containsKey(userId);
    }

    public boolean isLoggedIn(User user) {
        return user != null && isLoggedIn(user.getId());
    }

    public User requireAuthenticated(User candidate) {
        if (candidate == null || candidate.getId() == null) {
            throw new IllegalStateException("Authentication is required.");
        }
        User active = activeSessions.get(candidate.getId());
        if (active == null || !Objects.equals(active.getUsername(), candidate.getUsername())) {
            throw new IllegalStateException("Authentication is required.");
        }
        return active;
    }
}
