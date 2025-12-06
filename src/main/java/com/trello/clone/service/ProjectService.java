package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> getAllProjectsByUserEmail(String email) {
        List<ProjectResponse> projectResponseList = new ArrayList<>();

        List<Project> projects = projectRepository.findProjectsByEmail(email);

        for (Project project : projects) {
            projectResponseList.add(toProjectResponse(project));
        }

        return projectResponseList;
    }

    public ProjectResponse getProjectById(ObjectId projectId) {
        Project project = projectRepository.findById(projectId);

        return toProjectResponse(project);
    }

    public ProjectResponse createProject(CreateProjectRequest createProjectRequest, String email) {
        List<String> team = new ArrayList<>();
        team.add(email);

        List<String> phases = new ArrayList<>();

        List<String> invitedUsers = new ArrayList<>();

        Project project = new Project(
                createProjectRequest.getName(),
                phases,
                email,
                team,
                invitedUsers
        );

        projectRepository.persist(project);

        return toProjectResponse(project);
    }


    public ProjectResponse updateProject(UpdateProjectRequest updateProjectRequest, ObjectId projectId, String email) {
        Project project = projectRepository.findById(projectId);

        if (project == null) {
            return null;
        }

        if (updateProjectRequest.getName() != null) {
            project.setName(updateProjectRequest.getName());
        }

        if (updateProjectRequest.getPhases() != null) {
            project.setPhases(updateProjectRequest.getPhases());
        }

        if (Objects.equals(updateProjectRequest.getOwner(), email)) {
            project.setOwner(updateProjectRequest.getOwner());
        }

        if (updateProjectRequest.getTeam() != null) {
            project.setTeam(updateProjectRequest.getTeam());
        }

        if (updateProjectRequest.getInvitedUsers() != null) {
            project.setInvitedUsers(updateProjectRequest.getInvitedUsers());
        }

        projectRepository.update(project);

        return toProjectResponse(project);
    }

    public ProjectResponse deleteProject(ObjectId projectId, String email) {
        Project project = projectRepository.findById(projectId);

        if (project != null) {
            if (Objects.equals(project.getOwner(), email)) {
                projectRepository.delete(project);
                return toProjectResponse(project);
            }
            else {
                return null;
            }
        }
        else  {
            return null;
        }
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
