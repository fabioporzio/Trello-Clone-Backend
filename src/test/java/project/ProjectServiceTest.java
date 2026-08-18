package project;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.ProjectService;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.GenericException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.web.model.project.CreateProjectRequest;
import com.trello.clone.web.model.project.ProjectInvitationResponse;
import com.trello.clone.web.model.project.ProjectResponse;
import com.trello.clone.web.model.project.UpdateProjectRequest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final ObjectId PROJECT_ID = new ObjectId();

    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String OUTSIDER = "outsider@example.com";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, taskRepository, notificationRepository);
    }

    private Project board() {
        Project project = Project.create("Board", OWNER);
        project.setId(PROJECT_ID);
        project.addPhases(List.of("To Do", "Doing"));
        project.invite(MEMBER);
        project.acceptInvite(MEMBER);
        return project;
    }

    // READS

    @Test
    void getProjectById_projectNotFound_throwsNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> projectService.getProjectById(PROJECT_ID, MEMBER));
    }

    @Test
    void getProjectById_callerIsNotAMember_throwsNotFoundNotUnauthorized() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        assertThrows(NotFoundException.class,
                () -> projectService.getProjectById(PROJECT_ID, OUTSIDER));
    }

    @Test
    void getProjectById_normalisesTheEmail() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        assertEquals("Board",
                projectService.getProjectById(PROJECT_ID, "  MEMBER@Example.COM ").getName());
    }

    // CREATE

    @Test
    void createProject_normalisesTheOwnerAndAddsHimToTheTeam() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("  New board  ");

        projectService.createProject(request, "  OWNER@Example.COM ");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).persist(captor.capture());

        Project persisted = captor.getValue();
        assertEquals("New board", persisted.getName());
        assertEquals(OWNER, persisted.getOwner());
        assertTrue(persisted.isMember(OWNER));
    }

    // UPDATE

    @Test
    void updateProject_canTransferOwnershipAndInviteInTheSameRequest() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setNewOwner(MEMBER);
        request.setUsersToInvite(Set.of(OUTSIDER));

        projectService.updateProject(request, PROJECT_ID, OWNER);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).update(captor.capture());

        Project updated = captor.getValue();
        assertTrue(updated.isOwner(MEMBER));
        assertTrue(updated.isInvited(OUTSIDER));
    }

    @Test
    void updateProject_aPlainMemberCannotInvite() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setUsersToInvite(Set.of(OUTSIDER));

        assertThrows(UnauthorizedException.class,
                () -> projectService.updateProject(request, PROJECT_ID, MEMBER));

        verify(projectRepository, never()).update(any(Project.class));
    }

    @Test
    void updateProject_aMemberCanRemoveHimself() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setMembersToRemove(Set.of(MEMBER));

        projectService.updateProject(request, PROJECT_ID, MEMBER);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).update(captor.capture());
        assertTrue(!captor.getValue().isMember(MEMBER));
    }

    @Test
    void updateProject_aMemberCannotRemoveSomeoneElse() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setMembersToRemove(Set.of(OWNER));

        assertThrows(UnauthorizedException.class,
                () -> projectService.updateProject(request, PROJECT_ID, MEMBER));
    }

    @Test
    void updateProject_contradictoryPhases_isRejectedBeforeAnyMutation() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("New name");
        request.setPhasesToAdd(Set.of("Review"));
        request.setPhasesToRemove(Set.of("review"));

        assertThrows(BadRequestException.class,
                () -> projectService.updateProject(request, PROJECT_ID, OWNER));

        verify(projectRepository, never()).update(any(Project.class));
    }

    // PHASE and CASCADES

    @Test
    void updateProject_removingAPhaseThatStillHasTasks_isRejected() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.countByPhase(PROJECT_ID, "Doing")).thenReturn(3L);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setPhasesToRemove(Set.of("doing"));

        assertThrows(BadRequestException.class,
                () -> projectService.updateProject(request, PROJECT_ID, OWNER));

        verify(projectRepository, never()).update(any(Project.class));
    }

    @Test
    void updateProject_removingAnEmptyPhase_succeeds() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.countByPhase(PROJECT_ID, "Doing")).thenReturn(0L);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setPhasesToRemove(Set.of("doing"));

        projectService.updateProject(request, PROJECT_ID, OWNER);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).update(captor.capture());
        assertEquals(List.of("To Do"), captor.getValue().getPhases());
    }

    @Test
    void updateProject_phaseRename_cascadesToTasksWithTheCanonicalName() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setPhaseRenames(Map.of("  doing  ", "In development"));

        projectService.updateProject(request, PROJECT_ID, OWNER);

        verify(taskRepository).renamePhase(PROJECT_ID, "Doing", "In development");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).update(captor.capture());
        assertEquals(List.of("To Do", "In development"), captor.getValue().getPhases());
    }

    @Test
    void updateProject_renamingAnUnknownPhase_isRejectedWithoutTouchingTasks() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setPhaseRenames(Map.of("Ghost Phase", "Mew"));

        assertThrows(BadRequestException.class,
                () -> projectService.updateProject(request, PROJECT_ID, OWNER));

        verify(taskRepository, never()).renamePhase(any(), anyString(), anyString());
    }

    // INVITES

    @Test
    void acceptInvite_withoutAPendingInvitation_throwsNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        assertThrows(NotFoundException.class,
                () -> projectService.acceptInvite(PROJECT_ID, OUTSIDER));
    }

    @Test
    void acceptInvite_movesTheUserIntoTheTeam() {
        Project project = Project.create("Board", OWNER);
        project.invite(OUTSIDER);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.acceptInvite(PROJECT_ID, OUTSIDER);

        assertTrue(project.isMember(OUTSIDER));
        verify(projectRepository).update(project);
    }

    // DELETE

    @Test
    void deleteProject_aPlainMemberCannotDelete() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        assertThrows(UnauthorizedException.class,
                () -> projectService.deleteProject(PROJECT_ID, MEMBER));

        verify(projectRepository, never()).delete(any(Project.class));
        verify(taskRepository, never()).deleteByProject(any());
    }

    @Test
    void deleteProject_theOwnerAlsoRemovesItsTasks() {
        Project project = board();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.deleteProject(PROJECT_ID, OWNER);

        verify(taskRepository).deleteByProject(PROJECT_ID);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_normalisesTheEmail() {
        Project project = board();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.deleteProject(PROJECT_ID, "  OWNER@Example.COM ");

        verify(projectRepository).delete(project);
    }

    // LISTS

    @Test
    void getAllProjectsByUserEmail_normalisesTheEmailBeforeQuerying() {
        when(projectRepository.findProjectsByEmail(eq(MEMBER))).thenReturn(List.of(board()));

        assertEquals(1, projectService.getAllProjectsByUserEmail("  MEMBER@Example.COM ").size());
    }

    // NOTIFICATION TESTS

    @Test
    void updateProject_invitingSomeoneSendsTheNotification() {
        Project project = board();
        project.setId(PROJECT_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setUsersToInvite(Set.of(OUTSIDER));

        projectService.updateProject(request, PROJECT_ID, OWNER);

        verify(notificationRepository).addProjectInvite(OUTSIDER, project, OWNER);
    }

    @Test
    void updateProject_revokingAnInviteRemovesTheNotification() {
        Project project = board();
        project.setId(PROJECT_ID);
        project.invite(OUTSIDER);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setInvitesToRevoke(Set.of(OUTSIDER));

        projectService.updateProject(request, PROJECT_ID, OWNER);

        verify(notificationRepository).removeProjectInvite(OUTSIDER, PROJECT_ID.toHexString());
    }

    @Test
    void updateProject_succeedsEvenIfTheNotificationFails() {
        Project project = board();
        project.setId(PROJECT_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);
        doThrow(new RuntimeException("Redis down"))
                .when(notificationRepository).addProjectInvite(anyString(), any(Project.class), anyString());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setUsersToInvite(Set.of(OUTSIDER));

        ProjectResponse response = projectService.updateProject(request, PROJECT_ID, OWNER);

        assertTrue(response.getInvitedUsers().contains(OUTSIDER));
        verify(projectRepository).update(project);
    }

    @Test
    void updateProject_doesNotNotifyIfTheSaveFails() {
        Project project = board();
        project.setId(PROJECT_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);
        doThrow(new RuntimeException("Mongo down"))
                .when(projectRepository).update(any(Project.class));

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setUsersToInvite(Set.of(OUTSIDER));

        assertThrows(GenericException.class,
                () -> projectService.updateProject(request, PROJECT_ID, OWNER));

        verify(notificationRepository, never())
                .addProjectInvite(anyString(), any(Project.class), anyString());
    }

    @Test
    void acceptInvite_clearsTheInvitationNotification() {
        Project project = Project.create("Board", OWNER);
        project.setId(PROJECT_ID);
        project.invite(OUTSIDER);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.acceptInvite(PROJECT_ID, OUTSIDER);

        verify(notificationRepository).removeProjectInvite(OUTSIDER, PROJECT_ID.toHexString());
    }

    @Test
    void declineInvite_clearsTheInvitationAndItsNotification() {
        Project project = Project.create("Board", OWNER);
        project.setId(PROJECT_ID);
        project.invite(OUTSIDER);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.declineInvite(PROJECT_ID, OUTSIDER);

        assertFalse(project.isInvited(OUTSIDER));
        assertFalse(project.isMember(OUTSIDER), "Refusal must not allow to enter the team");
        verify(notificationRepository).removeProjectInvite(OUTSIDER, PROJECT_ID.toHexString());
    }

    @Test
    void getPendingInvitations_onlyExposesNameAndOwner() {
        Project project = board();
        project.setId(PROJECT_ID);
        project.invite(OUTSIDER);
        when(projectRepository.findByPendingInvite(OUTSIDER)).thenReturn(List.of(project));

        List<ProjectInvitationResponse> invitations =
                projectService.getPendingInvitations(OUTSIDER);

        assertEquals(1, invitations.size());
        assertEquals("Board", invitations.getFirst().getName());
        assertEquals(OWNER, invitations.getFirst().getOwner());
    }
}
