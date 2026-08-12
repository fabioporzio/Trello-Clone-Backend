package com.trello.clone.web.model.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Minimal info for new task creation")
public class CreateTaskRequest {

    @Schema(description = "Task Name", examples = "Wireframe Creation")
    @NotEmpty(message = "Task title is required")
    @NotBlank(message = "Task title is required")
    private String title;

    @Schema(description = "Task Description", examples = "Lorem ipsum...")
    @NotEmpty(message = "Task description is required")
    @NotBlank(message = "Task description is required")
    private String description;

    @Schema(description = "The phase the tasks belongs to", examples = "UX Design")
    @NotEmpty(message = "Task phase is required")
    @NotBlank(message = "Task phase is required")
    private String phase;

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
}
