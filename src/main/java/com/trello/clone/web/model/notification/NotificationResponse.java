package com.trello.clone.web.model.notification;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Notification's details issued after JWT access token validation")
public class NotificationResponse {

    @Schema(description = "Notification Id")
    private String id;

    @Schema(description = "Notification Receiver")
    private String receiver;

    @Schema(description = "Notification task or project ObjectId")
    private String taskOrProjectId;

    @Schema(description = "Notification category (project or task)")
    private String category;

    @Schema(description = "Notification sender")
    private String sender;

    @Schema(description = "Notification issued time")
    private String issuedAt;

    @Schema(description = "Notification content message")
    private String content;

    public NotificationResponse(String id, String receiver, String taskOrProjectId, String category, String sender, String issuedAt, String content) {
        this.id = id;
        this.receiver = receiver;
        this.taskOrProjectId = taskOrProjectId;
        this.category = category;
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

    public String getTaskOrProjectId() {
        return taskOrProjectId;
    }

    public void setTaskOrProjectId(String taskOrProjectId) {
        this.taskOrProjectId = taskOrProjectId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
