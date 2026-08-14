package com.trello.clone.utils;

import com.trello.clone.service.exception.BadRequestException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Helper for requests formats shared by ProjectService and TaskService */
public final class RequestDeltas {

    private RequestDeltas() {
    }

    public static boolean isNotEmpty(Collection<String> values) {
        return values != null && !values.isEmpty();
    }

    /** Refused the same value present in both additions and removals. */
    public static void rejectOverlap(Collection<String> left, Collection<String> right, String label) {
        if (!isNotEmpty(left) || !isNotEmpty(right)) {
            return;
        }

        List<String> conflicts = new ArrayList<>();

        for (String leftValue : left) {
            for (String rightValue : right) {
                if (isSameValue(leftValue, rightValue)) {
                    conflicts.add(leftValue.trim());
                    break;
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new BadRequestException("The same " + label
                    + " cannot be added and removed in one request: " + String.join(", ", conflicts));
        }
    }

    /** Trims, lowercases and removes duplicates. */
    public static Set<String> normalizedEmails(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();

        if (values == null) {
            return result;
        }

        for (String value : values) {
            String email = EmailUtils.normalize(value);
            if (email != null && !email.isBlank()) {
                result.add(email);
            }
        }

        return result;
    }

    private static boolean isSameValue(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }
}
