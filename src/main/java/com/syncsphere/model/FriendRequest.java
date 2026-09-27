package com.syncsphere.model;

import java.time.LocalDateTime;

public record FriendRequest(Long id, User sender, User receiver, String status,
                            LocalDateTime createdAt, LocalDateTime respondedAt) {
}