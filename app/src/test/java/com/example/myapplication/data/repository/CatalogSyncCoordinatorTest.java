package com.example.myapplication.data.repository;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.*;

public class CatalogSyncCoordinatorTest {
    private static final long INTERVAL = 900_000;
    private static final long RETRY_DELAY = 60_000;
    private long wall = 12_000_000;
    private long elapsed = 100_000;
    private long stored;
    private Long revision;
    private int calls;
    private int writes;
    private CompletableFuture<ApplyResult> response;
    private CatalogSyncCoordinator sync;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable work) { work.run(); }
            @Override public void postToMainThread(Runnable work) { work.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        sync = coordinator();
    }

    @After public void resetExecutor() { ArchTaskExecutor.getInstance().setDelegate(null); }

    @Test public void firstForegroundRefreshesWithoutSnapshotEvenWithRecentPreference() {
        stored = wall - 1;
        assertEquals(ApplyResult.APPLIED, foreground().join());
        assertEquals(1, calls);
        assertEquals(wall, stored);
        assertEquals(Long.valueOf(1), sync.replacements().getValue());
    }

    @Test public void skipsBeforeIntervalAndRefreshesAtItsBoundary() {
        foreground().join();
        tick(INTERVAL - 1);
        assertNull(foreground().join());
        tick(1);
        response = CompletableFuture.completedFuture(ApplyResult.UNCHANGED);
        assertEquals(ApplyResult.UNCHANGED, foreground().join());
        assertEquals(2, calls);
        assertEquals(2, writes);
        assertEquals(Long.valueOf(1), sync.replacements().getValue());
    }

    @Test public void manualRequestBypassesInterval() {
        foreground().join();
        assertNull(foreground().join());
        assertEquals(ApplyResult.APPLIED, sync.refresh().join());
        assertEquals(2, calls);
    }

    @Test public void failedAndStaleRefreshDoNotAdvanceStoredSuccess() {
        revision = 2L;
        stored = wall - INTERVAL;
        long oldSuccess = stored;
        response = new CompletableFuture<>();
        response.completeExceptionally(new IOException("offline"));
        assertThrows(CompletionException.class, () -> foreground().join());
        assertNull(foreground().join());
        tick(RETRY_DELAY);
        response = CompletableFuture.completedFuture(ApplyResult.STALE);
        assertEquals(ApplyResult.STALE, foreground().join());
        assertEquals(oldSuccess, stored);
        assertEquals(0, writes);
        assertNull(sync.replacements().getValue());
    }

    @Test public void foregroundAndDetailJoinOneRefreshAndCancellationIsIsolated() {
        response = new CompletableFuture<>();
        CompletableFuture<ApplyResult> startup = foreground();
        CompletableFuture<ApplyResult> fromDetail = sync.ensureSnapshot();
        CompletableFuture<ApplyResult> repeatedForeground = foreground();
        fromDetail.cancel(false);
        assertEquals(1, calls);
        response.complete(ApplyResult.APPLIED);
        assertEquals(ApplyResult.APPLIED, startup.join());
        assertEquals(ApplyResult.APPLIED, repeatedForeground.join());
        assertEquals(1, writes);
    }

    @Test public void reopeningDetailAfterFailedStartupDoesNotRetryUntilUserRequestsIt() {
        response = new CompletableFuture<>();
        response.completeExceptionally(new IOException("offline"));
        assertThrows(CompletionException.class, () -> foreground().join());
        assertThrows(CompletionException.class, () -> sync.ensureSnapshot().join());
        assertEquals(1, calls);

        response = CompletableFuture.completedFuture(ApplyResult.APPLIED);
        assertEquals(ApplyResult.APPLIED, sync.refresh().join());
        assertEquals(2, calls);
    }

    @Test public void restartUsesPersistedWallTimeAndFutureTimestampsAreDue() {
        foreground().join();
        tick(100);
        sync = coordinator();
        assertNull(foreground().join());
        wall -= 300;
        assertEquals(ApplyResult.APPLIED, foreground().join());
        assertEquals(2, calls);
    }

    @Test public void wallClockChangesDoNotCauseRepeatedForegroundRefreshInSameProcess() {
        foreground().join();
        wall += 24 * INTERVAL;
        elapsed += RETRY_DELAY;
        assertNull(foreground().join());
        assertNull(sync.onForeground(() -> false).join());
        assertEquals(1, calls);
    }

    private CompletableFuture<ApplyResult> foreground() { return sync.onForeground(() -> true); }

    private void tick(long millis) {
        wall += millis;
        elapsed += millis;
    }

    private CatalogSyncCoordinator coordinator() {
        RecipeCatalogRepository local = new RecipeCatalogRepository() {
            @Override public Long readRevision() { return revision; }
            @Override public Catalog readCatalog() { throw new AssertionError("foreground only needs revision"); }
            @Override public Lookup findById(String recipeId) { throw new AssertionError("foreground does not read recipes"); }
        };
        return new CatalogSyncCoordinator(local, () -> {
            ++calls;
            return (response == null ? CompletableFuture.completedFuture(ApplyResult.APPLIED) : response)
                    .thenApply(result -> {
                        if (result == ApplyResult.APPLIED) revision = 1L;
                        return result;
                    });
        }, Runnable::run, () -> wall, () -> elapsed, () -> stored, value -> {
            ++writes;
            stored = value;
        }, INTERVAL, RETRY_DELAY);
    }
}
