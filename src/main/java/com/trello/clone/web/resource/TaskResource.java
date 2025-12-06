package com.trello.clone.web.resource;

import com.trello.clone.service.TaskService;
import com.trello.clone.web.model.ErrorResponse;
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

import java.util.List;

@DenyAll
@Path("/api/task")
public class TaskResource {
    private final TaskService taskService;

    public TaskResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @GET
    @Path("/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getTaskById(
            @PathParam("taskId") String stringTaskId
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);
        TaskResponse taskResponse = taskService.getTaskById(taskId);

        if (taskResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(taskResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_PROJECT_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @GET
    @Path("/project/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getTasksByProjectId(
            @PathParam("projectId") String stringProjectId
    ) {
        ObjectId projectId = new ObjectId(stringProjectId);
        List<TaskResponse> taskResponseList = taskService.getAllTasksByProject(projectId);

        if (!taskResponseList.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity(taskResponseList)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_PROJECT_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createTask(
            @Valid CreateTaskRequest createTaskRequest
    ) {
        TaskResponse taskResponse = taskService.createTask(createTaskRequest);

        if (taskResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(taskResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_PROJECT_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @PUT
    @Path("/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateTask(
            @PathParam("taskId") String stringTaskId,
            UpdateTaskRequest updateTaskRequest
    ) {
        ObjectId taskId = new ObjectId(stringTaskId);
        TaskResponse taskResponse = taskService.updateTask(updateTaskRequest, taskId);

        if (taskResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(taskResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_PROJECT_RETRIEVAL",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }

    @DELETE
    @Path("/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response deleteTaskById(
            @PathParam("taskId") String stringTaskId,
            @Context SecurityContext securityContext
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ObjectId taskId = new ObjectId(stringTaskId);

        TaskResponse taskResponse = taskService.deleteTask(taskId, email);

        if (taskResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(taskResponse)
                    .build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "ERROR_DURING_PROJECT_Deletion",
                            "An unexpected error occurred"
                    ))
                    .build();
        }
    }
}
