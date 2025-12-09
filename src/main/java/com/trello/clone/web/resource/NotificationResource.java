package com.trello.clone.web.resource;

import com.trello.clone.service.NotificationService;
import com.trello.clone.web.model.notification.CreateNotificationRequest;
import com.trello.clone.web.model.notification.NotificationResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@Path("/api/notification")
public class NotificationResource {

    private final NotificationService notificationService;

    public NotificationResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getReceivedNotifications(
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();
        Map<String, List<NotificationResponse>> mappedNotificationsResponses = notificationService.getReceivedNotifications(email);
        return Response.ok()
                .entity(mappedNotificationsResponses)
                .build();
    }

    @POST
    @Path("/project/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createProjectNotification(
            @Valid CreateNotificationRequest createNotificationRequest,
            @PathParam("projectId") String stringProjectId,
            @Context SecurityContext securityContext
    ) {
        ObjectId projectId = new ObjectId(stringProjectId);
        String senderEmail = securityContext.getUserPrincipal().getName();
        notificationService.addProjectNotification(createNotificationRequest, projectId, senderEmail);

        return Response.status(Response.Status.CREATED)
                .entity("Notifications processed correctly")
                .build();
    }

    @POST
    @Path("/task/{taskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createTaskNotification(
            @Valid CreateNotificationRequest createNotificationRequest,
            @PathParam("taskId") String stringTaskId,
            @Context SecurityContext securityContext
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);
        String senderEmail = securityContext.getUserPrincipal().getName();
        notificationService.addTaskNotification(createNotificationRequest, taskId, senderEmail);

        return Response.status(Response.Status.CREATED)
                .entity("Notifications processed correctly")
                .build();
    }

    @DELETE
    @Path("/{sender}/{taskOrProject}/{taskOrProjectId}/{issuedAt}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response deleteNotification(
            @PathParam("sender") String sender,
            @PathParam("taskOrProject") String taskOrProject,
            @PathParam("taskOrProjectId") String taskOrProjectId,
            @PathParam("issuedAt") String issuedAt,
            @Context SecurityContext securityContext
    ) {
        String receiverEmail = securityContext.getUserPrincipal().getName();
        NotificationResponse notificationResponse = notificationService.deleteNotification(
                receiverEmail,
                taskOrProject,
                taskOrProjectId,
                sender,
                issuedAt
        );

        return Response.ok()
                .entity(notificationResponse)
                .build();
    }

}
