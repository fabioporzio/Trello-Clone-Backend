package task;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.DeadlineRepository;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.service.TaskService;
import com.trello.clone.service.exception.BadRequestException;
import com.trello.clone.service.exception.NotFoundException;
import com.trello.clone.service.exception.UnauthorizedException;
import com.trello.clone.web.model.task.CreateTaskRequest;
import com.trello.clone.web.model.task.TaskResponse;
import com.trello.clone.web.model.task.UpdateTaskRequest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final ObjectId PROJECT_ID = new ObjectId();
    private static final ObjectId TASK_ID = new ObjectId();

    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String OUTSIDER = "outsider@example.com";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeadlineRepository deadlineRepository;

    @Mock
    private ProjectRepository projectRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, deadlineRepository, projectRepository);
    }

    // FIXTURES

    private Project board() {
        Project project = Project.create("Board", OWNER);
        project.addPhases(List.of("To Do", "Doing"));
        project.invite(MEMBER);
        project.acceptInvite(MEMBER);
        return project;
    }

    private Task taskInToDo() {
        return Task.create("Fix login", null, "To Do", PROJECT_ID);
    }

    // READ & ACCESS

    @Test
    void getTaskById_projectNotFound_throwsNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> taskService.getTaskById(MEMBER, PROJECT_ID, TASK_ID));
    }

    @Test
    void getTaskById_callerIsNotAMember_throwsUnauthorized() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        assertThrows(UnauthorizedException.class,
                () -> taskService.getTaskById(OUTSIDER, PROJECT_ID, TASK_ID));

        verify(taskRepository, never()).findByIdAndProject(any(), any());
    }

    /** Case: task exists but belongs to another project. */
    @Test
    void getTaskById_taskBelongsToAnotherProject_throwsNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> taskService.getTaskById(MEMBER, PROJECT_ID, TASK_ID));
    }

    @Test
    void getTaskById_emailWithUppercase_isNormalised() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(taskInToDo());

        TaskResponse response = taskService.getTaskById("  MEMBER@Example.COM ", PROJECT_ID, TASK_ID);

        assertEquals("Fix login", response.getTitle());
    }

    // CREATE

    @Test
    void createTask_unknownPhase_throwsBadRequest() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Fix login");
        request.setPhase("Non Existing");

        assertThrows(BadRequestException.class,
                () -> taskService.createTask(request, MEMBER, PROJECT_ID));

        verify(taskRepository, never()).persist(any(Task.class));
    }

    @Test
    void createTask_canonicalisesThePhase() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Fix login");
        request.setPhase("  to do  ");

        taskService.createTask(request, MEMBER, PROJECT_ID);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).persist(captor.capture());
        assertEquals("To Do", captor.getValue().getPhase());
    }

    // UPDATE

    @Test
    void updateTask_contradictoryTags_isRejectedBeforeAnyMutation() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(taskInToDo());

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("New title");
        request.setTagsToAdd(List.of("Bug"));
        request.setTagsToRemove(List.of("bug"));

        assertThrows(BadRequestException.class,
                () -> taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID));

        verify(taskRepository, never()).update(any(Task.class));
    }

    @Test
    void updateTask_assigneeIsNotAProjectMember_throwsBadRequest() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(taskInToDo());

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneesToAdd(Set.of(OUTSIDER));

        assertThrows(BadRequestException.class,
                () -> taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID));

        verify(taskRepository, never()).update(any(Task.class));
    }

    @Test
    void updateTask_unknownPhase_throwsBadRequest() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(taskInToDo());

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setPhase("Non existing");

        assertThrows(BadRequestException.class,
                () -> taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID));
    }

    @Test
    void updateTask_memberCanEditATaskAssignedToSomeoneElse() {
        Task task = taskInToDo();
        task.assign(Set.of(OWNER));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Title updated");

        TaskResponse response = taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        assertEquals("Title updated", response.getTitle());
        verify(taskRepository).update(task);
    }

    // DELETE

    @Test
    void deleteTask_callerIsNeitherOwnerNorAssignee_throwsUnauthorized() {
        Task task = taskInToDo();
        task.assign(Set.of(OWNER));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        assertThrows(UnauthorizedException.class,
                () -> taskService.deleteTask(MEMBER, PROJECT_ID, TASK_ID));

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void deleteTask_ownerCanAlwaysDelete() {
        Task task = taskInToDo();
        task.assign(Set.of(MEMBER));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(OWNER, PROJECT_ID, TASK_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_assigneeCanDelete() {
        Task task = taskInToDo();
        task.assign(Set.of(MEMBER));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(MEMBER, PROJECT_ID, TASK_ID);

        verify(taskRepository).delete(task);
    }

    // GROUP BY

    @Test
    void getAllTasksByProject_returnsEveryPhaseInBoardOrder() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.getTasksByProjectIdTagsAndAssignees(eq(PROJECT_ID), any(), any()))
                .thenReturn(List.of(taskInToDo()));

        Map<String, List<TaskResponse>> grouped =
                taskService.getAllTasksByProject(MEMBER, PROJECT_ID, null, null);

        assertEquals(List.of("To Do", "Doing"), new ArrayList<>(grouped.keySet()));
        assertEquals(1, grouped.get("To Do").size());
        assertTrue(grouped.get("Doing").isEmpty());
    }

    @Test
    void getAllTasksByProject_orphanPhase_getsItsOwnBucketAtTheEnd() {
        Task orphan = Task.create("Old", null, "Ghost Phase", PROJECT_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.getTasksByProjectIdTagsAndAssignees(eq(PROJECT_ID), any(), any()))
                .thenReturn(List.of(orphan));

        Map<String, List<TaskResponse>> grouped =
                taskService.getAllTasksByProject(MEMBER, PROJECT_ID, null, null);

        assertEquals(List.of("To Do", "Doing", "Ghost Phase"), new ArrayList<>(grouped.keySet()));
        assertEquals(1, grouped.get("Ghost Phase").size());
    }
}
