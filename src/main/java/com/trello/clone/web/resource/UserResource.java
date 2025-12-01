package com.trello.clone.web.resource;

import com.trello.clone.service.UserService;
import com.trello.clone.web.model.CreateUserRequest;
import com.trello.clone.web.model.ErrorResponse;
import com.trello.clone.web.model.UserResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/user")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(@Valid CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.registerUser(createUserRequest);

        if (userResponse != null) {
            return Response.status(Response.Status.CREATED)
                    .entity(userResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_USER_REGISTRATION",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }
}
