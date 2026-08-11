package com.trello.clone.data.model;

import com.trello.clone.service.exception.BadRequestException;
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
    private List<String> phases;
    private String owner;
    private Set<String> team;
    private Set<String> invitedUsers;
    private Instant createdAt;
    private Instant updatedAt;

    public Project() {
    }

    public Project(String name, List<String> phases, String owner, Set<String> team, Set<String> invitedUsers, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.phases = phases;
        this.owner = requireEmail(owner);
        this.team = team;
        this.invitedUsers = invitedUsers;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        for (String raw : phasesToAdd) {
            String phase = requireAndTrimPhase(raw);
            if (indexOfPhase(phase) < 0) {
                phases.add(phase);
            }
        }

        // Checks if the new number of phases limit is met (max 50 phases)
        if (phases.size() > MAX_PHASES) {
            throw new BadRequestException("A project cannot have more than " + MAX_PHASES + " phases");
        }
    }

    /**
     * Removes phases that are not already present.
     */
    public void removePhases(Collection<String> phasesToRemove) {
        for (String phase : phasesToRemove) {
            int index = indexOfPhase(requireAndTrimPhase(phase));
            if (index >= 0) {
                phases.remove(index);
            }
        }
    }

    /**
     * Renames a phase while preserving its position.
     * This prevents renaming by removing and adding to move the phase to the last position.
     */
    public void renamePhase(String oldName, String newName) {

        // Checks if the old phase is actually present
        int index = indexOfPhase(requireAndTrimPhase(oldName));
        if (index < 0) {
            throw new BadRequestException("Unknown phase: " + oldName);
        }

        // Checks if there is another phase with the same name
        String renamed = requireAndTrimPhase(newName);
        int clash = indexOfPhase(renamed);
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
            String phase = requireAndTrimPhase(raw);
            int index = indexOfPhase(phase);
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

    /** Phase names are compared case-insensitively but stored as typed. */
    private int indexOfPhase(String phase) {
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).equalsIgnoreCase(phase)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if phase is empty.
     * Trims the phase name.
     */
    private static String requireAndTrimPhase(String phase) {
        if (phase == null || phase.isBlank()) {
            throw new BadRequestException("Phase names cannot be empty");
        }

        String trimmed = phase.trim();
        if (trimmed.length() > MAX_PHASE_LENGTH) {
            throw new BadRequestException("Phase names cannot exceed " + MAX_PHASE_LENGTH + " characters");
        }

        return trimmed;
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
        this.phases = phases;
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
        this.team = team;
    }

    public Set<String> getInvitedUsers() {
        return invitedUsers;
    }

    public void setInvitedUsers(Set<String> invitedUsers) {
        this.invitedUsers = invitedUsers;
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
