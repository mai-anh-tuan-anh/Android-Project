package com.example.myapplication.domain.catalog;

import com.example.myapplication.domain.model.Recipe;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Pure matching and ranking rules for Module 8. */
public final class IngredientSuggestionMatcher {
    private IngredientSuggestionMatcher() {}

    public static <T> List<T> suggest(List<T> recipes, Set<String> availableIngredients,
                                      Function<T, List<String>> ingredients,
                                      Predicate<T> hasCompleteIngredients,
                                      Function<T, String> difficulty,
                                      ToIntFunction<T> durationMinutes,
                                      Function<T, String> name,
                                      Function<T, String> recipeId) {
        if (recipes == null || availableIngredients == null || availableIngredients.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalizedAvailable = normalizeSet(availableIngredients);
        List<T> matches = new ArrayList<>();
        for (T recipe : recipes) {
            if (hasCompleteIngredients.test(recipe)
                    && countAvailable(ingredients.apply(recipe), normalizedAvailable) > 0) {
                matches.add(recipe);
            }
        }
        matches.sort(Comparator
                .comparingInt((T recipe) -> missing(ingredients.apply(recipe), normalizedAvailable).size())
                .thenComparingInt(recipe -> difficultyRank(difficulty.apply(recipe)))
                .thenComparingInt(durationMinutes)
                .thenComparing(recipe -> normalize(name.apply(recipe)))
                .thenComparing(recipeId));
        return matches;
    }

    public static int countAvailable(List<String> ingredients, Set<String> normalizedAvailable) {
        Set<String> distinct = new LinkedHashSet<>();
        if (ingredients == null) return 0;
        for (String ingredient : ingredients) {
            String normalized = normalize(ingredient);
            if (!normalized.isEmpty()) distinct.add(normalized);
        }
        int count = 0;
        for (String ingredient : distinct) if (normalizedAvailable.contains(ingredient)) count++;
        return count;
    }

    public static List<String> missing(List<String> ingredients, Set<String> availableIngredients) {
        if (ingredients == null) return Collections.emptyList();
        Set<String> normalizedAvailable = normalizeSet(availableIngredients);
        Set<String> distinct = new LinkedHashSet<>();
        List<String> missing = new ArrayList<>();
        for (String ingredient : ingredients) {
            String normalized = normalize(ingredient);
            if (!normalized.isEmpty() && distinct.add(normalized) && !normalizedAvailable.contains(normalized)) {
                missing.add(ingredient);
            }
        }
        return missing;
    }

    public static int total(List<String> ingredients) {
        if (ingredients == null) return 0;
        Set<String> distinct = new LinkedHashSet<>();
        for (String ingredient : ingredients) {
            String normalized = normalize(ingredient);
            if (!normalized.isEmpty()) distinct.add(normalized);
        }
        return distinct.size();
    }

    public static Set<String> normalizeSet(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) for (String value : values) normalized.add(normalize(value));
        return normalized;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd')
                .replaceAll("\\s+", " ").trim();
    }

    private static int difficultyRank(String difficulty) {
        if ("Dễ".equalsIgnoreCase(difficulty)) return 0;
        if ("Vừa".equalsIgnoreCase(difficulty) || "Trung bình".equalsIgnoreCase(difficulty)) return 1;
        if ("Khó".equalsIgnoreCase(difficulty)) return 2;
        return 3;
    }
}
