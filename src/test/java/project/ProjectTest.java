package project;

import com.trello.clone.data.model.Project;
import com.trello.clone.service.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {

    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String OUTSIDER = "outsider@example.com";

    private Project board() {
        Project project = Project.create("Board", OWNER);
        project.addPhases(List.of("To Do", "Doing", "Done"));
        return project;
    }

    private Project boardWithMember() {
        Project project = board();
        project.invite(MEMBER);
        project.acceptInvite(MEMBER);
        return project;
    }

    // CREATE

    @Test
    void create_putsTheOwnerInTheTeam() {
        Project project = Project.create("  Board  ", OWNER);

        assertEquals("Board", project.getName());
        assertEquals(OWNER, project.getOwner());
        assertTrue(project.isOwner(OWNER));
        assertTrue(project.isMember(OWNER), "The owner must also be a member");
        assertTrue(project.getPhases().isEmpty());
        assertTrue(project.getInvitedUsers().isEmpty());
        assertNotNull(project.getCreatedAt());
        assertEquals(project.getCreatedAt(), project.getUpdatedAt());
    }

    @Test
    void create_blankName_isRejected() {
        assertThrows(BadRequestException.class, () -> Project.create("  ", OWNER));
        assertThrows(BadRequestException.class, () -> Project.create(null, OWNER));
    }

    @Test
    void create_blankOwner_isRejected() {
        assertThrows(BadRequestException.class, () -> Project.create("Board", ""));
        assertThrows(BadRequestException.class, () -> Project.create("Board", null));
    }

    @Test
    void rename_tooLong_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.rename("a".repeat(101)));
    }

    // PHASES

    @Test
    void addPhases_appendsInOrderAndTrims() {
        Project project = Project.create("Board", OWNER);

        project.addPhases(List.of("  To Do  ", "Doing"));

        assertEquals(List.of("To Do", "Doing"), project.getPhases());
    }

    @Test
    void addPhases_isCaseInsensitivelyDistinct() {
        Project project = Project.create("Board", OWNER);

        project.addPhases(List.of("To Do"));
        project.addPhases(List.of("TO DO", "to do"));

        assertEquals(List.of("To Do"), project.getPhases());
    }

    @Test
    void addPhases_beyondLimit_isRejectedAndLeavesStateUntouched() {
        Project project = Project.create("Board", OWNER);
        List<String> fiftyOne = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            fiftyOne.add("phase" + i);
        }

        assertThrows(BadRequestException.class, () -> project.addPhases(fiftyOne));
        assertTrue(project.getPhases().isEmpty(), "The state must not change if te validation fails");
    }

    @Test
    void removePhases_isCaseInsensitive() {
        Project project = board();

        project.removePhases(List.of("DOING"));

        assertEquals(List.of("To Do", "Done"), project.getPhases());
    }

    @Test
    void renamePhase_preservesPosition() {
        Project project = board();

        project.renamePhase("Doing", "In development");

        assertEquals(List.of("To Do", "In development", "Done"), project.getPhases());
    }

    @Test
    void renamePhase_unknownPhase_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.renamePhase("Non existing", "New"));
    }

    @Test
    void renamePhase_collidingWithExisting_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.renamePhase("Doing", "DONE"));
    }

    @Test
    void reorderPhases_appliesTheGivenOrder() {
        Project project = board();

        project.reorderPhases(List.of("Done", "To Do", "Doing"));

        assertEquals(List.of("Done", "To Do", "Doing"), project.getPhases());
    }

    @Test
    void reorderPhases_missingOnePhase_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.reorderPhases(List.of("Done", "To Do")));
    }

    @Test
    void reorderPhases_listingOneTwice_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class,
                () -> project.reorderPhases(List.of("Done", "DONE", "To Do")));
    }

    @Test
    void reorderPhases_withUnknownPhase_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class,
                () -> project.reorderPhases(List.of("Done", "To Do", "Ghost Phase")));
    }

    // CANONICAL PHASE

    @Test
    void canonicalPhase_returnsTheStoredCasing() {
        Project project = board();

        assertEquals("To Do", project.canonicalPhase("to do"));
        assertEquals("To Do", project.canonicalPhase("TO DO"));
        assertNull(project.canonicalPhase("Non existing"));
    }

    @Test
    void hasPhase_agreesWithCanonicalPhase() {
        Project project = board();

        assertTrue(project.hasPhase("to do"));
        assertFalse(project.hasPhase("Non existing"));
    }

    @Test
    void requireExistingPhase_unknownPhase_isRejected() {
        Project project = board();

        assertEquals("To Do", project.requireExistingPhase("  to do  "));
        assertThrows(BadRequestException.class, () -> project.requireExistingPhase("Non existing"));
    }

    // OWNERSHIP

    @Test
    void transferOwnershipTo_aTeamMember_succeeds() {
        Project project = boardWithMember();

        project.transferOwnershipTo(MEMBER);

        assertTrue(project.isOwner(MEMBER));
        assertTrue(project.isMember(OWNER), "the old owner stays on the team");
    }

    @Test
    void transferOwnershipTo_someoneOutsideTheTeam_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.transferOwnershipTo(OUTSIDER));
        assertTrue(project.isOwner(OWNER), "The owner must not change");
    }

    @Test
    void transferOwnershipTo_theCurrentOwner_isANoOp() {
        Project project = board();

        project.transferOwnershipTo(OWNER);

        assertTrue(project.isOwner(OWNER));
    }

    // INVITES

    @Test
    void invite_addsToPendingInvitations() {
        Project project = board();

        project.invite(MEMBER);

        assertTrue(project.isInvited(MEMBER));
        assertFalse(project.isMember(MEMBER), "An invited user is not a member yet");
    }

    @Test
    void invite_someoneAlreadyInTheTeam_isRejected() {
        Project project = boardWithMember();

        assertThrows(BadRequestException.class, () -> project.invite(MEMBER));
    }

    @Test
    void acceptInvite_movesTheUserIntoTheTeam() {
        Project project = board();
        project.invite(MEMBER);

        project.acceptInvite(MEMBER);

        assertTrue(project.isMember(MEMBER));
        assertFalse(project.isInvited(MEMBER), "The invite must be consumed");
    }

    @Test
    void acceptInvite_withoutAPendingInvitation_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.acceptInvite(OUTSIDER));
    }

    @Test
    void revokeInvite_withoutAPendingInvitation_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.revokeInvite(OUTSIDER));
    }

    // MEMBERS

    @Test
    void removeMember_removesFromTheTeam() {
        Project project = boardWithMember();

        project.removeMember(MEMBER);

        assertFalse(project.isMember(MEMBER));
    }

    @Test
    void removeMember_theOwner_isRejected() {
        Project project = boardWithMember();

        assertThrows(BadRequestException.class, () -> project.removeMember(OWNER));
        assertTrue(project.isMember(OWNER));
    }

    @Test
    void removeMember_someoneNotInTheTeam_isRejected() {
        Project project = board();

        assertThrows(BadRequestException.class, () -> project.removeMember(OUTSIDER));
    }

    // TOUCH

    @Test
    void touch_movesUpdatedAtForward() throws InterruptedException {
        Project project = board();
        Instant before = project.getUpdatedAt();

        Thread.sleep(5);
        project.touch();

        assertTrue(project.getUpdatedAt().isAfter(before));
        assertEquals(project.getCreatedAt(), project.getCreatedAt(), "createdAt must not change");
    }
}
