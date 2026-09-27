package com.syncsphere.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypingIndicatorService {
    private static final long EXPIRY_MILLIS = 2500;
    private final Map<String, Long> typingUsers = new ConcurrentHashMap<>();

    public void setTyping(long userId, long conversationUserId) {
        typingUsers.put(key(userId, conversationUserId), Instant.now().toEpochMilli());
    }

    public boolean isTyping(long userId, long conversationUserId) {
        Long lastActivity = typingUsers.get(key(userId, conversationUserId));
        if (lastActivity == null) return false;
        if (Instant.now().toEpochMilli() - lastActivity > EXPIRY_MILLIS) {
            typingUsers.remove(key(userId, conversationUserId), lastActivity);
            return false;
        }
        return true;
    }

    public void clearConversation(long conversationUserId) {
        typingUsers.keySet().removeIf(key -> key.endsWith(":" + conversationUserId));
    }

    public void clearUser(long userId) {
        typingUsers.keySet().removeIf(key -> key.startsWith(userId + ":"));
    }

    private String key(long userId, long conversationUserId) { return userId + ":" + conversationUserId; }
}