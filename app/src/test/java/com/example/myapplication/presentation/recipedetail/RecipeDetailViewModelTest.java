package com.example.myapplication.presentation.recipedetail;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class RecipeDetailViewModelTest {
    private final CatalogRepository repository = new CatalogRepository();
    private CompletableFuture<ApplyResult> response = new CompletableFuture<>();
    private int refreshes;
    private RecipeDetailViewModel model;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable work) { work.run(); }
            @Override public void postToMainThread(Runnable work) { work.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        model = new RecipeDetailViewModel(repository, () -> {
            ++refreshes;
            return response;
        }, Runnable::run, Runnable::run);
    }

    @After public void tearDown() { ArchTaskExecutor.getInstance().setDelegate(null); }

    @Test public void initialServingsComeFromRecipe() {
        repository.recipe = recipe("other", 3, Recipe.IngredientDataStatus.COMPLETE);
        model.open("other");
        assertEquals(3, model.state().getValue().servings);
        assertEquals("100 g", amount());
        assertEquals(0, refreshes);
    }

    @Test public void plusMinusAndRepeatedChangesAlwaysUseOriginalAmount() {
        repository.recipe = recipe("id", 3, Recipe.IngredientDataStatus.COMPLETE);
        model.open("id");
        for (int i = 0; i < 3; i++) {
            model.beginAdjustment();
            model.stepServings(1);
            model.applyServings();
            assertEquals("133,33 g", amount());
            model.beginAdjustment();
            model.stepServings(-1);
            model.applyServings();
            assertEquals("100 g", amount());
        }
        assertEquals("100", repository.recipe.details.ingredientLines.get(0).amountDecimal);
    }

    @Test public void invalidInputsCannotReplaceTheAppliedServings() {
        repository.recipe = recipe("id", 2, Recipe.IngredientDataStatus.COMPLETE);
        model.open("id");
        model.beginAdjustment();
        for (String invalid : Arrays.asList("", "0", "-1", "1000", "9.5", "abc", "9999999999999")) {
            model.editServings(invalid);
            assertFalse(model.draft().getValue().valid());
            model.applyServings();
            assertEquals(2, model.state().getValue().servings);
        }
        model.editServings("999");
        model.stepServings(1);
        assertEquals(999, model.draft().getValue().servings);
        model.editServings("1");
        model.stepServings(-1);
        model.applyServings();
        assertEquals(1, model.state().getValue().servings);
    }

    @Test public void qualitativeNotesStayVerbatim() {
        repository.recipe = recipe("id", 2, Recipe.IngredientDataStatus.COMPLETE);
        model.open("id");
        model.beginAdjustment();
        model.editServings("4");
        assertEquals("Vừa đủ", model.draft().getValue().ingredients.get(1).amount);
        model.applyServings();
        assertEquals("Vừa đủ", model.state().getValue().ingredients.get(1).amount);
    }

    @Test public void incompleteOrUnknownDetailsShowKnownAmountsWithoutAdjustment() {
        for (Recipe.IngredientDataStatus data : Arrays.asList(Recipe.IngredientDataStatus.INCOMPLETE,
                Recipe.IngredientDataStatus.UNKNOWN)) {
            model.close();
            repository.recipe = recipe("id", 2, data);
            model.open("id");
            assertFalse(model.state().getValue().canAdjust);
            model.beginAdjustment();
            assertNull(model.draft().getValue());
            assertEquals("100 g", amount());
        }
    }

    @Test public void distinguishesMissingRemovedAndEmptyWithoutRefreshing() {
        repository.recipe = recipe("id", 2, Recipe.IngredientDataStatus.COMPLETE);
        model.open("missing");
        assertEquals(Status.MISSING, status());
        repository.removedId = "removed";
        model.open("removed");
        assertEquals(Status.REMOVED, status());
        repository.revision = 1L;
        repository.recipe = null;
        model.open("empty");
        assertEquals(Status.EMPTY, status());
        assertEquals(0, refreshes);
    }

    @Test public void emptyInitialDatabaseRefreshesOnceAndReadsAgain() {
        model.open("id");
        assertEquals(Status.LOADING, status());
        model.open("id");
        model.retry();
        assertEquals(1, refreshes);
        repository.recipe = recipe("id", 2, Recipe.IngredientDataStatus.COMPLETE);
        response.complete(ApplyResult.APPLIED);
        assertEquals(Status.AVAILABLE, status());
        assertEquals(2, model.state().getValue().servings);
    }

    @Test public void networkFailureCanBeRetriedAndDatabaseFailuresAreSeparate() {
        model.open("id");
        response.completeExceptionally(new IOException("offline"));
        assertEquals(Status.REMOTE_ERROR, status());
        response = new CompletableFuture<>();
        model.retry();
        assertEquals(2, refreshes);
        repository.databaseFailure = true;
        response.complete(ApplyResult.APPLIED);
        assertEquals(Status.DATABASE_ERROR, status());
    }

    @Test public void reattachingToTheSameRecipeKeepsDraftAndCommittedState() {
        repository.recipe = recipe("id", 2, Recipe.IngredientDataStatus.COMPLETE);
        model.open("id");
        model.beginAdjustment();
        model.editServings("4");
        model.open("id");
        assertEquals(4, model.draft().getValue().servings);
        model.applyServings();
        model.open("id");
        assertEquals(4, model.state().getValue().servings);
        assertNull(model.draft().getValue());
    }

    @Test public void completionFromAnEarlierRecipeCannotReplaceCurrentRecipe() {
        model.open("old");
        repository.recipe = recipe("new", 2, Recipe.IngredientDataStatus.COMPLETE);
        model.open("new");
        response.complete(ApplyResult.APPLIED);
        assertEquals("new", model.state().getValue().recipeId);
        assertEquals(Status.AVAILABLE, status());
    }

    private Status status() { return model.state().getValue().status; }
    private String amount() { return model.state().getValue().ingredients.get(0).amount; }

    private static Recipe recipe(String id, int servings, Recipe.IngredientDataStatus data) {
        List<RecipeIngredient> ingredients = Arrays.asList(
                new RecipeIngredient("fish", id, "fish", "100", "G", null, 1),
                new RecipeIngredient("herbs", id, "herbs", null, null, "Vừa đủ", 2));
        Recipe.Details details = new Recipe.Details(null, null, servings, null, null, null,
                Recipe.PublicationStatus.PUBLISHED, null, null, data, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), null, ingredients, Collections.emptyList());
        return Recipe.fromCatalog(id, "Canh", "", "Dễ", null, Collections.emptyList(), Arrays.asList("Cá", "Rau thơm"), details);
    }

    private static final class CatalogRepository implements RecipeCatalogRepository {
        Recipe recipe;
        Long revision;
        String removedId;
        boolean databaseFailure;

        @Override public Catalog readCatalog() {
            return new Catalog(recipe == null ? revision : Long.valueOf(1),
                    recipe == null ? Collections.emptyList() : Collections.singletonList(recipe),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        @Override public Lookup findById(String id) {
            if (databaseFailure) throw new IllegalStateException("local database");
            if (id.equals(removedId)) return Lookup.removed();
            return recipe != null && id.equals(recipe.id) ? Lookup.available(recipe) : Lookup.missing();
        }
    }
}
