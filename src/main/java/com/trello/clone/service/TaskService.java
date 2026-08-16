package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.DeadlineRepository;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.utils.EmailUtils;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.*;

import static com.trello.clone.utils.RequestDeltas.normalizedEmails;
import static com.trello.clone.utils.RequestDeltas.rejectOverlap;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final DeadlineRepository deadlineRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            NotificationRepository notificationRepository,
            DeadlineRepository deadlineRepository
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.notificationRepository = notificationRepository;
        this.deadlineRepository = deadlineRepository;
    }

    public TaskResponse getTaskById(String email, ObjectId  projectId, ObjectId taskId) {
        String requestSender = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, requestSender);
        Task task = requireTask(taskId, projectId);

        return toTaskResponse(task);
    }

    public Map<String, List<TaskResponse>> getAllTasksByProject(
            String email,
            ObjectId projectId,
            List<String> tags,
            List<String> assignees
    ) {
        String requestSender = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, requestSender);

        List<Task> tasks;
        try {
            tasks = taskRepository.getTasksByProjectIdTagsAndAssignees(
                    projectId,
                    normalizeTagFilters(tags),
                    normalizedEmails(assignees)
            );
        }
        catch (Exception e) {
            Log.error("Failed to gather tasks", e);
            throw new GenericException("Failed to gather tasks due to server error");
        }

        Map<String, List<TaskResponse>> groupedTasks = new LinkedHashMap<>();
        for (String phase : project.getPhases()) {
            groupedTasks.put(phase, new ArrayList<>());
        }

        for (Task task : tasks) {
            String phase = task.getPhase();
            List<TaskResponse> bucket = groupedTasks.get(phase);

            if (bucket == null) {
                bucket = new ArrayList<>();
                groupedTasks.put(phase, bucket);
            }

            bucket.add(toTaskResponse(task));
        }

        return groupedTasks;
    }

    public TaskResponse createTask(
            CreateTaskRequest request,
            String email,
            ObjectId projectId
    ) {
        String requestSender = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, requestSender);
        String phase = requirePhase(project, request.getPhase());

        Task task = Task.create(
                request.getTitle(),
                request.getDescription(),
                phase,
                projectId
        );
        persist(task);

        return toTaskResponse(task);
    }

    public TaskResponse updateTask(
            UpdateTaskRequest request,
            String email,
            ObjectId projectId,
            ObjectId taskId
    ) {
        String requestSender = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, requestSender);
        Task task = requireTask(taskId, projectId);

        rejectOverlap(request.getTagsToAdd(), request.getTagsToRemove(), "tags");
        rejectOverlap(request.getAssigneesToAdd(), request.getAssigneesToRemove(), "user");

        if (request.getTitle() != null) {
            task.rename(request.getTitle());
        }

        if (isNotEmpty(request.getTagsToRemove())) {
            task.removeTags(request.getTagsToRemove());
        }

        if (isNotEmpty(request.getTagsToAdd())) {
            task.addTags(request.getTagsToAdd());
        }

        if (request.getTagsToRename() != null) {
            for (Map.Entry<String, String> rename : request.getTagsToRename().entrySet()) {
                task.renameTag(rename.getKey(), rename.getValue());
            }
        }

        List<String> justAssigned = new ArrayList<>();
        List<String> justUnassigned = new ArrayList<>();

        if (isNotEmpty(request.getAssigneesToAdd())) {
            Set<String> toAssign = normalizedEmails(request.getAssigneesToAdd());
            for (String assignee : toAssign) {
                if (!project.isMember(assignee)) {
                    throw new BadRequestException(assignee + " is not a member of this project");
                }
            }
            task.assign(toAssign);
            justAssigned.addAll(toAssign);
        }

        if (isNotEmpty(request.getAssigneesToRemove())) {
            Set<String> toUnassign = normalizedEmails(request.getAssigneesToRemove());
            task.unassign(toUnassign);
            justUnassigned.addAll(toUnassign);
        }

        if (request.getDescription() != null) {
            task.describe(request.getDescription());
        }

        if (request.getPhase() != null) {
            task.moveTo(requirePhase(project, request.getPhase()));
        }

        boolean justCompleted = false;
        if (request.getCompleted() != null) {
            justCompleted = request.getCompleted() && !task.isCompleted();
            task.setCompleted(request.getCompleted());
        }

        if (request.getEndDate() != null) {
            task.scheduleFor(request.getEndDate());
        }

        update(task);

        if (justCompleted) {
            clearDeadline(task, project);
        }

        notifyAssignments(task, requestSender, justAssigned, justUnassigned);

        if (request.getEndDate() != null) {
            boolean success = deadlineRepository.scheduleNotification(task.getId(), request.getEndDate());
            if (!success) {
                throw new GenericException("Deadline not scheduled due to server error");
            }
        }

        return toTaskResponse(task);
    }

    public void deleteTask(String email, ObjectId projectId, ObjectId taskId) {
        String requestSender = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, requestSender);
        Task task = requireTask(taskId, projectId);
        requireTaskDeleter(project, task, requestSender);

        Set<String> assignees = new LinkedHashSet<>(task.getAssignees());
        String hexTaskId = task.getId().toHexString();

        try {
            taskRepository.delete(task);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to delete task '%s'", task.getTitle());
            throw new GenericException("Failed to delete task due to server error");
        }

        for (String assignee : assignees) {
            try {
                notificationRepository.removeTaskAssignment(assignee, hexTaskId);
            }
            catch (Exception e) {
                Log.errorf(e, "Task deleted but its notification was not removed for %s", assignee);
            }
        }

        clearDeadline(task, project);
    }

    // TASK ASSIGNMENT NOTIFICATION FLOW

    private void notifyAssignments(Task task, String actor, List<String> assigneesToAdd, List<String> assigneesToRemove) {
        if (assigneesToAdd.isEmpty() && assigneesToRemove.isEmpty()) {
            return;
        }

        String taskId = task.getId().toHexString();

        for (String assignee : assigneesToAdd) {
            if (!assignee.equals(actor)) {
                try {
                    notificationRepository.addTaskAssignment(assignee, task, actor);
                }
                catch (Exception e) {
                    Log.errorf(e, "Task assignment was revoked but no notification has been sent to %s", assignee);
                }
            }
        }

        for (String invitee : assigneesToRemove) {
            try {
                notificationRepository.removeTaskAssignment(invitee, taskId);
            } catch (Exception e) {
                Log.errorf(e, "Task assignment was revoked but no notification has been sent to %s", invitee);
            }
        }
    }

    // DEADLINE NOTIFICATIONS MANAGEMENT

    // TaskService
    private void clearDeadline(Task task, Project project) {
        deadlineRepository.cancel(task.getId());

        Collection<String> recipients = task.getAssignees().isEmpty()
                ? project.getTeam()
                : task.getAssignees();

        String taskId = task.getId().toHexString();
        for (String recipient : recipients) {
            try {
                notificationRepository.removeDeadlineNotification(recipient, taskId);
            }
            catch (Exception e) {
                Log.errorf(e, "Deadline notification not removed for %s", recipient);
            }
        }
    }

    // RULES

    private Task requireTask(ObjectId taskId, ObjectId projectId) {
        Task task = taskRepository.findByIdAndProject(taskId, projectId);

        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }

        return task;
    }

    private Project requireProject(ObjectId projectId) {
        Project project = projectRepository.findById(projectId);

        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }

        return project;
    }

    private void requireMember(Project project, String actor) {
        if (!project.isMember(actor)) {
            throw new UnauthorizedException("You are not a member of this project");
        }
    }

    private String requirePhase(Project project, String phase) {
        if (phase == null || phase.isBlank()) {
            throw new BadRequestException("Task phase cannot be empty");
        }

        String canonical = project.canonicalPhase(phase.trim());
        if (canonical == null) {
            throw new BadRequestException("The phase " + phase.trim()
                    + " does not exist on project " + project.getName());
        }

        return canonical;
    }

    private void requireTaskDeleter(Project project, Task task, String actor) {
        if (project.isOwner(actor)) {
            return;
        }

        if (task.getAssignees().contains(actor)) {
            return;
        }

        throw new UnauthorizedException("Only the assignees or the project owner can delete this task");
    }

    private List<String> normalizeTagFilters(Collection<String> tags) {
        List<String> normalizedTags = new ArrayList<>();

        if (tags == null) {
            return normalizedTags;
        }

        for (String tag : tags) {
            normalizedTags.add(tag == null ? null : tag.trim().toLowerCase(Locale.ROOT));
        }

        return normalizedTags;
    }

    // PERSISTENCE

    private void persist(Task task) {
        try {
            taskRepository.persist(task);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to persist task '%s'", task.getTitle());
            throw new GenericException("Failed to create task due to server error");
        }
    }

    private void update(Task task) {
        try {
            taskRepository.update(task);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to update task %s", task.getId());
            throw new GenericException("Failed to update task due to server error");
        }
    }

    // UTILS

    private static boolean isNotEmpty(Collection<String> values) {
        return values != null && !values.isEmpty();
    }

    private TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getPhase(),
                List.copyOf(task.getTags()),
                new LinkedHashSet<>(task.getAssignees()),
                task.getEndDate(),
                task.getProjectId()
        );
    }
}
