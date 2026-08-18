package com.trello.clone.data.model;

import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.utils.Labels;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.*;

@MongoEntity(collection = "projects")
public class Project {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PHASE_LENGTH = 60;
    private static final int MAX_PHASES = 50;

    private ObjectId id;
    private String name;
    private List<String> phases = new ArrayList<>();
    private String owner;
    private Set<String> team = new LinkedHashSet<>();
    private Set<String> invitedUsers = new LinkedHashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Project() {
    }

    public static Project create(String name, String ownerEmail) {
        Project project = new Project();
        project.rename(name);
        project.owner = requireEmail(ownerEmail);
        project.team.add(project.owner);
        project.createdAt = Instant.now();
        project.updatedAt = project.createdAt;
        return project;
    }

    // QUERIES
    public boolean isOwner(String email) {
        return owner != null && owner.equals(email);
    }

    public boolean isMember(String email) {
        return team.contains(email);
    }

    public boolean isInvited(String email) {
        return invitedUsers.contains(email);
    }

    public boolean hasPhase(String phase) {
        return canonicalPhase(phase) != null;
    }

    // BEHAVIOURS
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Project name cannot be empty");
        }

        // Checks if the project's name characters limit is met (max 100 characters)
        String trimmed = newName.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("Project name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        this.name = trimmed;
    }

    /**
     * Appends phases that are not already present.
     */
    public void addPhases(Collection<String> phasesToAdd) {
        List<String> candidates = new ArrayList<>(phases);
        for (String phase : phasesToAdd) {
            Labels.addDistinct(candidates, requirePhase(phase));
        }

        if (candidates.size() > MAX_PHASES) {
            throw new BadRequestException("A project cannot have more than " + MAX_PHASES + " phases");
        }

        this.phases = candidates;
    }

    /**
     * Removes phases that are not already present.
     */
    public void removePhases(Collection<String> phasesToRemove) {
        for (String phase : phasesToRemove) {
            Labels.remove(phases, requirePhase(phase));
        }
    }

    /**
     * Renames a phase while preserving its position.
     * This prevents renaming by removing and adding to move the phase to the last position.
     */
    public void renamePhase(String oldName, String newName) {
        int index = Labels.indexOf(phases, requirePhase(oldName));
        if (index < 0) {
            throw new BadRequestException("Unknown phase: " + oldName);
        }

        String renamed = requirePhase(newName);
        int clash = Labels.indexOf(phases, renamed);
        if (clash >= 0 && clash != index) {
            throw new BadRequestException("A phase named " + renamed + " already exists");
        }

        phases.set(index, renamed);
    }

    /**
     * Reorders the existing phases.
     * Each phase must be present only once.
     */
    public void reorderPhases(List<String> newOrder) {
        List<String> reordered = new ArrayList<>(newOrder.size());
        for (String raw : newOrder) {

            // Check if the phase exists
            String phase = requirePhase(raw);
            int index = Labels.indexOf(phases, phase);
            if (index < 0) {
                throw new BadRequestException("Unknown phase: " + phase);
            }

            // Checks if the same phase was previously ordered
            String existing = phases.get(index);
            if (reordered.contains(existing)) {
                throw new BadRequestException("Phase listed twice in the ordering: " + phase);
            }

            reordered.add(existing);
        }

        // New collection must contain the same number of elements
        if (reordered.size() != phases.size()) {
            throw new BadRequestException("The ordering must list every phase exactly once");
        }

        this.phases = reordered;
    }

    private static String requirePhase(String phase) {
        return Labels.require(phase, MAX_PHASE_LENGTH, "Phase names");
    }

    public String requireExistingPhase(String phase) {
        String canonical = canonicalPhase(requirePhase(phase));
        if (canonical == null) {
            throw new BadRequestException("Unknown phase: " + phase);
        }
        return canonical;
    }

    public void transferOwnershipTo(String newOwner) {
        String target = requireEmail(newOwner);
        if (target.equals(owner)) {
            return;
        }

        // Checks if the new owner is already a team member
        if (!team.contains(target)) {
            throw new BadRequestException("The new owner must already be a team member");
        }

        this.owner = target;
    }

    public void invite(String email) {
        String target = requireEmail(email);
        if (team.contains(target)) {
            throw new BadRequestException(target + " is already a team member");
        }

        invitedUsers.add(target);
    }

    public void revokeInvite(String email) {
        String target = requireEmail(email);
        if (!invitedUsers.remove(target)) {
            throw new BadRequestException("No pending invitation for " + target);
        }
    }

    /** Moves an invited user into the team. */
    public void acceptInvite(String email) {
        String target = requireEmail(email);
        if (!invitedUsers.remove(target)) {
            throw new BadRequestException("No pending invitation for " + target);
        }

        team.add(target);
    }

    public void removeMember(String email) {
        String target = requireEmail(email);
        if (isOwner(target)) {
            throw new BadRequestException("Transfer ownership before removing the owner from the team");
        }

        if (!team.remove(target)) {
            throw new BadRequestException(target + " is not a team member");
        }
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email cannot be empty");
        }

        return email;
    }

    public String canonicalPhase(String phase) {
        int index = Labels.indexOf(phases, phase);
        return index < 0 ? null : phases.get(index);
    }

    // GETTERS AND SETTERS

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPhases() {
        return phases;
    }

    public void setPhases(List<String> phases) {
        this.phases = phases == null ? new ArrayList<>() : new ArrayList<>(phases);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Set<String> getTeam() {
        return team;
    }

    public void setTeam(Set<String> team) {
        this.team = team == null ? new LinkedHashSet<>() : new LinkedHashSet<>(team);
    }

    public Set<String> getInvitedUsers() {
        return invitedUsers;
    }

    public void setInvitedUsers(Set<String> invitedUsers) {
        this.invitedUsers = invitedUsers == null ? new LinkedHashSet<>() : new LinkedHashSet<>(invitedUsers);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
