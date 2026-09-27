package com.syncsphere.model;

import java.time.LocalDateTime;

public class PrivateMessage extends Message {
    private Long receiverId;
    private String receiverUsername;

    public PrivateMessage() {
        super();
    }

    public PrivateMessage(Long id, Long senderId, Long receiverId, String senderUsername, String receiverUsername,
                         String content, LocalDateTime createdAt, boolean deleted) {
        super(id, senderId, senderUsername, content, createdAt, deleted);
        this.receiverId = receiverId;
        this.receiverUsername = receiverUsername;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    @Override
    public String getType() {
        return "PRIVATE";
    }
}
