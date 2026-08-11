package com.trello.clone.utils;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class EmailUtils {

    private EmailUtils() {
    }

    /** Used to make case-insensitive comparison between emails. */
    public String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
