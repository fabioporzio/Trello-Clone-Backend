package com.trello.clone.data.repository;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.bson.types.ObjectId;

import java.util.Collection;

@ApplicationScoped
public class RedisSubscriber {

    private final RedisDataSource redisDataSource;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    private PubSubCommands.RedisSubscriber subscriber;

    public RedisSubscriber(
            RedisDataSource redisDataSource,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            NotificationRepository notificationRepository
    ) {
        this.redisDataSource = redisDataSource;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            subscriber = redisDataSource.pubsub(String.class)
                    .subscribe("__keyevent@0__:expired", this::handleMessage);
            Log.info("Subscribed to Redis deadline expiry events");
        } catch (Exception e) {
            Log.error("Could not subscribe to Redis: deadline reminders are disabled", e);
        }
    }

    @PreDestroy
    void terminate() {
        if (subscriber != null) {
            subscriber.unsubscribe();
        }
    }

    private void handleMessage(String message) {
        try {
            if (!message.startsWith("trello-clone:deadlines:task:")) {
                return;
            }

            String[] parts = message.split(":");
            ObjectId taskId = new ObjectId(parts[3]);

            Task task = taskRepository.findById(taskId);
            if (task == null || task.isCompleted() || task.getEndDate() == null) {
                Log.debugf("Deadline event ignored for task %s", taskId);
                return;
            }

            Collection<String> recipients = task.getAssignees();
            if (recipients.isEmpty()) {
                Project project = projectRepository.findById(task.getProjectId());
                if (project == null) {
                    return;
                }
                recipients = project.getTeam();
            }

            notificationRepository.addDeadlineNotification(task, recipients);
        } catch (Exception e) {
            Log.errorf(e, "Failed to handle deadline event: %s", message);
        }
    }
}
