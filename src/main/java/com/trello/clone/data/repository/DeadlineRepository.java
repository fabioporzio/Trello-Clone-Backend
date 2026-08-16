package com.trello.clone.data.repository;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DeadlineRepository {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, String> stringCommands;

    public DeadlineRepository(RedisDataSource redisDataSource) {
        keyCommands = redisDataSource.key();
        stringCommands = redisDataSource.value(String.class);
    }

    public boolean scheduleNotification(ObjectId taskId, LocalDate deadline) {
        Instant taskDeadline = deadline.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long ttl = calculateNotificationTTL(taskDeadline);
        Log.debugf("Deadline TTL for task %s: %d seconds", taskId, ttl);

        String key = "trello-clone:deadlines:task:" + taskId;
        if (ttl > 0) {
            stringCommands.setex(key, ttl, "placeholder");
            return  true;
        }
        else {
            return false;
        }
    }

    public static long calculateNotificationTTL(Instant taskDeadline) {
        // TTL must end 1 day before the deadline
        Instant notificationTime = taskDeadline.minus(1, ChronoUnit.DAYS);

        Instant now = Instant.now();

        // Checks if notificationTime is after now
        if (notificationTime.isAfter(now)) {
            // Calculates the time between now and notificationTime and returns it in seconds
            Duration duration = Duration.between(now, notificationTime);
            return duration.toSeconds();
        }
        if (taskDeadline.isBefore(now)) {
            return 0;
        }
        else {
            return 1;
        }
    }

    public void cancel(ObjectId taskId) {
        keyCommands.del("trello-clone:deadlines:task:" + taskId);
    }
}
