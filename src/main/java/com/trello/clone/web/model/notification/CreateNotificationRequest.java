package com.trello.clone.web.model.notification;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class CreateNotificationRequest {

    @NotEmpty(message = "Sender is required")
    @NotNull(message = "Sender is required")
    private String sender;

    @NotEmpty(message = "Receiver is required")
    @NotNull(message = "Receiver is required")
    private String receiver;

    @NotEmpty(message = "Issuing time is required")
    @NotNull(message = "Issuing time is required")
    private String issuedAt;

    @NotEmpty(message = "Project or Task name is required")
    @NotNull(message = "Project or Task name is required")
    private String projectOrTaskName;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getProjectOrTaskName() {
        return projectOrTaskName;
    }

    public void setProjectOrTaskName(String projectOrTaskName) {
        this.projectOrTaskName = projectOrTaskName;
    }
}
