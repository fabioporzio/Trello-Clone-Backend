package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BAD_REQUEST", ex.getMessage()))
                .build();
    }
}
