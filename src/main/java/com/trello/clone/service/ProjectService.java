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

    public ProjectResponse updateProject(UpdateProjectRequest updateProjectRequest, ObjectId projectId) {
        Project project = projectRepository.findById(projectId);

        if (project != null) {
            project.setName(updateProjectRequest.getName());
            project.setTeam(updateProjectRequest.getTeam());
            project.setPhases(updateProjectRequest.getPhases());
            projectRepository.update(project);
            return toProjectResponse(project);
        }
        else {
            return null;
        }
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
