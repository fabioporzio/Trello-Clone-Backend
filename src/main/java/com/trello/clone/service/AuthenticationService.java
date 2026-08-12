package com.trello.clone.service;

import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.AuthenticationRepository;
import com.trello.clone.service.exception.InvalidCredentialsException;
import com.trello.clone.service.exception.TooManyAttemptsException;
import com.trello.clone.web.model.user.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final LoginThrottleService loginThrottleService;

    public AuthenticationService(
            AuthenticationRepository authenticationRepository,
            LoginThrottleService loginThrottleService
    ) {
        this.authenticationRepository = authenticationRepository;
        this.loginThrottleService = loginThrottleService;
    }

    public UserResponse authenticate(String email, String password) {
        long cooldown = loginThrottleService.checkCooldown(email);
        if (cooldown > 0) {
            throw new TooManyAttemptsException(cooldown);
        }

        User user = authenticationRepository.authenticate(email, password);
        if (user == null) {
            loginThrottleService.registerFailure(email);
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        loginThrottleService.reset(email);
        return toUserResponse(user);
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
