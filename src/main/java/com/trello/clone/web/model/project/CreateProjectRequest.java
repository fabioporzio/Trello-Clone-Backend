package com.trello.clone.web.model.project;

import jakarta.validation.constraints.NotBlank;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
