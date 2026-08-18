package com.trello.clone.data.model;

import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.utils.Labels;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@MongoEntity(collection = "tasks")
public class Task {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;
    private static final int MAX_TAG_LENGTH = 60;
    private static final int MAX_TAGS = 20;

    private ObjectId id;
    private String title;
    private String description;
    private boolean completed;
    private String phase;
    private List<String> tags = new ArrayList<>();
    private Set<String> assignees = new LinkedHashSet<>();
    private LocalDate endDate;
    private ObjectId projectId;

    public Task() {
    }

    public static Task create(String name, String description, String phase, ObjectId projectId) {
        Task task = new Task();
        task.rename(name);
        task.describe(description);
        task.setCompleted(false);
        task.moveTo(phase);
        task.setProjectId(projectId);
        return task;
    }

    // BEHAVIOURS
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Task name cannot be empty");
        }

        String trimmed = newName.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("Task title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
        this.title = trimmed;
    }

    public void describe(String description) {
        if (description == null || description.isBlank()) {
            this.description = null;
            return;
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Task description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        this.description = trimmed;
    }

    public void moveTo(String newPhase) {
        if (newPhase == null || newPhase.isBlank()) {
            throw new BadRequestException("Task phase cannot be empty");
        }

        this.phase = newPhase.trim();
    }

    public void scheduleFor(LocalDate newDeadline) {
        if (newDeadline != null && newDeadline.isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new BadRequestException("Deadline can't be in the past");
        }
        this.endDate = newDeadline;
    }

    public void addTags(Collection<String> tagsToAdd) {
        List<String> candidates = new ArrayList<>(tags);
        for (String tag : tagsToAdd) {
            Labels.addDistinct(candidates, requireTag(tag));
        }

        if (candidates.size() > MAX_TAGS) {
            throw new BadRequestException("A task cannot have more than " + MAX_TAGS + " tags");
        }

        this.tags = candidates;
    }

    public void removeTags(Collection<String> tagsToRemove) {
        for (String tag : tagsToRemove) {
            Labels.remove(tags, requireTag(tag));
        }
    }

    public void renameTag(String oldName, String newName) {
        int index = Labels.indexOf(tags, requireTag(oldName));
        if (index < 0) {
            throw new BadRequestException("Unknown tag: " + oldName);
        }

        String renamed = requireTag(newName);
        int clash = Labels.indexOf(tags, renamed);
        if (clash >= 0 && clash != index) {
            throw new BadRequestException("A tag named " + renamed + " already exists");
        }

        tags.set(index, renamed);
    }

    private static String requireTag(String tag) {
        return Labels.require(tag, MAX_TAG_LENGTH, "Tag names").toLowerCase(Locale.ROOT);
    }

    public void assign(Collection<String> emails) {
        assignees.addAll(emails);
    }

    public void unassign(Collection<String> emails) {
        assignees.removeAll(emails);
    }

    public ObjectId getId() {
        return id;
    }

    public  void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public Set<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(Set<String> assignees) {
        this.assignees = assignees == null ? new LinkedHashSet<>() : new LinkedHashSet<>(assignees);
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ObjectId getProjectId() {
        return projectId;
    }

    public void setProjectId(ObjectId projectId) {
        this.projectId = projectId;
    }
}
