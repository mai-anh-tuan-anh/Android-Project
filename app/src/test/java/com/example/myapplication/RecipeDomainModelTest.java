package com.example.myapplication;

import com.example.myapplication.domain.catalog.KitchenUnitCatalog;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Ingredient;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.Recipe.IngredientDataStatus;
import com.example.myapplication.domain.model.Recipe.PublicationStatus;
import com.example.myapplication.domain.model.RecipeIngredient;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RecipeDomainModelTest {
    private static final String RECIPE_ID = "recipe-01";

    @Test
    public void legacyConstructorsKeepTheirSummaryBehaviour() {
        List<String> categories = new ArrayList<>(Collections.singletonList("Món Việt"));
        List<String> ingredients = new ArrayList<>(Collections.singletonList("Cà chua"));
        Recipe shortSummary = new Recipe(RECIPE_ID, "Canh", "canh", "20 phút", "Dễ");
        Recipe withLists = new Recipe(RECIPE_ID, "Canh", "canh", "25 phút", "Dễ", 25,
                null, categories, ingredients);
        Recipe withPublication = new Recipe(RECIPE_ID, "Canh", "canh", "25 phút", "Dễ", 25,
                "Món chay", categories, ingredients, 1_000L);
        Recipe withCompleteFlag = new Recipe(RECIPE_ID, "Canh", "canh", "25 phút", "Dễ", 25,
                "Món chay", null, null, -1L, true);
        categories.clear();
        ingredients.clear();

        assertEquals(20, shortSummary.durationMinutes);
        assertEquals(30, new Recipe(RECIPE_ID, "Canh", "canh", null, "Dễ").durationMinutes);
        assertEquals("Không giới hạn", withLists.diet);
        assertEquals(Collections.singletonList("Món Việt"), withLists.categories);
        assertEquals(Collections.singletonList("Cà chua"), withLists.ingredients);
        assertEquals(0L, withLists.publishedAtMillis);
        assertEquals(1_000L, withPublication.publishedAtMillis);
        assertFalse(withPublication.ingredientListComplete);
        assertEquals(0L, withCompleteFlag.publishedAtMillis);
        assertTrue(withCompleteFlag.ingredientListComplete);
        assertTrue(withCompleteFlag.categories.isEmpty());
        assertTrue(withCompleteFlag.ingredients.isEmpty());
        for (Recipe recipe : Arrays.asList(shortSummary, withLists, withPublication, withCompleteFlag)) {
            assertNull(recipe.details);
        }
    }

    @Test
    public void catalogRecipePreservesAmountsRelationsAndStepOrder() {
        RecipeIngredient tomato = line("line-01", "100.2500", "G", null, 0);
        RecipeIngredient herb = line("line-02", null, null, "Vừa đủ", 1);
        CookingStep prep = step("step-01", 0, Collections.singletonList(tomato.recipeIngredientId));
        CookingStep cook = step("step-02", 1, Collections.emptyList());
        Recipe.Details details = details(3, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                Arrays.asList(herb, tomato), Arrays.asList(cook, prep));
        Recipe recipe = Recipe.fromCatalog(RECIPE_ID, "Canh", "canh", "Dễ", "Món chay",
                Arrays.asList("Món Việt", "Canh & súp"), Arrays.asList("Cà chua", "Rau thơm"), details);

        assertEquals(RECIPE_ID, recipe.id);
        assertEquals("canh", recipe.imageName);
        assertEquals("30 phút", recipe.duration);
        assertEquals(30, recipe.durationMinutes);
        assertEquals(Integer.valueOf(10), recipe.details.prepMinutes);
        assertEquals(Integer.valueOf(20), recipe.details.cookMinutes);
        assertEquals(1_000L, recipe.publishedAtMillis);
        assertEquals(Long.valueOf(2_000L), recipe.details.updatedAtMillis);
        assertEquals(Arrays.asList("cat-01", "cat-02"), recipe.details.categoryIds);
        assertEquals(Arrays.asList("diet-01", "diet-02"), recipe.details.dietIds);
        assertEquals(Arrays.asList("Món Việt", "Canh & súp"), recipe.categories);
        assertEquals(Arrays.asList(tomato, herb), recipe.details.ingredientLines);
        assertEquals("100.2500", recipe.details.ingredientLines.get(0).amountDecimal);
        assertEquals("Vừa đủ", recipe.details.ingredientLines.get(1).qualitativeNote);
        assertTrue(recipe.ingredientListComplete);
        assertTrue(recipe.details.hasIngredientsForServingAdjustment());
        assertEquals(Arrays.asList(prep, cook), recipe.details.steps);
        assertEquals(prep.stepId, step(prep.stepId, 5, prep.recipeIngredientIds).stepId);
        assertEquals("recipes/step-01.jpg", prep.imagePath);
        assertEquals(Integer.valueOf(300), prep.durationSeconds);
        assertEquals(Collections.singletonList(tomato.recipeIngredientId), prep.recipeIngredientIds);
    }

    @Test
    public void catalogSupportsDraftArchivedAndIncompleteDataWithoutInventingDuration() {
        Recipe.Details draft = new Recipe.Details(null, null, 2, 10, null, null,
                PublicationStatus.DRAFT, null, 2_000L, IngredientDataStatus.UNKNOWN,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null,
                Collections.emptyList(), Collections.emptyList());
        Recipe recipe = catalogRecipe(draft, Collections.emptyList());
        assertEquals(PublicationStatus.DRAFT, recipe.details.publicationStatus);
        assertEquals(IngredientDataStatus.UNKNOWN, recipe.details.ingredientDataStatus);
        assertEquals(Integer.valueOf(10), recipe.details.prepMinutes);
        assertNull(recipe.details.cookMinutes);
        assertNull(recipe.details.durationMinutes);
        assertNull(recipe.details.publishedAtMillis);
        assertEquals(Recipe.UNKNOWN_DURATION_MINUTES, recipe.durationMinutes);
        assertEquals("", recipe.duration);
        assertFalse(recipe.ingredientListComplete);
        assertFalse(recipe.details.hasIngredientsForServingAdjustment());

        RecipeIngredient knownLine = line("line-01", "100", "G", null, 0);
        CookingStep unresolvedStep = step("step-01", 0, Collections.singletonList("line-missing"));
        Recipe.Details archived = details(2, PublicationStatus.ARCHIVED, IngredientDataStatus.INCOMPLETE,
                Collections.singletonList(knownLine), Collections.singletonList(unresolvedStep));
        Recipe archivedRecipe = catalogRecipe(archived, Collections.singletonList("Cà chua"));
        assertEquals(PublicationStatus.ARCHIVED, archivedRecipe.details.publicationStatus);
        assertEquals(IngredientDataStatus.INCOMPLETE, archivedRecipe.details.ingredientDataStatus);
        assertEquals(Collections.singletonList("line-missing"), archivedRecipe.details.steps.get(0).recipeIngredientIds);
        assertFalse(archivedRecipe.ingredientListComplete);
        assertFalse(archivedRecipe.details.hasIngredientsForServingAdjustment());
    }

    @Test
    public void membershipAndServingReadinessUseDifferentChecks() {
        RecipeIngredient withoutUnit = line("line-01", "100", null, null, 0);
        Recipe.Details withoutQuantity = details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                Collections.singletonList(withoutUnit), Collections.emptyList());
        Recipe knownMembership = catalogRecipe(withoutQuantity, Collections.singletonList("Cà chua"));
        assertTrue(knownMembership.ingredientListComplete);
        assertFalse(knownMembership.details.hasIngredientsForServingAdjustment());

        RecipeIngredient missingAmountAndNote = line("line-01", null, null, null, 0);
        Recipe.Details missing = details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                Collections.singletonList(missingAmountAndNote), Collections.emptyList());
        assertFalse(missing.hasIngredientsForServingAdjustment());

        RecipeIngredient quantified = line("line-01", "100", "G", null, 0);
        Recipe.Details unresolvedNames = details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                Collections.singletonList(quantified), Collections.emptyList());
        assertFalse(catalogRecipe(unresolvedNames, Collections.emptyList()).ingredientListComplete);
        assertTrue(unresolvedNames.hasIngredientsForServingAdjustment());
    }

    @Test
    public void validatesBaseServingsAndKeepsUnknownDurationsNullable() {
        assertEquals(1, emptyDetails(1).baseServings);
        assertEquals(999, emptyDetails(999).baseServings);
        assertThrows(IllegalArgumentException.class, () -> emptyDetails(0));
        assertThrows(IllegalArgumentException.class, () -> emptyDetails(1000));
        assertThrows(IllegalArgumentException.class, () -> new Recipe.Details(null, null, 2, null, -1, null,
                PublicationStatus.DRAFT, null, null, IngredientDataStatus.UNKNOWN,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null,
                Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void validatesDecimalTextAndCanonicalUnitWithoutUsingAliases() {
        assertTrue(KitchenUnitCatalog.isKnownUnitId("NHUM"));
        assertTrue(KitchenUnitCatalog.isKnownUnitId("LAT"));
        assertEquals("G", KitchenUnitCatalog.canonicalId("gram"));
        assertFalse(KitchenUnitCatalog.isKnownUnitId("gram"));
        assertEquals("0.0050", line("line-01", "0.0050", "NHUM", null, 0).amountDecimal);
        assertThrows(IllegalArgumentException.class, () -> line("line-01", "0,5", "G", null, 0));
        assertThrows(IllegalArgumentException.class, () -> line("line-01", "1e-3", "G", null, 0));
        assertThrows(IllegalArgumentException.class, () -> line("line-01", "-1", "G", null, 0));
        assertThrows(IllegalArgumentException.class, () -> line("line-01", "1", "gram", null, 0));
    }

    @Test
    public void validatesRecipeOwnershipAndStepReferences() {
        RecipeIngredient line = line("line-01", "100", "G", null, 0);
        CookingStep unresolvedStep = step("step-01", 0, Collections.singletonList("line-missing"));
        assertThrows(IllegalArgumentException.class,
                () -> details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                        Collections.singletonList(line), Collections.singletonList(unresolvedStep)));
        Recipe.Details otherRecipe = details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                Collections.singletonList(line), Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> Recipe.fromCatalog("other", "Canh", "canh", "Dễ",
                null, Collections.emptyList(), Collections.singletonList("Cà chua"), otherRecipe));
        assertThrows(IllegalArgumentException.class,
                () -> details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                        Arrays.asList(line, line("line-02", "50", "G", null, 0)), Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> details(2, PublicationStatus.PUBLISHED, IngredientDataStatus.COMPLETE,
                        Collections.emptyList(), Arrays.asList(step("step-01", 0, Collections.emptyList()),
                                step("step-01", 1, Collections.emptyList()))));
    }

    @Test
    public void catalogCollectionsAreDefensivelyCopiedAndReadOnly() {
        List<String> ids = new ArrayList<>(Collections.singletonList("line-01"));
        List<String> aliases = new ArrayList<>(Collections.singletonList("Cà"));
        List<String> categories = new ArrayList<>(Collections.singletonList("cat-01"));
        List<String> diets = new ArrayList<>(Collections.singletonList("diet-01"));
        List<String> tools = new ArrayList<>(Collections.singletonList("Dao"));
        Ingredient ingredient = new Ingredient("ingredient-01", "Cà chua", "Rau củ", Ingredient.Status.ACTIVE, aliases);
        CookingStep prep = step("step-01", 0, ids);
        List<CookingStep> steps = new ArrayList<>(Collections.singletonList(prep));
        List<RecipeIngredient> lines = new ArrayList<>(Collections.singletonList(line("line-01", "100", "G", null, 0)));
        Recipe.Details details = new Recipe.Details(null, null, 2, null, null, null,
                PublicationStatus.PUBLISHED, null, null, IngredientDataStatus.COMPLETE,
                categories, diets, tools, null, lines, steps);
        for (List<?> source : Arrays.asList(ids, aliases, categories, diets, tools, lines, steps)) source.clear();

        assertEquals(Collections.singletonList("Cà"), ingredient.aliases);
        assertEquals(Collections.singletonList("line-01"), prep.recipeIngredientIds);
        assertEquals(Collections.singletonList("cat-01"), details.categoryIds);
        assertEquals(Collections.singletonList("diet-01"), details.dietIds);
        assertEquals(Collections.singletonList("Dao"), details.tools);
        assertEquals(1, details.ingredientLines.size());
        assertEquals(Collections.singletonList(prep), details.steps);
        for (List<?> result : Arrays.asList(ingredient.aliases, prep.recipeIngredientIds, details.categoryIds,
                details.dietIds, details.tools, details.ingredientLines, details.steps)) {
            assertThrows(UnsupportedOperationException.class, result::clear);
        }
    }

    private static Recipe.Details emptyDetails(int baseServings) {
        return details(baseServings, PublicationStatus.DRAFT, IngredientDataStatus.UNKNOWN,
                Collections.emptyList(), Collections.emptyList());
    }

    private static Recipe.Details details(int baseServings, PublicationStatus publicationStatus,
                                          IngredientDataStatus ingredientDataStatus,
                                          List<RecipeIngredient> lines, List<CookingStep> steps) {
        return new Recipe.Details("Mô tả", "recipes/cover.jpg", baseServings, 10, 20, 30,
                publicationStatus, 1_000L, 2_000L, ingredientDataStatus,
                Arrays.asList("cat-01", "cat-02"), Arrays.asList("diet-01", "diet-02"),
                Collections.singletonList("Nồi"), "Mẹo", lines, steps);
    }

    private static Recipe catalogRecipe(Recipe.Details details, List<String> ingredientNames) {
        return Recipe.fromCatalog(RECIPE_ID, "Canh", "canh", "Dễ", null,
                Collections.emptyList(), ingredientNames, details);
    }

    private static RecipeIngredient line(String lineId, String amount, String unitId, String note, int order) {
        return new RecipeIngredient(lineId, RECIPE_ID, "ingredient-" + lineId, amount, unitId, note, order);
    }

    private static CookingStep step(String stepId, int order, List<String> ingredientIds) {
        return new CookingStep(stepId, RECIPE_ID, "Sơ chế", "Rửa nguyên liệu", order, 300,
                "recipes/" + stepId + ".jpg", ingredientIds);
    }
}
