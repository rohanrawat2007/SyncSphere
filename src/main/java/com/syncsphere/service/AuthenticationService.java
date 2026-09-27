package com.syncsphere.service;

import com.syncsphere.dao.UserDAO;
import com.syncsphere.exception.InvalidLoginException;
import com.syncsphere.exception.UserAlreadyExistsException;
import com.syncsphere.model.User;
import com.syncsphere.util.PasswordUtil;
import com.syncsphere.util.ValidationUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationService {
    private final UserDAO userDAO = new UserDAO();
    private final Map<String, User> activeSessions = new ConcurrentHashMap<>();

    public User register(String username, String password) throws UserAlreadyExistsException {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);

        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            throw new UserAlreadyExistsException("User '" + username + "' already exists.");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setRole("USER");
        user.setStatus("OFFLINE");
        user.setOnline(false);

        User saved = userDAO.save(user);
        return saved;
    }

    public User login(String username, String password) throws InvalidLoginException {
        ValidationUtil.validateUsername(username);
        if (password == null || password.isBlank()) {
            throw new InvalidLoginException("Password is required.");
        }

        User user = userDAO.validateLogin(username, password);
        if (user == null) {
            throw new InvalidLoginException("Invalid username or password.");
        }

        user.setStatus("ONLINE");
        user.setOnline(true);
        userDAO.updateStatus(user.getId(), "ONLINE");
        activeSessions.put(user.getUsername(), user);
        SessionManager.getInstance().login(user);
        return user;
    }

    public User loginGoogleUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Google account information is unavailable.");
        }
        user.setStatus("ONLINE");
        user.setOnline(true);
        userDAO.updateStatus(user.getId(), "ONLINE");
        activeSessions.put(user.getUsername(), user);
        SessionManager.getInstance().login(user);
        return user;
    }

    public void logout(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        activeSessions.remove(username);

        User user = userDAO.findByUsername(username);
        if (user != null) {
            userDAO.updateStatus(user.getId(), "OFFLINE");
            user.setStatus("OFFLINE");
            user.setOnline(false);
            SessionManager.getInstance().logout(user);
        }
    }

    public boolean isUserLoggedIn(String username) {
        return activeSessions.containsKey(username);
    }

    public Map<String, User> getActiveSessions() {
        return activeSessions;
    }
}
