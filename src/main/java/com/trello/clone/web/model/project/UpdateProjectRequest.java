package com.trello.clone.web.model.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Schema(
        description = "Model for project update. All fields are optional; only the provided deltas are applied.",
        examples = """
        {
          "name": "Trello Clone Backend",
          "phasesToAdd": ["Deployment", "Monitoring"],
          "phasesToRemove": ["Login Throttling"],
          "phaseRenames": {"JWT Implementation": "Authentication"},
          "phaseOrder": ["Authentication", "User Search", "Deployment", "Monitoring"],
          "newOwner": "newlead@example.com",
          "usersToInvite": ["designer@example.com"],
          "invitesToRevoke": ["olduser@example.com"],
          "membersToRemove": ["formermember@example.com"]
        }
        """
)
public class UpdateProjectRequest {

    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String name;

    private Set<@NotBlank @Size(max = 60) String> phasesToAdd;
    private Set<@NotBlank @Size(max = 60) String> phasesToRemove;

    /** Key: current name, value: new name. */
    private Map<@NotBlank String, @NotBlank @Size(max = 60) String> phaseRenames;

    /** Must list every phase exactly once. Applied last. */
    @Size(max = 50, message = "A project cannot have more than 50 phases")
    private List<@NotBlank String> phaseOrder;

    /** New owner must already be a team member. */
    @Email
    private String newOwner;

    private Set<@Email String> usersToInvite;
    private Set<@Email String> invitesToRevoke;

    @Schema(
            description = "Already accepted members that must be removed fro the project",
            examples = {"user1@email.com", "user2@email.com"}
    )
    private Set<@Email String> membersToRemove;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getPhasesToAdd() {
        return phasesToAdd;
    }

    public void setPhasesToAdd(Set<String> phasesToAdd) {
        this.phasesToAdd = phasesToAdd;
    }

    public Set<String> getPhasesToRemove() {
        return phasesToRemove;
    }

    public void setPhasesToRemove(Set<String> phasesToRemove) {
        this.phasesToRemove = phasesToRemove;
    }

    public Map<String, String> getPhaseRenames() {
        return phaseRenames;
    }

    public void setPhaseRenames(Map<String, String> phaseRenames) {
        this.phaseRenames = phaseRenames;
    }

    public List<String> getPhaseOrder() {
        return phaseOrder;
    }

    public void setPhaseOrder(List<String> phaseOrder) {
        this.phaseOrder = phaseOrder;
    }

    public String getNewOwner() {
        return newOwner;
    }

    public void setNewOwner(String newOwner) {
        this.newOwner = newOwner;
    }

    public Set<String> getUsersToInvite() {
        return usersToInvite;
    }

    public void setUsersToInvite(Set<String> usersToInvite) {
        this.usersToInvite = usersToInvite;
    }

    public Set<String> getInvitesToRevoke() {
        return invitesToRevoke;
    }

    public void setInvitesToRevoke(Set<String> invitesToRevoke) {
        this.invitesToRevoke = invitesToRevoke;
    }

    public Set<String> getMembersToRemove() {
        return membersToRemove;
    }

    public void setMembersToRemove(Set<String> membersToRemove) {
        this.membersToRemove = membersToRemove;
    }
}
