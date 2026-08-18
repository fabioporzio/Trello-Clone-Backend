package com.trello.clone.web.model.user;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Minimal public user info for member lookup")
public class UserSummaryResponse {

    @Schema(description = "User identifier")
    private String id;

    @Schema(description = "Public username")
    private String username;

    public UserSummaryResponse() {
    }

    public UserSummaryResponse(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

