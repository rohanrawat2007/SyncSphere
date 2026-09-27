package com.syncsphere.service;

import com.syncsphere.exception.MessageBlockedException;
import com.syncsphere.exception.UnauthorizedActionException;
import com.syncsphere.filter.BannedWordFilter;
import com.syncsphere.model.User;
import com.syncsphere.database.DBConnection;

import java.util.List;
import java.util.Locale;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ModerationService {
    private final BannedWordFilter bannedWordFilter;

    public ModerationService() {
        this.bannedWordFilter = new BannedWordFilter(List.of("spam", "scam", "hack", "banned"));
    }

    public boolean isMessageAllowed(String message) {
        return !bannedWordFilter.containsBlockedContent(message);
    }

    public String processMessage(String message) throws MessageBlockedException {
        if (message == null || message.isBlank()) {
            return message;
        }
        if (bannedWordFilter.containsBlockedContent(message)) {
            throw new MessageBlockedException("Message contains banned words and has been blocked.");
        }
        return bannedWordFilter.sanitize(message);
    }

    public void muteUser(User moderator, User targetUser) throws UnauthorizedActionException {
        requireModerator(moderator);
        if (targetUser == null) {
            throw new IllegalArgumentException("Target user is required.");
        }
        targetUser.setStatus("MUTED");
        record(moderator, targetUser, null, "MUTE", "Manual mute");
    }

    public void unmuteUser(User moderator, User targetUser) throws UnauthorizedActionException {
        requireModerator(moderator);
        if (targetUser == null) {
            throw new IllegalArgumentException("Target user is required.");
        }
        targetUser.setStatus(targetUser.isOnline() ? "ONLINE" : "OFFLINE");
        record(moderator, targetUser, null, "UNMUTE", "Manual unmute");
    }

    public void deleteMessage(User moderator) throws UnauthorizedActionException {
        requireModerator(moderator);
    }

    public void warn(User moderator, User targetUser, String reason) throws UnauthorizedActionException {
        requireModerator(moderator);
        if (targetUser == null) throw new IllegalArgumentException("Target user is required.");
        record(moderator, targetUser, null, "WARN", reason);
    }

    public void deleteMessage(User moderator, Long messageId, String reason) throws UnauthorizedActionException {
        requireModerator(moderator);
        if (messageId == null) throw new IllegalArgumentException("Message is required.");
        new com.syncsphere.dao.MessageDAO().deleteMessage(messageId);
        record(moderator, null, messageId, "DELETE", reason);
    }

    public void kick(User moderator, User targetUser, String reason) throws UnauthorizedActionException {
        requireModerator(moderator);
        if (targetUser == null) throw new IllegalArgumentException("Target user is required.");
        targetUser.setStatus("OFFLINE");
        SessionManager.getInstance().logout(targetUser);
        record(moderator, targetUser, null, "KICK", reason);
    }

    private void record(User moderator, User target, Long messageId, String action, String reason) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO moderation_logs (moderator_id, target_user_id, message_id, action, reason) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, moderator.getId());
            if (target == null) statement.setNull(2, java.sql.Types.BIGINT); else statement.setLong(2, target.getId());
            if (messageId == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, messageId);
            statement.setString(4, action); statement.setString(5, reason);
            statement.executeUpdate();
        } catch (SQLException ignored) { }
    }

    private void requireModerator(User moderator) throws UnauthorizedActionException {
        if (moderator == null || !moderator.isModerator()) {
            throw new UnauthorizedActionException("Only moderators can perform moderation actions.");
        }
    }
}
