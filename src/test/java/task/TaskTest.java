package task;

import com.trello.clone.data.model.Task;
import com.trello.clone.service.exception.BadRequestException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    private static final ObjectId PROJECT_ID = new ObjectId();

    private Task newTask() {
        return Task.create("Fix login", null, "To Do", PROJECT_ID);
    }

    // CREATE

    @Test
    void create_setsInitialState() {
        Task task = Task.create("  Fix login  ", "  qualcosa da fare  ", "  To Do  ", PROJECT_ID);

        assertEquals("Fix login", task.getTitle());
        assertEquals("qualcosa da fare", task.getDescription());
        assertEquals("To Do", task.getPhase());
        assertEquals(PROJECT_ID, task.getProjectId());
        assertEquals(false, task.isCompleted());
        assertNull(task.getEndDate());
        assertTrue(task.getTags().isEmpty());
        assertTrue(task.getAssignees().isEmpty());
    }

    @Test
    void create_withoutDescription_isAllowed() {
        Task task = Task.create("Fix login", null, "To Do", PROJECT_ID);

        assertNull(task.getDescription());
    }

    // RENAME

    @Test
    void rename_blankTitle_isRejected() {
        Task task = newTask();

        assertThrows(BadRequestException.class, () -> task.rename("   "));
        assertThrows(BadRequestException.class, () -> task.rename(null));
    }

    @Test
    void rename_titleTooLong_isRejected() {
        Task task = newTask();
        String tooLong = "a".repeat(101);

        assertThrows(BadRequestException.class, () -> task.rename(tooLong));
    }

    @Test
    void rename_atExactLimit_isAccepted() {
        Task task = newTask();
        String atLimit = "a".repeat(100);

        task.rename(atLimit);

        assertEquals(atLimit, task.getTitle());
    }

    // DESCRIBE

    @Test
    void describe_blank_clearsDescription() {
        Task task = Task.create("Fix login", "una descrizione", "To Do", PROJECT_ID);

        task.describe("   ");

        assertNull(task.getDescription());
    }

    @Test
    void describe_tooLong_isRejected() {
        Task task = newTask();
        String tooLong = "a".repeat(10_001);

        assertThrows(BadRequestException.class, () -> task.describe(tooLong));
    }

    // MOVE TO

    @Test
    void moveTo_trimsAndAssigns() {
        Task task = newTask();

        task.moveTo("  Doing  ");

        assertEquals("Doing", task.getPhase());
    }

    @Test
    void moveTo_blankPhase_isRejected() {
        Task task = newTask();

        assertThrows(BadRequestException.class, () -> task.moveTo(""));
        assertThrows(BadRequestException.class, () -> task.moveTo(null));
    }

    // SCHEDULE FOR

    @Test
    void scheduleFor_pastDate_isRejected() {
        Task task = newTask();
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        assertThrows(BadRequestException.class, () -> task.scheduleFor(yesterday));
    }

    @Test
    void scheduleFor_today_isAccepted() {
        Task task = newTask();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        task.scheduleFor(today);

        assertEquals(today, task.getEndDate());
    }

    @Test
    void scheduleFor_null_clearsDeadline() {
        Task task = newTask();
        task.scheduleFor(LocalDate.now(ZoneOffset.UTC).plusDays(3));

        task.scheduleFor(null);

        assertNull(task.getEndDate());
    }

    // TAGS

    @Test
    void addTags_normalisesToLowercase() {
        Task task = newTask();

        task.addTags(List.of("  BUG  ", "Urgent"));

        assertEquals(List.of("bug", "urgent"), task.getTags());
    }

    @Test
    void addTags_isCaseInsensitivelyDistinct() {
        Task task = newTask();

        task.addTags(List.of("bug"));
        task.addTags(List.of("BUG", "Bug"));

        assertEquals(List.of("bug"), task.getTags());
    }

    @Test
    void addTags_blankTag_isRejected() {
        Task task = newTask();
        List<String> withBlank = new ArrayList<>();
        withBlank.add("bug");
        withBlank.add(null);

        assertThrows(BadRequestException.class, () -> task.addTags(withBlank));
    }

    @Test
    void addTags_beyondLimit_isRejected() {
        Task task = newTask();
        List<String> twentyOne = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            twentyOne.add("tag" + i);
        }

        assertThrows(BadRequestException.class, () -> task.addTags(twentyOne));
    }

    @Test
    void removeTags_isCaseInsensitive() {
        Task task = newTask();
        task.addTags(List.of("bug", "urgent"));

        task.removeTags(List.of("BUG"));

        assertEquals(List.of("urgent"), task.getTags());
    }

    @Test
    void removeTags_unknownTag_isIgnored() {
        Task task = newTask();
        task.addTags(List.of("bug"));

        task.removeTags(List.of("inesistente"));

        assertEquals(List.of("bug"), task.getTags());
    }

    // RENAME TAG

    @Test
    void renameTag_preservesPosition() {
        Task task = newTask();
        task.addTags(List.of("uno", "due", "tre"));

        task.renameTag("due", "DUE-BIS");

        assertEquals(List.of("uno", "due-bis", "tre"), task.getTags());
    }

    @Test
    void renameTag_unknownTag_isRejected() {
        Task task = newTask();
        task.addTags(List.of("bug"));

        assertThrows(BadRequestException.class, () -> task.renameTag("inesistente", "nuovo"));
    }

    @Test
    void renameTag_collidingWithExisting_isRejected() {
        Task task = newTask();
        task.addTags(List.of("bug", "urgent"));

        assertThrows(BadRequestException.class, () -> task.renameTag("bug", "URGENT"));
    }

    @Test
    void renameTag_toItselfWithDifferentCase_isAllowed() {
        Task task = newTask();
        task.addTags(List.of("bug"));

        task.renameTag("bug", "BUG");

        assertEquals(List.of("bug"), task.getTags());
    }

    // ASSIGNEES

    @Test
    void assign_onEmptySet_addsEveryone() {
        Task task = newTask();

        task.assign(Set.of("a@x.com", "b@x.com"));

        assertEquals(Set.of("a@x.com", "b@x.com"), task.getAssignees());
    }

    @Test
    void assign_isIdempotent() {
        Task task = newTask();

        task.assign(Set.of("a@x.com"));
        task.assign(Set.of("a@x.com"));

        assertEquals(1, task.getAssignees().size());
    }

    @Test
    void unassign_removesOnlyTheGivenEmails() {
        Task task = newTask();
        task.assign(Set.of("a@x.com", "b@x.com"));

        task.unassign(Set.of("a@x.com", "mai@presente.com"));

        assertEquals(Set.of("b@x.com"), task.getAssignees());
    }
}
