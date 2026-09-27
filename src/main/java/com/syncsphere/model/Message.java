package com.syncsphere.model;

import java.time.LocalDateTime;

public abstract class Message {
    protected Long id;
    protected Long senderId;
    protected String senderUsername;
    protected String content;
    protected LocalDateTime createdAt;
    protected boolean deleted;
    protected Long replyToMessageId;
    protected boolean edited;

    public Message() {
        this.createdAt = LocalDateTime.now();
    }

    public Message(Long id, Long senderId, String senderUsername, String content, LocalDateTime createdAt, boolean deleted) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.createdAt = createdAt;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public abstract String getType();

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}
