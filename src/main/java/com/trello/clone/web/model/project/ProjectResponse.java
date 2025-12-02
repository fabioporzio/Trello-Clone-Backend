package com.trello.clone.web.model.project;

import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ProjectResponse {

    ObjectId id;
    String name;
    List<String> phases;
    String owner;
    List<String> team;

    public ProjectResponse(ObjectId id, String name, List<String> phases, String owner, List<String> team) {
        this.id = id;
        this.name = name;
        this.phases = phases;
        this.owner = owner;
        this.team = team;
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
}
