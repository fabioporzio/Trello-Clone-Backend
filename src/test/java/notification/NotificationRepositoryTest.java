package notification;

import com.trello.clone.data.model.Notification;
import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.repository.NotificationRepository;
import com.trello.clone.service.exception.NotFoundException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.redis.client.Response;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class NotificationRepositoryTest {

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    RedisDataSource redisDataSource;

    private static final String SENDER = "owner@example.com";

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private Project projectFixture() {
        Project project = Project.create("Board", SENDER);
        project.setId(new ObjectId());
        return project;
    }

    private Task taskFixture() {
        Task task = Task.create("Fix login", null, "To Do", new ObjectId());
        task.setId(new ObjectId());
        return task;
    }

    @Test
    void projectInvite_survivesRoundTrip() {
        String receiver = uniqueEmail();
        Project project = projectFixture();

        notificationRepository.addProjectInvite(receiver, project, SENDER);

        List<Notification> found = notificationRepository.getNotifications(receiver);

        assertEquals(1, found.size());
        Notification notification = found.getFirst();
        assertNotNull(notification.getId(), "Id must be present on the field");
        assertEquals(receiver, notification.getReceiver());
        assertEquals("project", notification.getCategory());
        assertEquals(project.getId().toHexString(), notification.getProjectOrTaskId());
        assertEquals(SENDER, notification.getSender());
        assertNotNull(notification.getIssuedAt());
        assertTrue(notification.getContent().contains("Board"));
    }

    @Test
    void taskAssignment_survivesRoundTrip() {
        String receiver = uniqueEmail();
        Task task = taskFixture();

        notificationRepository.addTaskAssignment(receiver, task, SENDER);

        List<Notification> found = notificationRepository.getNotifications(receiver);

        assertEquals(1, found.size());
        assertEquals("task", found.getFirst().getCategory());
        assertEquals(task.getId().toHexString(), found.getFirst().getProjectOrTaskId());
    }

    @Test
    void emptyInbox_returnsEmptyList() {
        assertTrue(notificationRepository.getNotifications(uniqueEmail()).isEmpty());
    }

    @Test
    void inbox_holdsSeveralNotifications() {
        String receiver = uniqueEmail();

        notificationRepository.addProjectInvite(receiver, projectFixture(), SENDER);
        notificationRepository.addProjectInvite(receiver, projectFixture(), SENDER);
        notificationRepository.addTaskAssignment(receiver, taskFixture(), SENDER);

        assertEquals(3, notificationRepository.getNotifications(receiver).size());
    }

    @Test
    void invitingTwiceDoesNotDuplicate() {
        String receiver = uniqueEmail();
        Project project = projectFixture();

        notificationRepository.addProjectInvite(receiver, project, SENDER);
        notificationRepository.addProjectInvite(receiver, project, SENDER);

        assertEquals(1, notificationRepository.getNotifications(receiver).size());
    }

    @Test
    void aDifferentSenderReusesTheSameNotification() {
        String receiver = uniqueEmail();
        Project project = projectFixture();

        notificationRepository.addProjectInvite(receiver, project, SENDER);
        notificationRepository.addProjectInvite(receiver, project, "altro@example.com");

        assertEquals(1, notificationRepository.getNotifications(receiver).size());
    }

    @Test
    void eachNotificationGetsItsOwnTtl() {
        String receiver = uniqueEmail();
        Project project = projectFixture();

        notificationRepository.addProjectInvite(receiver, project, SENDER);

        String key = "trello-clone:users:" + receiver + ":notifications";
        String field = notificationRepository.getNotifications(receiver).getFirst().getId();

        Response response = redisDataSource.execute("HTTL", key, "FIELDS", "1", field);
        long remaining = response.get(0).toLong();

        assertTrue(remaining > 0,
                "HEXPIRE has not set the TTL on the fields (value: " + remaining + ")");
        assertTrue(remaining <= 7 * 24 * 60 * 60);
    }

    @Test
    void deleteNotification_removesIt() {
        String receiver = uniqueEmail();
        notificationRepository.addProjectInvite(receiver, projectFixture(), SENDER);

        String id = notificationRepository.getNotifications(receiver).getFirst().getId();
        notificationRepository.deleteNotification(receiver, id);

        assertTrue(notificationRepository.getNotifications(receiver).isEmpty());
    }

    @Test
    void deleteNotification_onAMissingOne_throwsNotFound() {
        String receiver = uniqueEmail();

        assertThrows(NotFoundException.class,
                () -> notificationRepository.deleteNotification(receiver, "id doesn't exist"));
    }

    @Test
    void removeProjectInvite_isIdempotent() {
        String receiver = uniqueEmail();
        Project project = projectFixture();
        String projectId = project.getId().toHexString();

        notificationRepository.addProjectInvite(receiver, project, SENDER);

        assertDoesNotThrow(() -> notificationRepository.removeProjectInvite(receiver, projectId));
        assertDoesNotThrow(() -> notificationRepository.removeProjectInvite(receiver, projectId));

        assertTrue(notificationRepository.getNotifications(receiver).isEmpty());
    }

    @Test
    void removeTaskAssignment_removesOnlyTheGivenTask() {
        String receiver = uniqueEmail();
        Task first = taskFixture();
        Task second = taskFixture();

        notificationRepository.addTaskAssignment(receiver, first, SENDER);
        notificationRepository.addTaskAssignment(receiver, second, SENDER);

        notificationRepository.removeTaskAssignment(receiver, first.getId().toHexString());

        List<Notification> remaining = notificationRepository.getNotifications(receiver);
        assertEquals(1, remaining.size());
        assertEquals(second.getId().toHexString(), remaining.getFirst().getProjectOrTaskId());
    }

    @Test
    void theInboxIsCaseInsensitive() {
        String receiver = uniqueEmail();

        notificationRepository.addProjectInvite(receiver.toUpperCase(), projectFixture(), SENDER);

        assertEquals(1, notificationRepository.getNotifications(receiver).size(),
                "Read and Write must use the same inbox");
    }

    @Test
    void oneUserInboxDoesNotLeakIntoAnother() {
        String first = uniqueEmail();
        String second = uniqueEmail();

        notificationRepository.addProjectInvite(first, projectFixture(), SENDER);

        assertEquals(1, notificationRepository.getNotifications(first).size());
        assertTrue(notificationRepository.getNotifications(second).isEmpty());
    }
}
