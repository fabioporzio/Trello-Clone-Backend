package com.trello.clone.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Notification {

    @JsonIgnore
    private String id;

    private String receiver;
    private String category;
    private String projectOrTaskId;
    private String sender;
    private String issuedAt;
    private String content;

    public Notification() {
    }

    public Notification(String receiver, String category, String projectOrTaskId, String sender, String issuedAt, String content) {
        this.receiver = receiver;
        this.category = category;
        this.projectOrTaskId = projectOrTaskId;
        this.sender = sender;
        this.issuedAt = issuedAt;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProjectOrTaskId() {
        return projectOrTaskId;
    }

    public void setProjectOrTaskId(String projectOrTaskId) {
        this.projectOrTaskId = projectOrTaskId;
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
}
