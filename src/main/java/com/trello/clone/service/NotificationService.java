package com.trello.clone.service;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.web.model.notification.NotificationResponse;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Map<String, List<NotificationResponse>> getReceivedNotifications(String email) {
        List<Notification> notifications;
        try {
            notifications = notificationRepository.getNotifications(email);
        }
        catch (Exception e) {
            Log.error("Failed to gather notification", e);
            throw new GenericException("Failed to gather notifications due to server error");
        }

        Map<String, List<NotificationResponse>> mappedNotificationsResponses = new LinkedHashMap<>();
        for (Notification notification : notifications) {
            NotificationResponse notificationResponse = toNotificationResponse(notification);

            String category = notificationResponse.getCategory();
            mappedNotificationsResponses.putIfAbsent(category, new ArrayList<>());
            mappedNotificationsResponses.get(category).add(notificationResponse);
        }

        for (List<NotificationResponse> responses : mappedNotificationsResponses.values()) {
            responses.sort((r1, r2) -> r2.getIssuedAt().compareTo(r1.getIssuedAt()));
        }

        return mappedNotificationsResponses;
    }

    public void deleteNotification(String receiver, String notificationId) {
        try {
            notificationRepository.deleteNotification(receiver, notificationId);
        }
        catch (NotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            Log.error("Failed to delete notification", e);
            throw new GenericException("Failed to delete notification due to server error");
        }
    }

    //UTILS

    private NotificationResponse toNotificationResponse(Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getReceiver(),
                notification.getCategory(),
                notification.getProjectOrTaskId(),
                notification.getSender(),
                notification.getIssuedAt(),
                notification.getContent()
        );
    }
}
