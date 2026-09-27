package com.syncsphere.dao;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.Message;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class MessageDAO {
    private static final List<Message> MEMORY_MESSAGES = new CopyOnWriteArrayList<>();
    private static final AtomicLong MEMORY_ID = new AtomicLong(1L);

    public Long savePublicMessage(Long senderId, String content) {
        return savePublicMessage(senderId, content, null);
    }

    public Long savePublicMessage(Long senderId, String content, Long replyToMessageId) {
        try {
            String sql = "INSERT INTO messages (sender_id, receiver_id, message, message_type, reply_to_message_id, created_at, is_deleted) VALUES (?, NULL, ?, 'PUBLIC', ?, ?, false)";
              try (Connection connection = DBConnection.getConnection();
                  PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, senderId);
                statement.setString(2, content);
                if (replyToMessageId == null) statement.setNull(3, Types.BIGINT); else statement.setLong(3, replyToMessageId);
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : null;
                }
            }
        } catch (SQLException e) {
            User sender = new UserDAO().findById(senderId);
            MEMORY_MESSAGES.add(new PublicMessage(MEMORY_ID.getAndIncrement(), senderId,
                    sender != null ? sender.getUsername() : "Unknown",
                    content, LocalDateTime.now(), false));
                return MEMORY_MESSAGES.get(MEMORY_MESSAGES.size() - 1).getId();
        }
    }

            public Long savePrivateMessage(Long senderId, Long receiverId, String content) {
                return savePrivateMessage(senderId, receiverId, content, null);
            }

            public Long savePrivateMessage(Long senderId, Long receiverId, String content, Long replyToMessageId) {
        try {
                    String sql = "INSERT INTO messages (sender_id, receiver_id, message, message_type, reply_to_message_id, created_at, is_deleted) VALUES (?, ?, ?, 'PRIVATE', ?, ?, false)";
              try (Connection connection = DBConnection.getConnection();
                  PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, senderId);
                statement.setLong(2, receiverId);
                statement.setString(3, content);
                if (replyToMessageId == null) statement.setNull(4, Types.BIGINT); else statement.setLong(4, replyToMessageId);
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : null;
                }
            }
        } catch (SQLException e) {
            User sender = new UserDAO().findById(senderId);
            User receiver = new UserDAO().findById(receiverId);
            MEMORY_MESSAGES.add(new PrivateMessage(MEMORY_ID.getAndIncrement(), senderId, receiverId,
                    sender != null ? sender.getUsername() : "Unknown",
                    receiver != null ? receiver.getUsername() : "Unknown",
                    content, LocalDateTime.now(), false));
            return MEMORY_MESSAGES.get(MEMORY_MESSAGES.size() - 1).getId();
        }
    }

    public void updateMessage(long messageId, long senderId, String content) throws SQLException {
        String sql = "UPDATE messages SET message = ?, is_edited = true WHERE id = ? AND sender_id = ? AND is_deleted = false";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content);
            statement.setLong(2, messageId);
            statement.setLong(3, senderId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Message was not found or cannot be edited.");
            }
        }
    }

    public List<Message> findPublicMessages() {
        try {
            String sql = "SELECT m.*, u.username AS sender_username FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.message_type = 'PUBLIC' AND m.is_deleted = false ORDER BY m.created_at ASC";
            List<Message> messages = new ArrayList<>();
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new PublicMessage(
                            resultSet.getLong("id"),
                            resultSet.getLong("sender_id"),
                            resultSet.getString("sender_username"),
                            resultSet.getString("message"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getBoolean("is_deleted")
                    ));
                            messages.get(messages.size() - 1).setReplyToMessageId((Long) resultSet.getObject("reply_to_message_id"));
                            messages.get(messages.size() - 1).setEdited(resultSet.getBoolean("is_edited"));
                }
            }
            return messages;
        } catch (SQLException e) {
            return MEMORY_MESSAGES.stream()
                    .filter(msg -> msg instanceof PublicMessage && !msg.isDeleted())
                    .toList();
        }
    }

    public List<Message> findPrivateMessagesBetween(Long userA, Long userB) {
        try {
            String sql = "SELECT m.*, u1.username AS sender_username, u2.username AS receiver_username " +
                    "FROM messages m " +
                    "JOIN users u1 ON m.sender_id = u1.id " +
                    "LEFT JOIN users u2 ON m.receiver_id = u2.id " +
                    "WHERE m.is_deleted = false AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)) " +
                    "AND m.message_type = 'PRIVATE' ORDER BY m.created_at ASC";
            List<Message> messages = new ArrayList<>();
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userA);
                statement.setLong(2, userB);
                statement.setLong(3, userB);
                statement.setLong(4, userA);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        messages.add(new PrivateMessage(
                                resultSet.getLong("id"),
                                resultSet.getLong("sender_id"),
                                resultSet.getLong("receiver_id"),
                                resultSet.getString("sender_username"),
                                resultSet.getString("receiver_username"),
                                resultSet.getString("message"),
                                resultSet.getTimestamp("created_at").toLocalDateTime(),
                                resultSet.getBoolean("is_deleted")
                        ));
                            messages.get(messages.size() - 1).setReplyToMessageId((Long) resultSet.getObject("reply_to_message_id"));
                            messages.get(messages.size() - 1).setEdited(resultSet.getBoolean("is_edited"));
                    }
                }
            }
            return messages;
        } catch (SQLException e) {
            return MEMORY_MESSAGES.stream()
                    .filter(msg -> msg instanceof PrivateMessage && !msg.isDeleted())
                    .filter(msg -> (Objects.equals(msg.getSenderId(), userA) && Objects.equals(((PrivateMessage) msg).getReceiverId(), userB)) ||
                            (Objects.equals(msg.getSenderId(), userB) && Objects.equals(((PrivateMessage) msg).getReceiverId(), userA)))
                    .toList();
        }
    }

    public void deleteMessage(long messageId) {
        try {
            String sql = "UPDATE messages SET is_deleted = true WHERE id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, messageId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            MEMORY_MESSAGES.stream()
                    .filter(msg -> Objects.equals(msg.getId(), messageId))
                    .forEach(msg -> msg.setDeleted(true));
        }
    }

    public List<Message> searchMessages(String query) {
        try {
            String sql = "SELECT m.*, u.username AS sender_username, u2.username AS receiver_username " +
                    "FROM messages m JOIN users u ON m.sender_id = u.id LEFT JOIN users u2 ON m.receiver_id = u2.id " +
                    "WHERE m.message LIKE ? AND m.is_deleted = false ORDER BY m.created_at ASC";
            List<Message> messages = new ArrayList<>();
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "%" + query + "%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        if ("PUBLIC".equalsIgnoreCase(resultSet.getString("message_type"))) {
                            messages.add(new PublicMessage(
                                    resultSet.getLong("id"),
                                    resultSet.getLong("sender_id"),
                                    resultSet.getString("sender_username"),
                                    resultSet.getString("message"),
                                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                                        resultSet.getBoolean("is_deleted")
                            ));
                                    messages.get(messages.size() - 1).setReplyToMessageId((Long) resultSet.getObject("reply_to_message_id"));
                                    messages.get(messages.size() - 1).setEdited(resultSet.getBoolean("is_edited"));
                        } else {
                            messages.add(new PrivateMessage(
                                    resultSet.getLong("id"),
                                    resultSet.getLong("sender_id"),
                                    resultSet.getLong("receiver_id"),
                                    resultSet.getString("sender_username"),
                                    resultSet.getString("receiver_username"),
                                    resultSet.getString("message"),
                                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                                        resultSet.getBoolean("is_deleted")
                            ));
                                    messages.get(messages.size() - 1).setReplyToMessageId((Long) resultSet.getObject("reply_to_message_id"));
                                    messages.get(messages.size() - 1).setEdited(resultSet.getBoolean("is_edited"));
                        }
                    }
                }
            }
            return messages;
        } catch (SQLException e) {
            return MEMORY_MESSAGES.stream()
                    .filter(msg -> !msg.isDeleted())
                    .filter(msg -> msg.getContent() != null && msg.getContent().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }
    }
}
