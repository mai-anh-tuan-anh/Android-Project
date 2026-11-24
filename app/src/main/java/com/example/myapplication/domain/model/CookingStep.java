package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A step's identity is independent of its position in the recipe. */
public final class CookingStep {
    public final String stepId;
    public final String recipeId;
    public final String title;
    public final String instruction;
    public final int sortOrder;
    public final Integer durationSeconds;
    public final String imagePath;
    public final List<String> recipeIngredientIds;

    public CookingStep(String stepId, String recipeId, String title, String instruction, int sortOrder,
                       Integer durationSeconds, String imagePath, List<String> recipeIngredientIds) {
        if (stepId == null || stepId.trim().isEmpty() || recipeId == null || recipeId.trim().isEmpty()) {
            throw new IllegalArgumentException("cooking step requires a step and recipe ID");
        }
        if (title == null || title.trim().isEmpty() || instruction == null || instruction.trim().isEmpty()) {
            throw new IllegalArgumentException("cooking step requires a title and instruction");
        }
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must be non-negative");
        if (durationSeconds != null && durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive when present");
        }
        List<String> ingredientIds = new ArrayList<>(recipeIngredientIds);
        Set<String> seenIds = new HashSet<>();
        for (String id : ingredientIds) {
            if (id == null || id.trim().isEmpty() || !seenIds.add(id)) {
                throw new IllegalArgumentException("step ingredient references must be distinct, non-blank IDs");
            }
        }
        this.stepId = stepId;
        this.recipeId = recipeId;
        this.title = title;
        this.instruction = instruction;
        this.sortOrder = sortOrder;
        this.durationSeconds = durationSeconds;
        this.imagePath = imagePath;
        this.recipeIngredientIds = Collections.unmodifiableList(ingredientIds);
    }
}
