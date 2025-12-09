package com.trello.clone.data.repository;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class NotificationRepository {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, String> stringCommands;
    private final ReactiveValueCommands<String, String> reactiveStringCommands;
    private final ProjectRepository projectRepository;

    public NotificationRepository(
            RedisDataSource redisDataSource,
            ReactiveRedisDataSource reactiveRedisDataSource,
            ProjectRepository projectRepository
    ) {
        keyCommands = redisDataSource.key();
        stringCommands = redisDataSource.value(String.class);
        reactiveStringCommands = reactiveRedisDataSource.value(String.class);
        this.projectRepository = projectRepository;
    }

    long sevenDaysTtl = 7 * 24 * 60 * 60;
    long twoDaysTtl = 2 * 24 * 60 * 60;

    public List<Notification> getNotifications(String email) {
        List<String> keys = keyCommands.keys(
                "trello-clone|users|" +
                        email +
                        "|notifications|*"
        );

        List<Notification> notifications = new ArrayList<>();
        for (String key : keys) {
            String[] splitKey = key.split("\\|");
            String content = stringCommands.get(key);

            Notification notification = getNotification(splitKey, content);

            notifications.add(notification);
        }

        return notifications;
    }

    private Notification getNotification(String[] splitKey, String content) {
        Notification notification;
        if (Objects.equals(splitKey[4], "deadline")) {
            notification = new Notification(
                    splitKey[2],
                    splitKey[4],
                    splitKey[5],
                    null,
                    null,
                    content
            );

        }
        else {
            notification = new Notification(
                    splitKey[2],
                    splitKey[4],
                    splitKey[5],
                    splitKey[6],
                    splitKey[7],
                    content
            );

        }
        return notification;
    }

    public void addProjectNotification(CreateNotificationRequest request, ObjectId projectId, String senderEmail) {
        String key = "trello-clone|users|" +
                request.getReceiver() +
                "|notifications|project|" +
                projectId + "|" +
                senderEmail + "|" +
                request.getIssuedAt();

        String message = "You have been invited to project " +
                request.getProjectOrTaskName() +
                " by " +
                senderEmail;

        stringCommands.setex(key, sevenDaysTtl, message);

        if (!keyCommands.exists(key)) {
            throw new GenericException(
                    "Failed to create notification for receiver " +
                            request.getReceiver()
            );
        }
    }

    public void addTaskNotification(CreateNotificationRequest request, ObjectId taskId, String senderEmail) {
        String key = "trello-clone|users|" +
                request.getReceiver() +
                "|notifications|task|" +
                taskId + "|" +
                senderEmail + "|" +
                request.getIssuedAt();

        String message = "You have been assigned to " +
                request.getProjectOrTaskName() +
                " by " +
                senderEmail;

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

        if (!task.getAssignees().isEmpty()) {
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
        else {
            Project project = projectRepository.findById(task.getProjectId());
            for (String teamMember : project.getTeam()) {
                String key = "trello-clone|users|" + teamMember + "|notifications|deadline|" + task.getId();
                String message = "Task " + task.getTitle() + " is due on " + formattedDate;

                reactiveStringCommands.setex(key, twoDaysTtl, message)
                        .subscribe().with(
                                unused -> {},
                                Throwable::printStackTrace
                        );
            }
        }
    }

    public Notification deleteNotification(
            String receiver,
            String taskOrProject,
            String taskOrProjectId,
            String sender,
            String issuedAt

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
                receiver,
                taskOrProject,
                taskOrProjectId,
                sender,
                issuedAt,
                null
        );
    }
}
