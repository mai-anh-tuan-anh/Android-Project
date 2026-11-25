package com.example.myapplication.presentation.cooking;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.model.CookingSession;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.domain.usecase.ServingCalculator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class CookingSessionViewModel extends ViewModel {
    public enum Failure { MISSING, REMOVED, NO_STEPS, INVALID_SERVINGS, CATALOG_ERROR }

    private final RecipeCatalogRepository catalog;
    private final Executor io;
    private final Executor main;
    private final LongSupplier clock;
    private final Supplier<String> sessionIds;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private int request;

    public CookingSessionViewModel(RecipeCatalogRepository catalog, Executor io, Executor main,
                                   LongSupplier clock, Supplier<String> sessionIds) {
        this.catalog = catalog;
        this.io = io;
        this.main = main;
        this.clock = clock;
        this.sessionIds = sessionIds;
    }

    public LiveData<State> state() { return state; }

    public void start(String recipeId, int servings) {
        if (state.getValue() != null) return;
        int startRequest = ++request;
        state.setValue(new State(null, null));
        CompletableFuture.supplyAsync(() -> load(recipeId, servings), io)
                .whenCompleteAsync((result, error) -> {
                    if (startRequest == request) state.setValue(error == null ? result : failure(Failure.CATALOG_ERROR));
                }, main);
    }

    private State load(String recipeId, int servings) {
        if (recipeId == null || recipeId.trim().isEmpty()) return failure(Failure.MISSING);
        if (!ServingCalculator.isValidServings(servings)) return failure(Failure.INVALID_SERVINGS);
        Lookup lookup = catalog.findById(recipeId);
        if (lookup.status == Lookup.Status.REMOVED) return failure(Failure.REMOVED);
        Recipe recipe = lookup.recipe;
        if (lookup.status != Lookup.Status.AVAILABLE || recipe == null || !recipeId.equals(recipe.id)) {
            return failure(Failure.MISSING);
        }
        Recipe.Details details = recipe.details;
        if (details == null) return failure(Failure.NO_STEPS);
        if (details.publicationStatus != Recipe.PublicationStatus.PUBLISHED) {
            return failure(details.publicationStatus == Recipe.PublicationStatus.ARCHIVED
                    ? Failure.REMOVED : Failure.MISSING);
        }
        if (details.steps.isEmpty()) return failure(Failure.NO_STEPS);
        if (!CookingSession.supportsServings(details, servings)) return failure(Failure.INVALID_SERVINGS);
        return new State(CookingSession.start(sessionIds.get(), recipe, servings, clock.getAsLong()), null);
    }

    public void next() { update(CookingSession::next); }
    public void previous() { update(CookingSession::previous); }
    public void pause() { update(CookingSession::pause); }
    public void resume() { update(CookingSession::resume); }
    public void complete() { update(session -> session.complete(clock.getAsLong())); }
    public void cancel() { update(session -> session.cancel(clock.getAsLong())); }

    public void leave() {
        ++request;
        state.setValue(null);
    }

    @Override protected void onCleared() { ++request; }

    private void update(UnaryOperator<CookingSession> transition) {
        State current = state.getValue();
        if (current == null || current.session == null) return;
        CookingSession updated = transition.apply(current.session);
        if (updated != current.session) state.setValue(new State(updated, null));
    }

    private static State failure(Failure failure) { return new State(null, failure); }

    public static final class State {
        public final CookingSession session;
        public final Failure failure;

        private State(CookingSession session, Failure failure) {
            this.session = session;
            this.failure = failure;
        }
    }
}
