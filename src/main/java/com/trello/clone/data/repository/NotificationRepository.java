package com.trello.clone.data.repository;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.model.Task;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NotificationRepository {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, String> stringCommands;
    private final ReactiveValueCommands<String, String> reactiveStringCommands;

    public NotificationRepository(
            RedisDataSource redisDataSource,
            ReactiveRedisDataSource reactiveRedisDataSource
    ) {
        keyCommands = redisDataSource.key();
        stringCommands = redisDataSource.value(String.class);
        reactiveStringCommands = reactiveRedisDataSource.value(String.class);
    }

    long sevenDaysTtl = 7 * 24 * 60 * 60;
    long twoDaysTtl = 2 * 24 * 60 * 60;

    public List<Notification> getNotifications(String email) {
        List<String> keys = keyCommands.keys(
                "trello-clone|users|" +
                        email +
                        "|notifications|received|*"
        );

        List<Notification> notifications = new ArrayList<>();
        for (String key : keys) {
            String[] splitKey = key.split("\\|");
            String content = stringCommands.get(key);
            Notification receivedNotification = new Notification(
                    splitKey[6],
                    splitKey[7],
                    content,
                    splitKey[4],
                    splitKey[5]
            );

            notifications.add(receivedNotification);
        }

        return notifications;
    }

    public void addProjectNotification(CreateNotificationRequest request, Object projectId) {
        String key = "trello-clone|users|" +
                request.getReceiver() +
                "|notifications|project|" +
                projectId + "|" +
                request.getSender() + "|" +
                request.getIssuedAt();

        String message = "You have been invited to project " +
                request.getProjectOrTaskName() +
                " by " +
                request.getSender();

        stringCommands.setex(key, sevenDaysTtl, message);

        if (!keyCommands.exists(key)) {
            throw new GenericException(
                    "Failed to create notification for receiver " +
                            request.getReceiver()
            );
        }
    }

    public void addTaskNotification(CreateNotificationRequest request, Object taskId) {
        String key = "trello-clone|users|" +
                request.getReceiver() +
                "|notifications|task|" +
                taskId + "|" +
                request.getSender() + "|" +
                request.getIssuedAt();

        String message = "You have been assigned to " +
                request.getProjectOrTaskName() +
                " by " +
                request.getSender();

        stringCommands.setex(key, sevenDaysTtl, message);

        if (!keyCommands.exists(key)) {
            throw new GenericException(
                    "Failed to create notification for receiver " +
                            request.getReceiver()
            );
        }
    }

    public void addDeadlineNotification(Task task) {
        String isoString = task.getEndDate(); // "2025-12-06T23:53:04.710+01:00"
        ZonedDateTime dateTime = ZonedDateTime.parse(isoString);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        String formattedDate = dateTime.format(formatter);

        for (String assignee : task.getAssignees()) {
            String key = "trello-clone|users|" + assignee + "|notifications|deadline|" + task.getId();
            String message = "Task " + task.getTitle() + " is due on " + formattedDate;

            reactiveStringCommands.setex(key, twoDaysTtl, message)
                    .subscribe().with(
                            unused -> {},
                            Throwable::printStackTrace
                    );
        }
    }

    public Notification deleteNotification(
            String receiver,
            String sender,
            String taskOrProject,
            String issuedAt,
            String taskOrProjectId
    ) {
        String receiverKey = "trello-clone|users|" +
                receiver +
                "|notifications|" +
                taskOrProject + "|" +
                taskOrProjectId + "|" +
                sender + "|" +
                issuedAt;

        boolean success = keyCommands.del(receiverKey) > 0;

        if (!success) {
            throw new GenericException("Failed to delete notification for receiver " + receiver);
        }

        return new Notification(
                sender,
                issuedAt,
                null,
                taskOrProject,
                taskOrProjectId
        );
    }
}
