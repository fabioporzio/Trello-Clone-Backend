package com.trello.clone.data.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;

@MongoEntity(collection = "tasks")
public class Task {

    private ObjectId id;
    private String title;
    private String description;
    private boolean completed;
    private String phase;
    private List<String> tags;
    private List<String> assignees;
    private String endDate;
    private ObjectId idProject;

    public Task() {
    }

    public Task(String title, String description, boolean completed, String phase, List<String> tags, List<String> assignees, String endDate, ObjectId idProject) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.phase = phase;
        this.tags = tags;
        this.assignees = assignees;
        this.endDate = endDate;
        this.idProject = idProject;
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

    public List<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public ObjectId getIdProject() {
        return idProject;
    }

    public void setIdProject(ObjectId idProject) {
        this.idProject = idProject;
    }
}
