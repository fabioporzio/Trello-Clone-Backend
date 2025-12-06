package com.trello.clone.service;

import com.trello.clone.data.model.Credential;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.CredentialRepository;
import com.trello.clone.data.repository.UserRepository;
import com.trello.clone.web.model.user.*;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserService {
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public UserService(UserRepository userRepository,  CredentialRepository credentialRepository) {
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

    public UserResponse updateUserEmail(UpdateUserEmailRequest request) {
        Credential userCredentials = credentialRepository.authenticate(request.getCurrentEmail(), request.getPassword());

        if (userCredentials == null) {
            return null;
        }

        if (userCredentials.getEmail().equals(request.getNewEmail())) {
            return null;
        }

        boolean exists = userRepository.count("email", request.getNewEmail()) > 0;
        if (exists) {
            return null;
        }

        User user = userRepository.findByEmail(request.getCurrentEmail());
        if (user != null) {
            user.setEmail(request.getNewEmail());
            userRepository.update(user);

            userCredentials.setEmail(request.getNewEmail());
            credentialRepository.update(userCredentials);

            return toUserResponse(user);
        }
        else {
            return null;
        }
    }

    public UserResponse updateUserUsername(UpdateUserUsernameRequest request) {
        Credential userCredentials = credentialRepository.authenticate(request.getEmail(), request.getPassword());

        if (userCredentials == null) {
            return null;
        }

        User user = userRepository.findByEmail(request.getEmail());
        if (user != null) {
            if (user.getUsername().equals(request.getNewUsername())) {
                return null;
            }
            user.setUsername(request.getNewUsername());
            userRepository.update(user);
            return toUserResponse(user);
        }
        else {
            return null;
        }
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

    public UserResponse updateUserPassword(UpdateUserPasswordRequest request) {
        Credential userCredentials = credentialRepository.authenticate(request.getEmail(), request.getCurrentPassword());

        if (userCredentials == null) {
            return null;
        }

        if (BcryptUtil.matches(request.getNewPassword(), userCredentials.getPassword())) {
            return null;
        }

        String newHashedPassword = BcryptUtil.bcryptHash(request.getNewPassword());
        userCredentials.setPassword(newHashedPassword);

        credentialRepository.update(userCredentials);
        return toUserResponse(userRepository.findByEmail(request.getEmail()));
    }

    public ObjectId getIdByUser(UserResponse userResponse) {
        User user =  userRepository.findByEmail(userResponse.getEmail());

        if (user != null) {
            return user.getId();
        }
        else {
            return null;
        }
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll().stream().toList();

        if (!users.isEmpty()) {
            List<UserResponse> userResponseList = new ArrayList<>();
            for (User user : users) {
                userResponseList.add(toUserResponse(user));
            }
            return userResponseList;
        }
        else {
            return null;
        }
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
