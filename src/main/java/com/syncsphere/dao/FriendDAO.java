package com.syncsphere.dao;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.Friend;
import com.syncsphere.model.FriendRequest;
import com.syncsphere.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendDAO {
    public User findByExactUsername(String username, long currentUserId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE LOWER(username) = LOWER(?) AND id <> ?")) {
            statement.setString(1, username.trim()); statement.setLong(2, currentUserId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? mapUser(result) : null; }
        }
    }

    public List<User> searchUsers(String query, long currentUserId) throws SQLException {
        List<User> users = new ArrayList<>();
        if (query == null || query.isBlank()) return users;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE LOWER(username) LIKE LOWER(?) AND id <> ? ORDER BY username LIMIT 20")) {
            statement.setString(1, "%" + query.trim() + "%"); statement.setLong(2, currentUserId);
            try (ResultSet result = statement.executeQuery()) { while (result.next()) users.add(mapUser(result)); }
        }
        return users;
    }

    public User findById(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? mapUser(result) : null; }
        }
    }

    public FriendRequest findRequest(long requestId) throws SQLException {
        String sql = "SELECT r.id, r.status, r.created_at, r.responded_at, " + userColumns("s") + ", " + userColumns("u") +
                " FROM friend_requests r JOIN users s ON s.id = r.sender_id JOIN users u ON u.id = r.receiver_id WHERE r.id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId); try (ResultSet result = statement.executeQuery()) { return result.next() ? mapRequest(result) : null; }
        }
    }

    public FriendRequest findDirectedRequest(long senderId, long receiverId) throws SQLException {
        String sql = "SELECT r.id, r.status, r.created_at, r.responded_at, " + userColumns("s") + ", " + userColumns("u") +
                " FROM friend_requests r JOIN users s ON s.id = r.sender_id JOIN users u ON u.id = r.receiver_id WHERE r.sender_id = ? AND r.receiver_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, senderId); statement.setLong(2, receiverId); try (ResultSet result = statement.executeQuery()) { return result.next() ? mapRequest(result) : null; }
        }
    }

    public List<FriendRequest> incomingRequests(long receiverId) throws SQLException {
        String sql = "SELECT r.id, r.status, r.created_at, r.responded_at, " + userColumns("s") + ", " + userColumns("u") +
                " FROM friend_requests r JOIN users s ON s.id = r.sender_id JOIN users u ON u.id = r.receiver_id WHERE r.receiver_id = ? AND r.status = 'PENDING' ORDER BY r.created_at DESC";
        List<FriendRequest> requests = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, receiverId); try (ResultSet result = statement.executeQuery()) { while (result.next()) requests.add(mapRequest(result)); }
        }
        return requests;
    }

    public long createOrReopenRequest(long senderId, long receiverId) throws SQLException {
        FriendRequest existing = findDirectedRequest(senderId, receiverId);
        if (existing != null && "PENDING".equals(existing.status())) throw new IllegalStateException("Friend request already pending.");
        if (existing != null) {
            try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE friend_requests SET status = 'PENDING', created_at = CURRENT_TIMESTAMP, responded_at = NULL WHERE id = ?")) { statement.setLong(1, existing.id()); statement.executeUpdate(); return existing.id(); }
        }
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO friend_requests (sender_id, receiver_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, senderId); statement.setLong(2, receiverId); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (keys.next()) return keys.getLong(1); }
        }
        throw new SQLException("Friend request was not created.");
    }

    public void respond(long requestId, long receiverId, String status) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE friend_requests SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ? AND receiver_id = ? AND status = 'PENDING'")) {
            statement.setString(1, status); statement.setLong(2, requestId); statement.setLong(3, receiverId);
            if (statement.executeUpdate() == 0) throw new IllegalStateException("Friend request is unavailable.");
        }
    }

    public void createFriendship(long firstUserId, long secondUserId) throws SQLException {
        long low = Math.min(firstUserId, secondUserId), high = Math.max(firstUserId, secondUserId);
        String sql = DBConnection.isPostgres()
            ? "INSERT INTO friends (user_id, friend_id) VALUES (?, ?) ON CONFLICT (user_id, friend_id) DO NOTHING"
            : "INSERT IGNORE INTO friends (user_id, friend_id) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, low); statement.setLong(2, high); statement.executeUpdate(); }
    }

    public boolean areFriends(long firstUserId, long secondUserId) throws SQLException {
        long low = Math.min(firstUserId, secondUserId), high = Math.max(firstUserId, secondUserId);
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?")) { statement.setLong(1, low); statement.setLong(2, high); try (ResultSet result = statement.executeQuery()) { return result.next(); } }
    }

    public List<Friend> findFriends(long userId) throws SQLException {
        String sql = "SELECT f.created_at, u.* FROM friends f JOIN users u ON u.id = IF(f.user_id = ?, f.friend_id, f.user_id) WHERE f.user_id = ? OR f.friend_id = ? ORDER BY u.username";
        List<Friend> friends = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { statement.setLong(1, userId); statement.setLong(2, userId); statement.setLong(3, userId); try (ResultSet result = statement.executeQuery()) { while (result.next()) friends.add(new Friend(mapUser(result), result.getTimestamp("created_at").toLocalDateTime())); } }
        return friends;
    }

    public void removeFriendship(long firstUserId, long secondUserId) throws SQLException {
        long low = Math.min(firstUserId, secondUserId), high = Math.max(firstUserId, secondUserId);
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM friends WHERE user_id = ? AND friend_id = ?")) { statement.setLong(1, low); statement.setLong(2, high); if (statement.executeUpdate() == 0) throw new IllegalStateException("Friendship does not exist."); }
    }

    public void addRequestNotification(long receiverId, long requestId, String type, String content) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO notifications (user_id, friend_request_id, type, content) VALUES (?, ?, ?, ?)")) { statement.setLong(1, receiverId); statement.setLong(2, requestId); statement.setString(3, type); statement.setString(4, content); statement.executeUpdate(); }
    }

    private FriendRequest mapRequest(ResultSet result) throws SQLException { return new FriendRequest(result.getLong("id"), mapUser(result, "s_"), mapUser(result, "u_"), result.getString("status"), result.getTimestamp("created_at").toLocalDateTime(), result.getTimestamp("responded_at") == null ? null : result.getTimestamp("responded_at").toLocalDateTime()); }
    private User mapUser(ResultSet result) throws SQLException { return mapUser(result, ""); }
    private User mapUser(ResultSet result, String prefix) throws SQLException { User user = new User(); user.setId(result.getLong(prefix + "id")); user.setUsername(result.getString(prefix + "username")); user.setEmail(result.getString(prefix + "email")); user.setGoogleId(result.getString(prefix + "google_id")); user.setAuthProvider(result.getString(prefix + "auth_provider")); user.setRole(result.getString(prefix + "role")); user.setStatus(result.getString(prefix + "status")); Timestamp created = result.getTimestamp(prefix + "created_at"); if (created != null) user.setCreatedAt(created.toLocalDateTime()); return user; }
    private String userColumns(String alias) { return alias + ".id AS " + alias + "_id, " + alias + ".username AS " + alias + "_username, " + alias + ".email AS " + alias + "_email, " + alias + ".google_id AS " + alias + "_google_id, " + alias + ".auth_provider AS " + alias + "_auth_provider, " + alias + ".role AS " + alias + "_role, " + alias + ".status AS " + alias + "_status, " + alias + ".created_at AS " + alias + "_created_at"; }
}
