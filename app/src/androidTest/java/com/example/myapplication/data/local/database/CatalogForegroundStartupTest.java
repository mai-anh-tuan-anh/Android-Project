package com.example.myapplication.data.local.database;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.repository.CatalogRefreshService;
import com.example.myapplication.data.repository.CatalogSyncCoordinator;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.presentation.main.MainActivity;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.State;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.Status;
import com.example.myapplication.testing.OfflineTestApplication;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CatalogForegroundStartupTest {
    @Test public void firstActivityStartsOneRefreshAndDetailJoinsWithoutTouchingPersonalDatabase() throws Exception {
        OfflineTestApplication app = ApplicationProvider.getApplicationContext();
        String catalogName = "catalog-sync-" + UUID.randomUUID() + ".db";
        String personalName = "personal-sync-" + UUID.randomUUID() + ".db";
        ExecutorService localIo = Executors.newSingleThreadExecutor();
        ExecutorService networkIo = Executors.newSingleThreadExecutor();
        AtomicInteger fetches = new AtomicInteger();
        AtomicLong success = new AtomicLong();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch finishFetch = new CountDownLatch(1);
        CountDownLatch detailJoined = new CountDownLatch(1);
        CountDownLatch detailReady = new CountDownLatch(1);

        try (RecipeCatalogDbHelper db = new RecipeCatalogDbHelper(app, catalogName)) {
            try (PersonalDataDbHelper personal = new PersonalDataDbHelper(app, personalName)) {
                personal.saveNote(new Recipe("recipe-soup", "Món cá nhân", "", "", ""), "Giữ nguyên ghi chú");
            }
            byte[] before = Files.readAllBytes(app.getDatabasePath(personalName).toPath());

            LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(db);
            CatalogRefreshService service = new CatalogRefreshService(() -> {
                fetches.incrementAndGet();
                fetchStarted.countDown();
                try {
                    if (!finishFetch.await(8, TimeUnit.SECONDS)) throw new IOException("test did not release refresh");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException(error);
                }
                return snapshot();
            }, local::applySnapshot, networkIo);
            CatalogSyncCoordinator sync = new CatalogSyncCoordinator(local, service::refresh, localIo,
                    () -> 1_000_000L, () -> 1234L, success::get, success::set, 900_000, 60_000);
            RecipeDetailViewModel detail = new RecipeDetailViewModel(local, () -> {
                java.util.concurrent.CompletableFuture<ApplyResult> request = sync.ensureSnapshot();
                detailJoined.countDown();
                return request;
            }, sync::refresh, localIo, ContextCompat.getMainExecutor(app), sync.replacements());
            Observer<State> ready = value -> {
                if (value != null && value.status == Status.AVAILABLE) detailReady.countDown();
            };

            app.injectForegroundSync(sync);
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                assertTrue("initial foreground did not start a refresh", fetchStarted.await(5, TimeUnit.SECONDS));
                assertFalse(app.foregroundResult().isDone());
                scenario.onActivity(activity -> {
                    detail.state().observeForever(ready);
                    detail.open("recipe-soup");
                });
                assertTrue("detail did not join the existing refresh", detailJoined.await(5, TimeUnit.SECONDS));
                assertEquals(1, fetches.get());
                finishFetch.countDown();
                assertEquals(ApplyResult.APPLIED, app.foregroundResult().get(5, TimeUnit.SECONDS));
                assertTrue(detailReady.await(5, TimeUnit.SECONDS));
                assertEquals(Long.valueOf(12), local.readRevision());
                assertEquals(1_000_000L, success.get());
                assertEquals(1, fetches.get());
                assertArrayEquals(before, Files.readAllBytes(app.getDatabasePath(personalName).toPath()));
                scenario.onActivity(activity -> detail.state().removeObserver(ready));
            } finally {
                finishFetch.countDown();
                app.injectForegroundSync(null);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> detail.state().removeObserver(ready));
            }
        } finally {
            networkIo.shutdownNow();
            localIo.shutdownNow();
            networkIo.awaitTermination(5, TimeUnit.SECONDS);
            localIo.awaitTermination(5, TimeUnit.SECONDS);
            app.deleteDatabase(catalogName);
            app.deleteDatabase(personalName);
        }
    }

    private String snapshot() throws IOException {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                .open("catalog/snapshot-v1.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
