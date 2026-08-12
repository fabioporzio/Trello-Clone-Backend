package com.trello.clone.web.resource;

import com.trello.clone.service.AuthenticationService;
import com.trello.clone.service.UserService;
import com.trello.clone.web.model.authentication.AccessTokenResponse;
import com.trello.clone.web.model.authentication.LoginRequest;
import com.trello.clone.web.model.authentication.TokenResponse;
import com.trello.clone.web.model.user.UserResponse;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Path("api/auth")
@Tag(name = "Authentication", description = "Login and token refresh operations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    private final AuthenticationService  authenticationService;
    private final UserService userService;

    public AuthenticationResource(AuthenticationService authenticationService,  UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @POST
    @Path("/login")
    @RateLimited(bucket = "login", identityResolver = IpResolver.class)
    @Operation(
            summary = "Authenticate a user",
            description = "Validates email and password and returns a JWT access token plus a refresh token."
    )
    @APIResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TokenResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Invalid email or password")
    @APIResponse(responseCode = "429", description = "Too many login attempts — try again later")
    public Response login(@Valid LoginRequest request) {
        UserResponse user = authenticationService.authenticate(request.getEmail(), request.getPassword());

        String accessToken = getAccessToken(user);
        String refreshToken = getRefreshToken(user);

        return Response.ok(new TokenResponse(accessToken, refreshToken)).build();
    }

    @POST
    @Path("/refresh")
    @RolesAllowed({"refresh_token"})
    @Operation(
            summary = "Refresh the access token",
            description = "Exchanges a valid refresh token (sent as a Bearer token) for a new access token."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponse(
            responseCode = "200",
            description = "New access token issued",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AccessTokenResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Missing, invalid, or expired refresh token")
    public Response refresh(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();

        UserResponse user = userService.getUserByEmail(email);

        String accessToken = getAccessToken(user);
        return Response.ok(new AccessTokenResponse(accessToken)).build();
    }

    private String getAccessToken(UserResponse user) {
        ObjectId id = userService.getIdByUser(user);
        return Jwt
                .issuer("trello-clone-jwt")
                .subject(user.getEmail())
                .upn(user.getEmail())
                .groups(Set.of("access_token"))
                .claim(Claims.nickname.name(), user.getEmail())
                .claim("id", id.toHexString())
                .expiresIn(Duration.ofMinutes(10))
                .issuedAt(Instant.now())
                .sign();
    }

    private String getRefreshToken(UserResponse user) {
        ObjectId id = userService.getIdByUser(user);
        return Jwt
                .issuer("trello-clone-jwt")
                .subject(user.getEmail())
                .upn(user.getEmail())
                .groups(Set.of("refresh_token"))
                .claim(Claims.nickname.name(), user.getEmail())
                .claim("id", id.toHexString())
                .expiresIn(Duration.ofHours(1))
                .issuedAt(Instant.now())
                .sign();
    }
}
