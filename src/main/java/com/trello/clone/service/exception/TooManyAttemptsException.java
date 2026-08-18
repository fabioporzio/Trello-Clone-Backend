package com.trello.clone.service.exception;

public class TooManyAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyAttemptsException(long retryAfterSeconds) {
        super("Too many login attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
