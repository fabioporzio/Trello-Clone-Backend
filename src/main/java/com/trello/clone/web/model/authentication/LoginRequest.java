package com.trello.clone.web.model.authentication;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Credentials for user authentication")
public class LoginRequest {

    @Schema(description = "User email address", examples = "fabio@example.com")
    @NotBlank
    private String email;

    @Schema(description = "User password", examples = "S3cureP@ssw0rd")
    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
