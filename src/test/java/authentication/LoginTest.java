package authentication;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class LoginTest {

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
    void loginIsCaseInsensitive() {
        String email = uniqueEmail();
        registerUser(email);

        attemptLogin(email.toUpperCase(), PASSWORD).statusCode(200);
    }

    @Test
    void registrationRejectsTheSameEmailWithDifferentCasing() {
        String email = uniqueEmail();
        registerUser(email);

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email.toUpperCase(), "username", "other", "password", PASSWORD))
                .when().post("/api/user/register")
                .then().statusCode(409);
    }
}
