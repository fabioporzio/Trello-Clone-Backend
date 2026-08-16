package com.trello.clone.data.repository;

import com.trello.clone.data.model.Task;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
@Startup // We want to create the bean instance on startup to subscribe to the channel.
public class RedisSubscriber {
    private final PubSubCommands<String> pub;
    private final PubSubCommands.RedisSubscriber subscriber;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    public RedisSubscriber(RedisDataSource ds, TaskRepository taskRepository, NotificationRepository notificationRepository) {
        pub = ds.pubsub(String.class);
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
        if (!message.startsWith("trello-clone|deadlines|task")) {
            System.out.println("Ignoring message: " + message);
            return; // Ignora chiavi che non appartengono alla categoria "task"
        }

        System.out.println(message);
        String[] splitMessage = message.split("\\|");
        String stringTaskId = splitMessage[3];
        ObjectId taskId = new ObjectId(stringTaskId);
        Task task = taskRepository.findById(taskId);
        //notificationRepository.addDeadlineNotification(task);
    }
}
