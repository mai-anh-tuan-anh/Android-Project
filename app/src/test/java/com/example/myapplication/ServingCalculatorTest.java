package com.example.myapplication;

import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.usecase.ServingCalculator;
import com.example.myapplication.domain.usecase.ServingCalculator.ExactAmount;
import com.example.myapplication.domain.usecase.ServingCalculator.Result;
import com.example.myapplication.domain.usecase.ServingCalculator.ScaledIngredient;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ServingCalculatorTest {
    private static final String RECIPE_ID = "recipe-01";
    private static final String INGREDIENT_ID = "ingredient-01";
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    @Test
    public void scalesOriginalAmountForSameMoreAndFewerServings() {
        Recipe recipe = recipe(4, line("300", "G", null));

        assertQuantity("300", "G", amount(recipe, 4));
        assertQuantity("450", "G", amount(recipe, 6));
        assertQuantity("150", "G", amount(recipe, 2));
        assertEquals(6, ServingCalculator.calculate(recipe, 6).servings);
    }

    @Test
    public void acceptsBothEndpointsAndRejectsInvalidServings() {
        Recipe recipe = recipe(1, line("2", "G", null));
        assertTrue(ServingCalculator.isValidServings(1));
        assertTrue(ServingCalculator.isValidServings(999));
        assertQuantity("2", "G", amount(recipe, 1));
        assertQuantity("1998", "G", amount(recipe, 999));
        assertQuantity("2", "G", amount(recipe(999, line("1998", "G", null)), 1));

        for (int invalid : new int[]{0, -1, 1000}) {
            assertFalse(ServingCalculator.isValidServings(invalid));
            assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(recipe, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> recipe(0, line("2", "G", null)));
        assertThrows(IllegalArgumentException.class, () -> recipe(1000, line("2", "G", null)));
    }

    @Test
    public void keepsRepeatingDecimalsExactUntilDisplayAndAddsBeforeRounding() {
        ExactAmount oneThird = amount(recipe(3, line("1", "G", null)), 1);
        assertEquals(BigDecimal.ONE, oneThird.numerator);
        assertEquals(BigInteger.valueOf(3), oneThird.denominator);
        assertEquals("0,33 g", oneThird.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertThrows(IllegalArgumentException.class,
                () -> oneThird.formatForDisplay(RoundingMode.UNNECESSARY, VIETNAMESE));

        ExactAmount twoThirds = oneThird.add(oneThird);
        assertEquals(BigDecimal.valueOf(2), twoThirds.numerator);
        assertEquals(BigInteger.valueOf(3), twoThirds.denominator);
        assertEquals("0,67 g", twoThirds.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertQuantity("1", "G", twoThirds.add(oneThird));
        ExactAmount oneHalf = amount(recipe(2, line("1", "G", null)), 1);
        ExactAmount fiveSixths = oneThird.add(oneHalf);
        assertEquals(BigDecimal.valueOf(5), fiveSixths.numerator);
        assertEquals(BigInteger.valueOf(6), fiveSixths.denominator);
    }

    @Test
    public void recomputesEachChangeFromSourceAndPreservesTrailingZeros() {
        RecipeIngredient original = line("1.2300", "G", null);
        Recipe recipe = recipe(7, original);
        ExactAmount first = amount(recipe, 1);
        assertEquals("0,18 g", first.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertQuantity("2.46", "G", amount(recipe, 14));
        assertQuantity("1.23", "G", amount(recipe, 7));
        assertEquals(0, first.compareQuantity(amount(recipe, 1)));
        assertEquals("1.2300", first.numerator.toPlainString());
        assertEquals("1.2300", original.amountDecimal);
        assertSame(original, recipe.details.ingredientLines.get(0));
    }

    @Test
    public void qualitativeLineAndCookingTimesAreUnchanged() {
        RecipeIngredient herb = new RecipeIngredient("line-02", RECIPE_ID, "herb", null, null, "Vừa đủ", 1);
        Recipe recipe = recipe(4, line("300", "G", null), herb);
        Result result = ServingCalculator.calculate(recipe, 8);
        ScaledIngredient qualitative = result.ingredients.get(1);

        assertSame(recipe, result.recipe);
        assertEquals(Integer.valueOf(10), result.recipe.details.prepMinutes);
        assertEquals(Integer.valueOf(20), result.recipe.details.cookMinutes);
        assertEquals(Integer.valueOf(30), result.recipe.details.durationMinutes);
        assertSame(herb, qualitative.original);
        assertNull(qualitative.amount);
        assertEquals("Vừa đủ", qualitative.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertThrows(UnsupportedOperationException.class, result.ingredients::clear);
    }

    @Test
    public void rejectsLegacyOrIncompleteRecipeInsteadOfReturningPartialQuantities() {
        Recipe legacy = new Recipe(RECIPE_ID, "Canh", "", "30 phút", "Dễ");
        assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(legacy, 1));
        Recipe missingIngredientData = recipe(4, Recipe.IngredientDataStatus.INCOMPLETE, line("300", "G", null));
        assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(missingIngredientData, 1));
        Recipe empty = recipe(4);
        assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(empty, 1));
        Recipe missingUnit = recipe(4, line("300", null, null));
        assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(missingUnit, 1));
        Recipe missingAmount = recipe(4, line(null, "G", null));
        assertThrows(IllegalArgumentException.class, () -> ServingCalculator.calculate(missingAmount, 1));
    }

    @Test
    public void invalidDecimalsAndUnitsCannotBecomeQuantities() {
        assertThrows(IllegalArgumentException.class, () -> line("0,5", "G", null));
        assertThrows(IllegalArgumentException.class, () -> line("1e3", "G", null));
        assertThrows(IllegalArgumentException.class, () -> line("-1", "G", null));
        assertThrows(IllegalArgumentException.class, () -> line("1", "CUSTOM:chén đầy", null));
        assertThrows(IllegalArgumentException.class, () -> ExactAmount.of(INGREDIENT_ID, "gram", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> ExactAmount.of(INGREDIENT_ID, "G", null));
    }

    @Test
    public void convertsAndAddsCompatibleUnitsWithoutRounding() {
        ExactAmount halfKg = decimal("0.5", "KG");
        ExactAmount grams = decimal("250", "G");
        assertQuantity("500", "G", halfKg.convertTo("G"));
        assertQuantity("0.25", "KG", grams.convertTo("KG"));
        assertQuantity("750", "G", halfKg.add(grams));
        assertQuantity("750", "G", grams.add(halfKg));
        assertQuantity("4.929", "ML", decimal("1", "TSP").convertTo("ML"));
        assertQuantity("1", "ML", decimal("1", "ML").convertTo("TSP").convertTo("ML"));
        assertQuantity("504.929", "ML", decimal("0.5", "L").add(decimal("1", "TSP")));
        assertQuantity("3", "QUA", decimal("1", "QUA").add(decimal("2", "QUA")));
    }

    @Test
    public void rejectsMixingDifferentIngredientsAndIncompatibleUnits() {
        ExactAmount grams = decimal("100", "G");
        ExactAmount oil = ExactAmount.of("oil", "G", BigDecimal.ONE);
        ExactAmount milliliters = decimal("1", "ML");
        assertThrows(IllegalArgumentException.class, () -> grams.add(oil));
        assertThrows(IllegalArgumentException.class, () -> grams.compareQuantity(oil));
        assertThrows(IllegalArgumentException.class, () -> grams.add(milliliters));
        assertThrows(IllegalArgumentException.class, () -> grams.compareQuantity(milliliters));
        assertThrows(IllegalArgumentException.class, () -> decimal("1", "BAT").convertTo("ML"));
        assertThrows(IllegalArgumentException.class, () -> decimal("1", "NHUM").convertTo("G"));
        assertThrows(IllegalArgumentException.class, () -> decimal("1", "QUA").add(decimal("1", "TRAI")));
    }

    @Test
    public void comparesStockAgainstTheExactRequirement() {
        ExactAmount oneThird = amount(recipe(3, line("1", "G", null)), 1);
        assertTrue(oneThird.compareQuantity(decimal("0.33", "G")) > 0);
        assertTrue(oneThird.compareQuantity(decimal("0.34", "G")) < 0);
        assertTrue(oneThird.compareQuantity(decimal("0.00033", "KG")) > 0);
        assertEquals(0, oneThird.add(oneThird).add(oneThird).compareQuantity(decimal("1000", "MG")));
    }

    @Test
    public void formatsTinyPositiveAmountsInSupportedUnitsOrWithAnUpperBound() {
        ExactAmount smallMass = decimal("0.005", "G");
        assertEquals("5 mg", smallMass.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertEquals("2 ml", decimal("0.002", "L").formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertEquals("< 0,01 ml", decimal("0.0001", "ML").formatForDisplay(RoundingMode.DOWN, VIETNAMESE));
        assertEquals("< 0,01 quả", decimal("0.0001", "QUA").formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertEquals("0 g", decimal("0", "G").formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertEquals("G", smallMass.unitId);
        assertQuantity("0.005", "G", smallMass);
    }

    @Test
    public void letsCallersChooseTheDisplayRoundingMode() {
        ExactAmount amount = decimal("1.235", "G");
        assertEquals("1,24 g", amount.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE));
        assertEquals("1,23 g", amount.formatForDisplay(RoundingMode.DOWN, VIETNAMESE));
        assertEquals("1.23 g", amount.formatForDisplay(RoundingMode.DOWN, Locale.US));
        assertQuantity("1.235", "G", amount);
    }

    private static Recipe recipe(int baseServings, RecipeIngredient... ingredients) {
        return recipe(baseServings, Recipe.IngredientDataStatus.COMPLETE, ingredients);
    }

    private static Recipe recipe(int baseServings, Recipe.IngredientDataStatus status, RecipeIngredient... ingredients) {
        Recipe.Details details = new Recipe.Details(null, null, baseServings, 10, 20, 30,
                Recipe.PublicationStatus.PUBLISHED, 1_000L, 1_000L, status,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null,
                Arrays.asList(ingredients), Collections.emptyList());
        return Recipe.fromCatalog(RECIPE_ID, "Canh", "", "Dễ", null, Collections.emptyList(),
                Arrays.asList("Cá", "Rau thơm").subList(0, ingredients.length), details);
    }

    private static RecipeIngredient line(String decimal, String unitId, String note) {
        return new RecipeIngredient("line-01", RECIPE_ID, INGREDIENT_ID, decimal, unitId, note, 0);
    }

    private static ExactAmount amount(Recipe recipe, int servings) {
        return ServingCalculator.calculate(recipe, servings).ingredients.get(0).amount;
    }

    private static ExactAmount decimal(String amount, String unit) {
        return ExactAmount.of(INGREDIENT_ID, unit, new BigDecimal(amount));
    }

    private static void assertQuantity(String expected, String unit, ExactAmount actual) {
        assertEquals(unit, actual.unitId);
        assertEquals(0, decimal(expected, unit).compareQuantity(actual));
    }
}
