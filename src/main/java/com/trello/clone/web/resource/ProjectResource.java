package com.trello.clone.web.resource;

import com.trello.clone.service.ProjectService;
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

@Path("api/project")
public class ProjectResource  {

    private final ProjectService projectService;

    public ProjectResource(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getAllProjectsByEmail(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        List<ProjectResponse> projectResponseList = projectService.getAllProjectsByUserEmail(email);

        return Response.ok()
                .entity(projectResponseList)
                .build();
    }

    @GET
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response getProjectById(@PathParam("projectId") String stringProjectId) {
        ObjectId projectId = new ObjectId(stringProjectId);
        ProjectResponse projectResponse = projectService.getProjectById(projectId);
        return Response.ok()
                .entity(projectResponse)
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response createProject(@Context SecurityContext securityContext,
                                  CreateProjectRequest createProjectRequest) {

        String email = securityContext.getUserPrincipal().getName();
        ProjectResponse projectResponse = projectService.createProject(createProjectRequest, email);

        return Response.status(Response.Status.CREATED)
                .entity(projectResponse)
                .build();
    }

    @PUT
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response updateProject(
            @PathParam("projectId") String stringProjectId,
            @Context SecurityContext securityContext,
            UpdateProjectRequest updateProjectRequest
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ObjectId projectId = new ObjectId(stringProjectId);

        ProjectResponse projectResponse = projectService.updateProject(updateProjectRequest, projectId, email);

        return Response.ok()
                .entity(projectResponse)
                .build();
    }

    @DELETE
    @Path("/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"access_token"})
    public Response deleteProjectById(
            @PathParam("projectId") String stringProjectId,
            @Context SecurityContext securityContext
    ) {

        String email = securityContext.getUserPrincipal().getName();
        ObjectId projectId = new ObjectId(stringProjectId);

        ProjectResponse projectResponse = projectService.deleteProject(projectId, email);
        return Response.ok(projectResponse).build();
    }
}

