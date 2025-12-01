package com.trello.clone.data.repository;

import com.trello.clone.data.model.Credential;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CredentialRepository implements PanacheMongoRepository<Credential> {

    public Credential authenticate(String email, String password) {
        Credential userCredentials = findByEmail(email);
        if (userCredentials != null) {
            boolean matches = BcryptUtil.matches(password, userCredentials.getPassword());
            if (matches) {
                return userCredentials;
            }
            else {
                return null;
            }
        }
        return null;
    }

    public Credential findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
