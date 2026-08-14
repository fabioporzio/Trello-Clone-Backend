package com.trello.clone.web.model.user;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "User's email and username issued after JWT access token validation")
public class UserResponse {

    @Schema(description = "User objectId")
    private String objectId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User password")
    private String username;

    public UserResponse(String objectId, String email, String username) {
        this.objectId = objectId;
        this.email = email;
        this.username = username;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
