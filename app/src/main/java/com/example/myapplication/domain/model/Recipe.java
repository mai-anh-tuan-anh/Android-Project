package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Domain data for a recipe; deliberately independent of Activity and Android UI. */
public final class Recipe {
    /** Catalog projection for the legacy primitive field when the total is unknown. */
    public static final int UNKNOWN_DURATION_MINUTES = -1;

    public enum PublicationStatus { DRAFT, PUBLISHED, ARCHIVED }
    public enum IngredientDataStatus { COMPLETE, INCOMPLETE, UNKNOWN }

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
    /** Null for legacy summaries read from fixtures or personal storage. */
    public final Details details;

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
        this(id, title, imageName, duration, difficulty, durationMinutes, diet, categories, ingredients,
                publishedAtMillis, ingredientListComplete, null);
    }

    private Recipe(String id, String title, String imageName, String duration, String difficulty,
                   int durationMinutes, String diet, List<String> categories, List<String> ingredients,
                   long publishedAtMillis, boolean ingredientListComplete, Details details) {
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
        this.details = details;
    }

    /** Names are compatibility projections resolved by the catalog mapper. */
    public static Recipe fromCatalog(String id, String title, String imageName, String difficulty,
                                     String diet, List<String> categoryNames, List<String> ingredientNames,
                                     Details details) {
        if (id == null || id.trim().isEmpty() || title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("catalog recipe requires an ID and title");
        }
        Objects.requireNonNull(details, "details");
        for (RecipeIngredient line : details.ingredientLines) {
            if (!id.equals(line.recipeId)) throw new IllegalArgumentException("ingredient line belongs to another recipe");
        }
        for (CookingStep step : details.steps) {
            if (!id.equals(step.recipeId)) throw new IllegalArgumentException("cooking step belongs to another recipe");
        }
        List<String> names = ingredientNames == null ? Collections.emptyList() : new ArrayList<>(ingredientNames);
        boolean completeNames = details.ingredientDataStatus == IngredientDataStatus.COMPLETE
                && !details.ingredientLines.isEmpty() && names.size() == details.ingredientLines.size()
                && names.stream().allMatch(name -> name != null && !name.trim().isEmpty());
        Integer total = details.durationMinutes;
        return new Recipe(id, title, imageName, total == null ? "" : total + " phút", difficulty,
                total == null ? UNKNOWN_DURATION_MINUTES : total, diet, categoryNames, names,
                details.publishedAtMillis == null ? 0L : details.publishedAtMillis, completeNames, details);
    }

    public static final class Details {
        public final String description;
        public final String imagePath;
        public final int baseServings;
        public final Integer prepMinutes;
        public final Integer cookMinutes;
        public final Integer durationMinutes;
        public final PublicationStatus publicationStatus;
        public final Long publishedAtMillis;
        public final Long updatedAtMillis;
        public final IngredientDataStatus ingredientDataStatus;
        public final List<String> categoryIds;
        public final List<String> dietIds;
        public final List<String> tools;
        public final String tip;
        public final List<RecipeIngredient> ingredientLines;
        public final List<CookingStep> steps;

        public Details(String description, String imagePath, int baseServings,
                       Integer prepMinutes, Integer cookMinutes, Integer durationMinutes,
                       PublicationStatus publicationStatus, Long publishedAtMillis, Long updatedAtMillis,
                       IngredientDataStatus ingredientDataStatus, List<String> categoryIds, List<String> dietIds,
                       List<String> tools, String tip, List<RecipeIngredient> ingredientLines, List<CookingStep> steps) {
            if (baseServings < 1 || baseServings > 999) {
                throw new IllegalArgumentException("baseServings must be between 1 and 999");
            }
            if ((prepMinutes != null && prepMinutes < 0) || (cookMinutes != null && cookMinutes < 0)
                    || (durationMinutes != null && durationMinutes < 0)) {
                throw new IllegalArgumentException("recipe durations must be non-negative");
            }
            if ((publishedAtMillis != null && publishedAtMillis < 0)
                    || (updatedAtMillis != null && updatedAtMillis < 0)) {
                throw new IllegalArgumentException("recipe timestamps must be non-negative");
            }
            this.description = description;
            this.imagePath = imagePath;
            this.baseServings = baseServings;
            this.prepMinutes = prepMinutes;
            this.cookMinutes = cookMinutes;
            this.durationMinutes = durationMinutes;
            this.publicationStatus = Objects.requireNonNull(publicationStatus, "publicationStatus");
            this.publishedAtMillis = publishedAtMillis;
            this.updatedAtMillis = updatedAtMillis;
            this.ingredientDataStatus = Objects.requireNonNull(ingredientDataStatus, "ingredientDataStatus");
            this.categoryIds = copyDistinctIds(categoryIds);
            this.dietIds = copyDistinctIds(dietIds);
            this.tools = Collections.unmodifiableList(new ArrayList<>(tools));
            this.tip = tip;

            List<RecipeIngredient> lines = new ArrayList<>(ingredientLines);
            Set<String> ingredientLineIds = new HashSet<>();
            Set<Integer> ingredientOrders = new HashSet<>();
            for (RecipeIngredient line : lines) {
                if (!ingredientLineIds.add(line.recipeIngredientId) || !ingredientOrders.add(line.sortOrder)) {
                    throw new IllegalArgumentException("ingredient line IDs and positions must be unique within a recipe");
                }
            }
            lines.sort(Comparator.comparingInt(line -> line.sortOrder));
            this.ingredientLines = Collections.unmodifiableList(lines);

            List<CookingStep> orderedSteps = new ArrayList<>(steps);
            Set<String> stepIds = new HashSet<>();
            Set<Integer> stepOrders = new HashSet<>();
            for (CookingStep step : orderedSteps) {
                if (!stepIds.add(step.stepId) || !stepOrders.add(step.sortOrder)) {
                    throw new IllegalArgumentException("step IDs and positions must be unique within a recipe");
                }
                if (ingredientDataStatus == IngredientDataStatus.COMPLETE
                        && !ingredientLineIds.containsAll(step.recipeIngredientIds)) {
                    throw new IllegalArgumentException("step refers to an unknown ingredient line");
                }
            }
            orderedSteps.sort(Comparator.comparingInt(step -> step.sortOrder));
            this.steps = Collections.unmodifiableList(orderedSteps);
        }

        /** Ingredient membership may be known even when an amount or unit is missing. */
        public boolean hasIngredientsForServingAdjustment() {
            return ingredientDataStatus == IngredientDataStatus.COMPLETE && !ingredientLines.isEmpty()
                    && ingredientLines.stream().allMatch(RecipeIngredient::hasAmountOrQualitativeNote);
        }

        private static List<String> copyDistinctIds(List<String> ids) {
            List<String> copy = new ArrayList<>(ids);
            Set<String> seenIds = new HashSet<>();
            for (String id : copy) {
                if (id == null || id.trim().isEmpty() || !seenIds.add(id)) {
                    throw new IllegalArgumentException("catalog relationship IDs must be distinct and non-blank");
                }
            }
            return Collections.unmodifiableList(copy);
        }
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
