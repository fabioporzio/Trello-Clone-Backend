package com.trello.clone.web.resource;

import com.trello.clone.service.UserService;
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
    @RolesAllowed({"access_token"})
    public Response getUserFromAccessToken(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse userResponse = userService.getUserByEmail(email);

        return Response.status(Response.Status.OK)
                .entity(userResponse)
                .build();
    }

    @GET
    @Path("/all")
    @RolesAllowed({"access_token"})
    public Response getAllUsers() {
        List<UserResponse> userResponseList = userService.getAllUsers();

        return Response.ok(userResponseList).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response registerUser(@Valid CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.registerUser(createUserRequest);
        return Response.status(Response.Status.CREATED)
                .entity(userResponse)
                .build();
    }

    @PUT
    @Path("/update-email")
    @RolesAllowed({"access_token"})
    public Response updateUserEmail(@Valid UpdateUserEmailRequest updateUserEmailRequest) {
        UserResponse userResponse = userService.updateUserEmail(updateUserEmailRequest);
        return Response.ok()
                .entity(userResponse)
                .build();
    }

    @PUT
    @Path("/update-username")
    @RolesAllowed({"access_token"})
    public Response updateUserUsername(@Valid UpdateUserUsernameRequest request) {
        UserResponse userResponse = userService.updateUserUsername(request);
        return Response.ok()
                .entity(userResponse)
                .build();
    }


    @PUT
    @Path("/update-password")
    @RolesAllowed({"access_token"})
    public Response updateUserPassword(@Valid UpdateUserPasswordRequest request) {
        UserResponse userResponse = userService.updateUserPassword(request);
        return Response.ok()
                .entity(userResponse)
                .build();
    }

}
