package com.trello.clone.web.resource;

import com.trello.clone.service.NotificationService;
import com.trello.clone.web.model.notification.NotificationResponse;
import com.trello.clone.web.model.project.ProjectResponse;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
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

import java.util.List;
import java.util.Map;

@Path("/api/notification")
@DenyAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private final NotificationService notificationService;

    public NotificationResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Gets all notifications saved in Redis by User email",
            description = "Validates JWT access token and returns all user's notifications in Redis based on email in JWT."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    public Response getReceivedNotifications(
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();
        Map<String, List<NotificationResponse>> mappedNotificationsResponses = notificationService.getReceivedNotifications(email);
        return Response.ok()
                .entity(mappedNotificationsResponses)
                .build();
    }

    @DELETE
    @Path("/{notificationId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Deletes a notification",
            description = "Validates JWT access token and deletes the task specified in the URL"
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(responseCode = "204", description = "Creation successful")
    @APIResponse(responseCode = "400", description = "Bad request")
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "Notification not founf")
    public Response deleteNotification(
            @PathParam("notificationId") String notificationId,
            @Context SecurityContext securityContext
    ) {
        String receiverEmail = securityContext.getUserPrincipal().getName();
        notificationService.deleteNotification(notificationId, receiverEmail);

        return Response.noContent()
                .build();
    }
}
