package com.trello.clone.web.model.task;

import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;

import java.time.ZonedDateTime;
import java.util.List;

public class CreateTaskRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String phase;

    private List<String> tags;

    private List<String> assignees;

    private ZonedDateTime endDate;

    private ObjectId projectId;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }

    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public ObjectId getProjectId() {
        return projectId;
    }

    public void setProjectId(ObjectId projectId) {
        this.projectId = projectId;

    }
}
