package project;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.repository.ProjectRepository;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProjectRepositoryTest {

    private final ProjectRepository projectRepository;

    public ProjectRepositoryTest(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String INVITED = "invited@example.com";

    @BeforeEach
    void cleanCollection() {
        projectRepository.deleteAll();
    }

    private Project persistedBoard() {
        Project project = Project.create("Board", OWNER);
        project.addPhases(List.of("To Do", "Doing", "Done"));
        project.invite(MEMBER);
        project.acceptInvite(MEMBER);
        project.invite(INVITED);
        projectRepository.persist(project);
        return project;
    }

    @Test
    void project_survivesRoundTrip() {
        Project original = persistedBoard();

        assertNotNull(original.getId(), "persist has not assigned any id");

        Project reloaded = projectRepository.findById(original.getId());

        assertNotNull(reloaded, "no project was found for the given id");
        assertEquals("Board", reloaded.getName());
        assertEquals(OWNER, reloaded.getOwner());
        assertEquals(List.of("To Do", "Doing", "Done"), reloaded.getPhases(),
                "the order of the phases must survive");
        assertEquals(Set.of(OWNER, MEMBER), reloaded.getTeam());
        assertEquals(Set.of(INVITED), reloaded.getInvitedUsers());
        assertNotNull(reloaded.getCreatedAt(), "createdAt is null after second read");
        assertNotNull(reloaded.getUpdatedAt(), "updatedAt is null after second read");
    }

    @Test
    void reloadedProject_keepsItsBehaviour() {
        Project original = persistedBoard();

        Project reloaded = projectRepository.findById(original.getId());

        reloaded.addPhases(List.of("Review"));
        reloaded.acceptInvite(INVITED);

        assertEquals(4, reloaded.getPhases().size());
        assertTrue(reloaded.isMember(INVITED));
    }

    @Test
    void findProjectsByEmail_matchesAnyTeamMember() {
        persistedBoard();

        Project other = Project.create("Other", "estraneo@example.com");
        projectRepository.persist(other);

        assertEquals(1, projectRepository.findProjectsByEmail(OWNER).size());
        assertEquals(1, projectRepository.findProjectsByEmail(MEMBER).size());
        assertEquals(0, projectRepository.findProjectsByEmail(INVITED).size(),
                "An invited user is not a member yet");
    }

    @Test
    void findByPendingInvite_matchesInvitedUsersOnly() {
        persistedBoard();

        assertEquals(1, projectRepository.findByPendingInvite(INVITED).size());
        assertEquals(0, projectRepository.findByPendingInvite(MEMBER).size());
    }
}
