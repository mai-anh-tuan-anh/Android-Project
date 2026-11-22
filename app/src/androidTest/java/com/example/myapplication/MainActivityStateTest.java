package com.example.myapplication;

import com.example.myapplication.presentation.main.MainActivity;
import com.example.myapplication.presentation.mealplan.MealPlanViewModel;
import com.example.myapplication.presentation.mealplan.MealPlanFragment;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.PlannedRecipe;
import com.example.myapplication.domain.model.PantryIngredient;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import com.example.myapplication.presentation.pantry.PantryFragment;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;
import com.example.myapplication.presentation.personaldata.PersonalDataFragment;
import com.example.myapplication.presentation.cookingjournal.CookingJournalViewModel;
import com.example.myapplication.domain.model.CookingLogEntry;
import com.example.myapplication.presentation.shopping.ShoppingViewModel;
import com.example.myapplication.domain.model.ShoppingListItem;
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.catalog.ShoppingCategoryCatalog;



import com.example.myapplication.domain.model.*;
import com.example.myapplication.domain.model.Recipe;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.lifecycle.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainActivityStateTest {
    @Test
    public void mealPlanPickerDraftSurvivesActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "screen", MainActivity.Screen.MEAL_PLAN_PICKER);
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) getField(viewModel, "selectedRecipeIds");
                    selected.clear();
                    selected.addAll(Arrays.asList("sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo"));
                    setField(viewModel, "pickerServings", 4);
                    setField(viewModel, "searchQuery", "com");
                    viewModel.setPickerSource(MealPlanViewModel.PickerSource.COLLECTION, 24L);
                    Method render = MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                    assertEquals("com", viewModel.getSearchQuery());
                    MealPlanFragment fragment = (MealPlanFragment) activity.getSupportFragmentManager()
                            .findFragmentByTag(MealPlanFragment.class.getName());
                    assertTrue(fragment != null && fragment.getView() != null);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });

            scenario.recreate();

            scenario.onActivity(activity -> {
                try {
                    assertEquals(MainActivity.Screen.MEAL_PLAN_PICKER, getField(activity, "screen"));
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    assertEquals(new ArrayList<>(Arrays.asList(
                            "sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo")),
                            getField(viewModel, "selectedRecipeIds"));
                    assertEquals(4, getField(viewModel, "pickerServings"));
                    assertEquals("com", getField(viewModel, "searchQuery"));
                    assertEquals(MealPlanViewModel.PickerSource.COLLECTION, viewModel.getPickerSource());
                    assertEquals(24L, viewModel.getPickerCollectionId());
                    MealPlanFragment fragment = (MealPlanFragment) activity.getSupportFragmentManager()
                            .findFragmentByTag(MealPlanFragment.class.getName());
                    assertTrue(fragment != null && fragment.getView() != null);
                    assertEquals("com", ((android.widget.EditText) getField(fragment,
                            "pickerSearch")).getText().toString());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void pickerServingsApplyToNewRecipesAndKeepExistingRecipeServings() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    setField(viewModel, "pickerServings", 7);
                    Method openPicker = MainActivity.class.getDeclaredMethod("openMealPlanPicker");
                    openPicker.setAccessible(true);
                    openPicker.invoke(activity);
                    assertEquals(1, viewModel.getPickerServings());

                    PlannedRecipe existing = new PlannedRecipe(
                            PreviewRecipeCatalog.designRecipes().get(2), 3);
                    assertEquals(2, MealPlanViewModel.servingsForSelection(2, null));
                    assertEquals(3, MealPlanViewModel.servingsForSelection(2, existing));
                    assertEquals(5, MealPlanViewModel.servingsForSelection(5, null));
                    assertEquals(3, MealPlanViewModel.servingsForSelection(5, existing));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void changingPickerSourceKeepsExistingSelections() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                MealPlanViewModel viewModel = (MealPlanViewModel) getFieldUnchecked(activity, "mealPlanViewModel");
                viewModel.toggleRecipe("favorite-recipe");
                viewModel.setPickerSource(MealPlanViewModel.PickerSource.FAVORITES, -1L);
                viewModel.setPickerSource(MealPlanViewModel.PickerSource.COLLECTION, 42L);

                assertEquals(Arrays.asList("favorite-recipe"), viewModel.getSelectedRecipeIds());
                assertEquals(MealPlanViewModel.PickerSource.COLLECTION, viewModel.getPickerSource());
                assertEquals(42L, viewModel.getPickerCollectionId());
            });
        }
    }

    @Test
    public void clearingOneJournalDateKeepsTheOtherDate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "screen", MainActivity.Screen.COOKING_JOURNAL);
                    setField(activity, "cookingJournalFromDate", 1_795_392_000_000L);
                    setField(activity, "cookingJournalToDate", 1_795_478_400_000L);
                    Method render = MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                    View clearStartDate = findViewWithDescription(activity.getWindow().getDecorView(),
                            "Xóa ngày bắt đầu");
                    if (clearStartDate == null) throw new AssertionError("Start-date clear button is missing");
                    clearStartDate.performClick();
                    assertEquals(0L, getField(activity, "cookingJournalFromDate"));
                    assertEquals(1_795_478_400_000L, getField(activity, "cookingJournalToDate"));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void leavingCookingJournalClearsBothDateBoundaries() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "screen", MainActivity.Screen.COOKING_JOURNAL);
                    setField(activity, "cookingLogReturnScreen", MainActivity.Screen.LIBRARY);
                    setField(activity, "cookingJournalFromDate", 1_795_392_000_000L);
                    setField(activity, "cookingJournalToDate", 1_795_478_400_000L);
                    Method render = MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                    activity.getOnBackPressedDispatcher().onBackPressed();
                    assertEquals(MainActivity.Screen.LIBRARY, getField(activity, "screen"));
                    assertEquals(0L, getField(activity, "cookingJournalFromDate"));
                    assertEquals(0L, getField(activity, "cookingJournalToDate"));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void categorySelectionFieldsKeepTheirDesignedTouchHeight() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    float density = activity.getResources().getDisplayMetrics().density;
                    Method openShoppingForm = MainActivity.class.getDeclaredMethod("openShoppingItemForm", String.class);
                    openShoppingForm.setAccessible(true);
                    openShoppingForm.invoke(activity, new Object[]{null});
                    View shoppingCategory = (View) getField(activity, "shoppingCategoryInput");
                    assertMinimumDpHeight(shoppingCategory, density, 56);

                    Method openPantry = MainActivity.class.getDeclaredMethod("openPantry", MainActivity.Screen.class);
                    openPantry.setAccessible(true);
                    openPantry.invoke(activity, MainActivity.Screen.SHOPPING);
                    PantryFragment pantry = (PantryFragment) activity.getSupportFragmentManager()
                            .findFragmentByTag(PantryFragment.class.getName());
                    Method openPantryForm = PantryFragment.class.getDeclaredMethod("openForm", String.class);
                    openPantryForm.setAccessible(true);
                    openPantryForm.invoke(pantry, new Object[]{null});
                    View pantryCategory = (View) getField(pantry, "categoryInput");
                    assertMinimumDpHeight(pantryCategory, density, 52);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void personalLibraryAndFavoritesAreHostedByTheFeatureFragment() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.onPersonalNavigateTab(3);
                PersonalDataFragment fragment = (PersonalDataFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(PersonalDataFragment.class.getName());
                if (fragment == null || fragment.getView() == null) {
                    throw new AssertionError("Personal data feature fragment is not attached");
                }
                assertTrue(containsText(fragment.getView(), "Kho của tôi"));
                TextView favoritesLink = findTextView(fragment.getView(), "Xem tất cả ›");
                if (favoritesLink == null) throw new AssertionError("Favorites link is missing");
                favoritesLink.performClick();
                assertEquals(MainActivity.Screen.FAVORITES, getFieldUnchecked(activity, "screen"));
                assertTrue(containsText(fragment.getView(), "Những công thức bạn đã lưu."));
                activity.onPersonalOpenCollections();
                assertEquals(MainActivity.Screen.COLLECTION_LIST, getFieldUnchecked(activity, "screen"));
                assertTrue(containsText(fragment.getView(), "Các bộ sưu tập bạn đã tạo."));
                activity.onPersonalCreateCollection();
                assertEquals(MainActivity.Screen.COLLECTION_FORM, getFieldUnchecked(activity, "screen"));
                assertTrue(containsText(fragment.getView(), "Chọn món để thêm"));
                activity.onPersonalOpenNote(com.example.myapplication.domain.catalog.PreviewRecipeCatalog
                        .designRecipes().get(0));
                assertEquals(MainActivity.Screen.NOTE, getFieldUnchecked(activity, "screen"));
                assertTrue(containsText(fragment.getView(), "Ghi chú sau khi nấu"));
            });
        }
    }

    @Test
    public void shoppingItemFormDoesNotAskUserToChooseStatus() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    Method openShoppingForm = MainActivity.class.getDeclaredMethod("openShoppingItemForm", String.class);
                    openShoppingForm.setAccessible(true);
                    openShoppingForm.invoke(activity, new Object[]{null});
                    View root = activity.getWindow().getDecorView();
                    assertFalse(containsText(root, "Trạng thái"));
                    assertFalse(containsText(root, "Cần mua"));
                    assertFalse(containsText(root, "Đã có"));
                    if (!containsText(root, "Lưu nguyên liệu")) {
                        throw new AssertionError("Shopping item save action is missing");
                    }
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void shoppingDataLoadsAndStatusChangesThroughAsyncViewModelState() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<ShoppingViewModel> viewModelRef = new AtomicReference<>();
            CountDownLatch initialLoad = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                ShoppingViewModel viewModel = (ShoppingViewModel) getFieldUnchecked(activity,
                        "shoppingViewModel");
                viewModelRef.set(viewModel);
                setFieldUnchecked(activity, "screen", MainActivity.Screen.SHOPPING);
                viewModel.shoppingState().observe(activity, state -> {
                    if (state != null && !state.loading) initialLoad.countDown();
                });
                viewModel.refreshItems();
            });
            if (!initialLoad.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Shopping list did not load asynchronously");
            }

            ShoppingViewModel viewModel = viewModelRef.get();
            String itemName = "Kiểm tra bất đồng bộ " + System.nanoTime();
            AtomicReference<String> savedItemId = new AtomicReference<>();
            CountDownLatch saved = new CountDownLatch(1);
            CountDownLatch deleted = new CountDownLatch(1);
            AtomicBoolean reloadStarted = new AtomicBoolean(false);
            Observer<ShoppingViewModel.SaveResult> saveObserver = result -> {
                if (result != null && result.success) saved.countDown();
            };
            scenario.onActivity(activity -> {
                viewModel.clearSaveResult();
                viewModel.saveResult().observe(activity, saveObserver);
                assertEquals(true, viewModel.saveItem(null, itemName, "1", "cái",
                        ShoppingCategoryCatalog.OTHER, ShoppingViewModel.NEED_TO_BUY));
            });
            if (!saved.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Shopping item save did not complete asynchronously");
            }

            scenario.onActivity(activity -> {
                for (ShoppingListItem item : viewModel.getItems()) {
                    if (itemName.equals(item.name)) savedItemId.set(item.id);
                }
                assertEquals(true, savedItemId.get() != null);
                String itemId = savedItemId.get();
                viewModel.setStatus(itemId, ShoppingViewModel.AVAILABLE);
                ShoppingListItem updated = viewModel.findItem(itemId);
                assertEquals(ShoppingViewModel.AVAILABLE, updated.status);
                viewModel.shoppingState().observe(activity, state -> {
                    if (state != null && state.loading) reloadStarted.set(true);
                    if (state != null && !state.loading && reloadStarted.get()) {
                        boolean remains = false;
                        for (ShoppingListItem item : state.items) remains |= itemId.equals(item.id);
                        if (!remains) deleted.countDown();
                    }
                });
                viewModel.deleteItem(itemId);
                viewModel.refreshItems();
                viewModel.saveResult().removeObserver(saveObserver);
            });
            if (!deleted.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Shopping item delete did not complete asynchronously");
            }
        }
    }

    @Test
    public void mealPlanLoadsAndPersistsPickerSelectionWithoutBlockingUi() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<MealPlanViewModel> viewModelRef = new AtomicReference<>();
            CountDownLatch loaded = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                MealPlanViewModel viewModel = (MealPlanViewModel) getFieldUnchecked(activity,
                        "mealPlanViewModel");
                viewModelRef.set(viewModel);
                viewModel.planState().observe(activity, state -> {
                    if (state != null && !state.loading) loaded.countDown();
                });
                viewModel.refresh();
            });
            if (!loaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("Meal plan did not load");

            MealPlanViewModel viewModel = viewModelRef.get();
            Recipe recipe = new Recipe("async-plan-" + System.nanoTime(), "Món kiểm tra async", "", "10 phút", "Dễ");
            CountDownLatch persisted = new CountDownLatch(1);
            AtomicBoolean refreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.beginPicker();
                viewModel.setPickerServings(2);
                viewModel.toggleRecipe(recipe.id);
                assertEquals(true, viewModel.saveSelection(Arrays.asList(recipe)));
                List<PlannedRecipe> optimistic = viewModel.getMealPlanRecipes();
                PlannedRecipe selected = null;
                for (PlannedRecipe item : optimistic) if (recipe.id.equals(item.recipe.id)) selected = item;
                assertEquals(2, selected == null ? 0 : selected.servings);
                viewModel.planState().observe(activity, state -> {
                    if (state != null && state.loading) refreshStarted.set(true);
                    if (state != null && !state.loading && refreshStarted.get()) {
                        for (PlannedRecipe item : state.recipes) {
                            if (recipe.id.equals(item.recipe.id) && item.servings == 2) persisted.countDown();
                        }
                    }
                });
                viewModel.refresh();
            });
            if (!persisted.await(5, TimeUnit.SECONDS)) throw new AssertionError("Meal plan was not persisted");
            CountDownLatch removed = new CountDownLatch(1);
            AtomicBoolean removeRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.removeMealPlanRecipe(recipe.id);
                viewModel.planState().observe(activity, state -> {
                    if (state != null && state.loading) removeRefreshStarted.set(true);
                    if (state != null && !state.loading && removeRefreshStarted.get()) {
                        boolean found = false;
                        for (PlannedRecipe item : state.recipes) found |= recipe.id.equals(item.recipe.id);
                        if (!found) removed.countDown();
                    }
                });
                viewModel.refresh();
            });
            if (!removed.await(5, TimeUnit.SECONDS)) throw new AssertionError("Meal plan removal was not persisted");
        }
    }

    @Test
    public void pantryItemsLoadSaveAndDeleteThroughAsyncState() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<PantryViewModel> viewModelRef = new AtomicReference<>();
            CountDownLatch loaded = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                PantryViewModel viewModel = (PantryViewModel) getFieldUnchecked(activity, "pantryViewModel");
                viewModelRef.set(viewModel);
                viewModel.pantryState().observe(activity, state -> {
                    if (state != null && !state.loading) loaded.countDown();
                });
                viewModel.refreshItems();
            });
            if (!loaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("Pantry items did not load");

            PantryViewModel viewModel = viewModelRef.get();
            String name = "Nguyên liệu kiểm tra async " + System.nanoTime();
            CountDownLatch saved = new CountDownLatch(1);
            CountDownLatch deleted = new CountDownLatch(1);
            AtomicReference<String> idRef = new AtomicReference<>();
            AtomicBoolean deletionRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.pantryState().observe(activity, state -> {
                    if (state == null || state.loading) return;
                    for (PantryIngredient item : state.items) {
                        if (name.equals(item.name)) saved.countDown();
                    }
                });
                assertEquals(true, viewModel.saveItem(null, name, "2", "PCS",
                        ShoppingCategoryCatalog.OTHER, "", "", ""));
            });
            if (!saved.await(5, TimeUnit.SECONDS)) throw new AssertionError("Pantry item save did not complete");
            scenario.onActivity(activity -> {
                for (PantryIngredient item : viewModel.getItems()) if (name.equals(item.name)) idRef.set(item.id);
                assertEquals(true, idRef.get() != null);
                viewModel.pantryState().observe(activity, state -> {
                    if (state != null && state.loading) deletionRefreshStarted.set(true);
                    if (state != null && !state.loading && deletionRefreshStarted.get()) {
                        boolean found = false;
                        for (PantryIngredient item : state.items) found |= idRef.get().equals(item.id);
                        if (!found) deleted.countDown();
                    }
                });
                viewModel.deleteItem(idRef.get());
                viewModel.refreshItems();
            });
            if (!deleted.await(5, TimeUnit.SECONDS)) throw new AssertionError("Pantry item delete did not complete");
        }
    }

    @Test
    public void personalFavoritesAndHistoryUseCachedLiveDataAndAsyncPersistence() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<PersonalDataViewModel> viewModelRef = new AtomicReference<>();
            CountDownLatch loaded = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                PersonalDataViewModel viewModel = (PersonalDataViewModel) getFieldUnchecked(activity,
                        "personalDataViewModel");
                viewModelRef.set(viewModel);
                viewModel.personalDataState().observe(activity, state -> {
                    if (state != null && !state.loading) loaded.countDown();
                });
            });
            if (!loaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("Personal data did not load");

            PersonalDataViewModel viewModel = viewModelRef.get();
            Recipe recipe = new Recipe("async-personal-" + System.nanoTime(), "Món cá nhân kiểm tra", "", "15 phút", "Dễ");
            CountDownLatch favoriteReloaded = new CountDownLatch(1);
            AtomicBoolean favoriteRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.addFavorite(recipe);
                assertEquals(true, viewModel.isFavorite(recipe.id));
                viewModel.personalDataState().observe(activity, state -> {
                    if (state != null && state.loading) favoriteRefreshStarted.set(true);
                    if (state != null && !state.loading && favoriteRefreshStarted.get()) {
                        for (Recipe favorite : state.favorites) {
                            if (recipe.id.equals(favorite.id)) favoriteReloaded.countDown();
                        }
                    }
                });
                viewModel.refresh(Collections.singletonList(recipe));
            });
            if (!favoriteReloaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("Favorite was not persisted");

            CountDownLatch historyReloaded = new CountDownLatch(1);
            AtomicBoolean historyRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.recordViewedRecipe(recipe);
                viewModel.personalDataState().observe(activity, state -> {
                    if (state != null && state.loading) historyRefreshStarted.set(true);
                    if (state != null && !state.loading && historyRefreshStarted.get()) {
                        for (Recipe viewed : state.history) {
                            if (recipe.id.equals(viewed.id)) historyReloaded.countDown();
                        }
                    }
                });
                viewModel.refresh(Collections.singletonList(recipe));
            });
            if (!historyReloaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("View history was not persisted");

            CountDownLatch removed = new CountDownLatch(1);
            AtomicBoolean removeRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.removeFavorite(recipe.id);
                viewModel.personalDataState().observe(activity, state -> {
                    if (state != null && state.loading) removeRefreshStarted.set(true);
                    if (state != null && !state.loading && removeRefreshStarted.get()) {
                        boolean found = false;
                        for (Recipe favorite : state.favorites) found |= recipe.id.equals(favorite.id);
                        if (!found) removed.countDown();
                    }
                });
                viewModel.refresh(Collections.singletonList(recipe));
            });
            if (!removed.await(5, TimeUnit.SECONDS)) throw new AssertionError("Favorite removal was not persisted");
        }
    }

    @Test
    public void cookingJournalLoadsSavesAndDeletesThroughAsyncState() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<CookingJournalViewModel> viewModelRef = new AtomicReference<>();
            CountDownLatch loaded = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                CookingJournalViewModel viewModel = (CookingJournalViewModel) getFieldUnchecked(activity,
                        "cookingJournalViewModel");
                viewModelRef.set(viewModel);
                viewModel.journalState().observe(activity, state -> {
                    if (state != null && !state.loading) loaded.countDown();
                });
                viewModel.refresh();
            });
            if (!loaded.await(5, TimeUnit.SECONDS)) throw new AssertionError("Cooking journal did not load");

            CookingJournalViewModel viewModel = viewModelRef.get();
            Recipe recipe = new Recipe("async-journal-" + System.nanoTime(), "Món nhật ký kiểm tra", "", "20 phút", "Dễ");
            CountDownLatch saved = new CountDownLatch(1);
            AtomicReference<String> logIdRef = new AtomicReference<>();
            scenario.onActivity(activity -> {
                viewModel.journalState().observe(activity, state -> {
                    if (state != null && !state.loading) {
                        for (CookingLogEntry log : state.logs) {
                            if (recipe.id.equals(log.recipeId)) {
                                logIdRef.set(log.id);
                                saved.countDown();
                            }
                        }
                    }
                });
                assertEquals(true, viewModel.save(null, recipe, System.currentTimeMillis(), 1,
                        20, 5, "Thêm rau thơm", ""));
            });
            if (!saved.await(5, TimeUnit.SECONDS)) throw new AssertionError("Cooking log save did not complete");

            CountDownLatch deleted = new CountDownLatch(1);
            AtomicBoolean deleteRefreshStarted = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                viewModel.journalState().observe(activity, state -> {
                    if (state != null && state.loading) deleteRefreshStarted.set(true);
                    if (state != null && !state.loading && deleteRefreshStarted.get()) {
                        boolean found = false;
                        for (CookingLogEntry log : state.logs) found |= logIdRef.get().equals(log.id);
                        if (!found) deleted.countDown();
                    }
                });
                viewModel.delete(logIdRef.get());
                viewModel.refresh();
            });
            if (!deleted.await(5, TimeUnit.SECONDS)) throw new AssertionError("Cooking log delete did not complete");
        }
    }

    private static boolean containsText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            if (containsText(group.getChildAt(index), expected)) return true;
        }
        return false;
    }

    private static TextView findTextView(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            TextView found = findTextView(group.getChildAt(index), expected);
            if (found != null) return found;
        }
        return null;
    }

    private static void assertMinimumDpHeight(View view, float density, int expectedDp) {
        int expectedPx = Math.round(expectedDp * density);
        int actualPx = ((TextView) view).getMinHeight();
        if (actualPx < expectedPx) {
            throw new AssertionError("Expected minimum height >= " + expectedDp + "dp, was "
                    + actualPx / density + "dp");
        }
    }

    private static View findViewWithDescription(View view, String description) {
        if (description.equals(view.getContentDescription())) return view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            View match = findViewWithDescription(group.getChildAt(index), description);
            if (match != null) return match;
        }
        return null;
    }

    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object getFieldUnchecked(Object target, String name) {
        try {
            return getField(target, name);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setFieldUnchecked(Object target, String name, Object value) {
        try {
            setField(target, name, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
