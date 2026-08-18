package com.trello.clone.data.repository;

import com.trello.clone.data.model.User;
import com.trello.clone.utils.EmailUtils;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserRepository implements PanacheMongoRepository<User>  {

    public User findByEmail(String email) {
        return find("email", EmailUtils.normalize(email)).firstResult();
    }

    public List<User> searchByUsernamePrefix(String searchTerm, int limit) {
        String safeTerm = "^" + Pattern.quote(searchTerm);   // anti-injection escape
        return find("{'username': {'$regex': ?1, '$options': 'i'}}", safeTerm)
                .page(0, limit)
                .list();
    }
}
