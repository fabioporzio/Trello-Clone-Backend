package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.web.model.project.ProjectResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

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
