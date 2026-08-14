package com.trello.clone.data.repository;

import com.mongodb.client.model.Indexes;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class MongoIndexInitializer {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public MongoIndexInitializer(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    void onStart(@Observes StartupEvent event) {
        projectRepository.mongoCollection().createIndex(Indexes.ascending("team"));
        projectRepository.mongoCollection().createIndex(Indexes.ascending("invitedUsers"));
        taskRepository.mongoCollection().createIndex(Indexes.ascending("projectId"));
        taskRepository.mongoCollection().createIndex(Indexes.ascending("phase"));
        Log.info("MongoDB indexes ensured");
    }
}
