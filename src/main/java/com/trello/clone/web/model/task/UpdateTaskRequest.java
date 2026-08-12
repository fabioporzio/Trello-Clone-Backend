package com.trello.clone.web.model.task;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(
        description = "Model for task update. All fields are optional; only the provided deltas are applied.",
        examples = """
        {
          "title": "Wireframe Creation",
          "description": "Lorem ipsum...",
          "completed": false,
          "phase": "UX Design",
          "tagsToAdd": ["Design", "Important"],
          "tagsToRemove": ["Design", "Important"],
          "assigneesToAdd": ["designer@example.com"],
          "assigneesToRemove": ["olduser@example.com"],
        }
        """
)
public class UpdateTaskRequest {

    private String title;
    private String description;
    private Boolean completed;
    private String phase;
    private List<String> tags;
    private List<String> assignees;
    private String endDate;

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

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
