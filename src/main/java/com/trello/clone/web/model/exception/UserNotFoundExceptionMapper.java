package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.UserNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UserNotFoundExceptionMapper implements ExceptionMapper<UserNotFoundException> {
    @Override
    public Response toResponse(UserNotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()))
                .build();
    }
}
