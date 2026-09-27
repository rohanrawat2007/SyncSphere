package com.syncsphere.model;

import java.time.LocalDateTime;

public record MessageReaction(Long messageId, Long userId, String reaction, LocalDateTime createdAt) {
}