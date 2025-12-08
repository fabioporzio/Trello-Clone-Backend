package com.trello.clone.web.model.notification;

public class NotificationResponse {

    private String sender;
    private String issuedAt;
    private String content;
    private String category;
    private String projectId;

    public NotificationResponse(String sender, String issuedAt, String content, String category, String projectId) {
        this.sender = sender;
        this.issuedAt = issuedAt;
        this.content = content;
        this.category = category;
        this.projectId = projectId;
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
