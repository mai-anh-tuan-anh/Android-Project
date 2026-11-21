package com.example.myapplication;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/** Stateless catalog rules used by the Discover screens. */
public final class DiscoverViewModel {
    public <T> List<T> recipesInCategory(List<T> recipes, String category,
                                        Function<T, List<String>> categories) {
        if (recipes == null) return Collections.emptyList();
        if (category == null || category.isEmpty() || "Tất cả".equals(category)) {
            return new ArrayList<>(recipes);
        }
        String wanted = normalize(category);
        List<T> result = new ArrayList<>();
        for (T recipe : recipes) {
            List<String> labels = categories.apply(recipe);
            if (labels == null) continue;
            for (String label : labels) {
                if (normalize(label).equals(wanted)) {
                    result.add(recipe);
                    break;
                }
            }
        }
        return result;
    }

    public <T> List<T> quickAndEasy(List<T> recipes, ToIntFunction<T> durationMinutes,
                                    Function<T, String> difficulty) {
        if (recipes == null) return Collections.emptyList();
        List<T> result = new ArrayList<>();
        for (T recipe : recipes) {
            if (durationMinutes.applyAsInt(recipe) <= 30
                    && "Dễ".equalsIgnoreCase(difficulty.apply(recipe))) result.add(recipe);
        }
        result.sort(Comparator.comparingInt(durationMinutes));
        return result;
    }

    public <T> List<T> newlyPublished(List<T> recipes, ToLongFunction<T> publishedAtMillis) {
        if (recipes == null) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<T> result = new ArrayList<>();
        for (T recipe : recipes) {
            long publishedAt = publishedAtMillis.applyAsLong(recipe);
            if (publishedAt > 0L && publishedAt <= now) result.add(recipe);
        }
        result.sort((left, right) -> Long.compare(
                publishedAtMillis.applyAsLong(right), publishedAtMillis.applyAsLong(left)));
        return result;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd')
                .replaceAll("\\s+", " ").trim();
    }
}
