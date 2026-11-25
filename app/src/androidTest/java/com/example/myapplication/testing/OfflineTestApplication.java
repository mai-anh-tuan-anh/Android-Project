package com.example.myapplication.testing;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.data.local.database.RecipeCatalogDbHelper;
import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.di.AppContainer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;

public final class OfflineTestApplication extends BepNhaApplication {
    private volatile CatalogSyncCoordinator foregroundSync;
    private volatile CompletableFuture<ApplyResult> foregroundResult;
    private volatile AppContainer testContainer;

    @Override
    protected AppContainer createAppContainer() {
        return new AppContainer(this, () -> { throw new IOException("test catalog offline"); },
                uri -> { throw new IOException("test images offline"); },
                new File(getCacheDir(), "instrumentation-recipe-images"),
                new LocalRecipeCatalogRepository(new RecipeCatalogDbHelper(this, "instrumentation-catalog.db")),
                new LocalPersonalDataRepository(this),
                new MutableLiveData<>(true), "instrumentation_catalog_sync");
    }

    @Override public AppContainer getAppContainer() {
        AppContainer override = testContainer;
        return override == null ? super.getAppContainer() : override;
    }

    public void injectContainer(AppContainer container) { testContainer = container; }

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
