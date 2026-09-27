package com.syncsphere.model;

import java.time.LocalDateTime;

public record Notification(Long id, Long userId, Long messageId, String type, String content,
                           boolean read, LocalDateTime createdAt) {
}