package com.example.myapplication.domain.model;


import com.example.myapplication.domain.model.*;
/** User-owned collection summary. */
public final class RecipeCollection {
    public final long id;
    public final String name;
    public final int recipeCount;

    public RecipeCollection(long id, String name, int recipeCount) {
        this.id = id;
        this.name = name;
        this.recipeCount = recipeCount;
    }
}
