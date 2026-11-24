package com.example.myapplication.domain.model;

import com.example.myapplication.domain.catalog.KitchenUnitCatalog;

import java.util.regex.Pattern;

/** One original ingredient line of a recipe, before serving adjustment. */
public final class RecipeIngredient {
    private static final Pattern DECIMAL = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    public final String recipeIngredientId;
    public final String recipeId;
    public final String ingredientId;
    public final String amountDecimal;
    public final String unitId;
    public final String qualitativeNote;
    public final int sortOrder;

    public RecipeIngredient(String recipeIngredientId, String recipeId, String ingredientId,
                            String amountDecimal, String unitId, String qualitativeNote, int sortOrder) {
        if (recipeIngredientId == null || recipeIngredientId.trim().isEmpty()
                || recipeId == null || recipeId.trim().isEmpty()
                || ingredientId == null || ingredientId.trim().isEmpty()) {
            throw new IllegalArgumentException("ingredient line requires a line, recipe and ingredient ID");
        }
        if (amountDecimal != null && !DECIMAL.matcher(amountDecimal).matches()) {
            throw new IllegalArgumentException("amountDecimal must be a non-negative plain decimal string");
        }
        if (unitId != null && !KitchenUnitCatalog.isKnownUnitId(unitId)) {
            throw new IllegalArgumentException("unitId must be a canonical kitchen unit ID");
        }
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must be non-negative");
        this.recipeIngredientId = recipeIngredientId;
        this.recipeId = recipeId;
        this.ingredientId = ingredientId;
        this.amountDecimal = amountDecimal;
        this.unitId = unitId;
        this.qualitativeNote = qualitativeNote;
        this.sortOrder = sortOrder;
    }

    boolean hasAmountOrQualitativeNote() {
        if (amountDecimal != null) return unitId != null;
        return qualitativeNote != null && !qualitativeNote.trim().isEmpty();
    }
}
