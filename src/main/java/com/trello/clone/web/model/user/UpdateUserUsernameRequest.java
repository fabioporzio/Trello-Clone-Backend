package com.trello.clone.web.model.user;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Model for user's username update")
public class UpdateUserUsernameRequest {

    @Schema(description = "User new username", examples = "fabio00")
    @NotBlank(message = "New username is required")
    private String newUsername;

    @Schema(description = "User current password", examples = "S3cureP@ssw0rd")
    @NotBlank(message = "Password is required")
    private String password;

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
