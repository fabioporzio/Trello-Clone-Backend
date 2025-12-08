package com.trello.clone.web.model.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.bson.types.ObjectId;

import java.util.List;

public class CreateTaskRequest {

    @NotEmpty(message = "Task title is required")
    @NotBlank(message = "Task title is required")
    private String title;

    @NotEmpty(message = "Task description is required")
    @NotBlank(message = "Task description is required")
    private String description;

    @NotEmpty(message = "Task phase is required")
    @NotBlank(message = "Task phase is required")
    private String phase;

    @NotEmpty(message = "Task's project is required")
    @NotBlank(message = "Task's project is required")
    private String projectId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
