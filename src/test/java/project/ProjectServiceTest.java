package project;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.ProjectService;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.web.model.project.CreateProjectRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, taskRepository);
    }

    private Project board() {
        Project project = Project.create("Board", OWNER);
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
        verify(taskRepository, never()).delete("projectId", PROJECT_ID);
    }

    @Test
    void deleteProject_theOwnerAlsoRemovesItsTasks() {
        Project project = board();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(project);

        projectService.deleteProject(PROJECT_ID, OWNER);

        verify(taskRepository).delete("projectId", new Object[]{PROJECT_ID});
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
}
