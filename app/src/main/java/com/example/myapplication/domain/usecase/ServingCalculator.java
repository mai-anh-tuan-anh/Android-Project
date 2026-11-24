package com.example.myapplication.domain.usecase;

import com.example.myapplication.domain.catalog.KitchenUnitCatalog;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Calculates quantities from a recipe's original amounts. */
public final class ServingCalculator {
    private static final int DISPLAY_SCALE = 2;
    private static final BigDecimal MIN_DISPLAY_AMOUNT = new BigDecimal("0.01");

    private ServingCalculator() { }

    public static boolean isValidServings(int servings) {
        return servings >= 1 && servings <= 999;
    }

    public static Result calculate(Recipe recipe, int servings) {
        Objects.requireNonNull(recipe, "recipe");
        if (!isValidServings(servings)) {
            throw new IllegalArgumentException("servings must be between 1 and 999");
        }
        Recipe.Details details = recipe.details;
        if (details == null) throw new IllegalArgumentException("recipe has no serving details");
        if (!details.hasIngredientsForServingAdjustment()) {
            throw new IllegalArgumentException("recipe lacks ingredient data for serving adjustment");
        }

        BigDecimal multiplier = BigDecimal.valueOf(servings);
        BigInteger divisor = BigInteger.valueOf(details.baseServings);
        List<ScaledIngredient> ingredients = new ArrayList<>(details.ingredientLines.size());
        for (RecipeIngredient original : details.ingredientLines) {
            ExactAmount amount = original.amountDecimal == null ? null : new ExactAmount(
                    original.ingredientId, original.unitId,
                    new BigDecimal(original.amountDecimal).multiply(multiplier), divisor);
            ingredients.add(new ScaledIngredient(original, amount));
        }
        return new Result(recipe, servings, ingredients);
    }

    public static final class Result {
        public final Recipe recipe;
        public final int servings;
        public final List<ScaledIngredient> ingredients;

        private Result(Recipe recipe, int servings, List<ScaledIngredient> ingredients) {
            this.recipe = recipe;
            this.servings = servings;
            this.ingredients = Collections.unmodifiableList(ingredients);
        }
    }

    public static final class ScaledIngredient {
        public final RecipeIngredient original;
        /** Null only for a qualitative line. */
        public final ExactAmount amount;

        private ScaledIngredient(RecipeIngredient original, ExactAmount amount) {
            this.original = original;
            this.amount = amount;
        }

        public String formatForDisplay(RoundingMode roundingMode, Locale locale) {
            return amount == null ? original.qualitativeNote : amount.formatForDisplay(roundingMode, locale);
        }
    }

    /** A non-negative quantity expressed as numerator / denominator in a canonical unit. */
    public static final class ExactAmount {
        public final String ingredientId;
        public final String unitId;
        public final BigDecimal numerator;
        public final BigInteger denominator;

        private ExactAmount(String ingredientId, String unitId, BigDecimal numerator, BigInteger denominator) {
            this.ingredientId = ingredientId;
            this.unitId = unitId;
            this.numerator = numerator;
            this.denominator = denominator;
        }

        /** Wraps an existing exact decimal quantity, for example from the pantry. */
        public static ExactAmount of(String ingredientId, String unitId, BigDecimal amount) {
            if (ingredientId == null || ingredientId.trim().isEmpty()) {
                throw new IllegalArgumentException("ingredientId must not be blank");
            }
            if (!KitchenUnitCatalog.isKnownUnitId(unitId)) {
                throw new IllegalArgumentException("unitId must be a canonical kitchen unit ID");
            }
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be a non-negative decimal");
            }
            return new ExactAmount(ingredientId, unitId, amount, BigInteger.ONE);
        }

        public ExactAmount convertTo(String targetUnitId) {
            if (unitId.equals(targetUnitId)) return this;
            if (!KitchenUnitCatalog.isKnownUnitId(targetUnitId)
                    || !KitchenUnitCatalog.dimension(unitId).equals(KitchenUnitCatalog.dimension(targetUnitId))) {
                throw new IllegalArgumentException("incompatible kitchen units");
            }

            BigDecimal baseNumerator = KitchenUnitCatalog.toBaseAmount(numerator, unitId);
            BigDecimal targetFactor = KitchenUnitCatalog.toBaseAmount(BigDecimal.ONE, targetUnitId);
            if (baseNumerator == null || targetFactor == null) {
                throw new IllegalArgumentException("no conversion available for these kitchen units");
            }
            // Dividing by a decimal factor a * 10^-s moves a into the denominator.
            return new ExactAmount(ingredientId, targetUnitId,
                    baseNumerator.scaleByPowerOfTen(targetFactor.scale()),
                    denominator.multiply(targetFactor.unscaledValue()));
        }

        /** Adds quantities of the same ingredient, using g or ml when a conversion exists. */
        public ExactAmount add(ExactAmount other) {
            requireSameIngredient(other);
            String aggregateUnit = unitId;
            switch (KitchenUnitCatalog.dimension(unitId)) {
                case "MASS": aggregateUnit = "G"; break;
                case "VOLUME": aggregateUnit = "ML"; break;
                default: break;
            }
            ExactAmount left = convertTo(aggregateUnit);
            ExactAmount right = other.convertTo(aggregateUnit);
            BigInteger commonFactor = left.denominator.gcd(right.denominator);
            BigInteger leftMultiplier = right.denominator.divide(commonFactor);
            BigInteger rightMultiplier = left.denominator.divide(commonFactor);
            BigDecimal sum = left.numerator.multiply(new BigDecimal(leftMultiplier))
                    .add(right.numerator.multiply(new BigDecimal(rightMultiplier)));
            return new ExactAmount(ingredientId, aggregateUnit, sum, left.denominator.multiply(leftMultiplier));
        }

        /** Compares quantities of the same ingredient by cross multiplication. */
        public int compareQuantity(ExactAmount other) {
            requireSameIngredient(other);
            ExactAmount comparable = other.convertTo(unitId);
            return numerator.multiply(new BigDecimal(comparable.denominator))
                    .compareTo(comparable.numerator.multiply(new BigDecimal(denominator)));
        }

        /** Formats at most two decimal places. The returned string is for display only. */
        public String formatForDisplay(RoundingMode roundingMode, Locale locale) {
            Objects.requireNonNull(roundingMode, "roundingMode");
            Objects.requireNonNull(locale, "locale");
            if (roundingMode == RoundingMode.UNNECESSARY) {
                throw new IllegalArgumentException("display rounding must allow inexact quantities");
            }

            ExactAmount displayAmount = this;
            if (isBelowDisplayMinimum()) {
                switch (KitchenUnitCatalog.dimension(unitId)) {
                    case "MASS": displayAmount = convertTo("MG"); break;
                    case "VOLUME": displayAmount = convertTo("ML"); break;
                    default: break;
                }
                if (displayAmount.isBelowDisplayMinimum()) {
                    return "< " + formatNumber(MIN_DISPLAY_AMOUNT, locale) + " " + KitchenUnitCatalog.label(unitId);
                }
            }

            BigDecimal rounded = displayAmount.numerator.divide(new BigDecimal(displayAmount.denominator),
                    DISPLAY_SCALE, roundingMode);
            return formatNumber(rounded, locale) + " " + KitchenUnitCatalog.label(displayAmount.unitId);
        }

        private boolean isBelowDisplayMinimum() {
            return numerator.signum() > 0
                    && numerator.compareTo(MIN_DISPLAY_AMOUNT.multiply(new BigDecimal(denominator))) < 0;
        }

        private void requireSameIngredient(ExactAmount other) {
            if (!ingredientId.equals(other.ingredientId)) {
                throw new IllegalArgumentException("quantities must refer to the same ingredient");
            }
        }

        private static String formatNumber(BigDecimal value, Locale locale) {
            char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            return value.stripTrailingZeros().toPlainString().replace('.', decimalSeparator);
        }
    }
}
