package com.syncsphere.service;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.MessageReaction;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.User;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageFeatureIntegrationTest {
    @Test
    void reactionsReadsAndPinsShouldPersistAndToggle() throws Exception {
        AuthenticationService authentication = new AuthenticationService();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User sender = authentication.register("fs_" + suffix, "StrongPass1!");
        User reader = authentication.register("fr_" + suffix, "StrongPass2!");
        PublicMessage message = new ChatService().sendPublicMessage(sender.getId(), "feature message " + suffix);
        MessageFeatureService features = new MessageFeatureService();

        features.toggleReaction(message.getId(), reader, "👍");
        List<MessageReaction> reactions = features.getReactions(message.getId());
        assertEquals(1, reactions.size());
        features.toggleReaction(message.getId(), reader, "👍");
        assertTrue(features.getReactions(message.getId()).isEmpty());

        features.markRead(message.getId(), reader);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM message_reads WHERE message_id = ? AND user_id = ?")) {
            statement.setLong(1, message.getId());
            statement.setLong(2, reader.getId());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }

        sender.setRole("MODERATOR");
        features.togglePin(message.getId(), sender);
        assertFalse(features.getPinnedMessageIds().isEmpty());
        features.togglePin(message.getId(), sender);
        assertTrue(features.getPinnedMessageIds().stream().noneMatch(message.getId()::equals));
    }

    @Test
    void replyEditMentionUnreadAndMuteShouldBeEnforced() throws Exception {
        AuthenticationService authentication = new AuthenticationService();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User sender = authentication.register("rs_" + suffix, "StrongPass1!");
        User receiver = authentication.register("rr_" + suffix, "StrongPass2!");
        ChatService chat = new ChatService();
        MessageFeatureService features = new MessageFeatureService();

        PublicMessage original = chat.sendPublicMessage(sender.getId(), "original " + suffix);
        PublicMessage reply = chat.sendPublicMessage(sender.getId(), "reply " + suffix, original.getId());
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT reply_to_message_id FROM messages WHERE id = ?")) {
            statement.setLong(1, reply.getId());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(original.getId(), result.getLong(1));
            }
        }

        assertThrows(IllegalStateException.class, () -> chat.editMessage(original.getId(), receiver, "not allowed"));
        chat.editMessage(original.getId(), sender, "edited " + suffix);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT is_edited FROM messages WHERE id = ?")) {
            statement.setLong(1, original.getId());
            try (ResultSet result = statement.executeQuery()) { assertTrue(result.next()); assertTrue(result.getBoolean(1)); }
        }

        PrivateMessage privateMessage = chat.sendPrivateMessage(sender.getId(), receiver.getId(), "@" + receiver.getUsername() + " hello");
        assertEquals(1, features.countUnreadPrivateMessages(receiver, sender.getId()));
        assertFalse(features.getUnreadNotifications(receiver).isEmpty());
        features.markRead(privateMessage.getId(), receiver);
        assertEquals(0, features.countUnreadPrivateMessages(receiver, sender.getId()));

        sender.setRole("MODERATOR");
        features.mute(sender, receiver, 120, "test mute");
        assertThrows(IllegalStateException.class, () -> chat.sendPublicMessage(receiver.getId(), "muted message"));
        features.unmute(sender, receiver);
        chat.sendPublicMessage(receiver.getId(), "unmuted message");

        ModerationService moderation = new ModerationService();
        assertThrows(com.syncsphere.exception.UnauthorizedActionException.class, () -> moderation.warn(receiver, sender, "not allowed"));
        moderation.warn(sender, receiver, "test warning");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM moderation_logs WHERE moderator_id = ? AND target_user_id = ? AND action = 'WARN'")) {
            statement.setLong(1, sender.getId()); statement.setLong(2, receiver.getId());
            try (ResultSet result = statement.executeQuery()) { assertTrue(result.next()); assertTrue(result.getInt(1) >= 1); }
        }
    }
}
