package com.example.myapplication.presentation.recipedetail;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeIngredient;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;
import com.example.myapplication.presentation.main.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RecipeDetailUiTest {
    @Test public void discoveryDetailAdjustsAppliesAndRetainsServingsOnRecreation() {
        FakeCatalog catalog = new FakeCatalog();
        catalog.loaded = true;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            installModel(scenario, catalog);
            onView(withText("Món canh")).perform(click());
            onView(withText("Canh chua cá")).perform(click());
            onView(withText("2 người")).check(matches(isDisplayed())).perform(click());
            onView(withContentDescription("Tăng khẩu phần")).perform(click());
            onView(withContentDescription("Tăng khẩu phần")).perform(click());
            onView(withText("300 g → 600 g")).check(matches(isDisplayed()));
            onView(withText("2 quả → 4 quả")).check(matches(isDisplayed()));
            onView(withText("Áp dụng khẩu phần")).perform(click());
            scenario.recreate();
            onView(withText("4 người")).check(matches(isDisplayed()));
            onView(withText("600 g")).check(matches(isDisplayed()));
            onView(withText("4 người")).perform(click());
            onView(withContentDescription("Giảm khẩu phần")).perform(click());
            onView(withContentDescription("Giảm khẩu phần")).perform(click());
            onView(withText("Áp dụng khẩu phần")).perform(click());
            onView(withText("300 g")).check(matches(isDisplayed()));
            assertEquals("300", catalog.recipe.details.ingredientLines.get(0).amountDecimal);
            pressBack();
            onView(withText("Món canh")).check(matches(isDisplayed()));
        }
    }

    @Test public void firstDownloadShowsFailureThenRetryLoadsAndBackKeepsPreviewRoute() {
        FakeCatalog catalog = new FakeCatalog();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            installModel(scenario, catalog);
            onView(withText("Món canh")).perform(click());
            onView(withText("Canh chua cá")).perform(click());
            onView(withText("Đang chuẩn bị kho món")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> catalog.response.completeExceptionally(new IOException("offline")));
            onView(withText("Chưa có công thức đã tải")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> catalog.response = new CompletableFuture<>());
            onView(withText("Thử lại")).perform(click());
            onView(withText("Đang chuẩn bị kho món")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                catalog.loaded = true;
                catalog.response.complete(ApplyResult.APPLIED);
            });
            onView(withText("300 g")).check(matches(isDisplayed()));
            pressBack();
            onView(withText("Món canh")).check(matches(isDisplayed()));
            onView(withText("Canh chua cá lóc")).perform(click());
            onView(withText("Canh chua cá lóc")).check(matches(isDisplayed()));
            onView(withText("📝 Ghi chú")).check(matches(isDisplayed()));
        }
    }

    private static void installModel(ActivityScenario<MainActivity> scenario, FakeCatalog catalog) {
        scenario.onActivity(activity -> new ViewModelProvider(activity, new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new RecipeDetailViewModel(catalog, () -> catalog.response,
                        Runnable::run, Runnable::run));
            }
        }).get(RecipeDetailViewModel.class));
    }

    private static final class FakeCatalog implements RecipeCatalogRepository {
        boolean loaded;
        CompletableFuture<ApplyResult> response = new CompletableFuture<>();
        final Recipe recipe = demo();

        @Override public Catalog readCatalog() {
            return new Catalog(loaded ? 1L : null, loaded ? Collections.singletonList(recipe) : Collections.emptyList(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        @Override public Lookup findById(String id) {
            return loaded && recipe.id.equals(id) ? Lookup.available(recipe) : Lookup.missing();
        }
    }

    private static Recipe demo() {
        String id = "recipe-0001";
        List<RecipeIngredient> lines = Arrays.asList(
                new RecipeIngredient("fish", id, "fish", "300", "G", null, 1),
                new RecipeIngredient("tomato", id, "tomato", "2", "QUA", null, 2),
                new RecipeIngredient("herbs", id, "herbs", null, null, "Vừa đủ", 3));
        Recipe.Details details = new Recipe.Details("Dữ liệu kiểm thử", null, 2, 10, 20, 30,
                Recipe.PublicationStatus.PUBLISHED, null, null, Recipe.IngredientDataStatus.COMPLETE,
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList("Nồi"), null,
                lines, Collections.singletonList(new CookingStep("step", id, "Chuẩn bị", "Rửa cá.", 1,
                        null, null, Collections.singletonList("fish"))));
        return Recipe.fromCatalog(id, "Canh chua cá", "", "Dễ", null, Collections.emptyList(),
                Arrays.asList("Cá", "Cà chua", "Rau thơm"), details);
    }
}
