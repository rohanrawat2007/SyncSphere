package com.syncsphere.model;

import java.time.LocalDateTime;

public record Friend(User user, LocalDateTime createdAt) {
}