package com.example.myapplication.data.repository;


import com.example.myapplication.data.local.database.PersonalDataDbHelper;
import com.example.myapplication.data.local.storage.CookingJournalPhotoStore;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.repository.PersonalDataRepository;

import android.app.Application;
import android.net.Uri;


import java.io.File;

import java.util.List;

/** Repository boundary for local personal data shared by modules 5–7. */
public final class LocalPersonalDataRepository implements PersonalDataRepository {
    private final PersonalDataDbHelper database;
    private final CookingJournalPhotoStore photoStore;

    public LocalPersonalDataRepository(Application application) {
        database = new PersonalDataDbHelper(application.getApplicationContext());
        photoStore = new CookingJournalPhotoStore(application.getApplicationContext());
    }

    @Override
    public void seedDesignPreviewIfNeeded(List<Recipe> recipes) {
        database.seedDesignPreviewIfNeeded(recipes);
    }

    @Override
    public void addFavorite(Recipe recipe) { database.addFavorite(recipe); }
    @Override
    public void removeFavorite(String recipeId) { database.removeFavorite(recipeId); }
    @Override
    public boolean isFavorite(String recipeId) { return database.isFavorite(recipeId); }
    @Override
    public List<Recipe> getFavorites() { return database.getFavorites(); }
    @Override
    public List<Recipe> getHistory() { return database.getHistory(); }
    @Override
    public void recordViewedRecipe(Recipe recipe) { database.recordViewedRecipe(recipe); }
    @Override
    public List<RecipeCollection> getCollections() { return database.getCollections(); }
    @Override
    public boolean deleteCollection(long collectionId) { return database.deleteCollection(collectionId); }
    @Override
    public List<Recipe> getCollectionRecipes(long id) { return database.getCollectionRecipes(id); }
    @Override
    public boolean isCollectionNameAvailable(String name, Long id) { return database.isCollectionNameAvailable(name, id); }
    @Override
    public boolean saveCollection(String name, List<Recipe> recipes, Long id) {
        return database.saveCollection(name, recipes, id);
    }
    @Override
    public Long saveCollectionAndGetId(String name, List<Recipe> recipes, Long id) {
        return database.saveCollectionAndGetId(name, recipes, id);
    }
    @Override
    public String getNote(String recipeId) { return database.getNote(recipeId); }
    @Override
    public void saveNote(Recipe recipe, String note) { database.saveNote(recipe, note); }
    @Override
    public List<PlannedRecipe> getMealPlanRecipes() { return database.getMealPlanRecipes(); }
    @Override
    public void setMealPlanRecipeServings(String id, int servings) { database.setMealPlanRecipeServings(id, servings); }
    @Override
    public void removeMealPlanRecipe(String id) { database.removeMealPlanRecipe(id); }
    @Override
    public void replaceMealPlanRecipes(List<PlannedRecipe> recipes) {
        database.replaceMealPlanRecipes(recipes);
    }
    @Override
    public List<ShoppingListItem> getShoppingItems() { return database.getShoppingItems(); }
    @Override
    public void saveShoppingItem(String id, String name, String amount, String unit, String category, String status) {
        database.saveShoppingItem(id, name, amount, unit, category, status);
    }
    @Override
    public void setShoppingItemStatus(String id, String status) { database.setShoppingItemStatus(id, status); }
    @Override
    public void deleteShoppingItem(String id) { database.deleteShoppingItem(id); }
    @Override
    public void clearShoppingItems() { database.clearShoppingItems(); }
    @Override
    public String getShoppingPlanSnapshot() { return database.getShoppingPlanSnapshot(); }
    @Override
    public void saveShoppingPlanSnapshot(String snapshot) { database.saveShoppingPlanSnapshot(snapshot); }
    @Override
    public List<PantryIngredient> getPantryItems() { return database.getPantryItems(); }
    @Override
    public PantryIngredient getPantryItem(String id) { return database.getPantryItem(id); }
    @Override
    public boolean savePantryItem(String id, String name, String amount, String unit, String category,
                           String expiryDate, String storageLocation, String purchaseDate) {
        return database.savePantryItem(id, name, amount, unit, category, expiryDate, storageLocation, purchaseDate);
    }
    @Override
    public boolean deletePantryItem(String id) { return database.deletePantryItem(id); }
    @Override
    public int deleteAllPantryItems() { return database.deleteAllPantryItems(); }
    @Override
    public List<CookingLogEntry> getCookingLogs(String recipeId) { return database.getCookingLogs(recipeId); }
    @Override
    public List<CookingLogEntry> getAllCookingLogs() { return database.getAllCookingLogs(); }
    @Override
    public CookingLogEntry getCookingLog(String id) { return database.getCookingLog(id); }
    @Override
    public boolean saveCookingLog(String id, Recipe recipe, long cookedAt, int servings,
                           Integer duration, Integer rating, String adjustment, String photoPath) {
        return database.saveCookingLog(id, recipe, cookedAt, servings, duration, rating, adjustment, photoPath);
    }
    @Override
    public boolean deleteCookingLog(String id) { return database.deleteCookingLog(id); }
    @Override
    public PhotoSaveResult saveCookingLogPhoto(String uriString) {
        CookingJournalPhotoStore.CopyResult result = photoStore.copy(Uri.parse(uriString));
        return new PhotoSaveResult(result.filename, result.tooLarge);
    }
    @Override
    public File getCookingLogPhoto(String filename) { return photoStore.resolve(filename); }
    @Override
    public void deleteCookingLogPhoto(String filename) { photoStore.delete(filename); }
}
