package com.example.myapplication.domain.model;


import com.example.myapplication.domain.model.*;
/** Ingredient tracked in the offline pantry. */
public final class PantryIngredient {
    public final String id;
    public final String ingredientId;
    public final String name;
    public final String amountDecimal;
    public final String unitId;
    public final String category;
    public final String expiryDate;
    public final String storageLocation;
    public final String purchaseDate;
    public final long updatedAt;

    public PantryIngredient(String id, String ingredientId, String name, String amountDecimal, String unitId,
                            String category, String expiryDate, String storageLocation, String purchaseDate,
                            long updatedAt) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.name = name;
        this.amountDecimal = amountDecimal;
        this.unitId = unitId;
        this.category = category;
        this.expiryDate = expiryDate;
        this.storageLocation = storageLocation;
        this.purchaseDate = purchaseDate;
        this.updatedAt = updatedAt;
    }
}
