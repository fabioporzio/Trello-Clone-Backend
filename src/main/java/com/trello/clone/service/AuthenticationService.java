package com.trello.clone.service;

import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.AuthenticationRepository;
import com.trello.clone.service.exception.InvalidCredentialsException;
import com.trello.clone.service.exception.TooManyAttemptsException;
import com.trello.clone.utils.EmailUtils;
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
        String normalizedEmail = EmailUtils.normalize(email);
        long cooldown = loginThrottleService.checkCooldown(normalizedEmail);
        if (cooldown > 0) {
            throw new TooManyAttemptsException(cooldown);
        }

        User user = authenticationRepository.authenticate(normalizedEmail, password);
        if (user == null) {
            loginThrottleService.registerFailure(email);
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        loginThrottleService.reset(email);
        return toUserResponse(user);
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId().toHexString(),
                user.getEmail(),
                user.getUsername()
        );
    }
}
