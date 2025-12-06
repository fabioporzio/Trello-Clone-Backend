package com.trello.clone.utils;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class MergeArraysUtils {

    private MergeArraysUtils() {}

    // Cleans a list of tags by removing null, empty strings and empty spaces at the beginning and at the end
    public List<String> cleanList(List<String> input) {
        if (input == null) return Collections.emptyList();

        return input.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // Merges two lists while maintaining order and removing duplicates
    public List<String> mergeDistinct(List<String> original, List<String> fromRequest) {

        List<String> left = original == null ? Collections.emptyList() : original;
        List<String> right = cleanList(fromRequest);

        return Stream.concat(left.stream(), right.stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
