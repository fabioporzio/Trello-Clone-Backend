package com.trello.clone.data.repository;

import com.trello.clone.data.model.Task;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TaskRepository implements PanacheMongoRepository<Task> {

    public Task findByIdAndProject(ObjectId taskId, ObjectId projectId) {
        return find("{'_id': ?1, 'projectId': ?2}", taskId, projectId).firstResult();
    }

    public long renamePhase(ObjectId projectId, String oldPhase, String newPhase) {
        return update("phase = ?1", newPhase)
                .where("projectId = ?1 and phase = ?2", projectId, oldPhase);
    }

    public long countByPhase(ObjectId projectId, String phase) {
        return count("projectId = ?1 and phase = ?2", projectId, phase);
    }

    public void deleteByProject(ObjectId projectId) {
        delete("projectId = ?1", projectId);
    }

    public List<Task> findDueBy(LocalDate date) {
        return list("{'endDate': {'$lte': ?1}, 'completed': false, 'notifiedAt': null}", date);
    }

    public List<Task> getTasksByProjectIdTagsAndAssignees(
            ObjectId projectId,
            List<String> tags,
            Set<String> assignees
    ) {
        Document filter = new Document("projectId", projectId);

        if (tags != null && !tags.isEmpty()) {
            filter.append("tags", new Document("$in", tags));
        }

        if (assignees != null && !assignees.isEmpty()) {
            filter.append("assignees", new Document("$in", assignees));
        }

        return mongoCollection().find(filter).into(new ArrayList<>());
    }

}
