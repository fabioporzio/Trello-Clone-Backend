package com.trello.clone.service;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import com.trello.clone.web.model.notification.NotificationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public Map<String, List<NotificationResponse>> getReceivedNotifications(String email) {
        List<Notification> notifications = notificationRepository.getNotifications(email);

        Map<String, List<NotificationResponse>> mappedNotificationsResponses = new HashMap<>();
        for (Notification notification : notifications) {
            NotificationResponse notificationResponse = toNotificationResponse(notification);

            String category = notificationResponse.getCategory();
            mappedNotificationsResponses.putIfAbsent(category, new ArrayList<>());
            mappedNotificationsResponses.get(category).add(notificationResponse);
        }

        return mappedNotificationsResponses;
    }

    public void addProjectNotification(
            CreateNotificationRequest createNotificationRequest,
            ObjectId projectId,
            String senderEmail
    ) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }

        this.notificationRepository.addProjectNotification(createNotificationRequest, projectId, senderEmail);
    }

    public void addTaskNotification(
            CreateNotificationRequest createNotificationRequest,
            ObjectId taskId,
            String senderEmail
    ) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task with ID " + taskId + " not found");
        }
        this.notificationRepository.addTaskNotification(createNotificationRequest, taskId, senderEmail);
    }

    public NotificationResponse deleteNotification(
            String receiver,
            String taskOrProject,
            String taskOrProjectId,
            String sender,
            String issuedAt

    ) {
        Notification notification = this.notificationRepository.deleteNotification(
                receiver,
                taskOrProject,
                taskOrProjectId,
                sender,
                issuedAt
        );

        return toNotificationResponse(notification);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {

        return new NotificationResponse(
                notification.getReceiver(),
                notification.getCategory(),
                notification.getTaskOrProjectId(),
                notification.getSender(),
                notification.getIssuedAt(),
                notification.getContent()
        );
    }
}
