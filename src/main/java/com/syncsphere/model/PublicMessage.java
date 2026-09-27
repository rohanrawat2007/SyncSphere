package com.syncsphere.model;

import java.time.LocalDateTime;

public class PublicMessage extends Message {
    public PublicMessage() {
        super();
    }

    public PublicMessage(Long id, Long senderId, String senderUsername, String content, LocalDateTime createdAt, boolean deleted) {
        super(id, senderId, senderUsername, content, createdAt, deleted);
    }

    @Override
    public String getType() {
        return "PUBLIC";
    }
}
