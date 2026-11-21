package com.example.myapplication;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * Presentation state and business filtering actions for Module 2 (Search and Filter).
 */
public final class DiscoverSearchViewModel extends AndroidViewModel {
    private static final String PREFS_NAME = "discover_search_prefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final int MAX_RECENT_SEARCHES = 8;

    private static final List<String> DEFAULT_SUGGESTIONS = Arrays.asList("cà chua", "món nhanh", "cá");
    private static final long RECENT_SEARCH_TTL_MS = 60L * 60L * 1000L;

    private final SharedPreferences prefs;
    private final List<String> recentSearches = new ArrayList<>();
    private final java.util.Map<String, Long> recentSearchTimes = new java.util.LinkedHashMap<>();

    // Active filters
    private String searchQuery = "";
    // 15 means <15m, 30 means 15..30m, and -1 means >30m.
    private Integer durationRange = null;
    private String difficulty = null;          // null or "Dễ", "Vừa", "Khó"
    private String diet = "Không giới hạn";     // "Không giới hạn" or "Món chay"
    private String includeIngredient = "";
    private String excludeIngredient = "";

    // Draft filters (for Search Filter bottom sheet/screen)
    private Integer draftDurationRange = null;
    private String draftDifficulty = null;
    private String draftDiet = "Không giới hạn";
    private String draftIncludeIngredient = "";
    private String draftExcludeIngredient = "";
    private final Set<String> selectedCookingIngredients = new LinkedHashSet<>();

    public DiscoverSearchViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadRecentSearches();
    }

    private void loadRecentSearches() {
        recentSearches.clear();
        recentSearchTimes.clear();
        String raw = prefs.getString(KEY_RECENT_SEARCHES, null);
        long now = System.currentTimeMillis();
        boolean changed = false;
        if (raw != null && !raw.trim().isEmpty()) {
            for (String entry : raw.split("\n")) {
                int separator = entry.indexOf('\t');
                if (separator < 0) { changed = true; continue; }
                try {
                    long timestamp = Long.parseLong(entry.substring(0, separator));
                    String query = entry.substring(separator + 1).trim();
                    if (!query.isEmpty() && now >= timestamp
                            && now - timestamp < RECENT_SEARCH_TTL_MS && !recentSearches.contains(query)) {
                        recentSearches.add(query);
                        recentSearchTimes.put(query, timestamp);
                    } else changed = true;
                } catch (NumberFormatException ignored) { changed = true; }
            }
        }
        if (changed) saveRecentSearches();
    }

    private void saveRecentSearches() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recentSearches.size(); i++) {
            if (i > 0) sb.append("\n");
            String query = recentSearches.get(i);
            sb.append(recentSearchTimes.getOrDefault(query, System.currentTimeMillis()))
                    .append('\t').append(query);
        }
        prefs.edit().putString(KEY_RECENT_SEARCHES, sb.toString()).apply();
    }

    private void expireRecentSearches() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<String> iterator = recentSearches.iterator();
        while (iterator.hasNext()) {
            String query = iterator.next();
            Long timestamp = recentSearchTimes.get(query);
            if (timestamp == null || now < timestamp || now - timestamp >= RECENT_SEARCH_TTL_MS) {
                iterator.remove();
                recentSearchTimes.remove(query);
                changed = true;
            }
        }
        if (changed) saveRecentSearches();
    }

    public List<String> getRecentSearches() {
        expireRecentSearches();
        return Collections.unmodifiableList(recentSearches);
    }

    public List<String> getSuggestions() {
        return Collections.unmodifiableList(DEFAULT_SUGGESTIONS);
    }

    public void addRecentSearch(String query) {
        if (query == null) return;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return;
        recentSearches.remove(trimmed);
        recentSearchTimes.put(trimmed, System.currentTimeMillis());
        recentSearches.add(0, trimmed);
        while (recentSearches.size() > MAX_RECENT_SEARCHES) {
            String removed = recentSearches.remove(recentSearches.size() - 1);
            recentSearchTimes.remove(removed);
        }
        saveRecentSearches();
    }

    public void removeRecentSearch(String query) {
        if (query != null && recentSearches.remove(query.trim())) {
            recentSearchTimes.remove(query.trim());
            saveRecentSearches();
        }
    }

    public void clearRecentSearches() {
        recentSearches.clear();
        recentSearchTimes.clear();
        saveRecentSearches();
    }

    public boolean isCookingIngredientSelected(String ingredient) {
        String wanted = normalize(ingredient);
        for (String selected : selectedCookingIngredients) if (normalize(selected).equals(wanted)) return true;
        return false;
    }

    public Set<String> getSelectedCookingIngredients() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(selectedCookingIngredients));
    }

    public void toggleCookingIngredient(String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) return;
        String wanted = normalize(ingredient);
        String existing = null;
        for (String selected : selectedCookingIngredients) if (normalize(selected).equals(wanted)) existing = selected;
        if (existing == null) selectedCookingIngredients.add(ingredient.trim());
        else selectedCookingIngredients.remove(existing);
    }

    public void clearCookingIngredients() { selectedCookingIngredients.clear(); }

    public List<MainActivity.RecipePreview> suggestRecipes(List<MainActivity.RecipePreview> recipes) {
        return IngredientSuggestionMatcher.suggest(recipes, selectedCookingIngredients,
                recipe -> recipe.ingredients, recipe -> recipe.ingredientListComplete,
                recipe -> recipe.difficulty, recipe -> recipe.durationMinutes,
                recipe -> recipe.title, recipe -> recipe.id);
    }

    public int availableIngredientCount(MainActivity.RecipePreview recipe) {
        return IngredientSuggestionMatcher.countAvailable(recipe.ingredients,
                IngredientSuggestionMatcher.normalizeSet(selectedCookingIngredients));
    }

    public List<String> missingIngredients(MainActivity.RecipePreview recipe) {
        return IngredientSuggestionMatcher.missing(recipe.ingredients, selectedCookingIngredients);
    }

    public int totalIngredientCount(MainActivity.RecipePreview recipe) {
        return IngredientSuggestionMatcher.total(recipe.ingredients);
    }

    public boolean hasCompleteRecipeIngredients(List<MainActivity.RecipePreview> recipes) {
        if (recipes == null) return false;
        for (MainActivity.RecipePreview recipe : recipes) {
            if (recipe.ingredientListComplete) return true;
        }
        return false;
    }

    public boolean hasIncompleteRecipeIngredients(List<MainActivity.RecipePreview> recipes) {
        if (recipes == null) return false;
        for (MainActivity.RecipePreview recipe : recipes) {
            if (!recipe.ingredientListComplete) return true;
        }
        return false;
    }

    // --- Search Query ---
    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
    }

    public void submitSearch(String query) {
        setSearchQuery(query);
        if (!searchQuery.trim().isEmpty()) {
            addRecentSearch(searchQuery.trim());
        }
    }

    // --- Filter State & Draft Management ---
    public void beginFilterDraft() {
        draftDurationRange = durationRange;
        draftDifficulty = difficulty;
        draftDiet = diet == null ? "Không giới hạn" : diet;
        draftIncludeIngredient = includeIngredient == null ? "" : includeIngredient;
        draftExcludeIngredient = excludeIngredient == null ? "" : excludeIngredient;
    }

    public void applyDraftFilter() {
        durationRange = draftDurationRange;
        difficulty = draftDifficulty;
        diet = draftDiet;
        includeIngredient = draftIncludeIngredient;
        excludeIngredient = draftExcludeIngredient;
    }

    public void resetDraftFilter() {
        draftDurationRange = null;
        draftDifficulty = null;
        draftDiet = "Không giới hạn";
        draftIncludeIngredient = "";
        draftExcludeIngredient = "";
    }

    public void resetActiveFilter() {
        durationRange = null;
        difficulty = null;
        diet = "Không giới hạn";
        includeIngredient = "";
        excludeIngredient = "";
        resetDraftFilter();
    }

    public boolean hasActiveFilters() {
        return durationRange != null
                || difficulty != null
                || (!"Không giới hạn".equalsIgnoreCase(diet) && diet != null && !diet.isEmpty())
                || (!includeIngredient.isEmpty())
                || (!excludeIngredient.isEmpty());
    }

    public List<String> getActiveFilterBadges() {
        List<String> badges = new ArrayList<>();
        if (durationRange != null) badges.add(durationBadge(durationRange));
        if (difficulty != null && !difficulty.isEmpty()) {
            badges.add(difficulty);
        }
        if (!"Không giới hạn".equalsIgnoreCase(diet) && diet != null && !diet.isEmpty()) {
            badges.add(diet);
        }
        if (!includeIngredient.trim().isEmpty()) {
            badges.add("+" + includeIngredient.trim());
        }
        if (!excludeIngredient.trim().isEmpty()) {
            badges.add("-" + excludeIngredient.trim());
        }
        return badges;
    }

    public void removeFilterBadge(String badge) {
        if (badge == null) return;
        if (badge.equals("< 15 phút") || badge.equals("15–30 phút") || badge.equals("> 30 phút")) durationRange = null;
        else if (badge.equals(difficulty)) difficulty = null;
        else if (badge.equals(diet)) diet = "Không giới hạn";
        else if (badge.startsWith("+")) includeIngredient = "";
        else if (badge.startsWith("-")) excludeIngredient = "";
    }

    // Draft setters/getters
    public Integer getDraftDurationRange() { return draftDurationRange; }
    public void setDraftDurationRange(Integer range) {
        if (range != null && range.equals(draftDurationRange)) draftDurationRange = null;
        else draftDurationRange = range;
    }

    public String getDraftDifficulty() { return draftDifficulty; }
    public void setDraftDifficulty(String diff) {
        if (diff != null && diff.equalsIgnoreCase(draftDifficulty)) draftDifficulty = null;
        else draftDifficulty = diff;
    }

    public String getDraftDiet() { return draftDiet; }
    public void setDraftDiet(String d) { draftDiet = d == null ? "Không giới hạn" : d; }

    public String getDraftIncludeIngredient() { return draftIncludeIngredient; }
    public void setDraftIncludeIngredient(String ing) { draftIncludeIngredient = ing == null ? "" : ing; }

    public String getDraftExcludeIngredient() { return draftExcludeIngredient; }
    public void setDraftExcludeIngredient(String ing) { draftExcludeIngredient = ing == null ? "" : ing; }

    // Active getters
    public Integer getDurationRange() { return durationRange; }
    public String getDifficulty() { return difficulty; }
    public String getDiet() { return diet; }
    public String getIncludeIngredient() { return includeIngredient; }
    public String getExcludeIngredient() { return excludeIngredient; }

    // --- Search & Filtering Logic ---
    public List<MainActivity.RecipePreview> filterRecipes(List<MainActivity.RecipePreview> allRecipes) {
        if (allRecipes == null) return Collections.emptyList();
        List<MainActivity.RecipePreview> results = new ArrayList<>();
        String normQuery = normalize(searchQuery);
        String normInclude = normalize(includeIngredient);
        String normExclude = normalize(excludeIngredient);

        for (MainActivity.RecipePreview recipe : allRecipes) {
            // 1. Query match (title, category, or ingredients)
            if (!normQuery.isEmpty()) {
                boolean matchTitle = normalize(recipe.title).contains(normQuery);
                boolean matchCategory = false;
                for (String cat : recipe.categories) {
                    if (normalize(cat).contains(normQuery)) {
                        matchCategory = true;
                        break;
                    }
                }
                boolean matchIngredient = false;
                for (String ing : recipe.ingredients) {
                    if (normalize(ing).contains(normQuery)) {
                        matchIngredient = true;
                        break;
                    }
                }
                if (!matchTitle && !matchCategory && !matchIngredient) {
                    continue;
                }
            }

            if (!matchesDuration(recipe.durationMinutes, durationRange)) continue;

            // 3. Difficulty filter
            if (difficulty != null && !difficulty.trim().isEmpty()) {
                if (!difficulty.equalsIgnoreCase(recipe.difficulty)) {
                    continue;
                }
            }

            // 4. Diet filter
            if ("Món chay".equalsIgnoreCase(diet)) {
                boolean isVegetarian = "Món chay".equalsIgnoreCase(recipe.diet)
                        || recipe.categories.contains("Món chay");
                if (!isVegetarian) {
                    continue;
                }
            }

            // 5. Included ingredient filter
            if (!normInclude.isEmpty()) {
                boolean hasIncluded = false;
                for (String ing : recipe.ingredients) {
                    if (normalize(ing).contains(normInclude)) {
                        hasIncluded = true;
                        break;
                    }
                }
                if (!hasIncluded) {
                    continue;
                }
            }

            // 6. Excluded ingredient filter
            if (!normExclude.isEmpty()) {
                boolean hasExcluded = false;
                for (String ing : recipe.ingredients) {
                    if (normalize(ing).contains(normExclude)) {
                        hasExcluded = true;
                        break;
                    }
                }
                if (hasExcluded) {
                    continue;
                }
            }

            results.add(recipe);
        }
        return results;
    }

    private static boolean matchesDuration(int minutes, Integer range) {
        if (range == null) return true;
        if (range == 15) return minutes < 15;
        if (range == 30) return minutes >= 15 && minutes <= 30;
        return minutes > 30;
    }

    private static String durationBadge(int range) {
        if (range == 15) return "< 15 phút";
        if (range == 30) return "15–30 phút";
        return "> 30 phút";
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return decomposed.replaceAll("\\s+", " ").trim();
    }
}
