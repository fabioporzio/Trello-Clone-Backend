package com.trello.clone.data.repository;

import com.trello.clone.data.model.Task;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TaskRepository implements PanacheMongoRepository<Task> {

    public List<Task> getTasksByProjectIdTagsAndAssignees(
            ObjectId projectId,
            List<String> tags,
            List<String> assignees
    ) {
        StringBuilder query = new StringBuilder("projectId = ?1");
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (tags != null && !tags.isEmpty()) {
            query.append(" and tags in ?").append(params.size() + 1);
            params.add(tags);
        }

        if (assignees != null && !assignees.isEmpty()) {
            query.append(" and assignees in ?").append(params.size() + 1);
            params.add(assignees);
        }

        return list(query.toString(), params.toArray());
    }

}
