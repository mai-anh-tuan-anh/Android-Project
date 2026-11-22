package com.example.myapplication.domain.repository;

import com.example.myapplication.domain.model.CookingLogEntry;
import com.example.myapplication.domain.model.PhotoSaveResult;
import com.example.myapplication.domain.model.PlannedRecipe;
import com.example.myapplication.domain.model.PantryIngredient;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.example.myapplication.domain.model.ShoppingListItem;

import java.io.File;
import java.util.List;

/** Data contract used by personal-data, meal-plan, shopping, pantry and journal features. */
public interface PersonalDataRepository {
    void seedDesignPreviewIfNeeded(List<Recipe> recipes);
    void addFavorite(Recipe recipe);
    void removeFavorite(String recipeId);
    boolean isFavorite(String recipeId);
    List<Recipe> getFavorites();
    List<Recipe> getHistory();
    void recordViewedRecipe(Recipe recipe);
    List<RecipeCollection> getCollections();
    boolean deleteCollection(long collectionId);
    List<Recipe> getCollectionRecipes(long id);
    boolean isCollectionNameAvailable(String name, Long id);
    boolean saveCollection(String name, List<Recipe> recipes, Long id);
    Long saveCollectionAndGetId(String name, List<Recipe> recipes, Long id);
    String getNote(String recipeId);
    void saveNote(Recipe recipe, String note);
    List<PlannedRecipe> getMealPlanRecipes();
    void setMealPlanRecipeServings(String id, int servings);
    void removeMealPlanRecipe(String id);
    void replaceMealPlanRecipes(List<PlannedRecipe> recipes);
    List<ShoppingListItem> getShoppingItems();
    void saveShoppingItem(String id, String name, String amount, String unit, String category, String status);
    void setShoppingItemStatus(String id, String status);
    void deleteShoppingItem(String id);
    void clearShoppingItems();
    String getShoppingPlanSnapshot();
    void saveShoppingPlanSnapshot(String snapshot);
    List<PantryIngredient> getPantryItems();
    PantryIngredient getPantryItem(String id);
    boolean savePantryItem(String id, String name, String amount, String unit, String category,
                           String expiryDate, String storageLocation, String purchaseDate);
    boolean deletePantryItem(String id);
    int deleteAllPantryItems();
    List<CookingLogEntry> getCookingLogs(String recipeId);
    List<CookingLogEntry> getAllCookingLogs();
    CookingLogEntry getCookingLog(String id);
    boolean saveCookingLog(String id, Recipe recipe, long cookedAt, int servings,
                           Integer duration, Integer rating, String adjustment, String photoPath);
    boolean deleteCookingLog(String id);
    PhotoSaveResult saveCookingLogPhoto(String uriString);
    File getCookingLogPhoto(String filename);
    void deleteCookingLogPhoto(String filename);
}
