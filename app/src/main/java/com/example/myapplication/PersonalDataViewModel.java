package com.example.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Presentation state and actions for module 5. */
public final class PersonalDataViewModel extends AndroidViewModel {
    private final PersonalDataRepository repository;
    private final List<String> selectedCollectionRecipeIds = new ArrayList<>();
    private String collectionNameDraft = "";
    private boolean collectionSubmitAttempted;

    public PersonalDataViewModel(@NonNull Application application) {
        super(application);
        repository = PersonalDataRepository.getInstance(application);
    }

    void seedDesignPreviewIfNeeded(List<MainActivity.RecipePreview> recipes) {
        repository.seedDesignPreviewIfNeeded(recipes);
    }
    void addFavorite(MainActivity.RecipePreview recipe) { repository.addFavorite(recipe); }
    void removeFavorite(String recipeId) { repository.removeFavorite(recipeId); }
    boolean isFavorite(String recipeId) { return repository.isFavorite(recipeId); }
    List<MainActivity.RecipePreview> getFavorites() { return repository.getFavorites(); }
    List<MainActivity.RecipePreview> getHistory() { return repository.getHistory(); }
    void recordViewedRecipe(MainActivity.RecipePreview recipe) { repository.recordViewedRecipe(recipe); }
    List<PersonalDataDbHelper.CollectionRow> getCollections() { return repository.getCollections(); }
    List<MainActivity.RecipePreview> getCollectionRecipes(long id) { return repository.getCollectionRecipes(id); }
    boolean saveCollection(String name, List<MainActivity.RecipePreview> recipes, Long id) {
        return repository.saveCollection(name, recipes, id);
    }

    void beginCollectionForm(String currentName, List<MainActivity.RecipePreview> currentRecipes) {
        collectionNameDraft = currentName == null ? "" : currentName;
        collectionSubmitAttempted = false;
        selectedCollectionRecipeIds.clear();
        if (currentRecipes != null) {
            for (MainActivity.RecipePreview recipe : currentRecipes) selectedCollectionRecipeIds.add(recipe.id);
        }
    }

    String getCollectionNameDraft() { return collectionNameDraft; }
    void setCollectionNameDraft(String name) { collectionNameDraft = name == null ? "" : name; }
    boolean wasCollectionSubmitAttempted() { return collectionSubmitAttempted; }
    void markCollectionSubmitAttempted() { collectionSubmitAttempted = true; }
    boolean isCollectionRecipeSelected(String recipeId) { return selectedCollectionRecipeIds.contains(recipeId); }
    void toggleCollectionRecipe(String recipeId) {
        if (selectedCollectionRecipeIds.contains(recipeId)) selectedCollectionRecipeIds.remove(recipeId);
        else selectedCollectionRecipeIds.add(recipeId);
    }
    boolean isCollectionNameAvailable(String name, Long collectionId) {
        return repository.isCollectionNameAvailable(name, collectionId);
    }
    Long saveCollectionForm(Long collectionId, List<MainActivity.RecipePreview> availableRecipes) {
        if (collectionNameDraft.trim().isEmpty() || !repository.isCollectionNameAvailable(collectionNameDraft, collectionId)) {
            return null;
        }
        Map<String, MainActivity.RecipePreview> recipesById = new HashMap<>();
        for (MainActivity.RecipePreview recipe : availableRecipes) recipesById.put(recipe.id, recipe);
        List<MainActivity.RecipePreview> selected = new ArrayList<>();
        for (String id : selectedCollectionRecipeIds) {
            MainActivity.RecipePreview recipe = recipesById.get(id);
            if (recipe != null) selected.add(recipe);
        }
        return repository.saveCollectionAndGetId(collectionNameDraft, selected, collectionId);
    }
    String getNote(String recipeId) { return repository.getNote(recipeId); }
    void saveNote(MainActivity.RecipePreview recipe, String note) { repository.saveNote(recipe, note); }
}
