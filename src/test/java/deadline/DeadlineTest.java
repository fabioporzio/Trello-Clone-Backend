package deadline;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.trello.clone.data.repository.DeadlineRepository.calculateNotificationTTL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeadlineTest {

    @Test
    void ttlIsZeroWhenTheDeadlineHasPassed() {
        long ttl = calculateNotificationTTL(Instant.now().minus(1, ChronoUnit.DAYS));
        assertEquals(0, ttl);
    }

    @Test
    void ttlIsOneWhenLessThanADayIsLeft() {
        long ttl = calculateNotificationTTL(Instant.now().plus(6, ChronoUnit.HOURS));
        assertEquals(1, ttl);
    }

    @Test
    void ttlEndsOneDayBeforeTheDeadline() {
        Instant deadline = Instant.now().plus(5, ChronoUnit.DAYS);
        long ttl = calculateNotificationTTL(deadline);

        assertTrue(ttl > 4 * 24 * 60 * 60 - 10);
        assertTrue(ttl <= 4 * 24 * 60 * 60);
    }
}
