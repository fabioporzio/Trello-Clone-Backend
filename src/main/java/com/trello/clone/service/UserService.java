package com.trello.clone.service;

import com.trello.clone.data.model.Credential;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.CredentialRepository;
import com.trello.clone.data.repository.UserRepository;
import com.trello.clone.web.model.CreateUserRequest;
import com.trello.clone.web.model.UserResponse;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public UserService(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    public UserResponse authenticate(String username, String password) {
        Credential credential = credentialRepository.authenticate(username, password);

        if (credential != null) {
            User user = userRepository.findByEmail(credential.getEmail());
            if (user != null) {
                return toUserResponse(user);
            }
            else {
                return null;
            }
        }
        return null;
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

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);

        if (user != null) {
            return toUserResponse(user);
        }
        else {
            return null;
        }
    }

    public ObjectId getIdByUser(UserResponse userResponse) {
        User user =  userRepository.findByEmail(userResponse.getEmail());

        if (user != null) {
            return user.id;
        }
        else {
            return null;
        }
    }
}
