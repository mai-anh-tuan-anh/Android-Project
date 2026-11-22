package com.example.myapplication;


import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.presentation.discover.DiscoverViewModel;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiscoverViewModelTest {
    private final DiscoverViewModel viewModel = new DiscoverViewModel();

    @Test
    public void categoryFilterMatchesWholeLabelsIgnoringVietnameseDiacritics() {
        List<Recipe> recipes = Arrays.asList(
                new Recipe("viet", Arrays.asList("Món Việt", "Món nhanh"), 20, "Dễ", 10, 99),
                new Recipe("asian", Collections.singletonList("Món Á"), 25, "Vừa", 20, 100),
                new Recipe("vegetarian", Collections.singletonList("Món chay"), 15, "Dễ", 30, 30));

        List<Recipe> results = viewModel.recipesInCategory(recipes, "mon viet", recipe -> recipe.categories);
        assertEquals(1, results.size());
        assertEquals("viet", results.get(0).id);

        results = viewModel.recipesInCategory(recipes, "Món", recipe -> recipe.categories);
        assertTrue(results.isEmpty());
    }

    @Test
    public void quickAndEasyRequiresBothConditionsAndSortsByTime() {
        List<Recipe> recipes = Arrays.asList(
                new Recipe("slow-easy", Collections.emptyList(), 35, "Dễ", 0, 0),
                new Recipe("fast-medium", Collections.emptyList(), 10, "Vừa", 0, 0),
                new Recipe("quick-easy", Collections.emptyList(), 20, "Dễ", 0, 0),
                new Recipe("fastest-easy", Collections.emptyList(), 12, "Dễ", 0, 0));

        List<Recipe> results = viewModel.quickAndEasy(recipes,
                recipe -> recipe.minutes, recipe -> recipe.difficulty);
        assertEquals(2, results.size());
        assertEquals("fastest-easy", results.get(0).id);
        assertEquals("quick-easy", results.get(1).id);
    }

    @Test
    public void newestRecipesSortByPublicationRatherThanEditTime() {
        long now = System.currentTimeMillis();
        List<Recipe> recipes = Arrays.asList(
                new Recipe("published-first", Collections.emptyList(), 20, "Dễ", now - 20_000, now - 1),
                new Recipe("published-last", Collections.emptyList(), 20, "Dễ", now - 10_000, now - 30_000),
                new Recipe("draft", Collections.emptyList(), 20, "Dễ", 0, now));

        List<Recipe> results = viewModel.newlyPublished(recipes, recipe -> recipe.publishedAt);
        assertEquals(2, results.size());
        assertEquals("published-last", results.get(0).id);
        assertEquals("published-first", results.get(1).id);
    }

    private static final class Recipe {
        final String id;
        final List<String> categories;
        final int minutes;
        final String difficulty;
        final long publishedAt;
        final long updatedAt;

        Recipe(String id, List<String> categories, int minutes, String difficulty,
               long publishedAt, long updatedAt) {
            this.id = id;
            this.categories = categories;
            this.minutes = minutes;
            this.difficulty = difficulty;
            this.publishedAt = publishedAt;
            this.updatedAt = updatedAt;
        }
    }
}
