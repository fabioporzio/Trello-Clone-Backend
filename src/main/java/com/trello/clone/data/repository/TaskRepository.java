package com.trello.clone.data.repository;

import com.trello.clone.data.model.Task;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class TaskRepository implements PanacheMongoRepository<Task> {

    public List<Task> getTasksByProjectId(ObjectId projectId) {
        return find("{ projectId: ?1 }", projectId).list();
    }

}
