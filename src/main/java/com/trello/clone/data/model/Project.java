package com.trello.clone.data.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;

@MongoEntity(collection = "projects")
public class Project {

    ObjectId id;
    String name;
    List<String> phases;
    String owner;
    List<String> team;

    public Project() {
    }

    public Project(String name, List<String> phases, String owner, List<String> team) {
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
