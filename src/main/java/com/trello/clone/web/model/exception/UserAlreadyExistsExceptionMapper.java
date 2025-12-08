package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UserAlreadyExistsExceptionMapper implements ExceptionMapper<UserAlreadyExistsException> {

    @Override
    public Response toResponse(UserAlreadyExistsException ex) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("USER_ALREADY_EXISTS", ex.getMessage()))
                .build();
    }
}
