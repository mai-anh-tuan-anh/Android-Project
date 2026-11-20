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
        assertTrue(data.saveCollection("  Bữa   cơm nhà  ", Arrays.asList(recipeA, recipeB), null));

        PersonalDataDbHelper.CollectionRow collection = data.getCollections().get(0);
        assertEquals("Bữa cơm nhà", collection.name);
        assertEquals(2, collection.recipeCount);
        assertFalse(data.isCollectionNameAvailable("BUA COM NHA", null));
        assertTrue(data.isCollectionNameAvailable("bữa cơm nhà", collection.id));

        assertTrue(data.saveCollection("Bữa tối", Collections.singletonList(recipeB), collection.id));
        PersonalDataDbHelper.CollectionRow edited = data.getCollections().get(0);
        assertEquals(collection.id, edited.id);
        assertEquals("Bữa tối", edited.name);
        assertEquals(1, edited.recipeCount);
        assertEquals(recipeB.id, data.getCollectionRecipes(edited.id).get(0).id);
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
    public void mealPlanRejectsDuplicatesAndKeepsServingsWithinOneAndNinetyNine() {
        data.addMealPlanRecipe(recipeA, 1);
        data.addMealPlanRecipe(recipeA, 5);

        List<PersonalDataDbHelper.PlannedRecipe> plan = data.getMealPlanRecipes();
        assertEquals(1, plan.size());
        assertEquals(1, plan.get(0).servings);

        data.setMealPlanRecipeServings(recipeA.id, 99);
        assertEquals(99, data.getMealPlanRecipes().get(0).servings);
        assertIllegalArgument(() -> data.setMealPlanRecipeServings(recipeA.id, 0));
        assertIllegalArgument(() -> data.setMealPlanRecipeServings(recipeA.id, 100));
        assertEquals(99, data.getMealPlanRecipes().get(0).servings);
    }

    @Test
    public void mealPlanReplacementIsAtomicAndSupportsDeselection() {
        data.addMealPlanRecipe(recipeA, 2);

        assertIllegalArgument(() -> data.replaceMealPlanRecipes(Arrays.asList(
                new PersonalDataDbHelper.PlannedRecipe(recipeB, 3),
                new PersonalDataDbHelper.PlannedRecipe(recipeA, 100))));
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
