package com.example.myapplication;


import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.catalog.IngredientSuggestionMatcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngredientSuggestionMatcherTest {
    @Test
    public void completeMealRanksFirstAndMissingIngredientsAreCounted() {
        List<Recipe> recipes = Arrays.asList(
                new Recipe("missing-two", "Món hai thiếu", "Dễ", 10, true,
                        Arrays.asList("Cà chua", "Thịt bò", "Tỏi", "Hành")),
                new Recipe("missing-one", "Món một thiếu", "Vừa", 20, true,
                        Arrays.asList("Cà chua", "Thịt bò", "Tỏi")),
                new Recipe("complete", "Món đủ", "Khó", 50, true,
                        Arrays.asList("Cà chua", "Thịt bò")),
                new Recipe("no-overlap", "Không khớp", "Dễ", 5, true,
                        Collections.singletonList("Cá hồi")),
                new Recipe("unknown", "Chưa rõ", "Dễ", 5, false,
                        Collections.singletonList("Cà chua")));

        List<Recipe> results = suggest(recipes, Set.of("cà chua", "thịt bò"));
        assertEquals(3, results.size());
        assertEquals("complete", results.get(0).id);
        assertEquals(0, IngredientSuggestionMatcher.missing(results.get(0).ingredients,
                Set.of("Cà chua", "Thịt bò")).size());
        assertEquals("missing-one", results.get(1).id);
        assertEquals(1, IngredientSuggestionMatcher.missing(results.get(1).ingredients,
                Set.of("Cà chua", "Thịt bò")).size());
        assertEquals("missing-two", results.get(2).id);
        assertEquals(2, IngredientSuggestionMatcher.missing(results.get(2).ingredients,
                Set.of("Cà chua", "Thịt bò")).size());
    }

    @Test
    public void rankingUsesDifficultyThenTimeThenNormalizedNameAndStableId() {
        List<Recipe> recipes = Arrays.asList(
                new Recipe("z-id", "Bánh táo", "Vừa", 10, true, Collections.singletonList("Cà chua")),
                new Recipe("b-id", "Canh cá", "Dễ", 20, true, Collections.singletonList("Cà chua")),
                new Recipe("a-id", "Canh ca", "Dễ", 20, true, Collections.singletonList("Cà chua")),
                new Recipe("hard", "Món khó", "Khó", 1, true, Collections.singletonList("Cà chua")));

        List<Recipe> results = suggest(recipes, Collections.singleton("Cà chua"));
        assertEquals(Arrays.asList("a-id", "b-id", "z-id", "hard"), ids(results));
    }

    @Test
    public void duplicateIngredientsDoNotInflateBadgesAndNoSelectionReturnsNoResults() {
        Recipe recipe = new Recipe("duplicate", "Trùng", "Dễ", 10, true,
                Arrays.asList("Cà chua", "ca  chua", "Tỏi"));
        assertEquals(2, IngredientSuggestionMatcher.total(recipe.ingredients));
        assertEquals(1, IngredientSuggestionMatcher.countAvailable(recipe.ingredients,
                IngredientSuggestionMatcher.normalizeSet(Collections.singleton("CÀ CHUA"))));
        assertEquals(1, IngredientSuggestionMatcher.missing(recipe.ingredients,
                Collections.singleton("Cà chua")).size());
        assertTrue(suggest(Collections.singletonList(recipe), Collections.emptySet()).isEmpty());
    }

    private static List<Recipe> suggest(List<Recipe> recipes, Set<String> available) {
        return IngredientSuggestionMatcher.suggest(recipes, available, recipe -> recipe.ingredients,
                recipe -> recipe.complete, recipe -> recipe.difficulty, recipe -> recipe.minutes,
                recipe -> recipe.name, recipe -> recipe.id);
    }

    private static List<String> ids(List<Recipe> recipes) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (Recipe recipe : recipes) result.add(recipe.id);
        return result;
    }

    private static final class Recipe {
        final String id;
        final String name;
        final String difficulty;
        final int minutes;
        final boolean complete;
        final List<String> ingredients;

        Recipe(String id, String name, String difficulty, int minutes, boolean complete, List<String> ingredients) {
            this.id = id;
            this.name = name;
            this.difficulty = difficulty;
            this.minutes = minutes;
            this.complete = complete;
            this.ingredients = ingredients;
        }
    }
}
