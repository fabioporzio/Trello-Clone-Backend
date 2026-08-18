package com.trello.clone.web.model.task;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Schema(description = "Task details issued after JWT access token validation")
public class TaskResponse {

    @Schema(description = "Task ObjectId")
    private ObjectId id;

    @Schema(description = "Task Title")
    private String title;

    @Schema(description = "Task Description")
    private String description;

    @Schema(description = "Task is completed or not")
    private boolean completed;

    @Schema(description = "The phase the task belong to")
    private String phase;

    @Schema(description = "Task tags used to classify it")
    private List<String> tags;

    @Schema(description = "List of users assigned to the task")
    private Set<String> assignees;

    @Schema(description = "Task end date")
    private LocalDate endDate;

    @Schema(description = "The project ObjectId the task belongs to")
    private ObjectId projectId;

    public TaskResponse(ObjectId id, String title, String description, boolean completed, String phase, List<String> tags, Set<String> assignees, LocalDate endDate, ObjectId projectId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.phase = phase;
        this.tags = tags;
        this.assignees = assignees;
        this.endDate = endDate;
        this.projectId = projectId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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

    public Set<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(Set<String> assignees) {
        this.assignees = assignees;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ObjectId getProjectId() {
        return projectId;
    }

    public void setProjectId(ObjectId projectId) {
        this.projectId = projectId;
    }
}
