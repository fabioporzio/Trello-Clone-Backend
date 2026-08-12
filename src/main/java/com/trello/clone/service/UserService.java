package com.trello.clone.service;

import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.AuthenticationRepository;
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
    private final AuthenticationRepository authenticationRepository;

    public UserService(
            UserRepository userRepository,
            AuthenticationRepository authenticationRepository
    ) {
        this.userRepository = userRepository;
        this.authenticationRepository = authenticationRepository;
    }

    public UserResponse registerUser(CreateUserRequest createUserRequest) {
        boolean exists = userRepository.count("email", createUserRequest.getEmail()) > 0;
        if (exists) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        try {
            String hashedPassword = BcryptUtil.bcryptHash(createUserRequest.getPassword());

            User user = new User(createUserRequest.getEmail(), createUserRequest.getUsername(), hashedPassword);
            userRepository.persist(user);

            return toUserResponse(user);
        }
        catch (Exception e) {
            throw new GenericException("Failed to register user due to server error");
        }
    }

    public UserResponse updateUserEmail(UpdateUserEmailRequest request, String email) {
        User user = authenticationRepository.authenticate(
                email,
                request.getPassword()
        );

        if (user == null) {
            throw new InvalidCredentialsException("Email and/or password are incorrect");
        }

        if (user.getEmail().equals(request.getNewEmail())) {
            throw new BadRequestException("New email cannot be the same as the current one");
        }

        boolean exists = userRepository.count("email", request.getNewEmail()) > 0;
        if (exists) {
            throw new EmailAlreadyUsedException("The provided new email is already used by another account");
        }

        user.setEmail(request.getNewEmail());

        try {
            userRepository.update(user);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user email due to server error");
        }

        return toUserResponse(user);
    }


    public UserResponse updateUserUsername(UpdateUserUsernameRequest request, String email) {

        User user = authenticationRepository.authenticate(email, request.getPassword());

        if (user == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
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

    public UserResponse updateUserPassword(UpdateUserPasswordRequest request, String email) {
        User userCredentials = authenticationRepository.authenticate(
                email,
                request.getCurrentPassword()
        );

        if (userCredentials == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        String newHashedPassword = BcryptUtil.bcryptHash(request.getNewPassword());
        userCredentials.setPassword(newHashedPassword);
        try {
            userRepository.update(userCredentials);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user password due to server error");
        }

        return toUserResponse(userCredentials);
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
            throw new UnauthorizedException("Unable to retrieve user. Please verify email and password");
        }
    }

    public List<UserSummaryResponse> searchUsers(String searchTerm) {
        int maxResults = 20;
        List<User> users = userRepository.searchByUsernamePrefix(searchTerm, maxResults);

        List<UserSummaryResponse> result = new ArrayList<>();
        for (User user : users) {
            result.add(new UserSummaryResponse(user.id.toString(), user.getUsername()));
        }
        return result;
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getUsername()
        );
    }
}
