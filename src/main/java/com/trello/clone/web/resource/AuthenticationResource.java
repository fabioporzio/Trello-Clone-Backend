package com.trello.clone.web.resource;

import com.trello.clone.data.repository.AuthenticationRepository;
import com.trello.clone.service.UserService;
import com.trello.clone.web.model.authentication.AccessTokenResponse;
import com.trello.clone.web.model.authentication.LoginRequest;
import com.trello.clone.web.model.authentication.TokenResponse;
import com.trello.clone.web.model.user.UserResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Path("/api/auth")
public class AuthenticationResource {

    private final UserService userService;
    private final AuthenticationRepository authenticationRepository;

    public AuthenticationResource(
            UserService userService,
            AuthenticationRepository authenticationRepository
    ) {
        this.userService = userService;
        this.authenticationRepository = authenticationRepository;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        UserResponse user = userService.authenticate(request.getEmail(), request.getPassword());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build();
        }

        String accessToken = getAccessToken(user);
        String refreshToken = getRefreshToken(user);

        boolean success = authenticationRepository.saveRefreshToken(refreshToken, user.getEmail());

        if (success) {
            return Response.ok(new TokenResponse(accessToken, refreshToken)).build();
        }
        else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }

    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"refresh_token"})
    public Response refresh(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();

        if (authenticationRepository.isRefreshTokenValid(email)) {
            UserResponse user = userService.getUserByEmail(email);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .build();
            }
            String accessToken = getAccessToken(user);
            return Response.ok(new AccessTokenResponse(accessToken)).build();
        }
        else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build();
        }
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
