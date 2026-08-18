package com.trello.clone.utils;

import com.trello.clone.service.exception.BadRequestException;

import java.util.List;

/**
 * Shared logics between lists on different entities (phases on
 * Project and tags on Task).
 */
public final class Labels {

    private Labels() {
    }

    /**
     * Validates and normalizes a single label
     *
     * @param what name used in error messages (phase / tag), so that the user can read
     *             a correct message
     */
    public static String require(String value, int maxLength, String what) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(what + " cannot be empty");
        }

        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BadRequestException(what + " cannot exceed " + maxLength + " characters");
        }

        return trimmed;
    }

    /** Position of the label, -1 if absent. Case-insensitive comparison. */
    public static int indexOf(List<String> values, String value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }

    /** Appends at the end if not already present. Returns true if the list is changed. */
    public static boolean addDistinct(List<String> values, String value) {
        if (indexOf(values, value) >= 0) {
            return false;
        }
        values.add(value);
        return true;
    }

    /** Removes a label if present. Returns true if the list is changed. */
    public static boolean remove(List<String> values, String value) {
        int index = indexOf(values, value);
        if (index < 0) {
            return false;
        }
        values.remove(index);
        return true;
    }
}
