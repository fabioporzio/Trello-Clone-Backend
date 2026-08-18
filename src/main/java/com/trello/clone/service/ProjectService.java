package com.trello.clone.service;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.utils.EmailUtils;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectInvitationResponse;
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
    private final NotificationRepository notificationRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            NotificationRepository notificationRepository
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
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

        List<String> justInvited = new ArrayList<>();
        List<String> justRevoked = new ArrayList<>();
        List<String> justRemoved = new ArrayList<>();

        if (isNotEmpty(request.getUsersToInvite())) {
            requireOwner(actorIsOwner);
            for (String invitee : normalizedEmails(request.getUsersToInvite())) {
                project.invite(invitee);
                justInvited.add(invitee);
            }
        }

        if (isNotEmpty(request.getInvitesToRevoke())) {
            requireOwner(actorIsOwner);
            for (String invitee : normalizedEmails(request.getInvitesToRevoke())) {
                project.revokeInvite(invitee);
                justRevoked.add(invitee);
            }
        }

        if (isNotEmpty(request.getMembersToRemove())) {
            for (String target : normalizedEmails(request.getMembersToRemove())) {
                removeMember(project, actor, target, actorIsOwner);
                justRemoved.add(target);
            }
        }

        project.touch();
        update(project);
        notifyInvites(project, actor, justInvited, justRevoked, justRemoved);

        return toProjectResponse(project);
    }

    // DELETE

    public void deleteProject(ObjectId projectId, String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        Project project = requireProject(projectId);
        requireOwner(project.isOwner(normalizedEmail));

        List<Task> tasks = taskRepository.list("projectId", projectId);

        try {
            taskRepository.deleteByProject(projectId);
            projectRepository.delete(project);
        }
        catch (Exception e) {
            Log.errorf(e, "Failed to delete project %s", projectId);
            throw new GenericException("Failed to delete project due to server error");
        }

        cleanUpNotifications(project, tasks);
    }

    // NOTIFICATIONS MANAGEMENT

    private void notifyInvites(
            Project project,
            String actor,
            List<String> invited,
            List<String> revoked,
            List<String> removed
    ) {
        if (invited.isEmpty() && revoked.isEmpty()) {
            return;
        }

        String projectId = project.getId().toHexString();
        for (String invitee : invited) {
            try {
                notificationRepository.addProjectInvite(invitee, project, actor);
            } catch (Exception e) {
                Log.errorf(e, "Invitation was saved but no notification has been sent to %s", invitee);
            }
        }

        for (String invitee : revoked) {
            try {
                notificationRepository.removeProjectInvite(invitee, projectId);
            } catch (Exception e) {
                Log.errorf(e, "Invitation was revoked but its notification was not removed for %s", invitee);
            }
        }

        for (String invitee : removed) {
            try {
                notificationRepository.removeProjectInvite(invitee, projectId);
            } catch (Exception e) {
                Log.errorf(e, "Invitation was removed but no notification has been sent to %s", invitee);
            }
        }
    }

    private void cleanUpNotifications(Project project, List<Task> tasks) {
        String projectId = project.getId().toHexString();

        try {
            Set<String> everyone = new LinkedHashSet<>(project.getTeam());
            everyone.addAll(project.getInvitedUsers());

            for (String user : everyone) {
                notificationRepository.removeProjectInvite(user, projectId);
            }

            for (Task task : tasks) {
                String taskId = task.getId().toHexString();
                for (String assignee : task.getAssignees()) {
                    notificationRepository.removeTaskAssignment(assignee, taskId);
                }
            }
        }
        catch (Exception e) {
            Log.errorf(e, "Project %s deleted but its notifications were not cleaned up", projectId);
        }
    }

    // INVITATION MANAGEMENT

    public List<ProjectInvitationResponse> getPendingInvitations(String email) {
        String actor = EmailUtils.normalize(email);

        List<Project> projects;
        try {
            projects = projectRepository.findByPendingInvite(actor);
        }
        catch (Exception e) {
            Log.error("Failed to gather pending invitations", e);
            throw new GenericException("Failed to gather invitations due to server error");
        }

        List<ProjectInvitationResponse> responses = new ArrayList<>(projects.size());
        for (Project project : projects) {
            responses.add(new ProjectInvitationResponse(
                    project.getId().toHexString(),
                    project.getName(),
                    project.getOwner()
            ));
        }
        return responses;
    }

    public ProjectResponse acceptInvite(ObjectId projectId, String email) {
        String actor = EmailUtils.normalize(email);
        Project project = requireProject(projectId);

        if (!project.isInvited(actor)) {
            throw new NotFoundException("No pending invitation for this project");
        }

        project.acceptInvite(actor);
        project.touch();
        update(project);

        try {
            notificationRepository.removeProjectInvite(actor, projectId.toHexString());
        }
        catch (Exception e) {
            Log.errorf(e, "Invite notification was not removed for user %s", actor);
        }

        return toProjectResponse(project);
    }

    public void declineInvite(ObjectId projectId, String email) {
        String actor = EmailUtils.normalize(email);
        Project project = requireProject(projectId);

        if (!project.isInvited(actor)) {
            throw new NotFoundException("No pending invitation for this project");
        }

        project.revokeInvite(actor);
        project.touch();
        update(project);

        try {
            notificationRepository.removeProjectInvite(actor, projectId.toHexString());
        }
        catch (Exception e) {
            Log.errorf(e, "Invite declined but its notification was not removed for %s", actor);
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
