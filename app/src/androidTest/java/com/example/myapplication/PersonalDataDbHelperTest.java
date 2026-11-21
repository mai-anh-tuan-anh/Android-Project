package com.example.myapplication;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PersonalDataDbHelperTest {
    private static final String TEST_DATABASE = "module5-crud-test.db";
    private final MainActivity.RecipePreview recipeA = recipe("recipe-a", "Món A");
    private final MainActivity.RecipePreview recipeB = recipe("recipe-b", "Món B");

    private Context context;
    private PersonalDataDbHelper data;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DATABASE);
        data = new PersonalDataDbHelper(context, TEST_DATABASE);
    }

    @After
    public void tearDown() {
        if (data != null) data.close();
        if (context != null) context.deleteDatabase(TEST_DATABASE);
    }

    @Test
    public void favoritesAreUniqueAndCanBeRemoved() {
        data.addFavorite(recipeA);
        data.addFavorite(recipeA);

        assertTrue(data.isFavorite(recipeA.id));
        assertEquals(1, data.getFavorites().size());

        data.removeFavorite(recipeA.id);
        assertFalse(data.isFavorite(recipeA.id));
        assertTrue(data.getFavorites().isEmpty());
    }

    @Test
    public void collectionsNormalizeNamesAndReplaceRecipeMembershipOnEdit() {
        assertTrue(data.isCollectionNameAvailable("Bữa cơm nhà", null));
        Long createdId = data.saveCollectionAndGetId("  Bữa\t\u00a0cơm   nhà  ", Arrays.asList(recipeA, recipeB), null);
        assertTrue(createdId != null);

        PersonalDataDbHelper.CollectionRow collection = data.getCollections().get(0);
        assertEquals(createdId.longValue(), collection.id);
        assertEquals("Bữa cơm nhà", collection.name);
        assertEquals(2, collection.recipeCount);
        assertFalse(data.isCollectionNameAvailable("BUA COM NHA", null));
        assertTrue(data.isCollectionNameAvailable("bữa cơm nhà", collection.id));
        assertFalse(data.isCollectionNameAvailable(" \t\u00a0 ", null));
        assertTrue(data.saveCollection("Đồ ăn", Collections.singletonList(recipeA), null));
        assertTrue(data.isCollectionNameAvailable("Do an", null)); // Vietnamese đ remains a distinct letter.
        assertFalse(data.isCollectionNameAvailable("ĐỒ ĂN", null));

        assertTrue(data.saveCollection("Bữa tối", Collections.singletonList(recipeB), collection.id));
        PersonalDataDbHelper.CollectionRow edited = data.getCollections().get(0);
        assertEquals(collection.id, edited.id);
        assertEquals("Bữa tối", edited.name);
        assertEquals(1, edited.recipeCount);
        assertEquals(recipeB.id, data.getCollectionRecipes(edited.id).get(0).id);
    }

    @Test
    public void deletingCollectionKeepsFavoriteAndRecipeNote() {
        Long collectionId = data.saveCollectionAndGetId("Món cuối tuần", Collections.singletonList(recipeA), null);
        assertTrue(collectionId != null);
        data.addFavorite(recipeA);
        data.saveNote(recipeA, "Nêm thêm tiêu");

        assertTrue(data.deleteCollection(collectionId));

        assertTrue(data.getCollections().isEmpty());
        assertTrue(data.getCollectionRecipes(collectionId).isEmpty());
        assertTrue(data.isFavorite(recipeA.id));
        assertEquals("Nêm thêm tiêu", data.getNote(recipeA.id));
    }

    @Test
    public void notesCanBeCreatedAndUpdated() {
        assertEquals("", data.getNote(recipeA.id));

        data.saveNote(recipeA, "Ghi chú lần đầu");
        assertEquals("Ghi chú lần đầu", data.getNote(recipeA.id));

        data.saveNote(recipeA, "Ghi chú đã sửa");
        assertEquals("Ghi chú đã sửa", data.getNote(recipeA.id));
    }

    @Test
    public void viewedHistoryUpsertsRecipesAndKeepsTenMostRecent() {
        for (int i = 0; i < 12; i++) {
            data.recordViewedRecipe(recipe("recipe-" + i, "Món " + i));
        }
        data.recordViewedRecipe(recipe("recipe-0", "Món 0"));

        List<MainActivity.RecipePreview> history = data.getHistory();
        assertEquals(10, history.size());
        assertEquals("recipe-0", history.get(0).id);
        assertFalse(containsRecipe(history, "recipe-1"));
    }

    @Test
    public void revisitingTwoDayOldRecipeMovesItToTheTopWithoutDuplicate() {
        data.recordViewedRecipe(recipeA);
        data.recordViewedRecipe(recipeB);
        long now = System.currentTimeMillis();
        data.getWritableDatabase().execSQL("UPDATE RecipeViewHistory SET viewedAt=? WHERE recipeId=?",
                new Object[]{now - 2L * 24 * 60 * 60 * 1000, recipeA.id});
        data.getWritableDatabase().execSQL("UPDATE RecipeViewHistory SET viewedAt=? WHERE recipeId=?",
                new Object[]{now - 24L * 60 * 60 * 1000, recipeB.id});

        data.recordViewedRecipe(recipeA);

        List<MainActivity.RecipePreview> history = data.getHistory();
        assertEquals(2, history.size());
        assertEquals(recipeA.id, history.get(0).id);
    }

    @Test
    public void viewedHistoryExpiresAfterThreeDaysOnClient() {
        data.recordViewedRecipe(recipeA);
        data.getWritableDatabase().execSQL("UPDATE RecipeViewHistory SET viewedAt=? WHERE recipeId=?",
                new Object[]{System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000 - 1, recipeA.id});

        assertTrue(data.getHistory().isEmpty());
    }

    @Test
    public void mealPlanRejectsDuplicatesAndKeepsServingsWithinOneAndNineHundredNinetyNine() {
        data.addMealPlanRecipe(recipeA, 1);
        data.addMealPlanRecipe(recipeA, 5);

        List<PersonalDataDbHelper.PlannedRecipe> plan = data.getMealPlanRecipes();
        assertEquals(1, plan.size());
        assertEquals(1, plan.get(0).servings);

        data.setMealPlanRecipeServings(recipeA.id, 999);
        assertEquals(999, data.getMealPlanRecipes().get(0).servings);
        assertIllegalArgument(() -> data.setMealPlanRecipeServings(recipeA.id, 0));
        assertIllegalArgument(() -> data.setMealPlanRecipeServings(recipeA.id, 1000));
        assertEquals(999, data.getMealPlanRecipes().get(0).servings);
    }

    @Test
    public void mealPlanReplacementIsAtomicAndSupportsDeselection() {
        data.addMealPlanRecipe(recipeA, 2);

        assertIllegalArgument(() -> data.replaceMealPlanRecipes(Arrays.asList(
                new PersonalDataDbHelper.PlannedRecipe(recipeB, 3),
                new PersonalDataDbHelper.PlannedRecipe(recipeA, 1000))));
        List<PersonalDataDbHelper.PlannedRecipe> afterRollback = data.getMealPlanRecipes();
        assertEquals(1, afterRollback.size());
        assertEquals(recipeA.id, afterRollback.get(0).recipe.id);
        assertEquals(2, afterRollback.get(0).servings);

        data.replaceMealPlanRecipes(Collections.singletonList(
                new PersonalDataDbHelper.PlannedRecipe(recipeB, 4)));
        List<PersonalDataDbHelper.PlannedRecipe> replaced = data.getMealPlanRecipes();
        assertEquals(1, replaced.size());
        assertEquals(recipeB.id, replaced.get(0).recipe.id);
        assertEquals(4, replaced.get(0).servings);

        data.replaceMealPlanRecipes(Collections.emptyList());
        assertTrue(data.getMealPlanRecipes().isEmpty());
    }

    @Test
    public void mealPlanServingRangeMigrationPreservesExistingRows() {
        data.close();
        data = null;
        context.deleteDatabase(TEST_DATABASE);

        android.database.sqlite.SQLiteDatabase legacy = context.openOrCreateDatabase(
                TEST_DATABASE, Context.MODE_PRIVATE, null);
        legacy.execSQL("CREATE TABLE MealPlan (planId INTEGER PRIMARY KEY CHECK(planId=1), title TEXT NOT NULL)");
        legacy.execSQL("INSERT INTO MealPlan(planId,title) VALUES(1,'Bữa cơm nhà')");
        legacy.execSQL("CREATE TABLE MealPlanRecipe (planId INTEGER NOT NULL DEFAULT 1, recipeId TEXT NOT NULL, " +
                "title TEXT NOT NULL, imageRes TEXT NOT NULL, duration TEXT NOT NULL, difficulty TEXT NOT NULL, " +
                "servings INTEGER NOT NULL CHECK(servings BETWEEN 1 AND 99), position INTEGER NOT NULL, " +
                "PRIMARY KEY(planId,recipeId), FOREIGN KEY(planId) REFERENCES MealPlan(planId) ON DELETE CASCADE)");
        legacy.execSQL("INSERT INTO MealPlanRecipe VALUES(1,'recipe-a','Món A','recipe_mi_y','20 phút','Dễ',99,0)");
        legacy.setVersion(2);
        legacy.close();

        data = new PersonalDataDbHelper(context, TEST_DATABASE);
        assertEquals(1, data.getMealPlanRecipes().size());
        assertEquals(99, data.getMealPlanRecipes().get(0).servings);
        data.setMealPlanRecipeServings(recipeA.id, 999);
        assertEquals(999, data.getMealPlanRecipes().get(0).servings);
    }

    @Test
    public void shoppingItemMovesBetweenNeedToBuyAndAvailableStates() {
        data.saveShoppingItem(null, "Cà chua", "2", "quả", "Rau củ", "NEED_TO_BUY");

        PersonalDataDbHelper.ShoppingItem item = data.getShoppingItems().get(0);
        assertEquals("NEED_TO_BUY", item.status);
        data.setShoppingItemStatus(item.id, "AVAILABLE");
        assertEquals("AVAILABLE", data.getShoppingItems().get(0).status);
        data.setShoppingItemStatus(item.id, "NEED_TO_BUY");
        assertEquals("NEED_TO_BUY", data.getShoppingItems().get(0).status);
    }

    @Test
    public void pantrySeparatesUnknownQuantityFromOutOfStockAndStoresPurchaseDate() {
        assertTrue(data.savePantryItem(null, "Muối", "", "", "Gia vị", null, "Tủ bếp",
                "IN_STOCK", "2026-11-20"));
        PersonalDataDbHelper.PantryItem item = data.getPantryItems().get(0);
        assertEquals("IN_STOCK", item.stockStatus);
        assertEquals(null, item.amountDecimal);
        assertEquals("2026-11-20", item.purchaseDate);

        assertTrue(data.savePantryItem(item.id, "Muối", "2", "G", "Gia vị", null, "Tủ bếp",
                "OUT_OF_STOCK", "2026-11-20"));
        item = data.getPantryItem(item.id);
        assertEquals("OUT_OF_STOCK", item.stockStatus);
        assertEquals(null, item.amountDecimal);
        assertEquals("", item.unitId);
        assertEquals("2026-11-20", item.purchaseDate);
    }

    @Test
    public void pantrySchemaNineMigrationPreservesExistingRows() {
        data.close();
        data = null;
        context.deleteDatabase(TEST_DATABASE);
        android.database.sqlite.SQLiteDatabase legacy = context.openOrCreateDatabase(
                TEST_DATABASE, Context.MODE_PRIVATE, null);
        legacy.execSQL("CREATE TABLE PantryItem (itemId TEXT PRIMARY KEY, ingredientId TEXT, name TEXT NOT NULL, " +
                "amountDecimal TEXT, unitId TEXT, category TEXT NOT NULL DEFAULT 'Khác', expiryDate TEXT, " +
                "storageLocation TEXT NOT NULL DEFAULT '', updatedAt INTEGER NOT NULL)");
        legacy.execSQL("INSERT INTO PantryItem(itemId,name,amountDecimal,unitId,category,storageLocation,updatedAt) " +
                "VALUES('pantry-a','Gạo','2','KG','Ngũ cốc','Tủ khô',1)");
        legacy.setVersion(8);
        legacy.close();

        data = new PersonalDataDbHelper(context, TEST_DATABASE);
        PersonalDataDbHelper.PantryItem migrated = data.getPantryItem("pantry-a");
        assertTrue(migrated != null);
        assertEquals("Gạo", migrated.name);
        assertEquals("IN_STOCK", migrated.stockStatus);
        assertEquals(null, migrated.purchaseDate);
        assertEquals("2", migrated.amountDecimal);
    }

    private static void assertIllegalArgument(Runnable operation) {
        try {
            operation.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static boolean containsRecipe(List<MainActivity.RecipePreview> recipes, String recipeId) {
        for (MainActivity.RecipePreview recipe : recipes) {
            if (recipe.id.equals(recipeId)) return true;
        }
        return false;
    }

    private static MainActivity.RecipePreview recipe(String id, String title) {
        return new MainActivity.RecipePreview(id, title, "recipe_mi_y", "20 phút", "Dễ");
    }
}
