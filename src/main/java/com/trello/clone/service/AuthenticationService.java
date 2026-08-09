package com.trello.clone.service;

import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.AuthenticationRepository;
import com.trello.clone.service.exception.InvalidCredentialsException;
import com.trello.clone.web.model.user.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;

    public AuthenticationService(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    public UserResponse authenticate(String email, String password) {
        User user = authenticationRepository.authenticate(email, password);

        if (user == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        return toUserResponse(user);
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
