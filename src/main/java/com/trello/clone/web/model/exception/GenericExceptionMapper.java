package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.GenericException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<GenericException> {

    @Override
    public Response toResponse(GenericException ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()))
                .build();
    }
}
