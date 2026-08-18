package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.TooManyAttemptsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TooManyAttemptsExceptionMapper implements ExceptionMapper<TooManyAttemptsException> {

    @Override
    public Response toResponse(TooManyAttemptsException e) {
        return Response.status(429)
                .header("Retry-After", e.getRetryAfterSeconds())
                .entity("Too many login attempts. Please try again later.")
                .build();
    }
}
