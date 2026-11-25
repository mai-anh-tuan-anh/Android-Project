package com.example.myapplication.presentation.catalog;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.presentation.catalog.CatalogHomeViewModel.State;
import com.example.myapplication.presentation.catalog.CatalogHomeViewModel.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CatalogHomeViewModelTest {
    private final MutableLiveData<Boolean> network = new MutableLiveData<>(true);
    private final FakeRepository repository = new FakeRepository();
    private final Observer<State> observer = state -> {};
    private CatalogSyncCoordinator sync;
    private CatalogHomeViewModel model;
    private CompletableFuture<ApplyResult> nextResult = new CompletableFuture<>();
    private long timestamp;
    private int requests;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable work) { work.run(); }
            @Override public void postToMainThread(Runnable work) { work.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        sync = new CatalogSyncCoordinator(repository, () -> {
            requests++;
            return nextResult;
        }, Runnable::run, () -> 12345L, () -> 500L, () -> timestamp, value -> timestamp = value, 900, 60);
        model = new CatalogHomeViewModel(repository, sync, network, Runnable::run, Runnable::run);
        model.state().observeForever(observer);
        model.readLocal();
    }

    @After public void tearDown() {
        model.state().removeObserver(observer);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void offlineFirstDownloadAndManualRetryShareStartupRequest() {
        assertEquals(Status.NOT_LOADED, state().status);
        network.setValue(false);
        assertEquals(Status.FIRST_OFFLINE, state().status);
        assertNull(state().catalog.revision);
        assertEquals(0, state().confirmedAt);

        sync.onForeground(() -> true);
        model.retry();
        sync.ensureSnapshot();
        assertEquals(1, requests);
        assertEquals(Status.FIRST_LOADING, state().status);
        nextResult.completeExceptionally(new IOException("offline"));
        assertEquals(Status.FIRST_OFFLINE, state().status);

        network.setValue(true);
        assertEquals(Status.FIRST_FAILURE, state().status);
        nextResult = new CompletableFuture<>();
        model.retry();
        repository.local = catalog(1L, Collections.emptyList());
        nextResult.complete(ApplyResult.APPLIED);
        assertEquals(2, requests);
        assertEquals(Status.EMPTY, state().status);
        assertEquals(12345L, state().confirmedAt);
    }

    @Test public void offlineRefreshFailureAndStaleKeepTheStoredSnapshotAndTimestamp() {
        timestamp = 100L;
        repository.local = catalog(2L, Collections.singletonList(new Recipe("id", "Canh", "", "", "")));
        model.readLocal();
        assertEquals(Status.AVAILABLE, state().status);
        network.setValue(false);
        assertEquals(Status.OFFLINE, state().status);
        assertSame(repository.local, state().catalog);

        network.setValue(true);
        model.retry();
        assertEquals(Status.REFRESHING, state().status);
        nextResult.completeExceptionally(new IOException("server did not respond"));
        assertEquals(Status.REFRESH_FAILED, state().status);
        assertSame(repository.local, state().catalog);
        assertEquals(100L, state().confirmedAt);

        nextResult = new CompletableFuture<>();
        model.retry();
        nextResult.complete(ApplyResult.STALE);
        assertEquals(Status.REFRESH_FAILED, state().status);
        assertEquals(100L, state().confirmedAt);

        nextResult = new CompletableFuture<>();
        model.retry();
        nextResult.complete(ApplyResult.UNCHANGED);
        assertEquals(Status.AVAILABLE, state().status);
        assertEquals(12345L, state().confirmedAt);
        assertSame(repository.local, state().catalog);
    }

    @Test public void sqliteFailureIsDifferentFromAnEmptyCatalogAndRetriesLocally() {
        repository.error = true;
        model.readLocal();
        assertEquals(Status.DATABASE_ERROR, state().status);
        repository.error = false;
        repository.local = catalog(0L, Collections.emptyList());
        model.retry();
        assertEquals(0, requests);
        assertEquals(Status.EMPTY, state().status);
        assertEquals(Long.valueOf(0L), state().catalog.revision);
    }

    private State state() { return model.state().getValue(); }

    private static RecipeCatalogRepository.Catalog catalog(Long revision, List<Recipe> recipes) {
        return new RecipeCatalogRepository.Catalog(revision, recipes, Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap());
    }

    private static class FakeRepository implements RecipeCatalogRepository {
        Catalog local = catalog(null, Collections.emptyList());
        boolean error;
        @Override public Catalog readCatalog() {
            if (error) throw new IllegalStateException("SQLite unavailable");
            return local;
        }
        @Override public Lookup findById(String id) { return Lookup.missing(); }
    }
}
