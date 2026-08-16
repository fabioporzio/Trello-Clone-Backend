package com.trello.clone.data.repository;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Collection;

@ApplicationScoped
@Startup // We want to create the bean instance on startup to subscribe to the channel.
public class RedisSubscriber {

    private final PubSubCommands<String> pub;
    private final PubSubCommands.RedisSubscriber subscriber;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    public RedisSubscriber(
            RedisDataSource ds,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            NotificationRepository notificationRepository
    ) {
        pub = ds.pubsub(String.class);
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        subscriber = pub.subscribe("__keyevent@0__:expired", message -> {
            handleMessage(message);
        });
    }

    @PreDestroy
    public void terminate() {
        subscriber.unsubscribe(); // Unsubscribe from all subscribed channels
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
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to handle deadline event: %s", message);
        }
    }
}
