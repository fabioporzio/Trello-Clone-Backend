package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
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

    public List<ProjectResponse> getAllProjectsByUser(String email) {
        List<ProjectResponse> projectResponseList = new ArrayList<>();

        List<Project> projects = projectRepository.findByEmailOrTeam(email);

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

        Project project = new Project(
                createProjectRequest.getName(),
                phases,
                email,
                team
        );

        projectRepository.persist(project);

        return toProjectResponse(project);
    }

    public ProjectResponse toProjectResponse(Project project) {

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getPhases(),
                project.getOwner(),
                project.getTeam()
        );
    }
}
