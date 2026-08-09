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
    public Response getTasksByProjectId(
            @PathParam("projectId") String stringProjectId,
            @QueryParam("tags") String tagsCsv,
            @QueryParam("assignees") String assigneesCsv
    ) {
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

        Map<String, List<TaskResponse>> mappedTasks = taskService.getAllTasksByProject(projectId, tags, assignees);
        return Response.ok()
                .entity(mappedTasks)
                .build();
    }

    @POST
    @RolesAllowed({"access_token"})
    public Response createTask(@Valid CreateTaskRequest createTaskRequest) {
        TaskResponse taskResponse = taskService.createTask(createTaskRequest);

        return Response.status(Response.Status.CREATED)
                .entity(taskResponse)
                .build();
    }

    @PUT
    @Path("/{taskId}")
    @RolesAllowed({"access_token"})
    public Response updateTask(
            @PathParam("taskId") String stringTaskId,
            @Context SecurityContext securityContext,
            UpdateTaskRequest updateTaskRequest
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
