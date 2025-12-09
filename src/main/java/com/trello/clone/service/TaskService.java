package com.trello.clone.service;

import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.DeadlineRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.utils.MergeArraysUtils;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;
    private final MergeArraysUtils mergeArraysUtils;
    private final DeadlineRepository deadlineRepository;

    public TaskService(TaskRepository taskRepository,  MergeArraysUtils utils,  DeadlineRepository deadlineRepository) {
        this.taskRepository = taskRepository;
        this.mergeArraysUtils = utils;
        this.deadlineRepository = deadlineRepository;
    }

    public TaskResponse getTaskById(ObjectId taskId) {
        Task task;
        try {
            task = taskRepository.findById(taskId);
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve task due to server error");
        }

        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }
        return toTaskResponse(task);
    }

    public Map<String, List<TaskResponse>> getAllTasksByProject(
            ObjectId projectId,
            List<String> tags,
            List<String> assignees
    ) {
        List<Task> tasks;

        try {
            tasks = taskRepository.getTasksByProjectIdTagsAndAssignees(projectId, tags, assignees);
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve tasks due to server error");
        }

        Map<String, List<TaskResponse>> groupedTasks = new HashMap<>();
        for (Task task : tasks) {
            TaskResponse taskResponse = toTaskResponse(task);
            String phase = task.getPhase();
            groupedTasks.putIfAbsent(phase, new ArrayList<>());
            groupedTasks.get(phase).add(taskResponse);
        }

        return groupedTasks;
    }


    public TaskResponse createTask(CreateTaskRequest request) {
        ObjectId projectId = new ObjectId(request.getProjectId());
        List<String> assignees = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                false,
                request.getPhase(),
                tags,
                assignees,
                null,
                projectId
        );

        try {
            taskRepository.persist(task);
        }
        catch (Exception e) {
            throw new GenericException("Failed to create task due to server error: " + e.getMessage());
        }

        return toTaskResponse(task);
    }

    public TaskResponse updateTask(UpdateTaskRequest request, ObjectId taskId, String email) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task not found: " + taskId);
        }

        if (request.getTitle() != null) {
            if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
                throw new UnauthorizedException("You are not allowed to update this task");
            }

            task.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
                throw new UnauthorizedException("You are not allowed to update this task");
            }

            task.setDescription(request.getDescription().trim());
        }

        if (request.getPhase() != null) {
            if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
                throw new UnauthorizedException("You are not allowed to update this task");
            }

            task.setPhase(request.getPhase().trim());
        }

        if (request.getCompleted() != null) {
            if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
                throw new UnauthorizedException("You are not allowed to update this task");
            }

            task.setCompleted(request.getCompleted());
        }

        if (request.getTags() != null) {
            if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
                throw new UnauthorizedException("You are not allowed to update this task");
            }

            if (request.getTags().size() > task.getTags().size()) {
                task.setTags(mergeArraysUtils.mergeDistinct(task.getTags(), request.getTags()));
            }
            else {
                task.setTags(request.getTags());
            }
        }

        if (request.getAssignees() != null) {
            if (request.getAssignees().size() > task.getAssignees().size()) {
                task.setAssignees(mergeArraysUtils.mergeDistinct(task.getAssignees(), request.getAssignees()));
            }
            else {
                task.setAssignees(request.getAssignees());
            }
        }

        if (request.getEndDate() != null) {
            task.setEndDate(request.getEndDate());
            boolean success = deadlineRepository.scheduleNotification(task.getId(), request.getEndDate());
            if (!success) {
                throw new GenericException("Deadline not scheduled due to server error");
            }
        }

        try {
            taskRepository.update(task);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update task due to server error");
        }

        return toTaskResponse(task);
    }

    public TaskResponse deleteTask(ObjectId taskId, String email) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }

        if (!task.getAssignees().isEmpty() && !task.getAssignees().contains(email)) {
            throw new UnauthorizedException("You are not allowed to delete this task");
        }

        try {
            taskRepository.delete(task);
        }
        catch (Exception e) {
            throw new GenericException("Failed to delete task due to server error");
        }

        return toTaskResponse(task);
    }

    private TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getPhase(),
                task.getTags(),
                task.getAssignees(),
                task.getEndDate(),
                task.getProjectId()
        );
    }
}
