package com.trello.clone.service;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import com.trello.clone.web.model.notification.NotificationResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
            Object projectId,
            String senderEmail
    ) {
        this.notificationRepository.addProjectNotification(createNotificationRequest, projectId, senderEmail);
    }

    public void addTaskNotification(
            CreateNotificationRequest createNotificationRequest,
            Object taskId,
            String senderEmail
    ) {
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
