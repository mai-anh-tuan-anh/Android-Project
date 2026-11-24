package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Catalog identity and approved names for a shared ingredient. */
public final class Ingredient {
    public enum Status { ACTIVE, INACTIVE }

    public final String ingredientId;
    public final String canonicalName;
    public final String groupName;
    public final Status status;
    public final List<String> aliases;

    public Ingredient(String ingredientId, String canonicalName, String groupName, Status status,
                      List<String> aliases) {
        if (ingredientId == null || ingredientId.trim().isEmpty()
                || canonicalName == null || canonicalName.trim().isEmpty()) {
            throw new IllegalArgumentException("catalog ingredient requires an ID and canonical name");
        }
        List<String> names = new ArrayList<>(aliases);
        for (String alias : names) {
            if (alias == null || alias.trim().isEmpty()) {
                throw new IllegalArgumentException("ingredient alias must not be blank");
            }
        }
        this.ingredientId = ingredientId;
        this.canonicalName = canonicalName;
        this.groupName = groupName;
        this.status = Objects.requireNonNull(status, "status");
        this.aliases = Collections.unmodifiableList(names);
    }
}
