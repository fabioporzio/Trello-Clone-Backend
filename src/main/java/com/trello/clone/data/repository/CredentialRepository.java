package com.trello.clone.data.repository;

import com.trello.clone.data.model.Credential;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CredentialRepository implements PanacheMongoRepository<Credential> {

}
