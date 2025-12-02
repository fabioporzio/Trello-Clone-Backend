package com.trello.clone.data.repository;

import com.trello.clone.data.model.Project;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProjectRepository implements PanacheMongoRepository<Project> {

    public List<Project> findByEmailOrTeam(String email) {
        return find("{'$or':[{'email':?1}, {'team':{'$in':[?1]}}]}", email)
                .list();
    }

}
