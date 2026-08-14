package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.utils.EmailUtils;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.*;

import static com.trello.clone.utils.RequestDeltas.normalizedEmails;
import static com.trello.clone.utils.RequestDeltas.rejectOverlap;

@ApplicationScoped
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectService(
            ProjectRepository projectRepository,
             TaskRepository taskRepository
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public List<ProjectResponse> getAllProjectsByUserEmail(String email) {
        List<Project> projects;
        try {
            projects = projectRepository.findProjectsByEmail(EmailUtils.normalize(email));
        }
        catch (Exception e) {
            Log.error("Failed to gather projects", e);
            throw new GenericException("Failed to gather projects due to server error");
        }

        List<ProjectResponse> responses = new ArrayList<>(projects.size());
        for (Project project : projects) {
            responses.add(toProjectResponse(project));
        }
        return responses;
    }

    public ProjectResponse getProjectById(ObjectId projectId, String email) {
        String actor = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, actor);
        return toProjectResponse(project);
    }

    // CREATE

    public ProjectResponse createProject(
            CreateProjectRequest request,
            String email
    ) {
        String owner = EmailUtils.normalize(email);

        Project project = Project.create(
                request.getName(),
                owner
        );

        persist(project);
        return toProjectResponse(project);
    }

    // UPDATE

    public ProjectResponse updateProject(UpdateProjectRequest request, ObjectId projectId, String email) {
        String actor = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireMember(project, actor);

        boolean actorIsOwner = project.isOwner(actor);

        // Reject contradictory instructions before mutating anything, so a bad
        // request never leaves the project half-updated.
        rejectOverlap(request.getPhasesToAdd(), request.getPhasesToRemove(), "phase");
        rejectOverlap(request.getUsersToInvite(), request.getInvitesToRevoke(), "user");
        rejectOverlap(request.getUsersToInvite(), request.getMembersToRemove(), "user");

        if (request.getName() != null) {
            project.rename(request.getName());
        }

        // Order matters: remove before add (frees a name for reuse in the same
        // call), rename before reorder (so the ordering can use the new names).
        if (isNotEmpty(request.getPhasesToRemove())) {
            List<String> toRemove = new ArrayList<>();
            for (String phase : request.getPhasesToRemove()) {
                String canonical = project.requireExistingPhase(phase);
                requireEmptyPhase(projectId, canonical);
                toRemove.add(canonical);
            }
            project.removePhases(toRemove);
        }

        if (isNotEmpty(request.getPhasesToAdd())) {
            project.addPhases(request.getPhasesToAdd());
        }

        if (request.getPhaseRenames() != null) {
            for (Map.Entry<String, String> rename : request.getPhaseRenames().entrySet()) {
                String current = project.requireExistingPhase(rename.getKey());
                taskRepository.renamePhase(projectId, current, rename.getValue().trim());
                project.renamePhase(current, rename.getValue());
            }
        }

        if (request.getPhaseOrder() != null) {
            project.reorderPhases(request.getPhaseOrder());
        }

        if (request.getNewOwner() != null) {
            requireOwner(actorIsOwner);
            project.transferOwnershipTo(EmailUtils.normalize(request.getNewOwner()));
        }

        if (isNotEmpty(request.getUsersToInvite())) {
            requireOwner(actorIsOwner);
            for (String invitee : normalizedEmails(request.getUsersToInvite())) {
                project.invite(invitee);
            }
        }

        if (isNotEmpty(request.getInvitesToRevoke())) {
            requireOwner(actorIsOwner);
            for (String invitee : normalizedEmails(request.getInvitesToRevoke())) {
                project.revokeInvite(invitee);
            }
        }

        if (isNotEmpty(request.getMembersToRemove())) {
            for (String target : normalizedEmails(request.getMembersToRemove())) {
                removeMember(project, actor, target, actorIsOwner);
            }
        }

        project.touch();
        update(project);
        return toProjectResponse(project);
    }

    // INVITATION FLOW

    public ProjectResponse acceptInvite(ObjectId projectId, String email) {
        String actor = EmailUtils.normalize(email);
        Project project = requireProject(projectId);

        if (!project.isInvited(actor)) {
            throw new NotFoundException("No pending invitation for this project");
        }

        project.acceptInvite(actor);
        project.touch();
        update(project);
        return toProjectResponse(project);
    }

    public void deleteProject(ObjectId projectId, String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireOwner(project.isOwner(normalizedEmail));

        try {
            long deleted = taskRepository.delete("projectId", projectId);
            Log.infof("Deleted %d tasks", deleted);
            projectRepository.delete(project);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to delete project %s", project.getId());
            throw new GenericException("Failed to delete project due to server error");
        }
    }

    // RULES

    private void requireOwner(boolean actorIsOwner) {
        if (!actorIsOwner) {
            throw new UnauthorizedException("Only the project owner can perform this operation");
        }
    }

    /**
     * Removing another member requires ownership; removing yourself is always
     * allowed. The "owner cannot be removed" invariant lives on the entity.
     */
    private void removeMember(Project project, String actor, String target, boolean actorIsOwner) {
        if (!actor.equals(target)) {
            requireOwner(actorIsOwner);
        }
        project.removeMember(target);
    }

    private Project requireProject(ObjectId projectId) {
        Project project = projectRepository.findById(projectId);

        if (project == null) {
            throw new NotFoundException("Project with ID " + projectId + " not found");
        }

        return project;
    }

    private void requireMember(Project project, String actor) {
        if (!project.isMember(actor)) {
            throw new NotFoundException("Project with ID " + project.getId() + " not found");
        }
    }

    private void requireEmptyPhase(ObjectId projectId, String phase) {
        long count = taskRepository.countByPhase(projectId, phase);
        if (count > 0) {
            throw new BadRequestException("Cannot remove phase '" + phase + "': "
                    + count + " task(s) still in it. Move them first.");
        }
    }

    // PERSISTENCE

    private void persist(Project project) {
        try {
            projectRepository.persist(project);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to persist project '%s' owned by %s", project.getName(), project.getOwner());
            throw new GenericException("Failed to create project due to server error");
        }
    }

    private void update(Project project) {
        try {
            projectRepository.update(project);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to update project %s", project.getId());
            throw new GenericException("Failed to update project due to server error");
        }
    }

    // UTILS

    private static boolean isNotEmpty(Collection<String> values) {
        return values != null && !values.isEmpty();
    }

    private ProjectResponse toProjectResponse (Project project){
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                List.copyOf(project.getPhases()),
                project.getOwner(),
                new LinkedHashSet<>(project.getTeam()),
                new LinkedHashSet<>(project.getInvitedUsers())
        );
    }
}
