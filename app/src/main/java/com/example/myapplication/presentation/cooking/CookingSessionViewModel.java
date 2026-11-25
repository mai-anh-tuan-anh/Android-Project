package com.example.myapplication.presentation.cooking;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.domain.model.CookingSession;
import com.example.myapplication.domain.model.CookingSession.TimerInput;
import com.example.myapplication.domain.model.CookingTimer;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Lookup;
import com.example.myapplication.domain.usecase.ServingCalculator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final LongSupplier elapsedClock;
    private final Supplier<String> sessionIds;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    // Only the current session can have entries in this map. Within that session, step IDs are unique.
    private final Map<String, CookingTimer> timers = new LinkedHashMap<>();
    private final ArrayDeque<CookingTimer> timerCompletions = new ArrayDeque<>();
    private int request;

    public CookingSessionViewModel(RecipeCatalogRepository catalog, Executor io, Executor main,
                                   LongSupplier clock, Supplier<String> sessionIds, LongSupplier elapsedClock) {
        this.catalog = catalog;
        this.io = io;
        this.main = main;
        this.clock = clock;
        this.elapsedClock = elapsedClock;
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

    public CookingTimer currentTimer() {
        State current = state.getValue();
        TimerInput input = current == null || current.session == null ? null : current.session.timerInput();
        if (input == null) return null;
        CookingTimer timer = timers.get(input.stepId);
        return timer == null ? CookingTimer.ready(input) : timer;
    }

    public long remainingMillis(CookingTimer timer) {
        return timer.remainingMillis(elapsedClock.getAsLong());
    }

    public void startOrResumeTimer() {
        long now = elapsedClock.getAsLong();
        refreshTimers(now);
        CookingTimer timer = currentTimer();
        if (timer == null || (timer.status != CookingTimer.Status.READY
                && timer.status != CookingTimer.Status.PAUSED)) return;
        pauseRunningTimer(now);
        timers.put(timer.stepId, timer.startOrResume(now));
    }

    public void pauseTimer() {
        long now = elapsedClock.getAsLong();
        refreshTimers(now);
        CookingTimer timer = currentTimer();
        if (timer != null && timer.status == CookingTimer.Status.RUNNING) timers.put(timer.stepId, timer.pause(now));
    }

    public void resetTimer() {
        refreshTimers();
        CookingTimer timer = currentTimer();
        if (timer != null) timers.put(timer.stepId, timer.reset());
    }

    public boolean refreshTimers() { return refreshTimers(elapsedClock.getAsLong()); }

    private boolean refreshTimers(long now) {
        boolean running = false;
        for (Map.Entry<String, CookingTimer> entry : timers.entrySet()) {
            CookingTimer timer = entry.getValue();
            CookingTimer refreshed = timer.refresh(now);
            if (refreshed != timer) {
                entry.setValue(refreshed);
                timerCompletions.add(refreshed);
            }
            running |= refreshed.status == CookingTimer.Status.RUNNING;
        }
        return running;
    }

    public CookingTimer takeTimerCompletion() { return timerCompletions.poll(); }

    /** In-memory snapshots for a future store. Unopened step timers default to READY. */
    public List<CookingTimer> timerSnapshots() {
        refreshTimers();
        return Collections.unmodifiableList(new ArrayList<>(timers.values()));
    }

    private void pauseRunningTimer(long now) {
        timers.replaceAll((stepId, timer) -> timer.pause(now));
    }

    private void clearTimers() {
        timers.clear();
        timerCompletions.clear();
    }

    public void leave() {
        ++request;
        clearTimers();
        state.setValue(null);
    }

    @Override protected void onCleared() {
        ++request;
        clearTimers();
    }

    private void update(UnaryOperator<CookingSession> transition) {
        State current = state.getValue();
        if (current == null || current.session == null) return;
        CookingSession updated = transition.apply(current.session);
        if (updated == current.session) return;
        if (updated.status == CookingSession.Status.COMPLETED || updated.status == CookingSession.Status.CANCELED) {
            clearTimers();
        } else {
            long now = elapsedClock.getAsLong();
            refreshTimers(now);
            if (updated.status == CookingSession.Status.PAUSED) pauseRunningTimer(now);
        }
        state.setValue(new State(updated, null));
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
