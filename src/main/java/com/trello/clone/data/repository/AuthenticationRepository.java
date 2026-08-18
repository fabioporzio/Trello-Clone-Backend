package com.trello.clone.data.repository;

import com.trello.clone.data.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationRepository {

    private final UserRepository userRepository;

    public AuthenticationRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            boolean matches = BcryptUtil.matches(password, user.getPassword());

            if (matches) {
                return user;
            }
            else {
                return null;
            }
        }
        return null;
    }
}
