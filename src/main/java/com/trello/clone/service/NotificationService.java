package com.trello.clone.service;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import com.trello.clone.web.model.notification.NotificationResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponse> getReceivedNotifications(String email) {
        List<Notification> receivedNotifications = notificationRepository.getNotifications(email);

        List<NotificationResponse> receivedNotificationResponses = new ArrayList<>();
        for (Notification receivedNotification : receivedNotifications) {
            receivedNotificationResponses.add(toReceivedNotificationResponse(receivedNotification));
        }

        return receivedNotificationResponses;
    }

    public void addProjectNotification(CreateNotificationRequest createNotificationRequest, Object projectId) {
        this.notificationRepository.addProjectNotification(createNotificationRequest, projectId);
    }

    public void addTaskNotification(CreateNotificationRequest createNotificationRequest, Object taskId) {
        this.notificationRepository.addTaskNotification(createNotificationRequest, taskId);
    }

    public NotificationResponse deleteNotification(String receiver, String sender, String taskOrProject, String issuedAt, String taskOrProjectId) {
        Notification receivedNotification = this.notificationRepository.deleteNotification(receiver, sender, taskOrProject, issuedAt, taskOrProjectId);

        return toReceivedNotificationResponse(receivedNotification);
    }

    private NotificationResponse toReceivedNotificationResponse(Notification receivedNotification) {

        return new NotificationResponse(
                receivedNotification.getSender(),
                receivedNotification.getIssuedAt(),
                receivedNotification.getContent(),
                receivedNotification.getCategory(),
                receivedNotification.getTaskOrProjectId()
        );
    }
}
