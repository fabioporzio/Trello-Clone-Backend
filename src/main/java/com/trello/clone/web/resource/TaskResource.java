package com.trello.clone.web.resource;

import com.trello.clone.service.TaskService;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.*;

@DenyAll
@Path("/api/project/{projectId}/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    private final TaskService taskService;

    public TaskResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @GET
    @Path("/{taskId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Gets task details by Task ObjectId",
            description = "Validates JWT access token and returns the task details based on the given ObjectId."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "Task not found")
    public Response getTaskById(
            @Context SecurityContext securityContext,
            @PathParam("projectId") ObjectId projectId,
            @PathParam("taskId") ObjectId taskId
    ) {
        String requestSenderEmail = securityContext.getUserPrincipal().getName();

        TaskResponse taskResponse = taskService.getTaskById(requestSenderEmail, projectId, taskId);

        return Response.ok()
                .entity(taskResponse)
                .build();
    }

    @GET
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Gets all tasks by project ObjectId",
            description = "Validates JWT access token and returns all tasks linked to the same project by its ObjectId."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "You are not a member of this project")
    @APIResponse(responseCode = "404", description = "Project not found")
    public Response getTasksByProjectId(
            @Context SecurityContext securityContext,
            @PathParam("projectId") ObjectId projectId,
            @QueryParam("tags") String tagsCsv,
            @QueryParam("assignees") String assigneesCsv
    ) {
        String email = securityContext.getUserPrincipal().getName();

        List<String> tags = parseCsv(tagsCsv);
        List<String> assignees = parseCsv(assigneesCsv);

        Map<String, List<TaskResponse>> mappedTasks = taskService.getAllTasksByProject(email, projectId, tags, assignees);
        return Response.ok()
                .entity(mappedTasks)
                .build();
    }

    @POST
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Creates a task",
            description = "Validates JWT access token and creates a task."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "201",
            description = "Creation successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "You are not a member of this project")
    @APIResponse(responseCode = "404", description = "Project not found")
    public Response createTask(
            @Valid CreateTaskRequest createTaskRequest,
            @Context SecurityContext securityContext,
            @PathParam("projectId") ObjectId projectId
    ) {
        String email = securityContext.getUserPrincipal().getName();

        TaskResponse taskResponse = taskService.createTask(createTaskRequest, email, projectId);

        return Response.status(Response.Status.CREATED)
                .entity(taskResponse)
                .build();
    }

    @PATCH
    @Path("/{taskId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Updates a task",
            description = "Validates JWT access token and updates a task."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Update successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "You are not a member of this project")
    @APIResponse(responseCode = "404", description = "Task or related project not found")
    public Response updateTask(
            @Valid UpdateTaskRequest updateTaskRequest,
            @Context SecurityContext securityContext,
            @PathParam("projectId") ObjectId projectId,
            @PathParam("taskId") ObjectId taskId
    ) {
        String email = securityContext.getUserPrincipal().getName();

        TaskResponse taskResponse = taskService.updateTask(updateTaskRequest, email, projectId, taskId);
        return Response.ok()
                .entity(taskResponse)
                .build();
    }

    @DELETE
    @Path("/{taskId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Deletes a task",
            description = "Validates JWT access token and deletes a task."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(responseCode = "204", description = "Delete successful")
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "You are not a member of this project")
    @APIResponse(responseCode = "404", description = "Task or related project not found")
    public Response deleteTaskById(
            @Context SecurityContext securityContext,
            @PathParam("projectId") ObjectId projectId,
            @PathParam("taskId") ObjectId taskId
    ) {
        String email = securityContext.getUserPrincipal().getName();

        taskService.deleteTask(email, projectId, taskId);
        return Response.noContent()
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    // UTILS

    private static List<String> parseCsv(String csv) {
        List<String> values = new ArrayList<>();

        if (csv == null || csv.isBlank()) {
            return values;
        }

        for (String raw : csv.split(",")) {
            String value = raw.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }

        return values;
    }
}
