package com.example.myapplication.presentation.mealplan;



import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation state and business actions for module 6. */
public final class MealPlanViewModel extends ViewModel {
    public static final int MIN_SERVINGS = 1;
    public static final int MAX_SERVINGS = 999;
    public enum PickerSource { ALL, FAVORITES, COLLECTION }

    private final PersonalDataRepository repository;
    private final List<String> selectedRecipeIds = new ArrayList<>();
    private int pickerServings = MIN_SERVINGS;
    private String searchQuery = "";
    private PickerSource pickerSource = PickerSource.ALL;
    private long pickerCollectionId = -1L;

    public MealPlanViewModel(PersonalDataRepository repository) {
        this.repository = repository;
    }

    public List<PlannedRecipe> getMealPlanRecipes() {
        return repository.getMealPlanRecipes();
    }

    public List<RecipeCollection> getRecipeCollections() { return repository.getCollections(); }

    public List<Recipe> getPickerRecipes(List<Recipe> defaultRecipes) {
        switch (pickerSource) {
            case FAVORITES:
                return repository.getFavorites();
            case COLLECTION:
                return pickerCollectionId > 0 ? repository.getCollectionRecipes(pickerCollectionId)
                        : new ArrayList<>();
            case ALL:
            default:
                return new ArrayList<>(defaultRecipes);
        }
    }

    public List<Recipe> getAllSelectableRecipes(List<Recipe> defaultRecipes) {
        Map<String, Recipe> recipesById = new LinkedHashMap<>();
        addUniqueRecipes(recipesById, repository.getFavorites());
        for (RecipeCollection collection : repository.getCollections()) {
            addUniqueRecipes(recipesById, repository.getCollectionRecipes(collection.id));
        }
        addUniqueRecipes(recipesById, defaultRecipes);
        for (PlannedRecipe planned : repository.getMealPlanRecipes()) {
            if (!recipesById.containsKey(planned.recipe.id)) {
                recipesById.put(planned.recipe.id, planned.recipe);
            }
        }
        return new ArrayList<>(recipesById.values());
    }

    private static void addUniqueRecipes(Map<String, Recipe> output,
                                         List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            if (!output.containsKey(recipe.id)) output.put(recipe.id, recipe);
        }
    }

    public void removeMealPlanRecipe(String recipeId) { repository.removeMealPlanRecipe(recipeId); }

    public RemovedPlanRecipe removeMealPlanRecipeForUndo(String recipeId) {
        List<PlannedRecipe> current = repository.getMealPlanRecipes();
        for (int i = 0; i < current.size(); i++) {
            PlannedRecipe planned = current.get(i);
            if (planned.recipe.id.equals(recipeId)) {
                current.remove(i);
                repository.replaceMealPlanRecipes(current);
                return new RemovedPlanRecipe(planned, i);
            }
        }
        return null;
    }

    public boolean restoreMealPlanRecipe(RemovedPlanRecipe removed) {
        if (removed == null) return false;
        List<PlannedRecipe> current = repository.getMealPlanRecipes();
        for (PlannedRecipe planned : current) {
            if (planned.recipe.id.equals(removed.recipe.recipe.id)) return false;
        }
        current.add(Math.min(removed.position, current.size()), removed.recipe);
        repository.replaceMealPlanRecipes(current);
        return true;
    }

    public boolean moveMealPlanRecipe(String recipeId, int direction) {
        if (direction != -1 && direction != 1) return false;
        List<PlannedRecipe> current = repository.getMealPlanRecipes();
        int index = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).recipe.id.equals(recipeId)) { index = i; break; }
        }
        int destination = index + direction;
        if (index < 0 || destination < 0 || destination >= current.size()) return false;
        PlannedRecipe moved = current.get(index);
        current.set(index, current.get(destination));
        current.set(destination, moved);
        repository.replaceMealPlanRecipes(current);
        return true;
    }

    public MealDurationEstimate estimateTotalDuration(List<PlannedRecipe> planned) {
        int totalMinutes = 0;
        int knownDurations = 0;
        for (PlannedRecipe item : planned) {
            int minutes = parseDurationMinutes(item.recipe.duration);
            if (minutes <= 0) continue;
            totalMinutes += minutes;
            knownDurations++;
        }
        return new MealDurationEstimate(totalMinutes, knownDurations, planned.size() - knownDurations);
    }

    private int parseDurationMinutes(String value) {
        if (value == null || value.trim().isEmpty()
                || Pattern.compile("\\d+\\s*[-–]\\s*\\d+").matcher(value).find()) return -1;
        Matcher matcher = Pattern.compile("(?:(\\d+)\\s*giờ)?\\s*(?:(\\d+)\\s*phút)?",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(value);
        if (!matcher.find() || (matcher.group(1) == null && matcher.group(2) == null)) return -1;
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return hours * 60 + minutes;
    }

    public static final class RemovedPlanRecipe {
        final PlannedRecipe recipe;
        final int position;
        RemovedPlanRecipe(PlannedRecipe recipe, int position) {
            this.recipe = recipe;
            this.position = position;
        }
    }

    public static final class MealDurationEstimate {
        public final int knownMinutes;
        public final int knownRecipes;
        public final int unknownRecipes;
        public MealDurationEstimate(int knownMinutes, int knownRecipes, int unknownRecipes) {
            this.knownMinutes = knownMinutes;
            this.knownRecipes = knownRecipes;
            this.unknownRecipes = unknownRecipes;
        }
    }

    public void setMealPlanRecipeServings(String recipeId, int servings) {
        validateServings(servings);
        repository.setMealPlanRecipeServings(recipeId, servings);
    }

    public void beginPicker() {
        selectedRecipeIds.clear();
        for (PlannedRecipe planned : repository.getMealPlanRecipes()) {
            selectedRecipeIds.add(planned.recipe.id);
        }
        pickerServings = MIN_SERVINGS;
        searchQuery = "";
        pickerSource = PickerSource.ALL;
        pickerCollectionId = -1L;
    }

    public void restorePickerDraft(List<String> recipeIds, int servings, String query) {
        restorePickerDraft(recipeIds, servings, query, PickerSource.ALL, -1L);
    }

    public void restorePickerDraft(List<String> recipeIds, int servings, String query,
                            PickerSource source, long collectionId) {
        selectedRecipeIds.clear();
        if (recipeIds != null) selectedRecipeIds.addAll(recipeIds);
        pickerServings = Math.max(MIN_SERVINGS, Math.min(MAX_SERVINGS, servings));
        searchQuery = query == null ? "" : query;
        pickerSource = source == null ? PickerSource.ALL : source;
        pickerCollectionId = pickerSource == PickerSource.COLLECTION ? collectionId : -1L;
    }

    public List<String> getSelectedRecipeIds() { return new ArrayList<>(selectedRecipeIds); }
    public int getPickerServings() { return pickerServings; }
    public String getSearchQuery() { return searchQuery; }
    public PickerSource getPickerSource() { return pickerSource; }
    public long getPickerCollectionId() { return pickerCollectionId; }
    public void setPickerSource(PickerSource source, long collectionId) {
        pickerSource = source == null ? PickerSource.ALL : source;
        pickerCollectionId = pickerSource == PickerSource.COLLECTION ? collectionId : -1L;
    }
    public void setPickerServings(int servings) {
        validateServings(servings);
        pickerServings = servings;
    }

    public void setSearchQuery(String query) { searchQuery = query == null ? "" : query; }

    public void toggleRecipe(String recipeId) {
        if (selectedRecipeIds.contains(recipeId)) selectedRecipeIds.remove(recipeId);
        else selectedRecipeIds.add(recipeId);
    }

    public boolean isSelected(String recipeId) { return selectedRecipeIds.contains(recipeId); }

    public boolean isRecipeInPlan(String recipeId) {
        for (PlannedRecipe item : repository.getMealPlanRecipes()) {
            if (item.recipe.id.equals(recipeId)) return true;
        }
        return false;
    }

    public List<Recipe> filterRecipes(List<Recipe> recipes) {
        List<Recipe> matches = new ArrayList<>();
        String query = normalizeSearchText(searchQuery);
        for (Recipe recipe : recipes) {
            if (query.isEmpty() || normalizeSearchText(recipe.title).contains(query)) matches.add(recipe);
        }
        return matches;
    }

    public boolean saveSelection(List<Recipe> availableRecipes) {
        List<PlannedRecipe> currentPlan = repository.getMealPlanRecipes();
        if (selectedRecipeIds.isEmpty() && currentPlan.isEmpty()) return false;

        Map<String, PlannedRecipe> currentById = new HashMap<>();
        for (PlannedRecipe current : currentPlan) {
            currentById.put(current.recipe.id, current);
        }
        Map<String, Recipe> availableById = new HashMap<>();
        for (Recipe recipe : availableRecipes) availableById.put(recipe.id, recipe);

        List<PlannedRecipe> replacement = new ArrayList<>();
        for (String id : selectedRecipeIds) {
            PlannedRecipe previous = currentById.get(id);
            Recipe recipe = previous == null ? availableById.get(id) : previous.recipe;
            if (recipe == null) continue;
            int servings = servingsForSelection(pickerServings, previous);
            replacement.add(new PlannedRecipe(recipe, servings));
        }
        repository.replaceMealPlanRecipes(replacement);
        selectedRecipeIds.clear();
        return true;
    }

    private static String normalizeSearchText(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private static void validateServings(int servings) {
        if (servings < MIN_SERVINGS || servings > MAX_SERVINGS) {
            throw new IllegalArgumentException("servings must be between 1 and 999");
        }
    }

    public static int servingsForSelection(int pickerServings,
                                    PlannedRecipe existingRecipe) {
        return existingRecipe == null ? pickerServings : existingRecipe.servings;
    }
}
