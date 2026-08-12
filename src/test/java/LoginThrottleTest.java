import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class LoginThrottleTest {

    private static final String PASSWORD = "TestPass123!";

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private void registerUser(String email) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "username", "tester", "password", PASSWORD))
                .when()
                .post("/api/user/register")
                .then()
                .statusCode(201);
    }

    private ValidatableResponse attemptLogin(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/login")
                .then();
    }

    @Test
    void firstAttemptsAreNotThrottled() {
        String email = uniqueEmail();

        // Non-Existing account: first two fails without a cooldown
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);
    }

    @Test
    void throttleKicksInAfterRepeatedFailures() {
        String email = uniqueEmail();

        // Three failures -> sets cooldown
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);

        // Blocks next attempt
        attemptLogin(email, "wrong")
                .statusCode(429)
                .header("Retry-After", notNullValue());
    }

    @Test
    void correctPasswordDuringCooldownIsStillRejected() {
        String email = uniqueEmail();
        registerUser(email);

        // Three failures -> sets cooldown
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);

        // password is correct, but since cooldown is active we check for 429 error code
        attemptLogin(email, PASSWORD)
                .statusCode(429)
                .header("Retry-After", notNullValue());
    }

    @Test
    void successfulLoginResetsTheCounter() {
        String email = uniqueEmail();
        registerUser(email);

        // Two failures and a success -> count is reset
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, PASSWORD).statusCode(200);

        // if reset worked, we get two errors with 401 error code
        attemptLogin(email, "wrong").statusCode(401);
        attemptLogin(email, "wrong").statusCode(401);
    }
}