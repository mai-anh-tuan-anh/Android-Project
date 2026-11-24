package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Domain data for a recipe; deliberately independent of Activity and Android UI. */
public final class Recipe {
    public final String id;
    public final String title;
    public final String imageName;
    public final String duration;
    public final String difficulty;
    public final int durationMinutes;
    public final String diet;
    public final List<String> categories;
    public final List<String> ingredients;
    public final long publishedAtMillis;
    public final boolean ingredientListComplete;

    public Recipe(String id, String title, String imageName, String duration, String difficulty) {
        this(id, title, imageName, duration, difficulty, parseMinutes(duration), "Không giới hạn",
                Collections.emptyList(), Collections.emptyList(), 0L);
    }

    public Recipe(String id, String title, String imageName, String duration, String difficulty,
                  int durationMinutes, String diet, List<String> categories, List<String> ingredients) {
        this(id, title, imageName, duration, difficulty, durationMinutes, diet, categories, ingredients, 0L);
    }

    public Recipe(String id, String title, String imageName, String duration, String difficulty,
                  int durationMinutes, String diet, List<String> categories, List<String> ingredients,
                  long publishedAtMillis) {
        this(id, title, imageName, duration, difficulty, durationMinutes, diet, categories, ingredients,
                publishedAtMillis, false);
    }

    public Recipe(String id, String title, String imageName, String duration, String difficulty,
                  int durationMinutes, String diet, List<String> categories, List<String> ingredients,
                  long publishedAtMillis, boolean ingredientListComplete) {
        this.id = id;
        this.title = title;
        this.imageName = imageName;
        this.duration = duration;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.diet = diet == null ? "Không giới hạn" : diet;
        this.categories = categories == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(categories));
        this.ingredients = ingredients == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.publishedAtMillis = Math.max(0L, publishedAtMillis);
        this.ingredientListComplete = ingredientListComplete;
    }

    private static int parseMinutes(String duration) {
        if (duration == null) return 30;
        String digits = duration.replaceAll("[^0-9]", "").trim();
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 30;
        }
    }
}
