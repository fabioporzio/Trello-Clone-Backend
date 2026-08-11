package com.trello.clone.data.repository;

import com.trello.clone.data.model.Project;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProjectRepository implements PanacheMongoRepository<Project> {

    public List<Project> findProjectsByEmail(String email) {
        return list("team", email);
    }

    /** Projects the user has been invited to but has not joined yet. */
    public List<Project> findByPendingInvite(String email) {
        return list("invitedUsers", email);
    }
}
