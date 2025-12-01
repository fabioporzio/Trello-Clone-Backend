package com.trello.clone.service;

import com.trello.clone.data.model.Credential;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.CredentialRepository;
import com.trello.clone.data.repository.UserRepository;
import com.trello.clone.web.model.CreateUserRequest;
import com.trello.clone.web.model.UserResponse;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public UserService(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    public UserResponse registerUser(CreateUserRequest createUserRequest) {

        boolean exists = userRepository.count("email", createUserRequest.getEmail()) > 0;
        if (exists) {
            return null;
        }

        String hashedPassword = BcryptUtil.bcryptHash(createUserRequest.getPassword());

        User user = new User (
                createUserRequest.getEmail(),
                createUserRequest.getUsername()
        );

        Credential userCredentials = new Credential (
                createUserRequest.getEmail(),
                hashedPassword
        );

        userRepository.persist(user);
        credentialRepository.persist(userCredentials);

        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
