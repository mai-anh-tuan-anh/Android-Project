package com.example.myapplication.presentation.recipedetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.domain.catalog.KitchenUnitCatalog;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Catalog;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.domain.usecase.ServingCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class RecipeDetailViewModel extends ViewModel {
    public enum Status { LOADING, AVAILABLE, NOT_LOADED, EMPTY, MISSING, REMOVED, DATABASE_ERROR, REMOTE_ERROR }

    private static final Locale DISPLAY_LOCALE = Locale.forLanguageTag("vi-VN");
    private final RecipeCatalogRepository repository;
    private final Supplier<CompletableFuture<ApplyResult>> refresh;
    private final Executor io;
    private final Executor main;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<Draft> draft = new MutableLiveData<>();
    private String recipeId;
    private int request;

    public RecipeDetailViewModel(RecipeCatalogRepository repository,
                                 Supplier<CompletableFuture<ApplyResult>> refresh,
                                 Executor io, Executor main) {
        this.repository = repository;
        this.refresh = refresh;
        this.io = io;
        this.main = main;
    }

    public LiveData<State> state() { return state; }
    public LiveData<Draft> draft() { return draft; }

    public void open(String id) {
        if (id.equals(recipeId)) return;
        close();
        recipeId = id;
        if (id.trim().isEmpty()) state.setValue(State.status(id, Status.MISSING));
        else load();
    }

    public void close() {
        ++request;
        recipeId = null;
        draft.setValue(null);
        state.setValue(null);
    }

    public void retry() {
        State current = state.getValue();
        if (current != null && current.canRetry()) load();
    }

    private void load() {
        String id = recipeId;
        int loadingRequest = ++request;
        state.setValue(State.status(id, Status.LOADING));
        CompletableFuture.supplyAsync(() -> read(id), io)
                .thenCompose(initial -> {
                    if (initial.status != Status.NOT_LOADED) return CompletableFuture.completedFuture(initial);
                    return refresh.get().handle((result, error) -> error == null)
                            .thenApplyAsync(success -> success ? read(id) : State.status(id, Status.REMOTE_ERROR), io);
                })
                .whenCompleteAsync((result, error) -> {
                    if (loadingRequest == request) {
                        state.setValue(error == null ? result : State.status(id, Status.REMOTE_ERROR));
                    }
                }, main);
    }

    private State read(String id) {
        try {
            Lookup lookup = repository.findById(id);
            if (lookup.status == Lookup.Status.AVAILABLE) return State.available(lookup.recipe);
            if (lookup.status == Lookup.Status.REMOVED) return State.status(id, Status.REMOVED);
            Catalog catalog = repository.readCatalog();
            if (catalog.revision == null) return State.status(id, Status.NOT_LOADED);
            for (Recipe recipe : catalog.recipes) {
                if (id.equals(recipe.id)) return State.available(recipe);
            }
            return State.status(id, catalog.recipes.isEmpty() ? Status.EMPTY : Status.MISSING);
        } catch (RuntimeException error) {
            return State.status(id, Status.DATABASE_ERROR);
        }
    }

    public void beginAdjustment() {
        State current = state.getValue();
        if (current != null && current.canAdjust) updateDraft(Integer.toString(current.servings), current);
    }

    public void editServings(String input) {
        if (draft.getValue() != null) updateDraft(input, state.getValue());
    }

    public void stepServings(int delta) {
        Draft current = draft.getValue();
        if (current == null || !current.valid()) return;
        int next = current.servings + delta;
        if (ServingCalculator.isValidServings(next)) editServings(Integer.toString(next));
    }

    public void applyServings() {
        Draft proposed = draft.getValue();
        State current = state.getValue();
        if (proposed == null || !proposed.valid() || current == null || !current.canAdjust) return;
        draft.setValue(null);
        state.setValue(new State(current.recipeId, Status.AVAILABLE, current.recipe, proposed.servings, proposed.ingredients));
    }

    public void cancelAdjustment() {
        if (draft.getValue() != null) draft.setValue(null);
    }

    private void updateDraft(String input, State current) {
        int value = 0;
        String trimmed = input.trim();
        try {
            if (trimmed.matches("[0-9]+")) value = Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            // A number too large for int is also outside the serving contract.
        }
        List<IngredientRow> rows = ServingCalculator.isValidServings(value)
                ? ingredients(current.recipe, value) : Collections.emptyList();
        draft.setValue(new Draft(input, value, rows));
    }

    @Override
    protected void onCleared() { ++request; }

    private static List<IngredientRow> ingredients(Recipe recipe, int servings) {
        Recipe.Details details = recipe.details;
        if (details == null) return Collections.emptyList();
        List<ServingCalculator.ScaledIngredient> scaled = details.hasIngredientsForServingAdjustment()
                ? ServingCalculator.calculate(recipe, servings).ingredients : Collections.emptyList();
        List<IngredientRow> rows = new ArrayList<>();
        for (int index = 0; index < details.ingredientLines.size(); index++) {
            RecipeIngredient line = details.ingredientLines.get(index);
            String name = index < recipe.ingredients.size() ? recipe.ingredients.get(index) : line.ingredientId;
            String original = originalAmount(line);
            String amount = scaled.isEmpty() ? original : scaled.get(index).formatForDisplay(RoundingMode.HALF_UP, DISPLAY_LOCALE);
            rows.add(new IngredientRow(name, original, amount, line.amountDecimal == null));
        }
        return Collections.unmodifiableList(rows);
    }

    private static String originalAmount(RecipeIngredient line) {
        if (line.amountDecimal == null) return line.qualitativeNote;
        BigDecimal quantity = new BigDecimal(line.amountDecimal);
        if (line.unitId != null) {
            return ServingCalculator.ExactAmount.of(line.ingredientId, line.unitId, quantity)
                    .formatForDisplay(RoundingMode.HALF_UP, DISPLAY_LOCALE);
        }
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(DISPLAY_LOCALE));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(quantity);
    }

    public static final class State {
        public final String recipeId;
        public final Status status;
        public final Recipe recipe;
        public final int servings;
        public final boolean canAdjust;
        public final List<IngredientRow> ingredients;

        private State(String recipeId, Status status, Recipe recipe, int servings, List<IngredientRow> ingredients) {
            this.recipeId = recipeId;
            this.status = status;
            this.recipe = recipe;
            this.servings = servings;
            this.ingredients = ingredients;
            canAdjust = recipe != null && recipe.details != null && recipe.details.hasIngredientsForServingAdjustment();
        }

        static State status(String id, Status status) {
            return new State(id, status, null, 0, Collections.emptyList());
        }

        static State available(Recipe recipe) {
            int base = recipe.details == null ? 0 : recipe.details.baseServings;
            return new State(recipe.id, Status.AVAILABLE, recipe, base, ingredients(recipe, base));
        }

        public boolean canRetry() {
            return status == Status.DATABASE_ERROR || status == Status.REMOTE_ERROR || status == Status.NOT_LOADED;
        }
    }

    public static final class Draft {
        public final String input;
        public final int servings;
        public final List<IngredientRow> ingredients;

        private Draft(String input, int servings, List<IngredientRow> ingredients) {
            this.input = input;
            this.servings = servings;
            this.ingredients = ingredients;
        }

        public boolean valid() { return ServingCalculator.isValidServings(servings); }
    }

    public static final class IngredientRow {
        public final String name;
        public final String original;
        public final String amount;
        public final boolean qualitative;

        private IngredientRow(String name, String original, String amount, boolean qualitative) {
            this.name = name;
            this.original = original;
            this.amount = amount;
            this.qualitative = qualitative;
        }
    }
}
