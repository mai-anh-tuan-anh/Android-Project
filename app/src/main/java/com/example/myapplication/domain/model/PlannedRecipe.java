package com.example.myapplication.domain.model;


import com.example.myapplication.domain.model.*;
/** A recipe and its independent serving count in today's plan. */
public final class PlannedRecipe {
    public final Recipe recipe;
    public final int servings;

    public PlannedRecipe(Recipe recipe, int servings) {
        this.recipe = recipe;
        this.servings = servings;
    }
}
