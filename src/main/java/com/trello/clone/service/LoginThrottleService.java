package com.trello.clone.service;

import com.trello.clone.service.exception.GenericException;
import com.trello.clone.utils.EmailUtils;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class LoginThrottleService {

    private static final String ATTEMPTS_PREFIX = "Trello-Clone:login:attempts:";
    private static final String COOLDOWN_PREFIX = "Trello-Clone:login:cooldown:";
    private static final long OBSERVATION_WINDOW_SECONDS = 900; // 15 min

    private final ValueCommands<String, Long> counters;
    private final KeyCommands<String> keys;

    private final EmailUtils emailUtils;

    public LoginThrottleService(RedisDataSource redisDataSource, EmailUtils emailUtils) {
        this.counters = redisDataSource.value(Long.class);
        this.keys = redisDataSource.key();
        this.emailUtils = emailUtils;
    }

    /**
     * Returns the remaining cooldown seconds.
     * Returns 0 if the account is not throttled.
     */
    public long checkCooldown(String email) {
        try {
            long ttl = keys.ttl(COOLDOWN_PREFIX + keyId(email));
            if (ttl > 0) {
                return ttl;
            }
            return 0;
        } catch (RedisKeyNotFoundException e) {
            return 0;
        }
    }

    /** To be called after a Failed login. Returns the applied cooldown (seconds). */
    public long registerFailure(String email) {
        String id = keyId(email);

        long count = counters.incr(ATTEMPTS_PREFIX + id);
        keys.expire(ATTEMPTS_PREFIX + id, OBSERVATION_WINDOW_SECONDS);

        long delay = cooldownForAttempts(count);
        if (delay > 0) {
            counters.setex(COOLDOWN_PREFIX + id, delay, 1L);
        }
        return delay;
    }

    /** To be called after a successful login. Sets everything back to 0. */
    public void reset(String email) {
        String id = keyId(email);
        keys.del(ATTEMPTS_PREFIX + id, COOLDOWN_PREFIX + id);
    }

    /** Returns cooldown seconds based on failed attempts.  */
    private long cooldownForAttempts(long attempts) {
        if (attempts <= 2) {
            return 0;
        }
        if (attempts == 3) {
            return 5;
        }
        if (attempts == 4) {
            return 15;
        }
        if (attempts == 5) {
            return 60;
        }
        if (attempts == 6) {
            return 300;
        }
        return 900;
    }

    /** Email is lowercased and ashed in order to be used as Redis key. */
    private String keyId(String email) {
        String normalizedEmail = emailUtils.normalize(email);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new GenericException("Unable to compute throttle key");
        }
    }
}
