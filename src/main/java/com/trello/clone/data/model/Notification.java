package com.trello.clone.data.model;

public class Notification {

    private String sender;
    private String issuedAt;
    private String content;
    private String category;
    private String taskOrProjectId;

    public Notification() {
    }

    public Notification(String sender, String issuedAt, String content, String category, String taskOrProjectId) {
        this.sender = sender;
        this.issuedAt = issuedAt;
        this.content = content;
        this.category = category;
        this.taskOrProjectId = taskOrProjectId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTaskOrProjectId() {
        return taskOrProjectId;
    }

    public void setTaskOrProjectId(String taskOrProjectId) {
        this.taskOrProjectId = taskOrProjectId;
    }
}
