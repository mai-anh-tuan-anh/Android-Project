package com.example.myapplication.data.local.database;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.remote.FirebaseCatalogRemoteDataSource;
import com.example.myapplication.data.remote.FirebaseCatalogRemoteDataSource.HttpStatusException;
import com.example.myapplication.data.repository.CatalogRefreshService;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.PlannedRecipe;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.domain.usecase.ServingCalculator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/** Run the online method first, then the offline method with the same explicit smokeRunId. */
@RunWith(AndroidJUnit4.class)
public class CatalogLiveSmokeTest {
    private static final String RECIPE_ID = "recipe-0001";
    private static final String COVER_PATH = "recipes/recipe-0001/cover-v1.jpg";
    private static final String MARKER_ID = "n2-06d-personal-marker";
    private static final String MARKER_NOTE = "Ghi chú kiểm thử N2-06D";
    private static final long TIMEOUT_SECONDS = 35;

    private Context context;
    private String catalogName;
    private String personalName;
    private ExecutorService io;

    @Before
    public void requireOptIn() {
        Bundle arguments = InstrumentationRegistry.getArguments();
        assumeTrue("Live Firebase smoke test requires -e liveCatalog true",
                "true".equals(arguments.getString("liveCatalog")));
        String runId = arguments.getString("smokeRunId", "");
        assertTrue("Provide a unique smokeRunId for both phases", runId.matches("[a-zA-Z0-9-]{1,48}"));
        context = ApplicationProvider.getApplicationContext();
        catalogName = "n2_06d_catalog_" + runId + ".db";
        personalName = "n2_06d_personal_" + runId + ".db";
        io = Executors.newSingleThreadExecutor();
    }

    @After
    public void stopWorker() throws InterruptedException {
        if (io != null) {
            io.shutdownNow();
            assertTrue("I/O worker did not stop", io.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    public void downloadsPersistsAndRefreshesLiveCatalog() throws Exception {
        assertFalse(context.getDatabasePath(catalogName).exists());
        assertFalse(context.getDatabasePath(personalName).exists());
        boolean readyForOffline = false;
        try {
            seedPersonalData();
            byte[] personalBefore = personalBytes();
            try (RecipeCatalogDbHelper database = new RecipeCatalogDbHelper(context, catalogName)) {
                LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(database);
                assertNull(local.readCatalog().revision);
                assertEquals(ApplyResult.APPLIED, refreshService(local).refresh().get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertCatalog(local);
                passed("1 first download APPLIED");
            }

            try (RecipeCatalogDbHelper reopened = new RecipeCatalogDbHelper(context, catalogName)) {
                LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(reopened);
                Recipe recipe = assertCatalog(local);
                passed("2 SQLite reopened");
                assertScaledServings(recipe);
                assertCatalog(local);
                passed("3 four servings and original amounts");
                assertCookingSteps(recipe.details.steps);
                passed("4 step order, links, timer metadata and images");
                assertEquals(ApplyResult.UNCHANGED, refreshService(local).refresh().get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertCatalog(local);
                passed("5 second refresh UNCHANGED");
            }

            assertArrayEquals(personalBefore, personalBytes());
            assertPersonalData();
            readyForOffline = true;
            passed("7 personal data unchanged after online refreshes");
        } finally {
            if (!readyForOffline) deleteTestDatabases();
        }
    }

    @Test
    public void keepsSnapshotWhenOffline() throws Exception {
        try {
            assertTrue("Run the online phase first with the same smokeRunId",
                    context.getDatabasePath(catalogName).exists());
            byte[] personalBefore = personalBytes();
            try (RecipeCatalogDbHelper database = new RecipeCatalogDbHelper(context, catalogName)) {
                LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(database);
                assertCookingSteps(assertCatalog(local).details.steps);
                ExecutionException error = assertThrows(ExecutionException.class,
                        () -> refreshService(local).refresh().get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertTrue("Expected a network error", error.getCause() instanceof IOException);
                assertFalse("Expected no HTTP response", error.getCause() instanceof HttpStatusException);
                assertCookingSteps(assertCatalog(local).details.steps);
                passed("6 offline error " + error.getCause().getClass().getSimpleName() + ", snapshot retained");
            }
            assertArrayEquals(personalBefore, personalBytes());
            assertPersonalData();
            passed("7 personal data unchanged after offline refresh");
        } finally {
            deleteTestDatabases();
        }
    }

    private CatalogRefreshService refreshService(LocalRecipeCatalogRepository local) {
        return new CatalogRefreshService(new FirebaseCatalogRemoteDataSource(), local::applySnapshot, io);
    }

    private static Recipe assertCatalog(LocalRecipeCatalogRepository local) {
        assertEquals(Long.valueOf(1), local.readCatalog().revision);
        assertEquals(1, local.readCatalog().recipes.size());
        Lookup lookup = local.findById(RECIPE_ID);
        assertEquals(Lookup.Status.AVAILABLE, lookup.status);
        Recipe recipe = lookup.recipe;
        assertEquals("Canh chua cá", recipe.title);
        assertEquals(2, recipe.details.baseServings);
        assertEquals(Recipe.PublicationStatus.PUBLISHED, recipe.details.publicationStatus);
        assertTrue(recipe.details.hasIngredientsForServingAdjustment());
        assertEquals(COVER_PATH, recipe.details.imagePath);
        assertEquals(4, recipe.details.steps.size());
        List<RecipeIngredient> lines = recipe.details.ingredientLines;
        assertEquals(5, lines.size());
        String[] amounts = {"300", "2", "20", null, null};
        String[] units = {"G", "QUA", "G", null, null};
        String[] notes = {null, null, null, "Vừa đủ", "Vừa đủ"};
        for (int index = 0; index < lines.size(); index++) {
            RecipeIngredient line = lines.get(index);
            assertEquals("line-0" + (index + 1), line.recipeIngredientId);
            assertEquals("ing-000" + (index + 1), line.ingredientId);
            assertEquals(amounts[index], line.amountDecimal);
            assertEquals(units[index], line.unitId);
            assertEquals(notes[index], line.qualitativeNote);
        }
        return recipe;
    }

    private static void assertScaledServings(Recipe recipe) {
        List<ServingCalculator.ScaledIngredient> ingredients = ServingCalculator.calculate(recipe, 4).ingredients;
        String[] expected = {"600 g", "4 quả", "40 g", "Vừa đủ", "Vừa đủ"};
        assertEquals(expected.length, ingredients.size());
        for (int index = 0; index < ingredients.size(); index++) {
            assertEquals(expected[index], ingredients.get(index)
                    .formatForDisplay(RoundingMode.HALF_UP, Locale.forLanguageTag("vi-VN")));
        }
    }

    private static void assertCookingSteps(List<CookingStep> steps) {
        List<List<String>> links = Arrays.asList(Arrays.asList("line-01"),
                Arrays.asList("line-02", "line-04"), Arrays.asList("line-01", "line-02", "line-03", "line-05"),
                Arrays.asList("line-04"));
        for (int index = 0; index < steps.size(); index++) {
            CookingStep step = steps.get(index);
            assertEquals("step-0" + (index + 1), step.stepId);
            assertEquals(index + 1, step.sortOrder);
            assertEquals(links.get(index), step.recipeIngredientIds);
            assertEquals(index == 1 ? Integer.valueOf(300) : null, step.durationSeconds);
            assertEquals(index >= 2 ? COVER_PATH : null, step.imagePath);
        }
    }

    private void seedPersonalData() {
        Recipe marker = new Recipe(MARKER_ID, "Món kiểm thử", "", "30 phút", "Dễ");
        try (PersonalDataDbHelper personal = new PersonalDataDbHelper(context, personalName)) {
            personal.saveNote(marker, MARKER_NOTE);
            personal.addFavorite(marker);
            personal.addMealPlanRecipe(marker, 3);
        }
    }

    private void assertPersonalData() {
        try (PersonalDataDbHelper personal = new PersonalDataDbHelper(context, personalName)) {
            assertEquals(MARKER_NOTE, personal.getNote(MARKER_ID));
            assertTrue(personal.isFavorite(MARKER_ID));
            PlannedRecipe marker = personal.getMealPlanRecipes().stream()
                    .filter(item -> MARKER_ID.equals(item.recipe.id)).findFirst().orElse(null);
            assertNotNull(marker);
            assertEquals(3, marker.servings);
        }
    }

    private byte[] personalBytes() throws IOException {
        return Files.readAllBytes(context.getDatabasePath(personalName).toPath());
    }

    private void deleteTestDatabases() {
        context.deleteDatabase(catalogName);
        context.deleteDatabase(personalName);
    }

    private static void passed(String step) {
        Log.i("CatalogLiveSmoke", "PASS " + step);
    }
}
