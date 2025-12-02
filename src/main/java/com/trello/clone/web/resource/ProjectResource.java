package com.trello.clone.web.resource;

import com.trello.clone.service.ProjectService;
import com.trello.clone.web.model.ErrorResponse;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;

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

    @GET
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getAllProjectsByUser(@PathParam("projectId") String stringProjectId) {
        ObjectId projectId = new ObjectId(stringProjectId);
        ProjectResponse projectResponse = projectService.getProjectById(projectId);

        if (projectResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(projectResponse)
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
    public Response createProject(
            @Context SecurityContext securityContext,
            CreateProjectRequest createProjectRequest
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ProjectResponse projectResponse = projectService.createProject(createProjectRequest, email);

        if (projectResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(projectResponse)
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
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateProject(
            @PathParam("projectId") String stringProjectId,
            UpdateProjectRequest updateProjectRequest
    ) {
        ObjectId projectId = new ObjectId(stringProjectId);
        ProjectResponse projectResponse = projectService.updateProject(updateProjectRequest, projectId);

        if (projectResponse != null) {
            return Response.status(Response.Status.OK)
                    .entity(projectResponse)
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

