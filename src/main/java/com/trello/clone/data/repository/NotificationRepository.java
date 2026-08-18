package com.trello.clone.data.repository;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.utils.EmailUtils;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class NotificationRepository {

    private static final String PREFIX = "trello-clone:users:";
    private static final long SEVEN_DAYS_TTL = 7 * 24 * 60 * 60;
    private static final long TWO_DAYS_TTL = 2 * 24 * 60 * 60;

    private final RedisDataSource redisDataSource;
    private final HashCommands<String, String, Notification> hashCommands;

    public NotificationRepository(RedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
        this.hashCommands = redisDataSource.hash(String.class, String.class, Notification.class);
    }

    public List<Notification> getNotifications(String email) {
        Map<String, Notification> stored = hashCommands.hgetall(inboxKey(EmailUtils.normalize(email)));

        List<Notification> notifications = new ArrayList<>();
        for (Map.Entry<String, Notification> entry : stored.entrySet()) {
            Notification notification = entry.getValue();
            notification.setId(entry.getKey());
            notifications.add(notification);
        }

        return notifications;
    }

    public void addProjectInvite(String receiver, Project project, String sender) {
        String target = EmailUtils.normalize(receiver);

        Notification notification = new Notification(
                target,
                "project",
                project.getId().toHexString(),
                sender,
                Instant.now().toString(),
                "You have been invited to project " + project.getName() + " by " + sender
        );

        store(target, projectInviteId(project.getId().toHexString(), target), notification);
    }

    public void addTaskAssignment(String receiver, Task task, String sender) {
        String target = EmailUtils.normalize(receiver);

        Notification notification = new Notification(
                target,
                "task",
                task.getId().toHexString(),
                sender,
                Instant.now().toString(),
                "You have been assigned task " + task.getTitle() + " by " + sender
        );

        store(target, taskAssignmentId(task.getId().toHexString(), target), notification);
    }

    public void deleteNotification(String receiver, String notificationId) {
        int removed = hashCommands.hdel(inboxKey(EmailUtils.normalize(receiver)), notificationId);

        if (removed == 0) {
            throw new NotFoundException("Notification " + notificationId + " not found");
        }
    }

    public void removeProjectInvite(String receiver, String projectId) {
        String target = EmailUtils.normalize(receiver);
        hashCommands.hdel(inboxKey(target), projectInviteId(projectId, target));
    }

    public void removeTaskAssignment(String receiver, String taskId) {
        String target = EmailUtils.normalize(receiver);
        hashCommands.hdel(inboxKey(target), taskAssignmentId(taskId, target));
    }

    private void store(String receiver, String notificationId, Notification notification) {
        String key = inboxKey(receiver);

        hashCommands.hset(key, notificationId, notification);
        expireField(key, notificationId, SEVEN_DAYS_TTL);
    }

    private void expireField(String key, String field, long seconds) {
        try {
            redisDataSource.execute(
                    "HEXPIRE",
                    key,
                    String.valueOf(seconds),
                    "FIELDS",
                    "1",
                    field
            );
        }
        catch (Exception e) {
            Log.errorf(e, "HEXPIRE failed on %s / %s", key, field);
        }
    }

    private static String inboxKey(String email) {
        return PREFIX + email + ":notifications";
    }

    private static String projectInviteId(String projectId, String receiver) {
        return deterministicId("project:" + projectId + ":" + receiver);
    }

    private static String taskAssignmentId(String taskId, String receiver) {
        return deterministicId("task:" + taskId + ":" + receiver);
    }

    private static String deterministicId(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    // DEADLINE NOTIFICATIONS

    public void addDeadlineNotification(Task task, Collection<String> recipients) {
        String taskId = task.getId().toHexString();
        String formattedDate = task.getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        for (String recipient : recipients) {
            String target = EmailUtils.normalize(recipient);

            Notification notification = new Notification(
                    target,
                    "deadline",
                    taskId,
                    null,
                    Instant.now().toString(),
                    "Task " + task.getTitle() + " is due on " + formattedDate
            );

            store(target, deadlineId(taskId, target), notification);
        }
    }

    public void removeDeadlineNotification(String receiver, String taskId) {
        String target = EmailUtils.normalize(receiver);
        hashCommands.hdel(inboxKey(target), deadlineId(taskId, target));
    }

    private static String deadlineId(String taskId, String receiver) {
        return deterministicId("deadline:" + taskId + ":" + receiver);
    }
}
