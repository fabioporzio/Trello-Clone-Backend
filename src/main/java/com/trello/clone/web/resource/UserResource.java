package com.trello.clone.web.resource;

import com.trello.clone.service.UserService;
import com.trello.clone.web.model.ErrorResponse;
import com.trello.clone.web.model.user.*;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@DenyAll
@Path("api/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getUser(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        UserResponse userResponse = userService.getUserByEmail(email);

        if (userResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(userResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_USER_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getAllUsers() {
        List<UserResponse> userResponseList= userService.getAllUsers();

        if (!userResponseList.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity(userResponseList)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_USER_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
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

    @PUT
    @Path("/update-email")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateUserEmail(@Valid UpdateUserEmailRequest updateUserEmailRequest) {
        UserResponse userResponse = userService.updateUserEmail(updateUserEmailRequest);

        if (userResponse != null) {
            return Response.status(Response.Status.OK)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_EMAIL_UPDATE",
                            "Email and/or password are incorrect"
                    ))
                    .build();
        }
    }

    @PUT
    @Path("/update-username")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateUserUsername(@Valid UpdateUserUsernameRequest updateUserUsernameRequest) {
        UserResponse userResponse = userService.updateUserUsername(updateUserUsernameRequest);

        if (userResponse != null) {
            return Response.status(Response.Status.OK)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_USERNAME_UPDATE",
                            "Email and/or password are incorrect"
                    ))
                    .build();
        }
    }

    @PUT
    @Path("/update-password")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateUserPassword(@Valid UpdateUserPasswordRequest updateUserPasswordRequest) {
        UserResponse userResponse = userService.updateUserPassword(updateUserPasswordRequest);

        if (userResponse != null) {
            return Response.status(Response.Status.OK)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_EMAIL_UPDATE",
                            "Email and/or password are incorrect"
                    ))
                    .build();
        }
    }
}
