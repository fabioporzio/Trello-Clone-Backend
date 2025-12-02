package com.trello.clone.web.model.task;

import org.bson.types.ObjectId;

import java.time.ZonedDateTime;
import java.util.List;

public class TaskResponse {

    private ObjectId id;

    private String title;

    private String description;

    private String phase;

    private List<String> tags;

    private List<String> assignees;

    private ZonedDateTime endDate;

    private ObjectId projectId;

    public TaskResponse(ObjectId id, String title, String description, String phase, List<String> tags, List<String> assignees, ZonedDateTime endDate,  ObjectId projectId) {
        this.id = id;
        this.title = title;
        this.description = description;
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
