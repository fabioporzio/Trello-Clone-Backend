package com.trello.clone.service;

import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
        List<Task> tasks = taskRepository.find("{ projectId: ?1 }", projectId).list();

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
                createTaskRequest.getPhase(),
                createTaskRequest.getTags(),
                createTaskRequest.getAssignees(),
                createTaskRequest.getEndDate(),
                createTaskRequest.getProjectId()
        );

        taskRepository.persist(task);

        return toTaskResponse(task);
    }

    public TaskResponse updateTask(UpdateTaskRequest updateTaskRequest, ObjectId taskId) {
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return null;
        }

        if (updateTaskRequest.getTitle() != null) {
            task.setTitle(updateTaskRequest.getTitle());
        }

        if (updateTaskRequest.getDescription() != null) {
            task.setDescription(updateTaskRequest.getDescription());
        }

        if (updateTaskRequest.getPhase() != null) {
            task.setPhase(updateTaskRequest.getPhase());
        }

        if (updateTaskRequest.getTags() != null) {
            task.setTags(updateTaskRequest.getTags());
        }

        if (updateTaskRequest.getAssignees() != null) {
            task.setAssignees(updateTaskRequest.getAssignees());
        }

        if (updateTaskRequest.getEndDate() != null) {
            task.setEndDate(updateTaskRequest.getEndDate());
        }

        taskRepository.update(task);

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
