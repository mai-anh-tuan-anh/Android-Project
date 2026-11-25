package com.example.myapplication.presentation.cooking;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.database.PersonalDataDbHelper;
import com.example.myapplication.data.local.database.RecipeCatalogDbHelper;
import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.repository.LocalPersonalDataRepository;
import com.example.myapplication.data.repository.LocalRecipeCatalogRepository;
import com.example.myapplication.di.AppContainer;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.model.CookingSession;
import com.example.myapplication.domain.model.CookingTimer;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class CookingSessionUiTest {
    private static final String RECIPE_ID = "recipe-0001";
    private static final String REMOVAL = "{\"meta\":{\"schemaVersion\":1,\"revision\":2},"
            + "\"removed\":{\"recipe-0001\":{\"removedAtMillis\":1795543600000}}}";

    @Test public void startFromDetailWithFourServingsReviewStepsAndCompleteAfterRecreation() throws Exception {
        try (Fixture fixture = new Fixture();
             ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            openDetail();
            tap("2 người");
            onView(withContentDescription("Tăng khẩu phần")).perform(click(), click());
            tap("Áp dụng khẩu phần");
            visible("4 người");
            tap("Bắt đầu nấu");
            visible("Bước 1 / 4");
            visible("Cá · 600 g");
            onView(withText("Bước trước")).check(matches(not(isEnabled())));
            capture("01-step-1-four-servings");

            tap("Hoàn thành bước");
            visible("Bước 2 / 4");
            visible("Cà chua · 4 quả");
            visible("Rau thơm · Vừa đủ");
            visible("Hẹn giờ 5 phút");
            onView(withText("Me · 40 g")).check(doesNotExist());
            CookingSession.Snapshot started = session(activity, fixture).snapshot;
            assertEquals(RECIPE_ID, started.recipeId);
            assertEquals(4, started.servings);
            assertEquals(300, session(activity, fixture).timerInput().durationSeconds);
            capture("02-step-2-ingredients");

            pressBack();
            visible("Bước tiếp theo");
            visible("Đã hoàn thành 1 trong 4 bước");
            capture("03-paused");
            onView(allOf(withText("Tiếp tục nấu"), isClickable())).perform(click());
            activity.recreate();
            visible("Bước 2 / 4");
            assertSame(started, session(activity, fixture).snapshot);

            tap("Bước trước");
            visible("Bước 1 / 4");
            visible("Đã hoàn thành 1 trong 4 bước");
            tap("Hoàn thành bước");
            tap("Hoàn thành bước");
            visible("Bước 3 / 4");
            visible("Me · 40 g");
            visible("Nước · Vừa đủ");
            tap("Hoàn thành bước");
            visible("Bước 4 / 4");
            tap("Bước trước");
            visible("Bước 3 / 4");
            tap("Hoàn thành bước");
            tap("Hoàn thành món");
            visible("Món đã sẵn sàng!");
            visible("Đã hoàn thành 4 trong 4 bước");
            CookingSession.Completion completed = session(activity, fixture).completion();
            assertEquals(started.sessionId, completed.sessionId);
            assertEquals(4, completed.servings);
            capture("04-completed");

            tap("Xem lại công thức");
            visible("4 người");
            visible("600 g");
            fixture.verifyPersonalData();
        }
    }

    @Test public void fiveMinuteTimerStartsPausesResumesAndResetsAcrossRecreationAndBackground() throws Exception {
        try (Fixture fixture = new Fixture();
             ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            openDetail();
            tap("Bắt đầu nấu");
            visible("Bước 1 / 4");
            onView(withText("Hẹn giờ 5 phút")).check(doesNotExist());
            tap("Hoàn thành bước");
            tap("Hẹn giờ 5 phút");
            visible("05:00");
            captureTimer("01-ready");
            tap("Bắt đầu");
            visible("Tạm dừng");
            SystemClock.sleep(1150);
            assertTrue(remaining(activity, fixture) < 299_000);
            captureTimer("02-running");
            tap("Tạm dừng");
            visible("Đang tạm dừng");
            long paused = remaining(activity, fixture);
            SystemClock.sleep(1050);
            assertEquals(paused, remaining(activity, fixture));
            captureTimer("03-paused");
            tap("Tiếp tục");
            long deadline = currentTimer(activity, fixture).deadlineElapsedMillis;
            activity.recreate();
            visible("Hẹn giờ nấu");
            visible("Tạm dừng");
            assertEquals(deadline, currentTimer(activity, fixture).deadlineElapsedMillis);
            long beforeBackground = remaining(activity, fixture);
            homeAndReopen(1250);
            awaitWindowFocus(activity);
            assertEquals(deadline, currentTimer(activity, fixture).deadlineElapsedMillis);
            long afterBackground = remaining(activity, fixture);
            assertTrue(beforeBackground - afterBackground >= 1250);
            assertTrue(Math.abs(afterBackground - (deadline - SystemClock.elapsedRealtime())) < 500);
            onView(withText("Đặt lại")).perform(scrollTo(), click());
            assertEquals(CookingTimer.Status.READY, currentTimer(activity, fixture).status);
            visible("05:00");
            visible("Bắt đầu");
            tap("Hủy hẹn giờ");
            visible("Bước 2 / 4");
        }
    }

    @Test public void elapsedTimerFinishesInBackgroundAndReturnsToTheSameStep() throws Exception {
        try (Fixture fixture = new Fixture(3);
             ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            openDetail();
            tap("Bắt đầu nấu");
            tap("Hoàn thành bước");
            tap("Hẹn giờ 3 giây");
            visible("00:03");
            tap("Bắt đầu");
            homeAndReopen(3250);
            awaitWindowFocus(activity);
            visible("00:00");
            visible("Đã hết giờ");
            assertEquals(CookingTimer.Status.FINISHED, currentTimer(activity, fixture).status);
            tap("Về bước nấu");
            visible("Bước 2 / 4");
            tap("Hẹn giờ 3 giây");
            visible("00:00");
            captureTimer("04-finished");
            tap("Đặt lại");
            visible("00:03");
        }
    }

    @Test public void removalDuringCookingKeepsCurrentSnapshotThenCancelReturnsToRemovedDetail() throws Exception {
        try (Fixture fixture = new Fixture();
             ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            openDetail();
            tap("Bắt đầu nấu");
            visible("Bước 1 / 4");
            CookingSession.Snapshot started = session(activity, fixture).snapshot;
            fixture.nextSnapshot.set(REMOVAL);
            assertEquals(ApplyResult.APPLIED, fixture.graph.catalogSync.refresh().get(8, TimeUnit.SECONDS));
            tap("Hoàn thành bước");
            visible("Bước 2 / 4");
            assertSame(started, session(activity, fixture).snapshot);
            tap("Tạm dừng");
            tap("Kết thúc phiên nấu");
            visible("Kết thúc phiên nấu?");
            capture("05-cancel-confirmation");
            tap("Kết thúc");
            visible("Công thức đã được gỡ");
            onView(withText("Bắt đầu nấu")).check(doesNotExist());
            fixture.verifyPersonalData();
        }
    }

    private static void openDetail() {
        visible("Món canh");
        tap("Món canh");
        tap("Canh chua cá");
        visible("2 người");
    }

    private static void tap(String text) {
        visible(text);
        onView(withText(text)).perform(click());
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

    private static void awaitWindowFocus(ActivityScenario<MainActivity> activity) {
        long limit = SystemClock.elapsedRealtime() + 5000;
        AtomicBoolean focused = new AtomicBoolean();
        do {
            activity.onActivity(host -> focused.set(host.getWindow().getDecorView().hasWindowFocus()));
            if (focused.get()) return;
            SystemClock.sleep(25);
        } while (SystemClock.elapsedRealtime() < limit);
        fail("Cooking window did not regain input focus");
    }

    private static void homeAndReopen(long waitMillis) throws IOException {
        runShell("input keyevent KEYCODE_HOME");
        SystemClock.sleep(waitMillis);
        String result = runShell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "
                + "-n com.example.myapplication/.presentation.main.MainActivity");
        assertTrue(result, result.contains("Status: ok"));
    }

    private static String runShell(String command) throws IOException {
        ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand(command);
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static CookingSession session(ActivityScenario<MainActivity> activity, Fixture fixture) {
        AtomicReference<CookingSession> current = new AtomicReference<>();
        activity.onActivity(host -> current.set(model(host, fixture).state().getValue().session));
        assertNotNull(current.get());
        return current.get();
    }

    private static CookingTimer currentTimer(ActivityScenario<MainActivity> activity, Fixture fixture) {
        AtomicReference<CookingTimer> timer = new AtomicReference<>();
        activity.onActivity(host -> timer.set(model(host, fixture).currentTimer()));
        return timer.get();
    }

    private static long remaining(ActivityScenario<MainActivity> activity, Fixture fixture) {
        AtomicLong remaining = new AtomicLong();
        activity.onActivity(host -> {
            CookingSessionViewModel model = model(host, fixture);
            remaining.set(model.remainingMillis(model.currentTimer()));
        });
        return remaining.get();
    }

    private static CookingSessionViewModel model(MainActivity host, Fixture fixture) {
        return new ViewModelProvider(host, new AppViewModelFactory(fixture.app)).get(CookingSessionViewModel.class);
    }

    private static void captureTimer(String name) throws IOException {
        capture("timerCapture", "n2-11-", name);
    }

    private static void capture(String name) throws IOException {
        capture("cookingCapture", "n2-10-", name);
    }

    private static void capture(String argument, String prefix, String name) throws IOException {
        if (!"1".equals(InstrumentationRegistry.getArguments().getString(argument))) return;
        OfflineTestApplication app = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(160);
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream stream = new FileOutputStream(new File(app.getCacheDir(), prefix + name + ".png"))) {
            if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException(name);
        } finally {
            screenshot.recycle();
        }
    }

    private static String approvedFixture() throws IOException {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                .open("catalog/cooking-session-v1.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final OfflineTestApplication app = ApplicationProvider.getApplicationContext();
        final String namespace = "n210-" + UUID.randomUUID();
        final RecipeCatalogDbHelper catalog = new RecipeCatalogDbHelper(app, namespace + "-catalog.db");
        final PersonalDataDbHelper personal = new PersonalDataDbHelper(app, namespace + "-personal.db");
        final AtomicReference<String> nextSnapshot = new AtomicReference<>();
        final AppContainer graph;

        Fixture() throws IOException { this(300); }

        Fixture(int timerDuration) throws IOException {
            LocalRecipeCatalogRepository local = new LocalRecipeCatalogRepository(catalog);
            String json = approvedFixture();
            if (timerDuration != 300) json = json.replace("\"durationSeconds\": 300",
                    "\"durationSeconds\": " + timerDuration);
            local.applySnapshot(json);
            nextSnapshot.set(json);
            Recipe saved = local.findById(RECIPE_ID).recipe;
            personal.addFavorite(saved);
            personal.saveNote(saved, "Ghi chú có sẵn");
            personal.addMealPlanRecipe(saved, 6);
            assertTrue(personal.saveCookingLog(null, saved, 1795543600000L, 6, 25, 5, "Nhật ký có sẵn", null));
            graph = new AppContainer(app, nextSnapshot::get, uri -> { throw new IOException("fixture offline"); },
                    new File(app.getCacheDir(), namespace), local, new LocalPersonalDataRepository(app, personal),
                    new MutableLiveData<>(false), namespace);
            app.injectContainer(graph);
        }

        void verifyPersonalData() {
            assertTrue(personal.isFavorite(RECIPE_ID));
            assertEquals("Ghi chú có sẵn", personal.getNote(RECIPE_ID));
            assertEquals(1, personal.getCookingLogs(RECIPE_ID).size());
            assertEquals("Nhật ký có sẵn", personal.getCookingLogs(RECIPE_ID).get(0).adjustmentNote);
            assertEquals(6, personal.getMealPlanRecipes().stream()
                    .filter(planned -> RECIPE_ID.equals(planned.recipe.id)).findFirst().get().servings);
        }

        @Override public void close() throws InterruptedException {
            app.injectContainer(null);
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
