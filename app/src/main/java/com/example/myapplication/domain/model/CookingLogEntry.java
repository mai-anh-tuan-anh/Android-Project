package com.example.myapplication.domain.model;


import com.example.myapplication.domain.model.*;
/** A locally stored cooking session linked to a stable recipe ID. */
public final class CookingLogEntry {
    public final String id;
    public final String recipeId;
    public final String title;
    public final String imageRes;
    public final long cookedAt;
    public final int servings;
    public final Integer actualDurationMinutes;
    public final Integer rating;
    public final String adjustmentNote;
    public final String photoLocalPath;
    public final long createdAt;
    public final long updatedAt;

    public CookingLogEntry(String id, String recipeId, String title, String imageRes, long cookedAt, int servings,
                           Integer actualDurationMinutes, Integer rating, String adjustmentNote, String photoLocalPath,
                           long createdAt, long updatedAt) {
        this.id = id;
        this.recipeId = recipeId;
        this.title = title;
        this.imageRes = imageRes;
        this.cookedAt = cookedAt;
        this.servings = servings;
        this.actualDurationMinutes = actualDurationMinutes;
        this.rating = rating;
        this.adjustmentNote = adjustmentNote;
        this.photoLocalPath = photoLocalPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
