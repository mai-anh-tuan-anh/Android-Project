package com.example.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Presentation state and business actions for module 6. */
public final class MealPlanViewModel extends AndroidViewModel {
    static final int MIN_SERVINGS = 1;
    static final int MAX_SERVINGS = 999;
    enum PickerSource { ALL, FAVORITES, COLLECTION }

    private final PersonalDataRepository repository;
    private final List<String> selectedRecipeIds = new ArrayList<>();
    private int pickerServings = MIN_SERVINGS;
    private String searchQuery = "";
    private PickerSource pickerSource = PickerSource.ALL;
    private long pickerCollectionId = -1L;

    public MealPlanViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    List<PersonalDataDbHelper.PlannedRecipe> getMealPlanRecipes() {
        return repository.getMealPlanRecipes();
    }

    List<PersonalDataDbHelper.CollectionRow> getRecipeCollections() { return repository.getCollections(); }

    List<MainActivity.RecipePreview> getPickerRecipes(List<MainActivity.RecipePreview> defaultRecipes) {
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

    List<MainActivity.RecipePreview> getAllSelectableRecipes(List<MainActivity.RecipePreview> defaultRecipes) {
        Map<String, MainActivity.RecipePreview> recipesById = new LinkedHashMap<>();
        addUniqueRecipes(recipesById, repository.getFavorites());
        for (PersonalDataDbHelper.CollectionRow collection : repository.getCollections()) {
            addUniqueRecipes(recipesById, repository.getCollectionRecipes(collection.id));
        }
        addUniqueRecipes(recipesById, defaultRecipes);
        for (PersonalDataDbHelper.PlannedRecipe planned : repository.getMealPlanRecipes()) {
            if (!recipesById.containsKey(planned.recipe.id)) {
                recipesById.put(planned.recipe.id, planned.recipe);
            }
        }
        return new ArrayList<>(recipesById.values());
    }

    private static void addUniqueRecipes(Map<String, MainActivity.RecipePreview> output,
                                         List<MainActivity.RecipePreview> recipes) {
        for (MainActivity.RecipePreview recipe : recipes) {
            if (!output.containsKey(recipe.id)) output.put(recipe.id, recipe);
        }
    }

    void removeMealPlanRecipe(String recipeId) { repository.removeMealPlanRecipe(recipeId); }
    void setMealPlanRecipeServings(String recipeId, int servings) {
        validateServings(servings);
        repository.setMealPlanRecipeServings(recipeId, servings);
    }

    void beginPicker() {
        selectedRecipeIds.clear();
        for (PersonalDataDbHelper.PlannedRecipe planned : repository.getMealPlanRecipes()) {
            selectedRecipeIds.add(planned.recipe.id);
        }
        pickerServings = MIN_SERVINGS;
        searchQuery = "";
        pickerSource = PickerSource.ALL;
        pickerCollectionId = -1L;
    }

    void restorePickerDraft(List<String> recipeIds, int servings, String query) {
        restorePickerDraft(recipeIds, servings, query, PickerSource.ALL, -1L);
    }

    void restorePickerDraft(List<String> recipeIds, int servings, String query,
                            PickerSource source, long collectionId) {
        selectedRecipeIds.clear();
        if (recipeIds != null) selectedRecipeIds.addAll(recipeIds);
        pickerServings = Math.max(MIN_SERVINGS, Math.min(MAX_SERVINGS, servings));
        searchQuery = query == null ? "" : query;
        pickerSource = source == null ? PickerSource.ALL : source;
        pickerCollectionId = pickerSource == PickerSource.COLLECTION ? collectionId : -1L;
    }

    List<String> getSelectedRecipeIds() { return new ArrayList<>(selectedRecipeIds); }
    int getPickerServings() { return pickerServings; }
    String getSearchQuery() { return searchQuery; }
    PickerSource getPickerSource() { return pickerSource; }
    long getPickerCollectionId() { return pickerCollectionId; }
    void setPickerSource(PickerSource source, long collectionId) {
        pickerSource = source == null ? PickerSource.ALL : source;
        pickerCollectionId = pickerSource == PickerSource.COLLECTION ? collectionId : -1L;
    }
    void setPickerServings(int servings) {
        validateServings(servings);
        pickerServings = servings;
    }

    void setSearchQuery(String query) { searchQuery = query == null ? "" : query; }

    void toggleRecipe(String recipeId) {
        if (selectedRecipeIds.contains(recipeId)) selectedRecipeIds.remove(recipeId);
        else selectedRecipeIds.add(recipeId);
    }

    boolean isSelected(String recipeId) { return selectedRecipeIds.contains(recipeId); }

    boolean isRecipeInPlan(String recipeId) {
        for (PersonalDataDbHelper.PlannedRecipe item : repository.getMealPlanRecipes()) {
            if (item.recipe.id.equals(recipeId)) return true;
        }
        return false;
    }

    List<MainActivity.RecipePreview> filterRecipes(List<MainActivity.RecipePreview> recipes) {
        List<MainActivity.RecipePreview> matches = new ArrayList<>();
        String query = normalizeSearchText(searchQuery);
        for (MainActivity.RecipePreview recipe : recipes) {
            if (query.isEmpty() || normalizeSearchText(recipe.title).contains(query)) matches.add(recipe);
        }
        return matches;
    }

    boolean saveSelection(List<MainActivity.RecipePreview> availableRecipes) {
        List<PersonalDataDbHelper.PlannedRecipe> currentPlan = repository.getMealPlanRecipes();
        if (selectedRecipeIds.isEmpty() && currentPlan.isEmpty()) return false;

        Map<String, PersonalDataDbHelper.PlannedRecipe> currentById = new HashMap<>();
        for (PersonalDataDbHelper.PlannedRecipe current : currentPlan) {
            currentById.put(current.recipe.id, current);
        }
        Map<String, MainActivity.RecipePreview> availableById = new HashMap<>();
        for (MainActivity.RecipePreview recipe : availableRecipes) availableById.put(recipe.id, recipe);

        List<PersonalDataDbHelper.PlannedRecipe> replacement = new ArrayList<>();
        for (String id : selectedRecipeIds) {
            PersonalDataDbHelper.PlannedRecipe previous = currentById.get(id);
            MainActivity.RecipePreview recipe = previous == null ? availableById.get(id) : previous.recipe;
            if (recipe == null) continue;
            int servings = servingsForSelection(pickerServings, previous);
            replacement.add(new PersonalDataDbHelper.PlannedRecipe(recipe, servings));
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

    static int servingsForSelection(int pickerServings,
                                    PersonalDataDbHelper.PlannedRecipe existingRecipe) {
        return existingRecipe == null ? pickerServings : existingRecipe.servings;
    }
}
