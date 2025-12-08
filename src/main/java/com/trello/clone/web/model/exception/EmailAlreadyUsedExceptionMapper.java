package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.EmailAlreadyUsedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EmailAlreadyUsedExceptionMapper implements ExceptionMapper<EmailAlreadyUsedException> {

    @Override
    public Response toResponse(EmailAlreadyUsedException ex) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("EMAIL_ALREADY_USED", ex.getMessage()))
                .build();
    }
}

