package com.syncsphere.service;

import com.syncsphere.dao.MessageDAO;
import com.syncsphere.dao.UserDAO;
import com.syncsphere.exception.MessageBlockedException;
import com.syncsphere.exception.MessageTooLongException;
import com.syncsphere.filter.BannedWordFilter;
import com.syncsphere.model.Message;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.model.User;
import com.syncsphere.util.Constants;
import com.syncsphere.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatService {
    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Map<Long, List<Message>> privateThreads = new ConcurrentHashMap<>();
    private final BannedWordFilter bannedWordFilter = new BannedWordFilter(List.of("spam", "scam", "hack", "banned"));
    private final ConcurrentMap<Long, ConcurrentLinkedDeque<Long>> recentMessages = new ConcurrentHashMap<>();
    private final MessageFeatureService featureService = new MessageFeatureService();
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,20})");

    public PublicMessage sendPublicMessage(Long senderId, String content) throws MessageBlockedException, MessageTooLongException {
        return sendPublicMessage(senderId, content, null);
    }

    public PublicMessage sendPublicMessage(Long senderId, String content, Long replyToMessageId) throws MessageBlockedException, MessageTooLongException {
        ensureNotMuted(senderId);
        ValidationUtil.validateMessage(content);
        if (content.length() > Constants.MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException("Message exceeds the maximum allowed length.");
        }
        if (bannedWordFilter.containsBlockedContent(content)) {
            throw new MessageBlockedException("Message contains blocked content and was not sent.");
        }

        String sanitized = bannedWordFilter.sanitize(content);
        Long messageId = saveWithRateLimit(senderId, sanitized, false, null, replyToMessageId);

        User sender = userDAO.findById(senderId);
        notifyMentionedUsers(messageId, sanitized, senderId);
        return new PublicMessage(messageId, senderId, sender != null ? sender.getUsername() : "Unknown", sanitized, java.time.LocalDateTime.now(), false);
    }

    public PrivateMessage sendPrivateMessage(Long senderId, Long receiverId, String content) throws MessageBlockedException, MessageTooLongException {
        return sendPrivateMessage(senderId, receiverId, content, null);
    }

    public PrivateMessage sendPrivateMessage(Long senderId, Long receiverId, String content, Long replyToMessageId) throws MessageBlockedException, MessageTooLongException {
        ensureNotMuted(senderId);
        ValidationUtil.validateMessage(content);
        if (content.length() > Constants.MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException("Message exceeds the maximum allowed length.");
        }
        if (bannedWordFilter.containsBlockedContent(content)) {
            throw new MessageBlockedException("Message contains blocked content and was not sent.");
        }

        String sanitized = bannedWordFilter.sanitize(content);
        Long messageId = saveWithRateLimit(senderId, sanitized, true, receiverId, replyToMessageId);

        User sender = userDAO.findById(senderId);
        User receiver = userDAO.findById(receiverId);
        try { if (!senderId.equals(receiverId)) featureService.addNotification(receiverId, messageId, "PRIVATE_MESSAGE", "New private message from " + (sender == null ? "user" : sender.getUsername())); } catch (Exception ignored) { }
        notifyMentionedUsers(messageId, sanitized, senderId);
        return new PrivateMessage(messageId, senderId, receiverId,
                sender != null ? sender.getUsername() : "Unknown",
                receiver != null ? receiver.getUsername() : "Unknown",
                sanitized, java.time.LocalDateTime.now(), false);
    }

    public List<Message> getPublicMessages() {
        return messageDAO.findPublicMessages();
    }

    public List<Message> getPrivateMessages(Long userA, Long userB) {
        return messageDAO.findPrivateMessagesBetween(userA, userB);
    }

    public List<Message> searchMessages(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        return messageDAO.searchMessages(query.trim()).stream()
                .filter(msg -> msg.getContent() != null && !msg.getContent().isBlank())
                .collect(Collectors.toList());
    }

    public void deleteMessage(Long messageId, User actor) {
        if (actor == null || messageId == null) throw new IllegalStateException("A signed-in user and message are required.");
        Long senderId = findMessageSender(messageId);
        if (senderId == null || (!actor.isModerator() && !actor.getId().equals(senderId))) {
            throw new IllegalStateException("Only the message owner or a moderator can delete messages.");
        }
        messageDAO.deleteMessage(messageId);
    }

    public void editMessage(Long messageId, User actor, String content) throws MessageBlockedException, MessageTooLongException {
        if (actor == null || messageId == null || !actor.getId().equals(findMessageSender(messageId))) {
            throw new IllegalStateException("Only the message owner can edit a message.");
        }
        ValidationUtil.validateMessage(content);
        if (content.length() > Constants.MAX_MESSAGE_LENGTH) throw new MessageTooLongException("Message exceeds the maximum allowed length.");
        if (bannedWordFilter.containsBlockedContent(content)) throw new MessageBlockedException("Message contains blocked content and was not saved.");
        try { messageDAO.updateMessage(messageId, actor.getId(), bannedWordFilter.sanitize(content.trim())); }
        catch (java.sql.SQLException exception) { throw new IllegalStateException("Message could not be edited.", exception); }
    }

    private Long findMessageSender(Long messageId) {
        return messageDAO.searchMessages("").stream().filter(message -> messageId.equals(message.getId())).map(Message::getSenderId).findFirst().orElse(null);
    }

    private Long saveWithRateLimit(Long senderId, String content, boolean privateMessage, Long receiverId, Long replyToMessageId) {
        long now = Instant.now().toEpochMilli();
        ConcurrentLinkedDeque<Long> timestamps = recentMessages.computeIfAbsent(senderId, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 10_000) timestamps.pollFirst();
            if (timestamps.size() >= 8) throw new IllegalStateException("You are sending messages too quickly. Please wait a moment.");
            Long messageId = privateMessage
                    ? messageDAO.savePrivateMessage(senderId, receiverId, content, replyToMessageId)
                    : messageDAO.savePublicMessage(senderId, content, replyToMessageId);
            timestamps.addLast(now);
            return messageId;
        }
    }

    private void ensureNotMuted(Long senderId) {
        User sender = userDAO.findById(senderId);
        try { if (sender != null && featureService.isMuted(sender)) throw new IllegalStateException("You are temporarily muted and cannot send messages."); }
        catch (java.sql.SQLException exception) { throw new IllegalStateException("Message permissions could not be checked.", exception); }
    }

    private void notifyMentionedUsers(Long messageId, String content, Long senderId) {
        if (messageId == null) return;
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            User mentioned = userDAO.findByUsername(matcher.group(1));
            if (mentioned != null && !mentioned.getId().equals(senderId)) {
                try { featureService.addMentionNotificationIfAbsent(mentioned.getId(), messageId, "You were mentioned in a message."); }
                catch (java.sql.SQLException ignored) { }
            }
        }
    }

    public void muteUser(Long targetUserId, User actor) {
        if (actor == null || !actor.isModerator()) {
            throw new IllegalStateException("Only moderators can mute users.");
        }
        userDAO.updateStatus(targetUserId, "MUTED");
    }

    public void unmuteUser(Long targetUserId, User actor) {
        if (actor == null || !actor.isModerator()) {
            throw new IllegalStateException("Only moderators can unmute users.");
        }
        userDAO.updateStatus(targetUserId, "ONLINE");
    }

    public List<User> getOnlineUsers() {
        return userDAO.findOnlineUsers();
    }

    public Map<Long, List<Message>> getPrivateThreads() {
        return privateThreads;
    }
}
