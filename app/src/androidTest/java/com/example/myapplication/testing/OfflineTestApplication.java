package com.example.myapplication.testing;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.di.AppContainer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;

public final class OfflineTestApplication extends BepNhaApplication {
    private volatile CatalogSyncCoordinator foregroundSync;
    private volatile CompletableFuture<ApplyResult> foregroundResult;

    @Override
    protected AppContainer createAppContainer() {
        return new AppContainer(this, () -> { throw new IOException("test catalog offline"); },
                uri -> { throw new IOException("test images offline"); },
                new File(getCacheDir(), "instrumentation-recipe-images"));
    }

    @Override
    protected void onAppForeground() {
        CatalogSyncCoordinator sync = foregroundSync;
        if (sync != null) foregroundResult = sync.onForeground(this::isInForeground);
    }

    public void injectForegroundSync(CatalogSyncCoordinator sync) {
        foregroundResult = null;
        foregroundSync = sync;
    }

    public CompletableFuture<ApplyResult> foregroundResult() { return foregroundResult; }
}
