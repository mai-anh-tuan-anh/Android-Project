package com.example.myapplication.presentation.catalog;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.data.repository.CatalogSyncCoordinator.Phase;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository.Catalog;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Presentation state for the SQLite snapshot and its independent network status. */
public final class CatalogHomeViewModel extends ViewModel {
    public enum Status {
        NOT_LOADED, FIRST_LOADING, FIRST_OFFLINE, FIRST_FAILURE,
        AVAILABLE, EMPTY, REFRESHING, OFFLINE, REFRESH_FAILED, DATABASE_ERROR
    }

    public static final class State {
        public final Status status;
        public final Catalog catalog;
        public final long confirmedAt;

        State(Status status, Catalog catalog, long confirmedAt) {
            this.status = status;
            this.catalog = catalog;
            this.confirmedAt = confirmedAt;
        }
    }

    private final RecipeCatalogRepository repository;
    private final CatalogSyncCoordinator sync;
    private final Executor io;
    private final Executor main;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private Catalog catalog;
    private boolean reading;
    private boolean failedRead;
    private boolean online = true;
    private Phase phase = Phase.IDLE;
    private int requestedRead;
    private boolean cleared;

    public CatalogHomeViewModel(RecipeCatalogRepository repository, CatalogSyncCoordinator sync,
                                LiveData<Boolean> connectivity, Executor io, Executor main) {
        this.repository = repository;
        this.sync = sync;
        this.io = io;
        this.main = main;
        state.addSource(connectivity, value -> {
            online = Boolean.TRUE.equals(value);
            publish();
        });
        state.addSource(sync.phase(), value -> {
            phase = value;
            publish();
        });
        state.addSource(sync.replacements(), generation -> readLocal());
    }

    public LiveData<State> state() { return state; }

    public void readLocal() {
        int read = ++requestedRead;
        reading = true;
        publish();
        CompletableFuture.supplyAsync(repository::readCatalog, io).whenCompleteAsync((local, error) -> {
            if (cleared || read != requestedRead) return;
            reading = false;
            failedRead = error != null;
            if (!failedRead) catalog = local;
            publish();
        }, main);
    }

    public void retry() {
        if (failedRead) readLocal();
        else sync.refresh();
    }

    private void publish() {
        boolean downloaded = catalog != null && catalog.revision != null;
        Status status = status(downloaded);
        long confirmed = downloaded ? sync.lastConfirmedAtMillis() : 0L;
        State previous = state.getValue();
        if (previous == null || previous.status != status || previous.catalog != catalog
                || previous.confirmedAt != confirmed) {
            state.setValue(new State(status, catalog, confirmed));
        }
    }

    private Status status(boolean downloaded) {
        if (failedRead) return Status.DATABASE_ERROR;
        if (!downloaded) {
            if (reading || phase == Phase.RUNNING) return Status.FIRST_LOADING;
            if (!online) return Status.FIRST_OFFLINE;
            return phase == Phase.FAILED || phase == Phase.STALE ? Status.FIRST_FAILURE : Status.NOT_LOADED;
        }
        if (phase == Phase.RUNNING) return Status.REFRESHING;
        if (!online) return Status.OFFLINE;
        if (phase == Phase.FAILED || phase == Phase.STALE) return Status.REFRESH_FAILED;
        return catalog.recipes.isEmpty() ? Status.EMPTY : Status.AVAILABLE;
    }

    @Override protected void onCleared() { cleared = true; }
}
