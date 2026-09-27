package com.syncsphere.dao;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserDAO {
    private static final Map<String, User> MEMORY_USERS = new ConcurrentHashMap<>();
    private static final AtomicLong MEMORY_ID = new AtomicLong(1L);

    public User findByUsername(String username) {
        String normalized = username == null ? null : username.trim();
        if (normalized == null || normalized.isBlank()) {
            return null;
        }

        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalized);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapRow(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            return MEMORY_USERS.get(normalized);
        }
        return MEMORY_USERS.get(normalized);
    }

    public User save(User user) {
        if (user == null) {
            return null;
        }
        try {
            String sql = "INSERT INTO users (username, password_hash, email, google_id, auth_provider, role, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getPasswordHash());
                statement.setString(3, user.getEmail());
                statement.setString(4, user.getGoogleId());
                statement.setString(5, user.getAuthProvider());
                statement.setString(6, user.getRole());
                statement.setString(7, user.getStatus());
                statement.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getLong(1));
                    }
                }
                return user;
            }
        } catch (SQLException e) {
            String username = user.getUsername();
            if (user.getId() == null) {
                user.setId(MEMORY_ID.getAndIncrement());
            }
            MEMORY_USERS.put(username, user);
            return user;
        }
    }

    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalizedEmail = email.trim();
        try {
            String sql = "SELECT * FROM users WHERE email = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalizedEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapRow(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            return MEMORY_USERS.values().stream()
                    .filter(user -> normalizedEmail.equalsIgnoreCase(user.getEmail()))
                    .findFirst()
                    .orElse(null);
        }
        return MEMORY_USERS.values().stream()
                .filter(user -> normalizedEmail.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public User validateLogin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        User user = findByUsername(username.trim());
        if (user == null) {
            return null;
        }

        if (user.getPasswordHash() == null || !com.syncsphere.util.PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }

        return user;
    }

    public User findByGoogleId(String googleId) {
        if (googleId == null || googleId.isBlank()) {
            return null;
        }
        try {
            String sql = "SELECT * FROM users WHERE google_id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, googleId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapRow(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            return MEMORY_USERS.values().stream()
                    .filter(user -> googleId.equals(user.getGoogleId()))
                    .findFirst()
                    .orElse(null);
        }
        return MEMORY_USERS.values().stream()
                .filter(user -> googleId.equals(user.getGoogleId()))
                .findFirst()
                .orElse(null);
    }

    public User linkGoogleIdentity(User user, String googleId) {
        if (user == null || user.getId() == null || googleId == null || googleId.isBlank()) {
            return user;
        }
        try {
            String sql = "UPDATE users SET google_id = ?, auth_provider = 'GOOGLE' WHERE id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, googleId);
                statement.setLong(2, user.getId());
                statement.executeUpdate();
            }
        } catch (SQLException ignored) {
            // Keep the in-memory fallback consistent with the database path.
        }
        user.setGoogleId(googleId);
        user.setAuthProvider("GOOGLE");
        return user;
    }

    public User updateStatus(Long userId, String status) {
        try {
            String sql = "UPDATE users SET status = ? WHERE id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, status);
                statement.setLong(2, userId);
                statement.executeUpdate();
                return findById(userId);
            }
        } catch (SQLException e) {
            User match = MEMORY_USERS.values().stream()
                    .filter(user -> Objects.equals(user.getId(), userId))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                match.setStatus(status);
                match.setOnline("ONLINE".equalsIgnoreCase(status));
                return match;
            }
            return null;
        }
    }

    public User updateUserStatus(Long userId, boolean isOnline, String customStatus) {
        User user = findById(userId);
        if (user != null) {
            user.setOnline(isOnline);
            user.setCustomStatus(customStatus);
            updateStatus(userId, isOnline ? "ONLINE" : "OFFLINE");
        }
        return user;
    }


    public User findById(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapRow(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            return MEMORY_USERS.values().stream()
                    .filter(user -> Objects.equals(user.getId(), userId))
                    .findFirst()
                    .orElse(null);
        }
        return MEMORY_USERS.values().stream()
                .filter(user -> Objects.equals(user.getId(), userId))
                .findFirst()
                .orElse(null);
    }

    public List<User> findOnlineUsers() {
        try {
            String sql = "SELECT * FROM users WHERE status = 'ONLINE' ORDER BY username";
            List<User> users = new ArrayList<>();
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapRow(resultSet));
                }
            }
            return users;
        } catch (SQLException e) {
            return MEMORY_USERS.values().stream()
                    .filter(User::isOnline)
                    .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                    .toList();
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setEmail(resultSet.getString("email"));
        user.setGoogleId(resultSet.getString("google_id"));
        user.setAuthProvider(resultSet.getString("auth_provider"));
        user.setRole(resultSet.getString("role"));
        String status = resultSet.getString("status");
        user.setStatus(status);
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        return user;
    }
}
