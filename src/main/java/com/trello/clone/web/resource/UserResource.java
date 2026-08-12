package com.trello.clone.web.resource;

import com.trello.clone.service.UserService;
import com.trello.clone.web.model.user.*;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@DenyAll
@Path("api/user")
@Tag(name = "User Resources", description = "Registration, update user info and users info retrieval operations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Returns user's data based on JWT access token",
            description = "Validates JWT access token and returns user's email and username."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getUserFromAccessToken(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse userResponse = userService.getUserByEmail(email);

        return Response.status(Response.Status.OK)
                .entity(userResponse)
                .build();
    }

    @GET
    @Path("/search")
    @RolesAllowed({"access_token"})
    @Operation(summary = "Search users by username",
            description = "Returns a limited list of users whose username starts with the query term.")
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(responseCode = "200", description = "Matching users",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserSummaryResponse.class)))
    public Response searchUsers(
            @QueryParam("searchTerm")
            @NotBlank(message = "Search term is required")
            @Size(min = 2, message = "Search term must be at least 2 characters")
            String searchTerm) {
        List<UserSummaryResponse> results = userService.searchUsers(searchTerm);
        return Response.ok(results).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    @Operation(
            summary = "Allows a user to register",
            description = "Allows user registration by entering email and password"
    )
    @APIResponse(
            responseCode = "201",
            description = "Registration successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))
    )
    @APIResponse(responseCode = "409", description = "A user with this email already exists")
    public Response registerUser(@Valid CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.registerUser(createUserRequest);
        return Response.status(Response.Status.CREATED)
                .entity(userResponse)
                .build();
    }

    @PUT
    @Path("/update-email")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Updates user email",
            description = "Validates JWT access token and updates user's email if current email and password are correct."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Email update successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "Email and/or password are incorrect")
    @APIResponse(responseCode = "409", description = "A user with this email already exists")
    public Response updateUserEmail(
            @Valid UpdateUserEmailRequest updateUserEmailRequest,
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse userResponse = userService.updateUserEmail(updateUserEmailRequest, email);
        return Response.ok()
                .entity(userResponse)
                .build();
    }

    @PUT
    @Path("/update-username")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Updates user username",
            description = "Validates JWT access token and updates user's username if email and password are correct."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Username update successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "Email and/or password are incorrect")
    public Response updateUserUsername(
            @Valid UpdateUserUsernameRequest request,
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse userResponse = userService.updateUserUsername(request, email);
        return Response.ok()
                .entity(userResponse)
                .build();
    }

    @PUT
    @Path("/update-password")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Updates user password",
            description = "Validates JWT access token and updates user's password if email and current password are correct."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Password update successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "Email and/or password are incorrect")
    public Response updateUserPassword(
            @Valid UpdateUserPasswordRequest request,
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse userResponse = userService.updateUserPassword(request, email);
        return Response.ok()
                .entity(userResponse)
                .build();
    }
}
