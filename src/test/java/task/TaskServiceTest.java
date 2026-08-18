package task;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.DeadlineRepository;
import com.trello.clone.data.repository.NotificationRepository;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private ProjectRepository projectRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeadlineRepository deadlineRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectRepository, notificationRepository, deadlineRepository);
    }

    // FIXTURES

    private Project board() {
        Project project = Project.create("Board", OWNER);
        project.setId(PROJECT_ID);
        project.addPhases(List.of("To Do", "Doing"));
        project.invite(MEMBER);
        project.acceptInvite(MEMBER);
        return project;
    }

    private Task taskInToDo() {
        Task task = Task.create("Fix login", null, "To Do", PROJECT_ID);
        task.setId(TASK_ID);
        return task;
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

    // NOTIFICATION TESTS

    @Test
    void updateTask_assigningSomeoneSendsTheNotification() {
        Task task = taskInToDo();
        task.setId(TASK_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneesToAdd(Set.of(OWNER));

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(notificationRepository).addTaskAssignment(OWNER, task, MEMBER);
    }

    @Test
    void updateTask_assigningYourselfSendsNoNotification() {
        Task task = taskInToDo();
        task.setId(TASK_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneesToAdd(Set.of(MEMBER));

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(notificationRepository, never())
                .addTaskAssignment(anyString(), any(Task.class), anyString());
    }

    @Test
    void updateTask_unassigningSomeoneRemovesTheNotification() {
        Task task = taskInToDo();
        task.setId(TASK_ID);
        task.assign(Set.of(OWNER));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneesToRemove(Set.of(OWNER));

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(notificationRepository).removeTaskAssignment(OWNER, TASK_ID.toHexString());
    }

    @Test
    void deleteTask_clearsTheNotificationsOfItsAssignees() {
        Task task = taskInToDo();
        task.setId(TASK_ID);
        task.assign(Set.of(MEMBER, OWNER));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(OWNER, PROJECT_ID, TASK_ID);

        verify(notificationRepository).removeTaskAssignment(MEMBER, TASK_ID.toHexString());
        verify(notificationRepository).removeTaskAssignment(OWNER, TASK_ID.toHexString());
    }

    @Test
    void updateTask_succeedsEvenIfTheNotificationFails() {
        Task task = taskInToDo();
        task.setId(TASK_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);
        doThrow(new RuntimeException("Redis down"))
                .when(notificationRepository).addTaskAssignment(anyString(), any(Task.class), anyString());

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneesToAdd(Set.of(OWNER));

        TaskResponse response = taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        assertTrue(response.getAssignees().contains(OWNER));
        verify(taskRepository).update(task);
    }

    // DEADLINE TESTS

    @Test
    void deleteTask_cancelsTheScheduledDeadline() {
        Task task = taskInToDo();
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(OWNER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository).cancel(TASK_ID);
    }

    @Test
    void deleteTask_clearsTheDeadlineNotificationOfItsAssignees() {
        Task task = taskInToDo();
        task.assign(Set.of(MEMBER));
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(OWNER, PROJECT_ID, TASK_ID);

        verify(notificationRepository).removeDeadlineNotification(MEMBER, TASK_ID.toHexString());
    }

    @Test
    void deleteTask_withoutAssignees_clearsTheWholeTeam() {
        Task task = taskInToDo();
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        taskService.deleteTask(OWNER, PROJECT_ID, TASK_ID);

        verify(notificationRepository).removeDeadlineNotification(OWNER, TASK_ID.toHexString());
        verify(notificationRepository).removeDeadlineNotification(MEMBER, TASK_ID.toHexString());
    }

    @Test
    void updateTask_completingATaskClearsItsDeadline() {
        Task task = taskInToDo();
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setCompleted(true);

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository).cancel(TASK_ID);
    }

    @Test
    void updateTask_completingAnAlreadyCompletedTaskDoesNothing() {
        Task task = taskInToDo();
        task.setCompleted(true);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setCompleted(true);

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository, never()).cancel(any());
        verify(notificationRepository, never())
                .removeDeadlineNotification(anyString(), anyString());
    }

    @Test
    void updateTask_reopeningATaskDoesNotClearAnything() {
        Task task = taskInToDo();
        task.setCompleted(true);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setCompleted(false);

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository, never()).cancel(any());
    }

    @Test
    void updateTask_withoutTouchingCompleted_leavesTheDeadlineAlone() {
        Task task = taskInToDo();
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Title updated");

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository, never()).cancel(any());
    }

    @Test
    void updateTask_settingADeadlineSchedulesIt() {
        Task task = taskInToDo();
        LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(5);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setEndDate(deadline);

        taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        verify(deadlineRepository).scheduleNotification(TASK_ID, deadline);
    }

    @Test
    void updateTask_succeedsEvenIfTheDeadlineCannotBeScheduled() {
        Task task = taskInToDo();
        LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(5);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(board());
        when(taskRepository.findByIdAndProject(TASK_ID, PROJECT_ID)).thenReturn(task);
        when(deadlineRepository.scheduleNotification(any(), any()))
                .thenThrow(new RuntimeException("[TEST] simulated Redis outage"));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setEndDate(deadline);

        TaskResponse response = taskService.updateTask(request, MEMBER, PROJECT_ID, TASK_ID);

        assertEquals(deadline, response.getEndDate());
        verify(taskRepository).update(task);
    }
}
