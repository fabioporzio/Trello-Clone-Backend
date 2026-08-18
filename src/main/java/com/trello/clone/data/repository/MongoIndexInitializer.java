package com.trello.clone.data.repository;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class MongoIndexInitializer {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public MongoIndexInitializer(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    void onStart(@Observes StartupEvent event) {
        userRepository.mongoCollection().createIndex(
                Indexes.ascending("email"),
                new IndexOptions().unique(true)
        );

        projectRepository.mongoCollection().createIndex(Indexes.ascending("team"));
        projectRepository.mongoCollection().createIndex(Indexes.ascending("invitedUsers"));

        taskRepository.mongoCollection().createIndex(
                Indexes.ascending("projectId", "phase")
        );

        Log.info("MongoDB indexes ensured");
    }
}
