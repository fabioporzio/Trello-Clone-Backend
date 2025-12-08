package com.trello.clone.web.model.project;

import org.bson.types.ObjectId;

import java.util.List;

public class ProjectResponse {

    private ObjectId id;
    private String name;
    private List<String> phases;
    private String owner;
    private List<String> team;
    private List<String> invitedUsers;

    public ProjectResponse(ObjectId id, String name, List<String> phases, String owner, List<String> team,  List<String> invitedUsers) {
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

    public List<String> getTeam() {
        return team;
    }

    public void setTeam(List<String> team) {
        this.team = team;
    }

    public List<String> getInvitedUsers() {
        return invitedUsers;
    }

    public void setInvitedUsers(List<String> invitedUsers) {
        this.invitedUsers = invitedUsers;
    }
}
