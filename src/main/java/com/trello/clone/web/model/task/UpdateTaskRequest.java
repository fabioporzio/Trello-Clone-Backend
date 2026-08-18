package com.trello.clone.web.model.task;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
          "tagsToRename": {"Design": "UX Design"},
          "assigneesToAdd": ["designer@example.com"],
          "assigneesToRemove": ["olduser@example.com"],
        }
        """
)
public class UpdateTaskRequest {

    @Size(max = 100, message = "Task name cannot exceed 100 characters")
    private String title;

    private String description;
    private Boolean completed;
    private String phase;

    private List<@NotBlank @Size(max = 20) String> tagsToAdd;
    private List<@NotBlank @Size(max = 20) String> tagsToRemove;

    /** Key: current name, value: new name. */
    private Map<@NotBlank String, @NotBlank @Size(max = 20) String> tagsToRename;

    private Set<@Email String> assigneesToAdd;
    private Set<@Email String> assigneesToRemove;

    @Future
    private LocalDate endDate;

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

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getTagsToAdd() {
        return tagsToAdd;
    }

    public void setTagsToAdd(List<String> tagsToAdd) {
        this.tagsToAdd = tagsToAdd;
    }

    public List<String> getTagsToRemove() {
        return tagsToRemove;
    }

    public void setTagsToRemove(List<String> tagsToRemove) {
        this.tagsToRemove = tagsToRemove;
    }

    public Map<String, String> getTagsToRename() {
        return tagsToRename;
    }

    public void setTagsToRename(Map<String, String> tagsToRename) {
        this.tagsToRename = tagsToRename;
    }

    public Set<String> getAssigneesToAdd() {
        return assigneesToAdd;
    }

    public void setAssigneesToAdd(Set<String> assigneesToAdd) {
        this.assigneesToAdd = assigneesToAdd;
    }

    public Set<String> getAssigneesToRemove() {
        return assigneesToRemove;
    }

    public void setAssigneesToRemove(Set<String> assigneesToRemove) {
        this.assigneesToRemove = assigneesToRemove;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
