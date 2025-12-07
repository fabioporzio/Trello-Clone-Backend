package com.trello.clone.service;

import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.DeadlineRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.utils.MergeArraysUtils;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

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
        Task task = taskRepository.findById(taskId);

        if (task != null) {
            return toTaskResponse(task);
        }
        else {
            return null;
        }
    }

    public List<TaskResponse> getAllTasksByProject(ObjectId projectId) {
        List<Task> tasks = taskRepository.getTasksByProjectId(projectId);

        if (!tasks.isEmpty()) {
            List<TaskResponse> taskResponseList = new ArrayList<>();

            for (Task task : tasks) {
                taskResponseList.add(toTaskResponse(task));
            }

            return taskResponseList;
        }
        else {
            return null;
        }
    }

    public TaskResponse createTask(CreateTaskRequest createTaskRequest) {
        Task task = new Task(
                createTaskRequest.getTitle(),
                createTaskRequest.getDescription(),
                false,
                createTaskRequest.getPhase(),
                null,
                null,
                null,
                createTaskRequest.getProjectId()
        );

        taskRepository.persist(task);

        return toTaskResponse(task);
    }

    public TaskResponse updateTask(UpdateTaskRequest updateTaskRequest, ObjectId taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task not found: " + taskId);
        }

        if (updateTaskRequest.getTitle() != null) {
            task.setTitle(updateTaskRequest.getTitle().trim());
        }

        if (updateTaskRequest.getDescription() != null) {
            task.setDescription(updateTaskRequest.getDescription().trim());
        }

        if (updateTaskRequest.getPhase() != null) {
            task.setPhase(updateTaskRequest.getPhase().trim());
        }

        if (updateTaskRequest.getCompleted() != null) {
            task.setCompleted(updateTaskRequest.getCompleted());
        }

        if (updateTaskRequest.getTags() != null) {
            task.setTags(mergeArraysUtils.mergeDistinct(task.getTags(), updateTaskRequest.getTags()));
        }

        if (updateTaskRequest.getAssignees() != null) {
            task.setTags(mergeArraysUtils.mergeDistinct(task.getAssignees(), updateTaskRequest.getAssignees()));
        }

        if (updateTaskRequest.getEndDate() != null) {
            task.setEndDate(updateTaskRequest.getEndDate());
            boolean success = deadlineRepository.scheduleNotification(task.getId(), updateTaskRequest.getEndDate());

            if (!success) {
                throw new RuntimeException("Deadline not scheduled");
            }
        }

        taskRepository.update(task);

        return toTaskResponse(task);
    }

    public TaskResponse deleteTask(ObjectId taskId, String email) {
        Task task = taskRepository.findById(taskId);

        if (task != null) {
            if (task.getAssignees().contains(email) || task.getAssignees().isEmpty()) {
                taskRepository.delete(task);
                return toTaskResponse(task);
            }
            else {
                return null;
            }
        }
        else  {
            return null;
        }
    }

    TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getPhase(),
                task.getTags(),
                task.getAssignees(),
                task.getEndDate(),
                task.getIdProject()
        );
    }
}
