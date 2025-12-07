package com.trello.clone.service;

import com.trello.clone.data.model.Credential;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.CredentialRepository;
import com.trello.clone.data.repository.UserRepository;
import com.trello.clone.service.exception.*;
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

    public UserResponse authenticate(String email, String password) {
        Credential credential = credentialRepository.authenticate(email, password);

        if (credential == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        User user = userRepository.findByEmail(credential.getEmail());
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        return toUserResponse(user);
    }

    public UserResponse registerUser(CreateUserRequest createUserRequest) {
        boolean exists = userRepository.count("email", createUserRequest.getEmail()) > 0;
        if (exists) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        try {
            String hashedPassword = BcryptUtil.bcryptHash(createUserRequest.getPassword());

            User user = new User(createUserRequest.getEmail(), createUserRequest.getUsername());
            Credential userCredentials = new Credential(createUserRequest.getEmail(), hashedPassword);

            userRepository.persist(user);
            credentialRepository.persist(userCredentials);

            return toUserResponse(user);
        }
        catch (Exception e) {
            throw new GenericException("Failed to register user due to server error");
        }
    }

    public UserResponse updateUserEmail(UpdateUserEmailRequest request) {

        Credential userCredentials = credentialRepository.authenticate(
                request.getCurrentEmail(),
                request.getPassword()
        );

        if (userCredentials == null) {
            throw new InvalidCredentialsException("Email and/or password are incorrect");
        }

        if (userCredentials.getEmail().equals(request.getNewEmail())) {
            throw new BadRequestException("New email cannot be the same as the current one");
        }

        boolean exists = userRepository.count("email", request.getNewEmail()) > 0;
        if (exists) {
            throw new EmailAlreadyUsedException("The provided new email is already used by another account");
        }

        User user = userRepository.findByEmail(request.getCurrentEmail());
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setEmail(request.getNewEmail());
        userCredentials.setEmail(request.getNewEmail());
        try {
            userRepository.update(user);
            credentialRepository.update(userCredentials);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user email due to server error");
        }

        return toUserResponse(user);
    }


    public UserResponse updateUserUsername(UpdateUserUsernameRequest request) {

        Credential userCredentials = credentialRepository.authenticate(request.getEmail(), request.getPassword());

        if (userCredentials == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.getUsername().equals(request.getNewUsername())) {
            throw new BadRequestException("The new username cannot be the same as the current one");
        }

        user.setUsername(request.getNewUsername());
        try {
            userRepository.update(user);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user username due to server error");
        }

        return toUserResponse(user);
    }


    public UserResponse getUserByEmail(String email) {
        User user;
        try {
            user = userRepository.findByEmail(email);
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve user due to server error");
        }

        if (user != null) {
            return toUserResponse(user);
        }
        else {
            throw new NotFoundException("No user found with email: " + email);
        }
    }

    public UserResponse updateUserPassword(UpdateUserPasswordRequest request) {
        Credential userCredentials = credentialRepository.authenticate(request.getEmail(), request.getCurrentPassword());

        if (userCredentials == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        if (BcryptUtil.matches(request.getNewPassword(), userCredentials.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the old one");
        }

        String newHashedPassword = BcryptUtil.bcryptHash(request.getNewPassword());
        userCredentials.setPassword(newHashedPassword);
        try {
            credentialRepository.update(userCredentials);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user password due to server error");
        }

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
        List<User> users;
        try {
            users = userRepository.findAll().stream().toList();
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve users due to server error");
        }

        List<UserResponse> userResponseList = new ArrayList<>();
        for (User user : users) {
            userResponseList.add(toUserResponse(user));
        }
        return userResponseList;
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
