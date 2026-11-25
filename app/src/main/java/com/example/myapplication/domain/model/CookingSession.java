package com.example.myapplication.domain.model;

import com.example.myapplication.domain.usecase.ServingCalculator;
import com.example.myapplication.domain.usecase.ServingCalculator.ExactAmount;
import com.example.myapplication.domain.usecase.ServingCalculator.ScaledIngredient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable progress over the recipe data captured at the start of a cooking session. */
public final class CookingSession {
    public enum Status { ACTIVE, PAUSED, COMPLETED, CANCELED }

    public final Snapshot snapshot;
    public final int stepIndex;
    public final int completedSteps;
    public final Status status;
    public final Long endedAtMillis;

    private CookingSession(Snapshot snapshot, int stepIndex, int completedSteps, Status status, Long endedAtMillis) {
        this.snapshot = snapshot;
        this.stepIndex = stepIndex;
        this.completedSteps = completedSteps;
        this.status = status;
        this.endedAtMillis = endedAtMillis;
    }

    public static CookingSession start(String sessionId, Recipe recipe, int servings, long startedAtMillis) {
        Recipe.Details details = recipe.details;
        if (details == null || details.publicationStatus != Recipe.PublicationStatus.PUBLISHED
                || details.steps.isEmpty() || !supportsServings(details, servings)) {
            throw new IllegalArgumentException("recipe cannot start a cooking session with these servings");
        }
        List<ScaledIngredient> scaled = details.hasIngredientsForServingAdjustment()
                ? ServingCalculator.calculate(recipe, servings).ingredients : Collections.emptyList();
        List<IngredientLine> ingredients = new ArrayList<>(details.ingredientLines.size());
        Map<String, IngredientLine> byId = new HashMap<>();
        for (int i = 0; i < details.ingredientLines.size(); i++) {
            RecipeIngredient line = details.ingredientLines.get(i);
            String name = i < recipe.ingredients.size() ? recipe.ingredients.get(i) : null;
            ExactAmount amount = scaled.isEmpty() ? originalAmount(line) : scaled.get(i).amount;
            IngredientLine entry = new IngredientLine(line, name, amount);
            ingredients.add(entry);
            byId.put(line.recipeIngredientId, entry);
        }

        List<Step> steps = new ArrayList<>(details.steps.size());
        for (CookingStep step : details.steps) {
            List<IngredientLine> related = new ArrayList<>();
            for (String id : step.recipeIngredientIds) {
                IngredientLine ingredient = byId.get(id);
                if (ingredient != null) related.add(ingredient);
            }
            steps.add(new Step(step, related));
        }
        return new CookingSession(new Snapshot(sessionId, recipe, servings, startedAtMillis, ingredients, steps),
                0, 0, Status.ACTIVE, null);
    }

    public static boolean supportsServings(Recipe.Details details, int servings) {
        return ServingCalculator.isValidServings(servings)
                && (servings == details.baseServings || details.hasIngredientsForServingAdjustment());
    }

    private static ExactAmount originalAmount(RecipeIngredient line) {
        return line.amountDecimal == null || line.unitId == null ? null
                : ExactAmount.of(line.ingredientId, line.unitId, new BigDecimal(line.amountDecimal));
    }

    public Step currentStep() { return snapshot.steps.get(stepIndex); }
    public boolean isLastStep() { return stepIndex == snapshot.steps.size() - 1; }

    public CookingSession next() {
        if (status != Status.ACTIVE || isLastStep()) return this;
        int nextIndex = stepIndex + 1;
        return new CookingSession(snapshot, nextIndex, Math.max(completedSteps, nextIndex), status, null);
    }

    public CookingSession previous() {
        if (status != Status.ACTIVE || stepIndex == 0) return this;
        return new CookingSession(snapshot, stepIndex - 1, completedSteps, status, null);
    }

    public CookingSession pause() {
        return status == Status.ACTIVE ? new CookingSession(snapshot, stepIndex, completedSteps, Status.PAUSED, null)
                : this;
    }

    public CookingSession resume() {
        return status == Status.PAUSED ? new CookingSession(snapshot, stepIndex, completedSteps, Status.ACTIVE, null)
                : this;
    }

    public CookingSession complete(long finishedAtMillis) {
        if (status != Status.ACTIVE || !isLastStep()) return this;
        return new CookingSession(snapshot, stepIndex, snapshot.steps.size(), Status.COMPLETED,
                Math.max(snapshot.startedAtMillis, finishedAtMillis));
    }

    public CookingSession cancel(long finishedAtMillis) {
        if (status != Status.ACTIVE && status != Status.PAUSED) return this;
        return new CookingSession(snapshot, stepIndex, completedSteps, Status.CANCELED,
                Math.max(snapshot.startedAtMillis, finishedAtMillis));
    }

    /** Timer identity and configured duration for N2-11. */
    public TimerInput timerInput() {
        CookingStep step = currentStep().source;
        return status == Status.ACTIVE && step.durationSeconds != null
                ? new TimerInput(snapshot.sessionId, step.stepId, step.durationSeconds) : null;
    }

    /** One result per completed session. N2-14 may use sessionId to prevent duplicate journals. */
    public Completion completion() {
        return status == Status.COMPLETED ? new Completion(snapshot, endedAtMillis) : null;
    }

    public static final class Snapshot {
        public final String sessionId;
        public final String recipeId;
        public final String recipeTitle;
        public final String recipeImagePath;
        public final int servings;
        public final long startedAtMillis;
        public final List<IngredientLine> ingredients;
        public final List<Step> steps;

        private Snapshot(String sessionId, Recipe recipe, int servings, long startedAtMillis,
                         List<IngredientLine> ingredients, List<Step> steps) {
            this.sessionId = Objects.requireNonNull(sessionId);
            this.recipeId = recipe.id;
            this.recipeTitle = recipe.title;
            this.recipeImagePath = recipe.details.imagePath;
            this.servings = servings;
            this.startedAtMillis = startedAtMillis;
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        }
    }

    public static final class Step {
        public final CookingStep source;
        /** Linked recipe quantities, which may represent the whole recipe. */
        public final List<IngredientLine> ingredients;

        private Step(CookingStep source, List<IngredientLine> ingredients) {
            this.source = source;
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        }
    }

    public static final class IngredientLine {
        public final RecipeIngredient source;
        public final String name;
        public final ExactAmount amount;

        private IngredientLine(RecipeIngredient source, String name, ExactAmount amount) {
            this.source = source;
            this.name = name;
            this.amount = amount;
        }
    }

    public static final class TimerInput {
        public final String sessionId;
        public final String stepId;
        public final int durationSeconds;

        private TimerInput(String sessionId, String stepId, int durationSeconds) {
            this.sessionId = sessionId;
            this.stepId = stepId;
            this.durationSeconds = durationSeconds;
        }
    }

    public static final class Completion {
        public final String sessionId;
        public final String recipeId;
        public final String recipeTitle;
        public final String recipeImagePath;
        public final int servings;
        public final long startedAtMillis;
        public final long endedAtMillis;

        private Completion(Snapshot snapshot, long endedAtMillis) {
            sessionId = snapshot.sessionId;
            recipeId = snapshot.recipeId;
            recipeTitle = snapshot.recipeTitle;
            recipeImagePath = snapshot.recipeImagePath;
            servings = snapshot.servings;
            startedAtMillis = snapshot.startedAtMillis;
            this.endedAtMillis = endedAtMillis;
        }
    }
}
