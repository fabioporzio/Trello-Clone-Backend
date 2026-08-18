package com.trello.clone.web.model.project;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Project's minimal details used to reconstruct invitations")
public class ProjectInvitationResponse {

    @Schema(description = "Project ObjectId")
    private String id;

    @Schema(description = "Project Name")
    private String name;

    @Schema(description = "Project Owner")
    private String owner;

    public ProjectInvitationResponse(String id, String name, String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
