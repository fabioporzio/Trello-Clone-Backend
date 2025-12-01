package com.trello.clone.data.repository;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationRepository {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, String> stringCommands;

    public AuthenticationRepository(RedisDataSource redisDataSource) {
        this.keyCommands = redisDataSource.key();
        this.stringCommands = redisDataSource.value(String.class);
    }

    public boolean saveRefreshToken(String refreshToken, String email) {
        String redisKey = "trello-clone:users:" + email + ":session:refresh-token";

        stringCommands.setex(redisKey, 180, refreshToken);

        return keyCommands.exists(redisKey);
    }

    public boolean isRefreshTokenValid(String email) {
        String redisKey = "trello-clone:users:" + email + ":session:refresh-token";

        return keyCommands.exists(redisKey);
    }
}
