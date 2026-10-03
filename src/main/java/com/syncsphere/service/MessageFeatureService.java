package com.syncsphere.service;

import com.syncsphere.dao.MessageFeatureDAO;
import com.syncsphere.model.MessageReaction;
import com.syncsphere.model.Notification;
import com.syncsphere.model.User;

import java.sql.SQLException;
import java.util.List;

public class MessageFeatureService {
    private final MessageFeatureDAO featureDAO = new MessageFeatureDAO();

    public void toggleReaction(long messageId, User user, String reaction) throws SQLException {
        requireUser(user);
        if (reaction == null || reaction.isBlank() || reaction.length() > 32) {
            throw new IllegalArgumentException("Choose a valid reaction.");
        }
        if (RemoteApiClient.isConfigured()) {
            try { RemoteApiClient.feature("toggleReaction", messageId, reaction.trim()); return; }
            catch (Exception exception) { throw new SQLException("Remote reaction failed.", exception); }
        }
        featureDAO.toggleReaction(messageId, user.getId(), reaction.trim());
    }

    public List<MessageReaction> getReactions(long messageId) throws SQLException {
        return featureDAO.findReactions(messageId);
    }

    public void markRead(long messageId, User user) throws SQLException {
        requireUser(user);
        if (RemoteApiClient.isConfigured()) {
            try { RemoteApiClient.feature("markRead", messageId, null); return; }
            catch (Exception exception) { throw new SQLException("Remote read receipt failed.", exception); }
        }
        featureDAO.markRead(messageId, user.getId());
    }

    public void togglePin(long messageId, User user) throws SQLException {
        requireModerator(user);
        if (RemoteApiClient.isConfigured()) {
            try { RemoteApiClient.feature("togglePin", messageId, null); return; }
            catch (Exception exception) { throw new SQLException("Remote pin failed.", exception); }
        }
        featureDAO.togglePin(messageId, user.getId());
    }

    public List<Long> getPinnedMessageIds() throws SQLException {
        if (RemoteApiClient.isConfigured()) {
            try { return RemoteApiClient.pinnedMessages(); }
            catch (Exception exception) { throw new SQLException("Remote pins could not be loaded.", exception); }
        }
        return featureDAO.findPinnedMessageIds();
    }

    public List<Notification> getUnreadNotifications(User user) throws SQLException {
        requireUser(user);
        if (RemoteApiClient.isConfigured()) {
            try { return RemoteApiClient.notifications(); }
            catch (Exception exception) { throw new SQLException("Remote notifications failed.", exception); }
        }
        return featureDAO.findUnreadNotifications(user.getId());
    }

    public List<Notification> getRecentNotifications(User user) throws SQLException {
        requireUser(user);
        return featureDAO.findRecentNotifications(user.getId());
    }

    public void markNotificationsRead(User user) throws SQLException {
        requireUser(user);
        if (RemoteApiClient.isConfigured()) {
            try { RemoteApiClient.markNotificationsRead(); return; }
            catch (Exception exception) { throw new SQLException("Remote notifications could not be updated.", exception); }
        }
        featureDAO.markNotificationsRead(user.getId());
    }

    public void addMentionNotificationIfAbsent(long userId, long messageId, String content) throws SQLException { featureDAO.addMentionNotificationIfAbsent(userId, messageId, content); }
    public void addNotification(long userId, Long messageId, String type, String content) throws SQLException { featureDAO.addNotification(userId, messageId, type, content); }
    public int countUnreadPrivateMessages(User user, long otherUserId) throws SQLException { requireUser(user); return featureDAO.countUnreadPrivateMessages(user.getId(), otherUserId); }
    public boolean isMuted(User user) throws SQLException { requireUser(user); return featureDAO.isMuted(user.getId()); }
    public void mute(User moderator, User target, long durationSeconds, String reason) throws SQLException { requireModerator(moderator); if (target == null) throw new IllegalArgumentException("Target user is required."); if (RemoteApiClient.isConfigured()) { try { RemoteApiClient.moderate("muteUser", target.getId(), reason); return; } catch (Exception exception) { throw new SQLException("Remote mute failed.", exception); } } featureDAO.setMute(target.getId(), moderator.getId(), durationSeconds, reason); }
    public void unmute(User moderator, User target) throws SQLException { requireModerator(moderator); if (target == null) throw new IllegalArgumentException("Target user is required."); if (RemoteApiClient.isConfigured()) { try { RemoteApiClient.moderate("unmuteUser", target.getId(), null); return; } catch (Exception exception) { throw new SQLException("Remote unmute failed.", exception); } } featureDAO.clearMute(target.getId()); }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) throw new IllegalArgumentException("A signed-in user is required.");
    }

    private void requireModerator(User user) {
        requireUser(user);
        if (!user.isModerator()) throw new IllegalStateException("Only moderators can pin messages.");
    }
}