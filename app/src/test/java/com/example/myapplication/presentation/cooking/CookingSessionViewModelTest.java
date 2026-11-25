package com.example.myapplication.presentation.cooking;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.myapplication.domain.model.CookingSession;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.presentation.cooking.CookingSessionViewModel.Failure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public final class CookingSessionViewModelTest {
    private static final String RECIPE = "recipe-0001";
    private final FakeCatalog catalog = new FakeCatalog();
    private CookingSessionViewModel model;
    private long clock = 1000;
    private int nextId;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable work) { work.run(); }
            @Override public void postToMainThread(Runnable work) { work.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        model = new CookingSessionViewModel(catalog, Runnable::run, Runnable::run,
                () -> clock, () -> "session-" + ++nextId);
    }

    @After public void tearDown() { ArchTaskExecutor.getInstance().setDelegate(null); }

    @Test public void startsWithExactRecipeAndServingsAndCalculatesIngredientsOnce() {
        Recipe recipe = sample();
        catalog.lookup = Lookup.available(recipe);
        model.start(RECIPE, 4);

        CookingSession.Snapshot snapshot = session().snapshot;
        assertEquals("session-1", snapshot.sessionId);
        assertEquals(RECIPE, snapshot.recipeId);
        assertEquals("Canh chua cá", snapshot.recipeTitle);
        assertEquals(4, snapshot.servings);
        assertEquals(1000L, snapshot.startedAtMillis);
        assertEquals(4, snapshot.steps.size());
        assertEquals("600 g", display(snapshot.ingredients.get(0)));
        assertEquals("4 quả", display(snapshot.ingredients.get(1)));
        assertEquals("40 g", display(snapshot.ingredients.get(2)));
        assertEquals("Vừa đủ", display(snapshot.ingredients.get(3)));
        assertEquals("Vừa đủ", display(snapshot.ingredients.get(4)));
        assertEquals("300", recipe.details.ingredientLines.get(0).amountDecimal);
        assertEquals(1, catalog.reads);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.steps.clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.ingredients.clear());
    }

    @Test public void removedMissingAndUnavailableStepsNeverCreateAPreviewSession() {
        catalog.lookup = Lookup.removed();
        assertStartFailure(RECIPE, 4, Failure.REMOVED);
        catalog.lookup = Lookup.missing();
        assertStartFailure(RECIPE, 4, Failure.MISSING);
        catalog.lookup = Lookup.available(sample());
        assertStartFailure("different-id", 4, Failure.MISSING);
        catalog.lookup = Lookup.available(new Recipe(RECIPE, "Món chưa có bước", "", "", ""));
        assertStartFailure(RECIPE, 2, Failure.NO_STEPS);
        catalog.lookup = Lookup.available(recipe(Recipe.IngredientDataStatus.COMPLETE, emptyList(), emptyList()));
        assertStartFailure(RECIPE, 2, Failure.NO_STEPS);
        assertEquals(0, nextId);
    }

    @Test public void databaseErrorAndInvalidServingsHaveDistinctFailures() {
        assertStartFailure(RECIPE, 0, Failure.INVALID_SERVINGS);
        assertStartFailure(RECIPE, 1000, Failure.INVALID_SERVINGS);
        assertEquals(0, catalog.reads);
        catalog.error = true;
        assertStartFailure(RECIPE, 2, Failure.CATALOG_ERROR);
    }

    @Test public void stepBoundsAndProgressAreIndependentOfReviewingPreviousSteps() {
        startSample();
        CookingSession first = session();
        model.previous();
        model.complete();
        assertSame(first, session());
        model.next();
        assertEquals("step-02", session().currentStep().source.stepId);
        assertEquals(1, session().completedSteps);
        CookingSession.TimerInput timer = session().timerInput();
        assertNotNull(timer);
        assertEquals(first.snapshot.sessionId, timer.sessionId);
        assertEquals("step-02", timer.stepId);
        assertEquals(300, timer.durationSeconds);
        model.previous();
        assertEquals(0, session().stepIndex);
        assertEquals(1, session().completedSteps);
        model.next();
        assertEquals(1, session().completedSteps);
        model.next();
        model.next();
        CookingSession last = session();
        model.next();
        assertSame(last, session());
        assertEquals("step-04", last.currentStep().source.stepId);
        assertEquals(3, last.completedSteps);
        assertSame(first.snapshot, last.snapshot);
    }

    @Test public void pauseResumeCompleteAndCancelAreBoundedAndTerminal() {
        startSample();
        model.next();
        model.pause();
        CookingSession paused = session();
        assertEquals(CookingSession.Status.PAUSED, paused.status);
        assertNull(paused.timerInput());
        model.next();
        model.complete();
        assertSame(paused, session());
        model.resume();
        assertEquals(1, session().stepIndex);
        model.next();
        model.next();
        clock = 1400L;
        model.complete();
        CookingSession done = session();
        assertEquals(CookingSession.Status.COMPLETED, done.status);
        assertEquals(4, done.completedSteps);
        CookingSession.Completion result = done.completion();
        assertNotNull(result);
        assertEquals(done.snapshot.sessionId, result.sessionId);
        assertEquals(RECIPE, result.recipeId);
        assertEquals(4, result.servings);
        assertEquals(1000L, result.startedAtMillis);
        assertEquals(1400L, result.endedAtMillis);
        model.complete();
        model.cancel();
        model.previous();
        assertSame(done, session());

        model.leave();
        startSample();
        model.pause();
        model.cancel();
        CookingSession canceled = session();
        assertEquals(CookingSession.Status.CANCELED, canceled.status);
        assertNull(canceled.completion());
        model.resume();
        model.complete();
        model.cancel();
        assertSame(canceled, session());
        assertNotEquals(done.snapshot.sessionId, canceled.snapshot.sessionId);
    }

    @Test public void aRemovalOrReplacementCannotChangeTheCurrentSnapshot() {
        startSample();
        CookingSession.Snapshot initial = session().snapshot;
        catalog.lookup = Lookup.available(recipe(Recipe.IngredientDataStatus.INCOMPLETE, emptyList(),
                singletonList(step("new-step", 0, null))));
        model.next();
        catalog.lookup = Lookup.removed();
        model.next();
        model.start(RECIPE, 2);
        assertEquals(1, catalog.reads);
        assertSame(initial, session().snapshot);
        assertEquals(4, session().snapshot.servings);
        assertEquals("40 g", display(session().currentStep().ingredients.get(2)));
        model.leave();
        assertStartFailure(RECIPE, 4, Failure.REMOVED);
    }

    @Test public void incompleteIngredientsOnlyUseTheKnownLinksAtBaseServings() {
        List<RecipeIngredient> lines = singletonList(line("fish", "300", null, null, 0));
        List<CookingStep> steps = singletonList(step("only", 0, null, "fish", "missing-line"));
        catalog.lookup = Lookup.available(recipe(Recipe.IngredientDataStatus.INCOMPLETE, lines, steps));
        assertStartFailure(RECIPE, 4, Failure.INVALID_SERVINGS);
        model.start(RECIPE, 2);
        assertEquals(1, session().currentStep().ingredients.size());
        assertEquals("fish", session().currentStep().ingredients.get(0).source.recipeIngredientId);
        assertNull(session().currentStep().ingredients.get(0).amount);
    }

    @Test public void leavingWhileLoadingIgnoresTheLateResultAndAllowsAnotherStart() {
        ArrayDeque<Runnable> queued = new ArrayDeque<>();
        catalog.lookup = Lookup.available(sample());
        model = new CookingSessionViewModel(catalog, queued::add, Runnable::run, () -> clock,
                () -> "session-" + ++nextId);
        model.start(RECIPE, 2);
        model.start(RECIPE, 3);
        assertEquals(1, queued.size());
        model.leave();
        model.start(RECIPE, 4);
        queued.remove().run();
        assertNull(model.state().getValue().session);
        queued.remove().run();
        assertEquals(4, session().snapshot.servings);
    }

    private CookingSession session() { return model.state().getValue().session; }

    private void startSample() {
        catalog.lookup = Lookup.available(sample());
        model.start(RECIPE, 4);
    }

    private void assertStartFailure(String id, int servings, Failure failure) {
        model.start(id, servings);
        assertNull(session());
        assertEquals(failure, model.state().getValue().failure);
        model.leave();
    }

    private static Recipe sample() {
        return recipe(Recipe.IngredientDataStatus.COMPLETE, Arrays.asList(
                line("fish", "300", "G", null, 0), line("tomato", "2", "QUA", null, 1),
                line("tamarind", "20", "G", null, 2), line("herbs", null, null, "Vừa đủ", 3),
                line("water", null, null, "Vừa đủ", 4)), Arrays.asList(
                step("step-03", 2, null, "fish", "tomato", "tamarind", "water"),
                step("step-01", 0, null, "fish"),
                step("step-04", 3, null, "herbs"),
                step("step-02", 1, 300, "tomato", "herbs")));
    }

    private static Recipe recipe(Recipe.IngredientDataStatus status, List<RecipeIngredient> lines,
                                 List<CookingStep> steps) {
        Recipe.Details details = new Recipe.Details(null, null, 2, null, null, null,
                Recipe.PublicationStatus.PUBLISHED, null, null, status, emptyList(), emptyList(),
                emptyList(), null, lines, steps);
        return Recipe.fromCatalog(RECIPE, "Canh chua cá", "", "Dễ", null, emptyList(),
                Arrays.asList("Cá", "Cà chua", "Me", "Rau thơm", "Nước"), details);
    }

    private static CookingStep step(String id, int order, Integer duration, String... refs) {
        return new CookingStep(id, RECIPE, "Bước " + order, "Hướng dẫn", order, duration, null, Arrays.asList(refs));
    }

    private static RecipeIngredient line(String id, String amount, String unit, String note, int order) {
        return new RecipeIngredient(id, RECIPE, id, amount, unit, note, order);
    }

    private static String display(CookingSession.IngredientLine line) {
        return line.amount == null ? line.source.qualitativeNote
                : line.amount.formatForDisplay(RoundingMode.HALF_UP, Locale.forLanguageTag("vi-VN"));
    }

    private static final class FakeCatalog implements RecipeCatalogRepository {
        Lookup lookup = Lookup.missing();
        int reads;
        boolean error;
        @Override public Catalog readCatalog() { throw new AssertionError("Use the exact ID lookup"); }
        @Override public Lookup findById(String id) {
            reads++;
            if (error) throw new IllegalStateException("SQLite unavailable");
            return lookup;
        }
    }
}
