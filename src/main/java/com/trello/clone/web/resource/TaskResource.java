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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@DenyAll
@Path("/api/task")
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
            @PathParam("taskId") String stringTaskId
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);

        TaskResponse taskResponse = taskService.getTaskById(taskId);

        return Response.ok()
                .entity(taskResponse)
                .build();
    }

    @GET
    @Path("/project/{projectId}")
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
            @PathParam("projectId") String stringProjectId,
            @QueryParam("tags") String tagsCsv,
            @QueryParam("assignees") String assigneesCsv
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ObjectId projectId = new ObjectId(stringProjectId);

        List<String> tags;
        if (tagsCsv != null && !tagsCsv.isEmpty()) {
            tags = Arrays.asList(tagsCsv.split(","));
        }
        else {
            tags = Collections.emptyList();
        }

        List<String> assignees;
        if (assigneesCsv != null && !assigneesCsv.isEmpty()) {
            assignees = Arrays.asList(assigneesCsv.split(","));
        }
        else {
            assignees = Collections.emptyList();
        }

        Map<String, List<TaskResponse>> mappedTasks = taskService.getAllTasksByProject(email, projectId, tags, assignees);
        return Response.ok()
                .entity(mappedTasks)
                .build();
    }

    @POST
    @Path("/project/{projectId}")
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
            @PathParam("projectId") String stringProjectId
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ObjectId projectId = new ObjectId(stringProjectId);

        TaskResponse taskResponse = taskService.createTask(createTaskRequest, email, projectId);

        return Response.status(Response.Status.CREATED)
                .entity(taskResponse)
                .build();
    }

    @PUT
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
            @PathParam("taskId") String stringTaskId,
            @Context SecurityContext securityContext,
            @Valid UpdateTaskRequest updateTaskRequest
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);
        String email = securityContext.getUserPrincipal().getName();

        TaskResponse taskResponse = taskService.updateTask(updateTaskRequest, taskId, email);
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
    @APIResponse(
            responseCode = "200",
            description = "Delete successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "403", description = "You are not a member of this project")
    @APIResponse(responseCode = "404", description = "Task not found")
    @APIResponse(responseCode = "404", description = "No project found for this task")
    public Response deleteTaskById(
            @PathParam("taskId") String stringTaskId,
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ObjectId taskId = new ObjectId(stringTaskId);

        TaskResponse taskResponse = taskService.deleteTask(taskId, email);
        return Response.ok()
                .entity(taskResponse)
                .build();
    }
}
