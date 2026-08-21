package com.trello.clone.utils;

import com.trello.clone.data.model.Project;
import com.trello.clone.data.model.Task;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.ProjectRepository;
import com.trello.clone.data.repository.TaskRepository;
import com.trello.clone.data.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Fills Database with sample data.
 * Inactive by default. Only works on the Render deploy.
 * Only works if the demo user does not already exist.
 */
@ApplicationScoped
public class DemoDataSeeder {

    public static final String DEMO_EMAIL = "demo@trello-clone.dev";
    public static final String DEMO_PASSWORD = "DemoPass123!";

    private static final String TEAMMATE_EMAIL = "teammate@trello-clone.dev";
    private static final String INVITEE_EMAIL = "invited@trello-clone.dev";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @ConfigProperty(name = "demo.data.enabled", defaultValue = "false")
    boolean demoDataEnabled;

    public DemoDataSeeder(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    void onStart(@Observes StartupEvent event) {
        if (!demoDataEnabled) {
            return;
        }

        try {
            seed();
        }
        catch (Exception e) {
            // Seeder does not prevent the application from running.
            // No demo data is better than a broken application.
            Log.error("Demo data seeding failed", e);
        }
    }

    private void seed() {
        if (userRepository.findByEmail(DEMO_EMAIL) != null) {
            Log.info("Demo data already present, skipping");
            return;
        }

        createUser(DEMO_EMAIL, "demo");
        createUser(TEAMMATE_EMAIL, "teammate");
        createUser(INVITEE_EMAIL, "invited");

        Project project = Project.create("Product Launch", DEMO_EMAIL);
        project.addPhases(List.of("Backlog", "In Progress", "Review", "Done"));
        project.invite(TEAMMATE_EMAIL);
        project.acceptInvite(TEAMMATE_EMAIL);
        project.invite(INVITEE_EMAIL);
        projectRepository.persist(project);

        seedTasks(project);

        Log.infof("Demo data seeded: project %s with a sample board", project.getId());
    }

    private void createUser(String email, String username) {
        User user = new User(email, username, BcryptUtil.bcryptHash(DEMO_PASSWORD));
        userRepository.persist(user);
    }

    private void seedTasks(Project project) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Task research = Task.create(
                "Competitor research",
                "Compare the three closest products and summarise the findings.",
                "Backlog",
                project.getId()
        );
        research.addTags(List.of("research", "discovery"));
        taskRepository.persist(research);

        Task api = Task.create(
                "Design the public API",
                "Draft the endpoint list and the authentication flow.",
                "In Progress",
                project.getId()
        );
        api.addTags(List.of("backend", "important"));
        api.assign(Set.of(DEMO_EMAIL));
        api.scheduleFor(today.plusDays(3));
        taskRepository.persist(api);

        Task onboarding = Task.create(
                "Onboarding screens",
                "Three screens, mobile first.",
                "In Progress",
                project.getId()
        );
        onboarding.addTags(List.of("design"));
        onboarding.assign(Set.of(TEAMMATE_EMAIL));
        taskRepository.persist(onboarding);

        Task copy = Task.create(
                "Landing page copy",
                null,
                "Review",
                project.getId()
        );
        copy.addTags(List.of("marketing"));
        copy.assign(Set.of(DEMO_EMAIL, TEAMMATE_EMAIL));
        taskRepository.persist(copy);

        Task setup = Task.create(
                "Set up the repository",
                "CI, linting and branch protection.",
                "Done",
                project.getId()
        );
        setup.addTags(List.of("backend"));
        setup.setCompleted(true);
        taskRepository.persist(setup);
    }
}
