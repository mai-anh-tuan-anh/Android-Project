package com.example.myapplication.presentation.catalog;

import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.database.PersonalDataDbHelper;
import com.example.myapplication.data.local.database.RecipeCatalogDbHelper;
import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.di.AppContainer;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.presentation.main.MainActivity;
import com.example.myapplication.testing.OfflineTestApplication;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public final class CatalogHomeUiTest {
    private static final String EMPTY = "{\"meta\":{\"schemaVersion\":1,\"revision\":13}}";
    private static final String REMOVAL = "{\"meta\":{\"schemaVersion\":1,\"revision\":13},"
            + "\"removed\":{\"recipe-soup\":{\"removedAtMillis\":1780200000000}}}";

    @Test public void firstOfflineShowsRetryAndAnEmptySnapshotHasItsOwnState() throws Exception {
        try (Fixture fixture = new Fixture(false)) {
            fixture.app.injectForegroundSync(fixture.graph.catalogSync);
            try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
                visible("Đang chuẩn bị kho món");
                capture("01-first-loading");
                fixture.response.completeExceptionally(new IOException("offline"));
                visible("Chưa có công thức đã tải");
                visible("Kết nối Internet để tải kho món lần đầu.");
                onView(withText(containsString("Đồng bộ lần cuối"))).check(doesNotExist());
                capture("02-first-offline");

                activity.onActivity(ignored -> fixture.connection.setValue(true));
                visible("Cập nhật chưa thành công");
                fixture.nextResponse();
                onView(withText("Thử lại")).perform(click());
                visible("Đang chuẩn bị kho món");
                fixture.response.complete(EMPTY);
                visible("Kho công thức đang trống");
                onView(withText(containsString("Đồng bộ lần cuối"))).check(matches(isDisplayed()));
                assertEquals(2, fixture.requests.get());
                capture("03-empty-catalog");
            }
        }
    }

    @Test public void cachedCatalogSurvivesOfflineAndRefreshFailureThenShowsTheRemovalInOpenDetail() throws Exception {
        try (Fixture fixture = new Fixture(false)) {
            long now = System.currentTimeMillis();
            fixture.local.applySnapshot(snapshot().replace("1780000000000", Long.toString(now))
                    .replace("1780100000000", Long.toString(now)));
            fixture.personal.saveNote(new Recipe("recipe-soup", "Canh mẫu kiểm thử", "", "30 phút", "Dễ"),
                    "Ghi chú giữ lại đúng món");
            try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
                visible("Đang ngoại tuyến");
                visible("Vừa công bố");
                capture("04-offline-with-catalog");
                activity.onActivity(ignored -> fixture.connection.setValue(true));
                visible("Cập nhật");
                capture("08-recently-published");
                onView(withText("Cập nhật")).perform(click());
                visible("Đang cập nhật công thức...");
                capture("05-refreshing");
                fixture.response.completeExceptionally(new IOException("server unavailable"));
                visible("Cập nhật chưa thành công");
                onView(allOf(withText("Canh mẫu kiểm thử"), hasSibling(hasDescendant(withText("Vừa công bố")))))
                        .check(matches(isDisplayed()));
                capture("06-refresh-failed");

                onView(withText("Món canh")).perform(click());
                onView(withText("Canh mẫu kiểm thử")).perform(scrollTo(), click());
                visible("3 người");
                onView(withText("3 người")).perform(click());
                visible("Điều chỉnh khẩu phần");
                fixture.nextResponse();
                fixture.graph.catalogSync.refresh();
                fixture.response.complete(REMOVAL);
                visible("Công thức đã được gỡ");
                onView(withText("Điều chỉnh khẩu phần")).check(doesNotExist());
                onView(withText("Bắt đầu nấu")).check(doesNotExist());
                capture("07-recipe-removed");

                onView(withText("Xem ghi chú đã lưu")).perform(click());
                visible("Ghi chú cá nhân");
                visible("Ghi chú giữ lại đúng món");
                pressBack();
                visible("Công thức đã được gỡ");
                onView(withText("Về Khám phá")).perform(click());
                visible("Cập nhật");
                onView(withText("Canh mẫu kiểm thử")).check(doesNotExist());
                assertEquals(2, fixture.requests.get());
            }
        }
    }

    private static void visible(String text) {
        long deadline = SystemClock.elapsedRealtime() + 7000;
        while (true) {
            try {
                onView(withText(text)).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionError error) {
                if (SystemClock.elapsedRealtime() >= deadline) throw error;
                SystemClock.sleep(40);
            }
        }
    }

    private static void capture(String name) throws IOException {
        if (!"1".equals(InstrumentationRegistry.getArguments().getString("catalogCapture"))) return;
        OfflineTestApplication app = ApplicationProvider.getApplicationContext();
        File output = new File(app.getCacheDir(), "n2-09-" + name + ".png");
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // The UI thread can be idle before SurfaceFlinger presents the just-asserted state.
        SystemClock.sleep(160);
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException(output.toString());
        } finally {
            screenshot.recycle();
        }
    }

    private static String snapshot() throws IOException {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                .open("catalog/snapshot-v1.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final OfflineTestApplication app = ApplicationProvider.getApplicationContext();
        final String namespace = "n209-" + UUID.randomUUID();
        final RecipeCatalogDbHelper catalog = new RecipeCatalogDbHelper(app, namespace + "-catalog.db");
        final PersonalDataDbHelper personal = new PersonalDataDbHelper(app, namespace + "-personal.db");
        final LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(catalog);
        final AtomicInteger requests = new AtomicInteger();
        final MutableLiveData<Boolean> connection;
        final AppContainer graph;
        volatile CompletableFuture<String> response = new CompletableFuture<>();

        Fixture(boolean online) {
            connection = new MutableLiveData<>(online);
            graph = new AppContainer(app, this::fetch, uri -> { throw new IOException("image unavailable"); },
                    new File(app.getCacheDir(), namespace), local, new LocalPersonalDataRepository(app, personal),
                    connection, namespace);
            app.injectContainer(graph);
        }

        void nextResponse() { response = new CompletableFuture<>(); }

        private String fetch() throws IOException {
            requests.incrementAndGet();
            try {
                return response.get(20, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException(error);
            } catch (ExecutionException | TimeoutException error) {
                throw new IOException(error);
            }
        }

        @Override public void close() throws InterruptedException {
            app.injectForegroundSync(null);
            app.injectContainer(null);
            response.completeExceptionally(new IOException("fixture closed"));
            graph.ioExecutor.shutdown();
            graph.ioExecutor.awaitTermination(5, TimeUnit.SECONDS);
            catalog.close();
            personal.close();
            app.deleteDatabase(namespace + "-catalog.db");
            app.deleteDatabase(namespace + "-personal.db");
            app.deleteSharedPreferences(namespace);
        }
    }
}
