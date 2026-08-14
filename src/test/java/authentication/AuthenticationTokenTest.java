package authentication;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthenticationTokenTest {

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

    private JsonPath login(String email) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    @Test
    void anAccessTokenCannotBeUsedToRefresh() {
        String email = uniqueEmail();
        registerUser(email);

        String accessToken = login(email).getString("accessToken");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(403);
    }

    @Test
    void aRefreshTokenIssuesANewAccessToken() {
        String email = uniqueEmail();
        registerUser(email);

        String refreshToken = login(email).getString("refreshToken");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + refreshToken)
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    void aRefreshTokenCannotAccessProtectedEndpoints() {
        String email = uniqueEmail();
        registerUser(email);

        String refreshToken = login(email).getString("refreshToken");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + refreshToken)
                .when()
                .get("/api/user")
                .then()
                .statusCode(403);
    }

    @Test
    void protectedEndpointsRejectRequestsWithoutAToken() {
        given().when().get("/api/user").then().statusCode(401);
        given().when().get("/api/project").then().statusCode(401);
        given().contentType(ContentType.JSON)
                .when().post("/api/auth/refresh").then().statusCode(401);
    }
}
