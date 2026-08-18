package com.trello.clone.service;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class RedisConnectivityCheck implements HealthCheck {

    private final RedisDataSource redisDataSource;

    public RedisConnectivityCheck(RedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis (notifications)");

        try {
            redisDataSource.execute("PING");
            builder.withData("reachable", true);
        }
        catch (Exception e) {
            builder.withData("reachable", false)
                    .withData("degraded", "notifications and deadline reminders unavailable")
                    .withData("error", e.getClass().getSimpleName());
        }

        return builder.up().build();
    }
}
