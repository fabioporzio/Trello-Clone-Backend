package com.trello.clone.data.repository;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DeadlineRepository {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, String> stringCommands;

    public DeadlineRepository(RedisDataSource redisDataSource) {
        keyCommands = redisDataSource.key();
        stringCommands = redisDataSource.value(String.class);
    }

    public boolean scheduleNotification(ObjectId taskId, String deadlineString) {
        Instant taskDeadline = Instant.parse(deadlineString);

        long ttl = calculateNotificationTTL(taskDeadline);
        System.out.println("TTL: " + ttl);

        String key = "trello-clone|deadlines|task|" + taskId;
        stringCommands.setex(key, 30, "placeholder");

        return keyCommands.exists(key);
    }

    public long calculateNotificationTTL(Instant taskDeadline) {
        // TTL must end 1 day before the deadline
        Instant notificationTime = taskDeadline.minus(1, ChronoUnit.DAYS);

        Instant now = Instant.now();

        // Checks if notificationTime is after now
        if (notificationTime.isAfter(now)) {
            // Calculates the time between now and notificationTime and returns it in seconds
            Duration duration = Duration.between(now, notificationTime);
            return duration.toSeconds();
        }
        else {
            if (taskDeadline.isBefore(now)) {
                // Deadline is already expired, so we return a negative number not to set the key in redis
                return 0;
            }
            else {
                // Deadline will happen before 24 hours, so TTL is set to 1 to trigger immediate notification
                return 1;
            }
        }
    }
}
