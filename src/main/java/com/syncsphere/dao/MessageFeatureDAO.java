package com.syncsphere.dao;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.MessageReaction;
import com.syncsphere.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageFeatureDAO {
    public void toggleReaction(long messageId, long userId, String reaction) throws SQLException {
        String select = "SELECT id FROM message_reactions WHERE message_id = ? AND user_id = ? AND reaction = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement check = connection.prepareStatement(select)) {
            check.setLong(1, messageId);
            check.setLong(2, userId);
            check.setString(3, reaction);
            try (ResultSet result = check.executeQuery()) {
                if (result.next()) {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM message_reactions WHERE id = ?")) {
                        delete.setLong(1, result.getLong(1));
                        delete.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO message_reactions (message_id, user_id, reaction) VALUES (?, ?, ?)")) {
                        insert.setLong(1, messageId);
                        insert.setLong(2, userId);
                        insert.setString(3, reaction);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    public List<MessageReaction> findReactions(long messageId) throws SQLException {
        List<MessageReaction> reactions = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT message_id, user_id, reaction, created_at FROM message_reactions WHERE message_id = ? ORDER BY created_at")) {
            statement.setLong(1, messageId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    reactions.add(new MessageReaction(result.getLong(1), result.getLong(2), result.getString(3),
                            result.getTimestamp(4).toLocalDateTime()));
                }
            }
        }
        return reactions;
    }

    public void markRead(long messageId, long userId) throws SQLException {
        String sql = DBConnection.isPostgres()
            ? "INSERT INTO message_reads (message_id, user_id) VALUES (?, ?) ON CONFLICT (message_id, user_id) DO UPDATE SET read_at = CURRENT_TIMESTAMP"
            : "INSERT INTO message_reads (message_id, user_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE read_at = CURRENT_TIMESTAMP";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    public void togglePin(long messageId, long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement check = connection.prepareStatement("SELECT message_id FROM pinned_messages WHERE message_id = ?")) {
            check.setLong(1, messageId);
            try (ResultSet result = check.executeQuery()) {
                if (result.next()) {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM pinned_messages WHERE message_id = ?")) {
                        delete.setLong(1, messageId);
                        delete.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO pinned_messages (message_id, pinned_by) VALUES (?, ?)")) {
                        insert.setLong(1, messageId);
                        insert.setLong(2, userId);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    public List<Long> findPinnedMessageIds() throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT message_id FROM pinned_messages ORDER BY pinned_at DESC")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getLong(1));
            }
        }
        return ids;
    }

    public void addNotification(long userId, Long messageId, String type, String content) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO notifications (user_id, message_id, type, content) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, userId);
            if (messageId == null) statement.setNull(2, Types.BIGINT); else statement.setLong(2, messageId);
            statement.setString(3, type);
            statement.setString(4, content);
            statement.executeUpdate();
        }
    }

    public void addMentionNotificationIfAbsent(long userId, long messageId, String content) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO notifications (user_id, message_id, type, content) " +
                     "SELECT ?, ?, 'MENTION', ? WHERE NOT EXISTS " +
                             "(SELECT 1 FROM notifications WHERE user_id = ? AND message_id = ? AND type = 'MENTION')")) {
            statement.setLong(1, userId); statement.setLong(2, messageId); statement.setString(3, content);
            statement.setLong(4, userId); statement.setLong(5, messageId); statement.executeUpdate();
        }
    }

    public int countUnreadPrivateMessages(long userId, long otherUserId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM messages m WHERE m.message_type = 'PRIVATE' AND m.receiver_id = ? AND m.sender_id = ? AND m.is_deleted = false AND NOT EXISTS (SELECT 1 FROM message_reads r WHERE r.message_id = m.id AND r.user_id = ?)")) {
            statement.setLong(1, userId); statement.setLong(2, otherUserId); statement.setLong(3, userId);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); }
        }
    }

    public boolean isMuted(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT expires_at FROM mutes WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return false;
                Timestamp expires = result.getTimestamp(1);
                if (expires != null && expires.toInstant().isBefore(java.time.Instant.now())) {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM mutes WHERE user_id = ?")) { delete.setLong(1, userId); delete.executeUpdate(); }
                    return false;
                }
                return true;
            }
        }
    }

    public void setMute(long userId, long moderatorId, long durationSeconds, String reason) throws SQLException {
        String sql = DBConnection.isPostgres()
            ? "INSERT INTO mutes (user_id, muted_by, expires_at, reason) VALUES (?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET muted_by = EXCLUDED.muted_by, expires_at = EXCLUDED.expires_at, reason = EXCLUDED.reason"
            : "INSERT INTO mutes (user_id, muted_by, expires_at, reason) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE muted_by = VALUES(muted_by), expires_at = VALUES(expires_at), reason = VALUES(reason)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId); statement.setLong(2, moderatorId);
            if (durationSeconds <= 0) statement.setNull(3, Types.TIMESTAMP); else statement.setTimestamp(3, Timestamp.from(java.time.Instant.now().plusSeconds(durationSeconds)));
            statement.setString(4, reason); statement.executeUpdate();
        }
    }

    public void clearMute(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM mutes WHERE user_id = ?")) { statement.setLong(1, userId); statement.executeUpdate(); }
    }

    public List<Notification> findUnreadNotifications(long userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, user_id, message_id, type, content, is_read, created_at FROM notifications WHERE user_id = ? AND is_read = false ORDER BY created_at DESC")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp timestamp = result.getTimestamp(7);
                    notifications.add(new Notification(result.getLong(1), result.getLong(2),
                            (Long) result.getObject(3), result.getString(4), result.getString(5),
                            result.getBoolean(6), timestamp == null ? null : timestamp.toLocalDateTime()));
                }
            }
        }
        return notifications;
    }

    public List<Notification> findRecentNotifications(long userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, user_id, message_id, type, content, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp timestamp = result.getTimestamp(7);
                    notifications.add(new Notification(result.getLong(1), result.getLong(2),
                            (Long) result.getObject(3), result.getString(4), result.getString(5),
                            result.getBoolean(6), timestamp == null ? null : timestamp.toLocalDateTime()));
                }
            }
        }
        return notifications;
    }

    public void markNotificationsRead(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE notifications SET is_read = true WHERE user_id = ? AND is_read = false")) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }
}