package com.trello.clone.service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.trello.clone.data.model.User;
import com.trello.clone.data.repository.AuthenticationRepository;
import com.trello.clone.data.repository.UserRepository;
import com.trello.clone.service.exception.*;
import com.trello.clone.utils.EmailUtils;
import com.trello.clone.web.model.user.*;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

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
        String normalizedEmail = EmailUtils.normalize(createUserRequest.getEmail());
        boolean exists = userRepository.count("email", normalizedEmail) > 0;
        if (exists) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        String hashedPassword = BcryptUtil.bcryptHash(createUserRequest.getPassword());
        User user = new User(normalizedEmail, createUserRequest.getUsername(), hashedPassword);
        try {
            userRepository.persist(user);
        }
        catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new UserAlreadyExistsException("A user with this email already exists");
            }
            throw new GenericException("Failed to register user due to server error");
        }

        return toUserResponse(user);
    }

    // TODO User userId as user identifier instead of the email or apply cascade logics (update email, update project members, update task assignees, invalidate token)
    public UserResponse updateUserEmail(UpdateUserEmailRequest request, String email) {
        String normalizedCurrentEmail = EmailUtils.normalize(email);
        String normalizedNewEmail = EmailUtils.normalize(request.getNewEmail());
        User user = authenticationRepository.authenticate(
                normalizedCurrentEmail,
                request.getPassword()
        );

        if (user == null) {
            throw new InvalidCredentialsException("Email and/or password are incorrect");
        }

        if (user.getEmail().equals(normalizedNewEmail)) {
            throw new BadRequestException("New email cannot be the same as the current one");
        }

        boolean exists = userRepository.count("email", normalizedNewEmail) > 0;
        if (exists) {
            throw new EmailAlreadyUsedException("The provided new email is already used by another account");
        }

        user.setEmail(normalizedNewEmail);

        try {
            userRepository.update(user);
        }
        catch (Exception e) {
            throw new GenericException("Failed to update user email due to server error");
        }

        return toUserResponse(user);
    }


    public UserResponse updateUserUsername(UpdateUserUsernameRequest request, String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        User user = authenticationRepository.authenticate(normalizedEmail, request.getPassword());

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
        String normalizedEmail = EmailUtils.normalize(email);
        User userCredentials = authenticationRepository.authenticate(
                normalizedEmail,
                request.getCurrentPassword()
        );

        if (userCredentials == null) {
            throw new InvalidCredentialsException("Email or password are incorrect");
        }

        if (BcryptUtil.matches(request.getNewPassword(), userCredentials.getPassword())) {
            throw new BadRequestException("The new password cannot be the same as the current one");
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

    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        User user;
        try {
            user = userRepository.findByEmail(normalizedEmail);
        }
        catch (Exception e) {
            throw new GenericException("Failed to retrieve user due to server error");
        }

        if (user != null) {
            return toUserResponse(user);
        }
        else {
            throw new NotFoundException("Unable to find user with the given email");
        }
    }

    public List<UserSummaryResponse> searchUsers(String searchTerm) {
        int maxResults = 20;
        List<User> users = userRepository.searchByUsernamePrefix(searchTerm, maxResults);

        List<UserSummaryResponse> result = new ArrayList<>();
        for (User user : users) {
            result.add(new UserSummaryResponse(user.getId().toHexString(), user.getUsername()));
        }
        return result;
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId().toHexString(),
                user.getEmail(),
                user.getUsername()
        );
    }
}
