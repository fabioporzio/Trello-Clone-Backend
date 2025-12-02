package com.trello.clone.web.resource;

import com.trello.clone.service.TaskService;
import com.trello.clone.web.model.ErrorResponse;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/task")
public class TaskResource {

    private final TaskService taskService;

    public TaskResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createTask(
            CreateTaskRequest createTaskRequest
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
}
