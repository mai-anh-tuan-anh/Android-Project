package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Applies the foreground refresh policy and publishes successful catalog replacements. */
public final class CatalogSyncCoordinator {
    public static final long DEFAULT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);
    public static final long DEFAULT_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final RecipeCatalogRepository catalog;
    private final Supplier<CompletableFuture<ApplyResult>> refresh;
    private final Executor io;
    private final LongSupplier wallClock;
    private final LongSupplier elapsedClock;
    private final LongSupplier storedSuccess;
    private final LongConsumer saveSuccess;
    private final long interval;
    private final long retryDelay;
    private final MutableLiveData<Long> replacements = new MutableLiveData<>();

    private CompletableFuture<ApplyResult> inFlight;
    private CompletableFuture<ApplyResult> lastRequest;
    private Long lastCompletion;
    private Long lastSuccess;
    private boolean lastConfirmed;
    private long generation;

    public CatalogSyncCoordinator(RecipeCatalogRepository catalog,
                                  Supplier<CompletableFuture<ApplyResult>> refresh,
                                  Executor io, LongSupplier wallClock, LongSupplier elapsedClock,
                                  LongSupplier storedSuccess, LongConsumer saveSuccess,
                                  long interval, long retryDelay) {
        this.catalog = catalog;
        this.refresh = refresh;
        this.io = io;
        this.wallClock = wallClock;
        this.elapsedClock = elapsedClock;
        this.storedSuccess = storedSuccess;
        this.saveSuccess = saveSuccess;
        this.interval = interval;
        this.retryDelay = retryDelay;
    }

    public LiveData<Long> replacements() { return replacements; }

    /** Completes with null when the foreground check decides to skip the network. */
    public CompletableFuture<ApplyResult> onForeground(BooleanSupplier stillForeground) {
        return CompletableFuture.supplyAsync(catalog::readRevision, io)
                .thenCompose(revision -> automatic(revision, stillForeground));
    }

    private synchronized CompletableFuture<ApplyResult> automatic(Long revision,
                                                                 BooleanSupplier stillForeground) {
        if (!stillForeground.getAsBoolean()) return CompletableFuture.completedFuture(null);
        if (inFlight != null) return inFlight.thenApply(Function.identity());

        long now = elapsedClock.getAsLong();
        if (isCoolingDown(now) && (revision == null || !lastConfirmed)) {
            return CompletableFuture.completedFuture(null);
        }
        if (revision != null && !isDue(now)) return CompletableFuture.completedFuture(null);
        return refresh();
    }

    /** A detail opened without a snapshot reuses a recent attempt; its Retry button uses refresh(). */
    public synchronized CompletableFuture<ApplyResult> ensureSnapshot() {
        if (inFlight != null) return inFlight.thenApply(Function.identity());
        if (isCoolingDown(elapsedClock.getAsLong())) return lastRequest.thenApply(Function.identity());
        return refresh();
    }

    private boolean isCoolingDown(long now) {
        return lastCompletion != null && now - lastCompletion < retryDelay;
    }

    private boolean isDue(long now) {
        if (lastSuccess != null) return now - lastSuccess >= interval;
        long saved = storedSuccess.getAsLong();
        long current = wallClock.getAsLong();
        return saved <= 0 || current < saved || current - saved >= interval;
    }

    /** Explicit retry skips the interval. All callers await the same underlying refresh. */
    public synchronized CompletableFuture<ApplyResult> refresh() {
        if (inFlight != null) return inFlight.thenApply(Function.identity());
        CompletableFuture<ApplyResult> result = new CompletableFuture<>();
        inFlight = result;
        try {
            refresh.get().whenCompleteAsync((applied, error) -> complete(result, applied, error), io);
        } catch (RuntimeException error) {
            complete(result, null, error);
        }
        return result.thenApply(Function.identity());
    }

    private void complete(CompletableFuture<ApplyResult> result, ApplyResult applied, Throwable error) {
        synchronized (this) {
            lastCompletion = elapsedClock.getAsLong();
            lastConfirmed = applied == ApplyResult.APPLIED || applied == ApplyResult.UNCHANGED;
            if (lastConfirmed) {
                lastSuccess = lastCompletion;
                try {
                    saveSuccess.accept(wallClock.getAsLong());
                } catch (RuntimeException unavailable) {
                    // The in-process policy and committed SQLite snapshot still work.
                }
            }
            if (applied == ApplyResult.APPLIED) replacements.postValue(++generation);
            lastRequest = result;
            inFlight = null;
        }
        if (error != null) result.completeExceptionally(error);
        else result.complete(applied);
    }
}
