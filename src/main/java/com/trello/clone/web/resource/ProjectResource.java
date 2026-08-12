package com.trello.clone.web.resource;

import com.trello.clone.service.ProjectService;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
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

import java.util.List;

@Path("api/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource  {

    private final ProjectService projectService;

    public ProjectResource(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Gets all projects by User email",
            description = "Validates JWT access token and returns all user's projects based on email in JWT."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    public Response getAllProjectsByEmail(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        List<ProjectResponse> projectResponseList = projectService.getAllProjectsByUserEmail(email);

        return Response.ok()
                .entity(projectResponseList)
                .build();
    }

    @GET
    @Path("/{projectId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Gets project details upon project ID",
            description = "Validates JWT access token and returns project details based on project ID."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Retrieval successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "No project found")
    public ProjectResponse getProjectById(
            @PathParam("projectId") ObjectId projectId,
            @Context SecurityContext securityContext
    ) {
        return projectService.getProjectById(projectId, securityContext.getUserPrincipal().getName());
    }

    @POST
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Creates a project",
            description = "Validates JWT access token and creates a project."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "201",
            description = "Creation successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Bad request")
    @APIResponse(responseCode = "401", description = "Token is expired")
    public Response createProject(
            @Context SecurityContext securityContext,
            @Valid CreateProjectRequest createProjectRequest
    ) {
        String email = securityContext.getUserPrincipal().getName();
        ProjectResponse projectResponse = projectService.createProject(createProjectRequest, email);

        return Response.status(Response.Status.CREATED)
                .entity(projectResponse)
                .build();
    }

    @PUT
    @Path("/{projectId}")
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Updates a project details",
            description = "Validates JWT access token and updates a project based on the provided details."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Update successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Bad request")
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "No project found")
    public Response updateProject(
            @PathParam("projectId") String stringProjectId,
            @Context SecurityContext securityContext,
            @Valid UpdateProjectRequest updateProjectRequest
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
    @RolesAllowed({"access_token"})
    @Operation(
            summary = "Deletes a project",
            description = "Validates JWT access token and deletes a project based on given project ID."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "Delete successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Token is expired")
    @APIResponse(responseCode = "404", description = "No project found")
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

