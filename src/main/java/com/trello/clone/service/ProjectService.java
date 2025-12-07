package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> getAllProjectsByUserEmail(String email) {
        List<ProjectResponse> projectResponseList = new ArrayList<>();

        List<Project> projects;
        try {
            projects = projectRepository.findProjectsByEmail(email);
        }
        catch (Exception e) {
            throw new GenericException("Failed to gather projects due to server error");
        }

        for (Project project : projects) {
            projectResponseList.add(toProjectResponse(project));
        }

        return projectResponseList;
    }

    public ProjectResponse getProjectById(ObjectId projectId) {
        Project project;
        try {
            project = projectRepository.findById(projectId);
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve project due to server error");
        }

        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }
        return toProjectResponse(project);
    }

    public ProjectResponse createProject(CreateProjectRequest request, String email) {

        List<String> team = new ArrayList<>();
        team.add(email);

        List<String> phases = new ArrayList<>();
        List<String> invitedUsers = new ArrayList<>();

        Project project = new Project(
                request.getName(),
                phases,
                email,
                team,
                invitedUsers
        );

        try {
            projectRepository.persist(project);
        }
        catch (Exception e) {
            throw new GenericException("Failed to create project due to server error: " + e.getMessage());
        }

        return toProjectResponse(project);
    }

    public ProjectResponse updateProject(UpdateProjectRequest request, ObjectId projectId, String email) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }

        try {
            if (request.getName() != null) {
                project.setName(request.getName());
            }

            if (request.getPhases() != null) {
                project.setPhases(request.getPhases());
            }

            // Solo il proprietario può aggiornare se è presente
            if (request.getOwner() != null && request.getOwner().equals(email)) {
                project.setOwner(request.getOwner());
            }

            if (request.getTeam() != null) {
                project.setTeam(request.getTeam());
            }

            if (request.getInvitedUsers() != null) {
                project.setInvitedUsers(request.getInvitedUsers());
            }

            projectRepository.update(project);

        }
        catch (Exception e) {
            throw new GenericException("Failed to update project due to server error: " + e.getMessage());
        }

        return toProjectResponse(project);
    }


    public ProjectResponse deleteProject(ObjectId projectId, String email) {

        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }

        if (!project.getOwner().equals(email)) {
            throw new UnauthorizedException("You are not allowed to delete this project");
        }

        try {
            projectRepository.delete(project);
        }
        catch (Exception e) {
            throw new GenericException("Failed to delete project due to server error: " + e.getMessage());
        }
        return toProjectResponse(project);
    }


    public ProjectResponse toProjectResponse (Project project){

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getPhases(),
                project.getOwner(),
                project.getTeam(),
                project.getInvitedUsers()
        );
    }
}
