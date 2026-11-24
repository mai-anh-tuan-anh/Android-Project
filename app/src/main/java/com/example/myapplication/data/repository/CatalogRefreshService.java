package com.example.myapplication.data.repository;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.remote.CatalogRemoteDataSource;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

/** Explicitly fetches one remote snapshot and passes it through the local catalog writer. */
public final class CatalogRefreshService {
    private final CatalogRemoteDataSource remote;
    private final Function<String, ApplyResult> applySnapshot;
    private final Executor ioExecutor;
    private CompletableFuture<ApplyResult> inFlight;

    public CatalogRefreshService(CatalogRemoteDataSource remote, Function<String, ApplyResult> applySnapshot,
                                 Executor ioExecutor) {
        this.remote = Objects.requireNonNull(remote, "remote");
        this.applySnapshot = Objects.requireNonNull(applySnapshot, "applySnapshot");
        this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
    }

    /** Concurrent callers share one refresh. Cancelling a caller's future does not cancel the shared work. */
    public synchronized CompletableFuture<ApplyResult> refresh() {
        if (inFlight != null && !inFlight.isDone()) return inFlight.thenApply(Function.identity());

        CompletableFuture<ApplyResult> request = CompletableFuture.supplyAsync(this::fetchAndApply, ioExecutor);
        inFlight = request;
        request.whenComplete((result, error) -> clearInFlight(request));
        return request.thenApply(Function.identity());
    }

    private ApplyResult fetchAndApply() {
        try {
            return applySnapshot.apply(remote.fetchSnapshot());
        } catch (IOException error) {
            throw new CompletionException(error);
        }
    }

    private synchronized void clearInFlight(CompletableFuture<ApplyResult> request) {
        if (inFlight == request) inFlight = null;
    }
}
