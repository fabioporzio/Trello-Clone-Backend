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

@Path("/api/notification")
public class NotificationResource {

    private final NotificationService notificationService;

    public NotificationResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getReceivedNotifications(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        List<NotificationResponse> notificationResponses = notificationService.getReceivedNotifications(email);
        return Response.ok(notificationResponses).build();
    }

    @POST
    @Path("/project/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createProjectNotification(
            @Valid CreateNotificationRequest createNotificationRequest,
            @PathParam("projectId") String stringProjectId
    ) {
        ObjectId projectId = new ObjectId(stringProjectId);
        notificationService.addProjectNotification(createNotificationRequest, projectId);

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
            @PathParam("taskId") String stringTaskId
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);
        notificationService.addTaskNotification(createNotificationRequest, taskId);

        return Response.status(Response.Status.CREATED)
                .entity("Notifications processed correctly")
                .build();
    }

    @DELETE
    @Path("/{receiver}/{sender}/{taskOrProject}/{issuedAt}/{taskOrProjectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response deleteNotification(
            @PathParam("receiver") String receiver,
            @PathParam("sender") String sender,
            @PathParam("taskOrProject") String taskOrProject,
            @PathParam("issuedAt") String issuedAt,
            @PathParam("taskOrProjectId") String taskOrProjectId
    ) {
        NotificationResponse notificationResponse = notificationService.deleteNotification(
                receiver,
                sender,
                taskOrProject,
                issuedAt,
                taskOrProjectId
        );

        return Response.ok()
                .entity(notificationResponse)
                .build();
    }

}
