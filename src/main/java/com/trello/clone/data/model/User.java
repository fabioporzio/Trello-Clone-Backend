package com.trello.clone.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@MongoEntity(collection = "users")
public class User {

    public ObjectId id;
    private String email;
    private String username;

    @Schema(hidden = true)
    @JsonIgnore
    private String password;

    public User() {
    }

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
