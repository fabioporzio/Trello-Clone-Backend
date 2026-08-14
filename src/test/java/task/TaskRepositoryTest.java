package task;

import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.TaskRepository;
import io.quarkus.test.junit.QuarkusTest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration Tests with a real MongoDB, run by Dev Services. */
@QuarkusTest
class TaskRepositoryTest {

    private final TaskRepository taskRepository;

    public TaskRepositoryTest(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    private static final ObjectId PROJECT_A = new ObjectId();
    private static final ObjectId PROJECT_B = new ObjectId();

    @BeforeEach
    void cleanCollection() {
        taskRepository.deleteAll();
    }

    private Task persisted(String title, String phase, ObjectId projectId) {
        Task task = Task.create(title, null, phase, projectId);
        taskRepository.persist(task);
        return task;
    }

    // ROUND-TRIP

    @Test
    void task_survivesRoundTrip() {
        LocalDate deadline = LocalDate.now(ZoneOffset.UTC).plusDays(7);

        Task original = Task.create("Fix login", "  una descrizione  ", "To Do", PROJECT_A);
        original.addTags(List.of("bug", "urgent"));
        original.assign(Set.of("a@example.com", "b@example.com"));
        original.scheduleFor(deadline);
        original.setCompleted(true);

        taskRepository.persist(original);

        assertNotNull(original.getId(), "persist non ha assegnato l'id");

        Task reloaded = taskRepository.findById(original.getId());

        assertNotNull(reloaded, "la task non e' stata ritrovata per id");
        assertEquals("Fix login", reloaded.getTitle());
        assertEquals("una descrizione", reloaded.getDescription());
        assertEquals("To Do", reloaded.getPhase());
        assertEquals(true, reloaded.isCompleted());
        assertEquals(deadline, reloaded.getEndDate());
        assertEquals(PROJECT_A, reloaded.getProjectId());
        assertEquals(List.of("bug", "urgent"), reloaded.getTags());
        assertEquals(Set.of("a@example.com", "b@example.com"), reloaded.getAssignees());
    }

    @Test
    void emptyCollections_comeBackEmptyNotNull() {
        Task original = persisted("Senza tag", "To Do", PROJECT_A);

        Task reloaded = taskRepository.findById(original.getId());

        assertNotNull(reloaded.getTags(), "tags e' null dopo la rilettura");
        assertNotNull(reloaded.getAssignees(), "assignees e' null dopo la rilettura");
        assertTrue(reloaded.getTags().isEmpty());
        assertTrue(reloaded.getAssignees().isEmpty());

        reloaded.addTags(List.of("nuovo"));
        assertEquals(List.of("nuovo"), reloaded.getTags());
    }

    @Test
    void nullDeadline_survivesRoundTrip() {
        Task original = persisted("Senza scadenza", "To Do", PROJECT_A);

        Task reloaded = taskRepository.findById(original.getId());

        assertNull(reloaded.getEndDate());
    }

    // findByIdAndProject

    @Test
    void findByIdAndProject_findsTheTaskInItsOwnProject() {
        Task task = persisted("Fix login", "To Do", PROJECT_A);

        Task found = taskRepository.findByIdAndProject(task.getId(), PROJECT_A);

        assertNotNull(found, "la query id + projectId non ha trovato la task");
        assertEquals("Fix login", found.getTitle());
    }

    @Test
    void findByIdAndProject_ignoresTasksOfOtherProjects() {
        Task task = persisted("Fix login", "To Do", PROJECT_A);

        Task found = taskRepository.findByIdAndProject(task.getId(), PROJECT_B);

        assertNull(found, "la task di un altro progetto e' stata restituita");
    }

    // countByPhase

    @Test
    void countByPhase_countsOnlyTheGivenProjectAndPhase() {
        persisted("A1", "Doing", PROJECT_A);
        persisted("A2", "Doing", PROJECT_A);
        persisted("A3", "To Do", PROJECT_A);
        persisted("B1", "Doing", PROJECT_B);

        assertEquals(2, taskRepository.countByPhase(PROJECT_A, "Doing"));
        assertEquals(1, taskRepository.countByPhase(PROJECT_A, "To Do"));
        assertEquals(0, taskRepository.countByPhase(PROJECT_A, "Inesistente"));
    }

    //  renamePhase

    @Test
    void renamePhase_touchesOnlyMatchingTasks() {
        Task a1 = persisted("A1", "Doing", PROJECT_A);
        Task a2 = persisted("A2", "Doing", PROJECT_A);
        Task a3 = persisted("A3", "To Do", PROJECT_A);
        Task b1 = persisted("B1", "Doing", PROJECT_B);

        long updated = taskRepository.renamePhase(PROJECT_A, "Doing", "In corso");

        assertEquals(2, updated, "numero di documenti aggiornati inatteso");

        assertEquals("In corso", taskRepository.findById(a1.getId()).getPhase());
        assertEquals("In corso", taskRepository.findById(a2.getId()).getPhase());
        assertEquals("To Do", taskRepository.findById(a3.getId()).getPhase());
        assertEquals("Doing", taskRepository.findById(b1.getId()).getPhase(),
                "la task di un altro progetto e' stata modificata");
    }

    @Test
    void renamePhase_withNoMatches_changesNothing() {
        Task a1 = persisted("A1", "To Do", PROJECT_A);

        long updated = taskRepository.renamePhase(PROJECT_A, "Inesistente", "Nuova");

        assertEquals(0, updated);
        assertEquals("To Do", taskRepository.findById(a1.getId()).getPhase());
    }

    // $in FILTERS

    private Task withTags(String title, List<String> tags) {
        Task task = Task.create(title, null, "To Do", PROJECT_A);
        task.addTags(tags);
        taskRepository.persist(task);
        return task;
    }

    @Test
    void tagFilter_isOrNotAnd() {
        withTags("solo bug", List.of("bug"));
        withTags("solo urgent", List.of("urgent"));
        withTags("entrambi", List.of("bug", "urgent"));
        withTags("nessuno dei due", List.of("docs"));

        List<Task> onlyBug = taskRepository.getTasksByProjectIdTagsAndAssignees(
                PROJECT_A, List.of("bug"), null);
        assertEquals(2, onlyBug.size(), "il filtro su un solo tag non ha trovato le task attese");

        List<Task> either = taskRepository.getTasksByProjectIdTagsAndAssignees(
                PROJECT_A, List.of("bug", "urgent"), null);
        assertEquals(3, either.size(), "$in dovrebbe essere OR: attese 3 task");
    }

    @Test
    void tagFilter_isCaseSensitiveOnStoredLowercase() {
        withTags("una task", List.of("BUG"));   // requireTag mette in minuscolo

        List<Task> found = taskRepository.getTasksByProjectIdTagsAndAssignees(
                PROJECT_A, List.of("bug"), null);

        assertEquals(1, found.size(), "il tag salvato non e' in minuscolo");
    }

    @Test
    void noFilters_returnEveryTaskOfTheProject() {
        persisted("A1", "To Do", PROJECT_A);
        persisted("A2", "Doing", PROJECT_A);
        persisted("B1", "To Do", PROJECT_B);

        List<Task> all = taskRepository.getTasksByProjectIdTagsAndAssignees(PROJECT_A, null, null);

        assertEquals(2, all.size());
    }

    @Test
    void emptyFilterList_isTreatedAsNoFilter() {
        persisted("A1", "To Do", PROJECT_A);
        persisted("A2", "Doing", PROJECT_A);

        List<Task> all = taskRepository.getTasksByProjectIdTagsAndAssignees(
                PROJECT_A, List.of(), Set.of());

        assertEquals(2, all.size(), "una lista di filtri vuota deve valere come nessun filtro");
    }

    @Test
    void assigneeFilter_matchesTasksContainingTheEmail() {
        Task assigned = Task.create("Assegnata", null, "To Do", PROJECT_A);
        assigned.assign(Set.of("a@example.com"));
        taskRepository.persist(assigned);

        persisted("Non assegnata", "To Do", PROJECT_A);

        List<Task> found = taskRepository.getTasksByProjectIdTagsAndAssignees(
                PROJECT_A, null, Set.of("a@example.com"));

        assertEquals(1, found.size());
        assertEquals("Assegnata", found.get(0).getTitle());
    }
}
