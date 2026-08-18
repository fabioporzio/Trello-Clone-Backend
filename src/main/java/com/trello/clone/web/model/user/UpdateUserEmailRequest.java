package com.trello.clone.web.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Model for user's email update")
public class UpdateUserEmailRequest {

    @Schema(description = "User new email address", examples = "fabio00@example.com")
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email format")
    private String newEmail;

    @Schema(description = "User password", examples = "S3cureP@ssw0rd")
    @NotBlank(message = "Password is required")
    private String password;

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
