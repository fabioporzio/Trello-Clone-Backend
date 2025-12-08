package com.trello.clone.web.model.exception;

import com.trello.clone.service.exception.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException ex) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("UNAUTHORIZED", ex.getMessage()))
                .build();
    }
}
