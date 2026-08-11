package com.trello.clone.web.model.project;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.Set;

public class ProjectResponse {

    private ObjectId id;
    private String name;
    private List<String> phases;
    private String owner;
    private Set<String> team;
    private Set<String> invitedUsers;

    public ProjectResponse() {
    }

    public ProjectResponse(ObjectId id, String name, List<String> phases, String owner, Set<String> team, Set<String> invitedUsers) {
        this.id = id;
        this.name = name;
        this.phases = phases;
        this.owner = owner;
        this.team = team;
        this.invitedUsers = invitedUsers;
    }

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
}
