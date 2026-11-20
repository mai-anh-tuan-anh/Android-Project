package com.example.myapplication;

import android.app.Application;

import java.util.List;

/** Repository boundary for local personal data shared by modules 5 and 6. */
final class PersonalDataRepository {
    private static PersonalDataRepository instance;
    private final PersonalDataDbHelper database;

    static synchronized PersonalDataRepository getInstance(Application application) {
        if (instance == null) instance = new PersonalDataRepository(application);
        return instance;
    }

    private PersonalDataRepository(Application application) {
        database = new PersonalDataDbHelper(application.getApplicationContext());
    }

    void seedDesignPreviewIfNeeded(List<MainActivity.RecipePreview> recipes) {
        database.seedDesignPreviewIfNeeded(recipes);
    }

    void addFavorite(MainActivity.RecipePreview recipe) { database.addFavorite(recipe); }
    void removeFavorite(String recipeId) { database.removeFavorite(recipeId); }
    boolean isFavorite(String recipeId) { return database.isFavorite(recipeId); }
    List<MainActivity.RecipePreview> getFavorites() { return database.getFavorites(); }
    List<MainActivity.RecipePreview> getHistory() { return database.getHistory(); }
    void recordViewedRecipe(MainActivity.RecipePreview recipe) { database.recordViewedRecipe(recipe); }
    List<PersonalDataDbHelper.CollectionRow> getCollections() { return database.getCollections(); }
    List<MainActivity.RecipePreview> getCollectionRecipes(long id) { return database.getCollectionRecipes(id); }
    boolean isCollectionNameAvailable(String name, Long id) { return database.isCollectionNameAvailable(name, id); }
    boolean saveCollection(String name, List<MainActivity.RecipePreview> recipes, Long id) {
        return database.saveCollection(name, recipes, id);
    }
    String getNote(String recipeId) { return database.getNote(recipeId); }
    void saveNote(MainActivity.RecipePreview recipe, String note) { database.saveNote(recipe, note); }
    List<PersonalDataDbHelper.PlannedRecipe> getMealPlanRecipes() { return database.getMealPlanRecipes(); }
    void setMealPlanRecipeServings(String id, int servings) { database.setMealPlanRecipeServings(id, servings); }
    void removeMealPlanRecipe(String id) { database.removeMealPlanRecipe(id); }
    void replaceMealPlanRecipes(List<PersonalDataDbHelper.PlannedRecipe> recipes) {
        database.replaceMealPlanRecipes(recipes);
    }
}
