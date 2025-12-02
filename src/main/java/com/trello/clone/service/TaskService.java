package com.trello.clone.service;

import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(CreateTaskRequest createTaskRequest) {
        Task task = new Task(
                createTaskRequest.getTitle(),
                createTaskRequest.getDescription(),
                createTaskRequest.getPhase(),
                createTaskRequest.getTags(),
                createTaskRequest.getAssignees(),
                createTaskRequest.getEndDate(),
                createTaskRequest.getProjectId()
        );

        taskRepository.persist(task);

        return toTaskResponse(task);
    }

    TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPhase(),
                task.getTags(),
                task.getAssignees(),
                task.getEndDate(),
                task.getProjectId()
        );
    }
}
