package com.example.myapplication.domain.repository;

import com.example.myapplication.domain.model.Ingredient;
import com.example.myapplication.domain.model.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Synchronous local catalog reads. Call on an I/O executor. */
public interface RecipeCatalogRepository {
    Catalog readCatalog();

    default Long readRevision() { return readCatalog().revision; }

    Lookup findById(String recipeId);

    final class Catalog {
        /** Null until a snapshot is stored. An empty snapshot still has a revision. */
        public final Long revision;
        public final List<Recipe> recipes;
        public final Map<String, String> categories;
        public final Map<String, String> diets;
        public final Map<String, Ingredient> ingredients;

        public Catalog(Long revision, List<Recipe> recipes, Map<String, String> categories,
                       Map<String, String> diets, Map<String, Ingredient> ingredients) {
            this.revision = revision;
            this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
            this.categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
            this.diets = Collections.unmodifiableMap(new LinkedHashMap<>(diets));
            this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
        }
    }

    final class Lookup {
        public enum Status { AVAILABLE, REMOVED, MISSING }

        public final Status status;
        public final Recipe recipe;

        private Lookup(Status status, Recipe recipe) {
            this.status = status;
            this.recipe = recipe;
        }

        public static Lookup available(Recipe recipe) { return new Lookup(Status.AVAILABLE, recipe); }

        public static Lookup removed() { return new Lookup(Status.REMOVED, null); }

        public static Lookup missing() { return new Lookup(Status.MISSING, null); }
    }
}
