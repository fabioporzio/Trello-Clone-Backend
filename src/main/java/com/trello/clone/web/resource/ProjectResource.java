package com.trello.clone.web.resource;

import com.trello.clone.service.ProjectService;
import com.trello.clone.web.model.ErrorResponse;
import com.trello.clone.web.model.project.ProjectResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/api/projects")
public class ProjectResource {

    private final ProjectService projectService;

    public ProjectResource(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getAllProjectsByUser(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        List<ProjectResponse> projectResponseList = projectService.getAllProjectsByUser(email);

        if (!projectResponseList.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity(projectResponseList)
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

