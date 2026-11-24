package com.example.myapplication.di;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.myapplication.data.image.HostingImageSource;
import com.example.myapplication.data.image.HostingImageUrlResolver;
import com.example.myapplication.data.image.RecipeImageLoader;
import com.example.myapplication.data.remote.CatalogRemoteDataSource;
import com.example.myapplication.data.remote.FirebaseCatalogRemoteDataSource;
import com.example.myapplication.data.repository.CatalogRefreshService;
import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.domain.repository.PersonalDataRepository;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.domain.repository.SearchHistoryRepository;
import com.example.myapplication.data.local.storage.SharedPreferencesSearchHistoryRepository;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** Application-scoped dependencies shared by all feature ViewModels. */
public final class AppContainer {
    private static final String LAST_SUCCESS = "last_success_wall_millis";
    private static final HostingImageUrlResolver IMAGES = new HostingImageUrlResolver(HostingImageUrlResolver.PRODUCTION_ORIGIN);

    public final PersonalDataRepository personalDataRepository;
    public final RecipeCatalogRepository recipeCatalogRepository;
    public final CatalogRefreshService catalogRefreshService;
    public final CatalogSyncCoordinator catalogSync;
    public final RecipeImageLoader recipeImageLoader;
    public final SearchHistoryRepository searchHistoryRepository;
    public final ExecutorService ioExecutor;

    public AppContainer(Application application) {
        this(application, new FirebaseCatalogRemoteDataSource(),
                new HostingImageSource(IMAGES, RecipeImageLoader.RESPONSE_LIMIT_BYTES),
                new File(application.getCacheDir(), RecipeImageLoader.CACHE_DIRECTORY));
    }

    public AppContainer(Application application, CatalogRemoteDataSource catalogSource,
                        RecipeImageLoader.Source imageSource, File imageDirectory) {
        ioExecutor = worker("bep-nha-data-io");
        personalDataRepository = new LocalPersonalDataRepository(application);
        LocalRecipeCatalogRepository localCatalog = new LocalRecipeCatalogRepository(application);
        recipeCatalogRepository = localCatalog;
        boolean debug = (application.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        catalogRefreshService = new CatalogRefreshService(() -> {
            if (debug) Log.d("RecipeCatalogSync", "Fetching catalog");
            return catalogSource.fetchSnapshot();
        }, localCatalog::applySnapshot, worker("bep-nha-catalog-refresh"));
        SharedPreferences syncSettings = application.getSharedPreferences("recipe_catalog_sync", Application.MODE_PRIVATE);
        catalogSync = new CatalogSyncCoordinator(localCatalog, catalogRefreshService::refresh, ioExecutor,
                System::currentTimeMillis, SystemClock::elapsedRealtime,
                () -> syncSettings.getLong(LAST_SUCCESS, 0),
                time -> syncSettings.edit().putLong(LAST_SUCCESS, time).apply(),
                CatalogSyncCoordinator.DEFAULT_INTERVAL_MILLIS, CatalogSyncCoordinator.DEFAULT_RETRY_DELAY_MILLIS);
        recipeImageLoader = new RecipeImageLoader(imageDirectory, IMAGES, uri -> {
            byte[] image = imageSource.fetch(uri);
            if (debug) Log.d("RecipeImageLoader", "Downloaded image bytes: " + image.length);
            return image;
        }, worker("bep-nha-recipe-images"), ContextCompat.getMainExecutor(application),
                RecipeImageLoader.DISK_LIMIT_BYTES, RecipeImageLoader.defaultMemoryLimit());
        searchHistoryRepository = new SharedPreferencesSearchHistoryRepository(application);
    }

    private static ExecutorService worker(String name) {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        });
    }
}
