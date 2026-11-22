package com.example.myapplication.presentation.personaldata;



import androidx.lifecycle.ViewModel;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Presentation state and actions for module 5. */
public final class PersonalDataViewModel extends ViewModel {
    private final PersonalDataRepository repository;
    private final List<String> selectedCollectionRecipeIds = new ArrayList<>();
    private String collectionNameDraft = "";
    private boolean collectionSubmitAttempted;

    public PersonalDataViewModel(PersonalDataRepository repository) {
        this.repository = repository;
    }

    public void seedDesignPreviewIfNeeded(List<Recipe> recipes) {
        repository.seedDesignPreviewIfNeeded(recipes);
    }
    public void addFavorite(Recipe recipe) { repository.addFavorite(recipe); }
    public void removeFavorite(String recipeId) { repository.removeFavorite(recipeId); }
    public boolean isFavorite(String recipeId) { return repository.isFavorite(recipeId); }
    public List<Recipe> getFavorites() { return repository.getFavorites(); }
    public List<Recipe> getHistory() { return repository.getHistory(); }
    public void recordViewedRecipe(Recipe recipe) { repository.recordViewedRecipe(recipe); }
    public List<RecipeCollection> getCollections() { return repository.getCollections(); }
    public boolean deleteCollection(long collectionId) { return repository.deleteCollection(collectionId); }
    public List<Recipe> getCollectionRecipes(long id) { return repository.getCollectionRecipes(id); }
    public boolean saveCollection(String name, List<Recipe> recipes, Long id) {
        return repository.saveCollection(name, recipes, id);
    }

    public void beginCollectionForm(String currentName, List<Recipe> currentRecipes) {
        collectionNameDraft = currentName == null ? "" : currentName;
        collectionSubmitAttempted = false;
        selectedCollectionRecipeIds.clear();
        if (currentRecipes != null) {
            for (Recipe recipe : currentRecipes) selectedCollectionRecipeIds.add(recipe.id);
        }
    }

    public String getCollectionNameDraft() { return collectionNameDraft; }
    public void setCollectionNameDraft(String name) { collectionNameDraft = name == null ? "" : name; }
    public boolean wasCollectionSubmitAttempted() { return collectionSubmitAttempted; }
    public void markCollectionSubmitAttempted() { collectionSubmitAttempted = true; }
    public boolean isCollectionRecipeSelected(String recipeId) { return selectedCollectionRecipeIds.contains(recipeId); }
    public void toggleCollectionRecipe(String recipeId) {
        if (selectedCollectionRecipeIds.contains(recipeId)) selectedCollectionRecipeIds.remove(recipeId);
        else selectedCollectionRecipeIds.add(recipeId);
    }
    public boolean isCollectionNameAvailable(String name, Long collectionId) {
        return repository.isCollectionNameAvailable(name, collectionId);
    }
    public Long saveCollectionForm(Long collectionId, List<Recipe> availableRecipes) {
        if (collectionNameDraft.trim().isEmpty() || !repository.isCollectionNameAvailable(collectionNameDraft, collectionId)) {
            return null;
        }
        Map<String, Recipe> recipesById = new HashMap<>();
        for (Recipe recipe : availableRecipes) recipesById.put(recipe.id, recipe);
        List<Recipe> selected = new ArrayList<>();
        for (String id : selectedCollectionRecipeIds) {
            Recipe recipe = recipesById.get(id);
            if (recipe != null) selected.add(recipe);
        }
        return repository.saveCollectionAndGetId(collectionNameDraft, selected, collectionId);
    }
    public String getNote(String recipeId) { return repository.getNote(recipeId); }
    public void saveNote(Recipe recipe, String note) { repository.saveNote(recipe, note); }
}
