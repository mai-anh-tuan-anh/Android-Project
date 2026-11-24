package com.example.myapplication.presentation.main;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.example.myapplication.presentation.cookingjournal.CookingJournalFragment;
import com.example.myapplication.presentation.discover.DiscoverFragment;
import com.example.myapplication.presentation.discover.DiscoverSearchViewModel;
import com.example.myapplication.presentation.mealplan.MealPlanFragment;
import com.example.myapplication.presentation.mealplan.MealPlanViewModel;
import com.example.myapplication.presentation.pantry.PantryFragment;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import com.example.myapplication.presentation.personaldata.PersonalDataFragment;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;
import com.example.myapplication.presentation.shopping.ShoppingFragment;
import com.example.myapplication.presentation.shopping.ShoppingViewModel;

import java.util.ArrayList;
import java.util.List;

/** Local personal data, meal planning, shopping, pantry, and cooking journal. */
public class MainActivity extends AppCompatActivity
        implements PantryFragment.Host,
                ShoppingFragment.Host,
                MealPlanFragment.Host,
                PersonalDataFragment.Host,
                CookingJournalFragment.Host,
                DiscoverFragment.Host {
    private static final String STATE_APP_SCREEN = "app.currentScreen";
    private static final String STATE_JOURNAL_RETURN_SCREEN = "journal.returnScreen";
    private static final String STATE_JOURNAL_RECIPE_ID = "journal.recipeId";
    private static final String STATE_JOURNAL_LOG_ID = "journal.logId";
    private static final String STATE_JOURNAL_FORM_RETURN = "journal.formReturn";
    private static final String STATE_SELECTED_CATEGORY = "discover.selectedCategory";
    private static final String STATE_DISCOVER_SEARCH_QUERY = "discover.searchQuery";
    private static final String STATE_DETAIL_RECIPE_ID = "discover.detailRecipeId";
    private static final String STATE_DETAIL_RETURN_SCREEN = "discover.detailReturnScreen";
    private static final String STATE_NOTE_RETURN_SCREEN = "personal.noteReturnScreen";
    private static final String STATE_COLLECTION_RETURN_SCREEN = "personal.collectionReturnScreen";
    private static final String STATE_COLLECTION_FORM_RETURN_SCREEN = "personal.collectionFormReturnScreen";
    private static final String STATE_COLLECTION_BACK_STACK = "personal.collectionBackStack";
    private static final String STATE_SELECTED_COLLECTION_ID = "personal.collectionId";
    private static final String STATE_NOTE_RECIPE_ID = "personal.noteRecipeId";
    private static final String STATE_SCREEN = "module6.screen";
    private static final String STATE_PICKED_RECIPE_IDS = "module6.pickedRecipeIds";
    private static final String STATE_PICKER_SERVINGS = "module6.pickerServings";
    private static final String STATE_SEARCH_QUERY = "module6.searchQuery";
    private static final String STATE_PICKER_SOURCE = "module6.pickerSource";
    private static final String STATE_PICKER_COLLECTION_ID = "module6.pickerCollectionId";
    private static final String STATE_SHOPPING_SCREEN = "module7.screen";
    private static final String STATE_SHOPPING_ITEM_ID = "module7.itemId";
    private static final String STATE_SHOPPING_FILTER = "module7.filter";
    private static final String STATE_SHOPPING_RETURN_SCREEN = "module7.returnScreen";

    private PersonalDataViewModel personalDataViewModel;
    private MealPlanViewModel mealPlanViewModel;
    private ShoppingViewModel shoppingViewModel;
    private PantryViewModel pantryViewModel;
    private DiscoverSearchViewModel searchViewModel;
    private FrameLayout mainContent;
    private FrameLayout fragmentContainer;
    private Screen cookingLogReturnScreen = Screen.NOTE;
    private Screen cookingLogFormReturnScreen = Screen.COOKING_LOGS;
    private String cookingLogRecipeId;
    private String editingCookingLogId;
    private Screen screen = Screen.DISCOVER;
    private boolean renderPending;
    private Screen noteReturnScreen = Screen.LIBRARY;
    private Screen collectionReturnScreen = Screen.LIBRARY;
    private Screen collectionFormReturnScreen = Screen.LIBRARY;
    private final ArrayList<Long> collectionBackStack = new ArrayList<>();
    private Screen detailReturnScreen = Screen.DISCOVER;
    private String detailRecipeId = "";
    private String selectedCategory = "";
    private String discoverSearchQuery = "";
    private long selectedCollectionId = -1L;
    private String noteRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private Screen shoppingReturnScreen = Screen.DISCOVER;
    private String editingShoppingItemId;
    private String shoppingStatusFilter;
    private Screen pantryReturnScreen = Screen.SHOPPING;

    public enum Screen {
        DISCOVER,
        CATEGORY_LIST,
        CATEGORY_RECIPES,
        NEW_RECIPES,
        SEARCH,
        SEARCH_FILTER,
        SEARCH_RESULTS,
        WHAT_TO_COOK_INGREDIENTS,
        WHAT_TO_COOK_RESULTS,
        RECIPE_DETAIL,
        LIBRARY,
        FAVORITES,
        COLLECTION_LIST,
        COLLECTION,
        COLLECTION_FORM,
        NOTE,
        MEAL_PLAN,
        MEAL_PLAN_PICKER,
        SHOPPING,
        SHOPPING_ITEM_FORM,
        PANTRY,
        PANTRY_FORM,
        COOKING_JOURNAL,
        COOKING_LOGS,
        COOKING_LOG_FORM
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainContent = findViewById(R.id.main_content);
        fragmentContainer = findViewById(R.id.main_fragment_container);
        configureSystemBars();
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (screen == Screen.DISCOVER) {
                                    finish();
                                    return;
                                } else if (isCookingJournalScreen()) {
                                    CookingJournalFragment fragment =
                                            activeCookingJournalFragment();
                                    if (fragment != null) {
                                        fragment.handleBackPressed();
                                        return;
                                    }
                                } else if (isPersonalDataScreen()) {
                                    handlePersonalDataBack();
                                    return;
                                } else if (isDiscoverScreen()) {
                                    DiscoverFragment fragment = activeDiscoverFragment();
                                    if (fragment != null) {
                                        fragment.handleBackPressed();
                                        return;
                                    }
                                } else if (isPantryScreen()) {
                                    PantryFragment fragment = activePantryFragment();
                                    if (fragment != null) {
                                        fragment.handleBackPressed();
                                        return;
                                    }
                                } else if (isShoppingScreen()) {
                                    ShoppingFragment fragment = activeShoppingFragment();
                                    if (fragment != null) {
                                        fragment.handleBackPressed();
                                        return;
                                    }
                                } else if (screen == Screen.SEARCH_FILTER
                                        || screen == Screen.SEARCH_RESULTS) {
                                    screen = Screen.SEARCH;
                                } else if (screen == Screen.WHAT_TO_COOK_RESULTS) {
                                    screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                                } else if (screen == Screen.CATEGORY_RECIPES) {
                                    screen = Screen.CATEGORY_LIST;
                                } else if (screen == Screen.NEW_RECIPES
                                        || screen == Screen.CATEGORY_LIST
                                        || screen == Screen.SEARCH
                                        || screen == Screen.WHAT_TO_COOK_INGREDIENTS) {
                                    screen = Screen.DISCOVER;
                                } else if (screen == Screen.RECIPE_DETAIL) {
                                    screen =
                                            detailReturnScreen == Screen.RECIPE_DETAIL
                                                    ? Screen.DISCOVER
                                                    : detailReturnScreen;
                                } else if (screen == Screen.PANTRY_FORM) {
                                    screen = Screen.PANTRY;
                                } else if (screen == Screen.PANTRY) {
                                    screen = pantryReturnScreen;
                                } else if (screen == Screen.COLLECTION_FORM) {
                                    screen = collectionFormReturnScreen;
                                } else if (screen == Screen.COLLECTION) {
                                    screen = collectionReturnScreen;
                                } else if (screen == Screen.NOTE) {
                                    screen = noteReturnScreen;
                                } else if (screen == Screen.MEAL_PLAN_PICKER) {
                                    screen = Screen.MEAL_PLAN;
                                } else if (screen == Screen.MEAL_PLAN || screen == Screen.LIBRARY) {
                                    screen = Screen.DISCOVER;
                                } else {
                                    screen = Screen.DISCOVER;
                                }
                                render();
                            }
                        });
        AppViewModelFactory viewModelFactory =
                new AppViewModelFactory((BepNhaApplication) getApplication());
        personalDataViewModel =
                new ViewModelProvider(this, viewModelFactory).get(PersonalDataViewModel.class);
        mealPlanViewModel =
                new ViewModelProvider(this, viewModelFactory).get(MealPlanViewModel.class);
        shoppingViewModel =
                new ViewModelProvider(this, viewModelFactory).get(ShoppingViewModel.class);
        pantryViewModel = new ViewModelProvider(this, viewModelFactory).get(PantryViewModel.class);
        searchViewModel =
                new ViewModelProvider(this, viewModelFactory).get(DiscoverSearchViewModel.class);
        personalDataViewModel.seedDesignPreviewIfNeeded(PreviewRecipeCatalog.designRecipes());
        restoreAppState(savedInstanceState);
        searchViewModel.setSearchQuery(discoverSearchQuery);
        restoreMealPlanState(savedInstanceState);
        if (screen == Screen.PANTRY) pantryViewModel.refreshItems();
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER)
            mealPlanViewModel.refresh();
        if (isShoppingScreen()) shoppingViewModel.refreshItems();
        render();
    }

    private void restoreAppState(Bundle state) {
        if (state == null) return;
        selectedCategory = state.getString(STATE_SELECTED_CATEGORY, "");
        try {
            cookingLogReturnScreen =
                    Screen.valueOf(
                            state.getString(STATE_JOURNAL_RETURN_SCREEN, Screen.NOTE.name()));
        } catch (IllegalArgumentException invalid) {
            cookingLogReturnScreen = Screen.NOTE;
        }
        cookingLogRecipeId = state.getString(STATE_JOURNAL_RECIPE_ID);
        editingCookingLogId = state.getString(STATE_JOURNAL_LOG_ID);
        try {
            cookingLogFormReturnScreen =
                    Screen.valueOf(
                            state.getString(STATE_JOURNAL_FORM_RETURN, Screen.COOKING_LOGS.name()));
        } catch (IllegalArgumentException invalid) {
            cookingLogFormReturnScreen = Screen.COOKING_LOGS;
        }
        discoverSearchQuery = state.getString(STATE_DISCOVER_SEARCH_QUERY, "");
        noteReturnScreen = readScreen(state, STATE_NOTE_RETURN_SCREEN, Screen.LIBRARY);
        collectionReturnScreen = readScreen(state, STATE_COLLECTION_RETURN_SCREEN, Screen.LIBRARY);
        collectionFormReturnScreen =
                readScreen(state, STATE_COLLECTION_FORM_RETURN_SCREEN, Screen.LIBRARY);
        selectedCollectionId = state.getLong(STATE_SELECTED_COLLECTION_ID, -1L);
        noteRecipeId = state.getString(STATE_NOTE_RECIPE_ID, noteRecipeId);
        long[] savedCollectionBackStack = state.getLongArray(STATE_COLLECTION_BACK_STACK);
        collectionBackStack.clear();
        if (savedCollectionBackStack != null) {
            for (long collectionId : savedCollectionBackStack) collectionBackStack.add(collectionId);
        }
        detailRecipeId = state.getString(STATE_DETAIL_RECIPE_ID, "");
        String savedDetailReturn =
                state.getString(STATE_DETAIL_RETURN_SCREEN, Screen.DISCOVER.name());
        try {
            detailReturnScreen = Screen.valueOf(savedDetailReturn);
        } catch (IllegalArgumentException e) {
            detailReturnScreen = Screen.DISCOVER;
        }
        String savedScreen = state.getString(STATE_APP_SCREEN);
        if (savedScreen != null) {
            try {
                screen = Screen.valueOf(savedScreen);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void restoreMealPlanState(Bundle state) {
        if (state == null) return;
        String savedScreen = state.getString(STATE_SCREEN);
        String savedShoppingScreen = state.getString(STATE_SHOPPING_SCREEN);
        if (Screen.SHOPPING.name().equals(savedShoppingScreen)
                || Screen.SHOPPING_ITEM_FORM.name().equals(savedShoppingScreen)) {
            screen = Screen.valueOf(savedShoppingScreen);
            editingShoppingItemId = state.getString(STATE_SHOPPING_ITEM_ID);
            String restoredShoppingFilter = state.getString(STATE_SHOPPING_FILTER);
            shoppingStatusFilter =
                    ShoppingViewModel.AVAILABLE.equals(restoredShoppingFilter)
                            ? ShoppingViewModel.AVAILABLE
                            : ShoppingViewModel.NEED_TO_BUY;
            try {
                shoppingReturnScreen =
                        Screen.valueOf(
                                state.getString(
                                        STATE_SHOPPING_RETURN_SCREEN, Screen.MEAL_PLAN.name()));
            } catch (IllegalArgumentException invalidScreen) {
                shoppingReturnScreen = Screen.MEAL_PLAN;
            }
            return;
        }
        if (!Screen.MEAL_PLAN.name().equals(savedScreen)
                && !Screen.MEAL_PLAN_PICKER.name().equals(savedScreen)) return;
        screen = Screen.valueOf(savedScreen);
        if (screen == Screen.MEAL_PLAN_PICKER) {
            ArrayList<String> savedIds = state.getStringArrayList(STATE_PICKED_RECIPE_IDS);
            MealPlanViewModel.PickerSource savedSource;
            String savedSourceName =
                    state.getString(STATE_PICKER_SOURCE, MealPlanViewModel.PickerSource.ALL.name());
            try {
                savedSource =
                        MealPlanViewModel.PickerSource.valueOf(
                                savedSourceName == null
                                        ? MealPlanViewModel.PickerSource.ALL.name()
                                        : savedSourceName);
            } catch (IllegalArgumentException invalidSource) {
                savedSource = MealPlanViewModel.PickerSource.ALL;
            }
            mealPlanViewModel.restorePickerDraft(
                    savedIds,
                    state.getInt(STATE_PICKER_SERVINGS, MealPlanViewModel.MIN_SERVINGS),
                    state.getString(STATE_SEARCH_QUERY, ""),
                    savedSource,
                    state.getLong(STATE_PICKER_COLLECTION_ID, -1L));
        } else {
        }
    }

    private Screen readScreen(Bundle state, String key, Screen fallback) {
        try {
            return Screen.valueOf(state.getString(key, fallback.name()));
        } catch (IllegalArgumentException invalid) {
            return fallback;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER) {
            outState.putString(STATE_SCREEN, screen.name());
            if (screen == Screen.MEAL_PLAN_PICKER) {
                outState.putStringArrayList(
                        STATE_PICKED_RECIPE_IDS,
                        new ArrayList<>(mealPlanViewModel.getSelectedRecipeIds()));
                outState.putInt(STATE_PICKER_SERVINGS, mealPlanViewModel.getPickerServings());
                outState.putString(STATE_PICKER_SOURCE, mealPlanViewModel.getPickerSource().name());
                outState.putLong(
                        STATE_PICKER_COLLECTION_ID, mealPlanViewModel.getPickerCollectionId());
                outState.putString(STATE_SEARCH_QUERY, mealPlanViewModel.getSearchQuery());
            }
        }
        discoverSearchQuery = searchViewModel.getSearchQuery();
        if (isShoppingScreen()) {
            outState.putString(STATE_SHOPPING_SCREEN, screen.name());
            outState.putString(STATE_SHOPPING_ITEM_ID, editingShoppingItemId);
            outState.putString(STATE_SHOPPING_FILTER, shoppingStatusFilter);
            outState.putString(STATE_SHOPPING_RETURN_SCREEN, shoppingReturnScreen.name());
        }
        outState.putString(STATE_JOURNAL_RETURN_SCREEN, cookingLogReturnScreen.name());
        if (isCookingJournalScreen()) {
            outState.putString(STATE_JOURNAL_RECIPE_ID, cookingLogRecipeId);
            outState.putString(STATE_JOURNAL_LOG_ID, editingCookingLogId);
            outState.putString(STATE_JOURNAL_FORM_RETURN, cookingLogFormReturnScreen.name());
        }
        outState.putString(STATE_APP_SCREEN, screen.name());
        outState.putString(STATE_SELECTED_CATEGORY, selectedCategory);
        outState.putString(
                STATE_DISCOVER_SEARCH_QUERY,
                searchViewModel == null ? discoverSearchQuery : searchViewModel.getSearchQuery());
        outState.putString(STATE_DETAIL_RECIPE_ID, detailRecipeId);
        outState.putString(STATE_DETAIL_RETURN_SCREEN, detailReturnScreen.name());
        outState.putString(STATE_NOTE_RETURN_SCREEN, noteReturnScreen.name());
        outState.putString(STATE_COLLECTION_RETURN_SCREEN, collectionReturnScreen.name());
        outState.putString(STATE_COLLECTION_FORM_RETURN_SCREEN, collectionFormReturnScreen.name());
        long[] collectionIds = new long[collectionBackStack.size()];
        for (int i = 0; i < collectionBackStack.size(); i++) collectionIds[i] = collectionBackStack.get(i);
        outState.putLongArray(STATE_COLLECTION_BACK_STACK, collectionIds);
        outState.putLong(STATE_SELECTED_COLLECTION_ID, selectedCollectionId);
        outState.putString(STATE_NOTE_RECIPE_ID, noteRecipeId);
        super.onSaveInstanceState(outState);
    }

    private void configureSystemBars() {
        android.view.Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (renderPending) {
            renderPending = false;
            render();
        }
    }

    private void render() {
        if (getSupportFragmentManager().isStateSaved()) {
            renderPending = true;
            return;
        }
        if (isDiscoverScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(DiscoverFragment.class.getName());
            if (restored instanceof DiscoverFragment) {
                ((DiscoverFragment) restored)
                        .syncScreen(screen, selectedCategory, detailRecipeId, detailReturnScreen);
                return;
            }
            DiscoverFragment fragment = new DiscoverFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, fragment, DiscoverFragment.class.getName())
                    .commitNow();
            fragment.syncScreen(screen, selectedCategory, detailRecipeId, detailReturnScreen);
            return;
        }
        if (isCookingJournalScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager()
                            .findFragmentByTag(CookingJournalFragment.class.getName());
            if (restored instanceof CookingJournalFragment) {
                CookingJournalFragment fragment = (CookingJournalFragment) restored;
                if (screen == Screen.COOKING_LOG_FORM && !fragment.isFormShowing())
                    fragment.openLogForm(editingCookingLogId, cookingLogRecipeId);
                else fragment.showScreen(screen.name(), cookingLogRecipeId);
                return;
            }
            String initialJournalScreen =
                    screen == Screen.COOKING_LOG_FORM
                            ? cookingLogFormReturnScreen.name()
                            : screen.name();
            CookingJournalFragment fragment =
                    CookingJournalFragment.newInstance(initialJournalScreen, cookingLogRecipeId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            fragment,
                            CookingJournalFragment.class.getName())
                    .commitNow();
            if (screen == Screen.COOKING_LOG_FORM)
                fragment.openLogForm(editingCookingLogId, cookingLogRecipeId);
            return;
        }
        if (isPantryScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(PantryFragment.class.getName());
            if (restored instanceof PantryFragment) {
                return;
            }
            PantryFragment fragment = PantryFragment.newInstance(createPantryFragmentArguments());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            fragment,
                            PantryFragment.class.getName())
                    .commitNow();
            return;
        }
        if (isShoppingScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(ShoppingFragment.class.getName());
            if (restored instanceof ShoppingFragment) {
                ShoppingFragment fragment = (ShoppingFragment) restored;
                if (screen == Screen.SHOPPING_ITEM_FORM && !fragment.isFormShowing())
                    fragment.showForm(editingShoppingItemId);
                else if (screen == Screen.SHOPPING && fragment.isFormShowing()) fragment.showList();
                return;
            }
            ShoppingFragment fragment =
                    ShoppingFragment.newInstance(
                            shoppingStatusFilter == null
                                    ? ShoppingViewModel.NEED_TO_BUY
                                    : shoppingStatusFilter);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            fragment,
                            ShoppingFragment.class.getName())
                    .commitNow();
            if (screen == Screen.SHOPPING_ITEM_FORM)
                fragment.showForm(editingShoppingItemId);
            return;
        }
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(MealPlanFragment.class.getName());
            if (restored instanceof MealPlanFragment) {
                ((MealPlanFragment) restored).showPicker(screen == Screen.MEAL_PLAN_PICKER);
                return;
            }
            MealPlanFragment fragment = MealPlanFragment.newInstance(screen == Screen.MEAL_PLAN_PICKER);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            fragment,
                            MealPlanFragment.class.getName())
                    .commitNow();
            return;
        }
        if (screen == Screen.LIBRARY
                || screen == Screen.FAVORITES
                || screen == Screen.COLLECTION_LIST
                || screen == Screen.COLLECTION
                || screen == Screen.COLLECTION_FORM
                || screen == Screen.NOTE) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager()
                            .findFragmentByTag(PersonalDataFragment.class.getName());
            if (restored instanceof PersonalDataFragment) {
                ((PersonalDataFragment) restored).showScreen(
                        personalDataSection(), selectedCollectionId, noteRecipeId);
                return;
            }
            PersonalDataFragment fragment =
                    PersonalDataFragment.newInstance(
                            personalDataSection(), selectedCollectionId, noteRecipeId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            fragment,
                            PersonalDataFragment.class.getName())
                    .commitNow();
            return;
        }
        screen = Screen.DISCOVER;
        render();
    }

    private void removeLegacyContent() {
        if (mainContent == null) return;
        for (int i = mainContent.getChildCount() - 1; i >= 0; i--) {
            View child = mainContent.getChildAt(i);
            if (child != fragmentContainer) mainContent.removeViewAt(i);
        }
    }

    private void openShoppingItemForm(String itemId) {
        editingShoppingItemId = itemId;
        screen = Screen.SHOPPING_ITEM_FORM;
        ShoppingFragment fragment = activeShoppingFragment();
        if (fragment != null) fragment.showForm(itemId);
        else render();
    }


    private void openShoppingList(Screen returnScreen) {
        shoppingReturnScreen = returnScreen == Screen.SHOPPING ? Screen.MEAL_PLAN : returnScreen;
        shoppingStatusFilter = ShoppingViewModel.NEED_TO_BUY;
        screen = Screen.SHOPPING;
        shoppingViewModel.refreshItems();
        if (returnScreen == Screen.MEAL_PLAN) shoppingViewModel.checkForPlanChangesAsync();
        render();
    }

    private void openMealPlanPicker() {
        mealPlanViewModel.beginPicker();
        mealPlanViewModel.refresh();
        screen = Screen.MEAL_PLAN_PICKER;
        render();
    }

    private void openPantry(Screen returnScreen) {
        pantryReturnScreen = returnScreen == Screen.PANTRY ? Screen.SHOPPING : returnScreen;
        screen = Screen.PANTRY;
        pantryViewModel.refreshItems();
        render();
    }

    private boolean isPantryScreen() {
        return screen == Screen.PANTRY || screen == Screen.PANTRY_FORM;
    }

    private PantryFragment activePantryFragment() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(PantryFragment.class.getName());
        return fragment instanceof PantryFragment ? (PantryFragment) fragment : null;
    }

    private boolean isDiscoverScreen() {
        return screen == Screen.DISCOVER
                || screen == Screen.CATEGORY_LIST
                || screen == Screen.CATEGORY_RECIPES
                || screen == Screen.NEW_RECIPES
                || screen == Screen.SEARCH
                || screen == Screen.SEARCH_FILTER
                || screen == Screen.SEARCH_RESULTS
                || screen == Screen.WHAT_TO_COOK_INGREDIENTS
                || screen == Screen.WHAT_TO_COOK_RESULTS
                || screen == Screen.RECIPE_DETAIL;
    }

    private boolean isPersonalDataScreen() {
        return screen == Screen.LIBRARY
                || screen == Screen.FAVORITES
                || screen == Screen.COLLECTION_LIST
                || screen == Screen.COLLECTION
                || screen == Screen.COLLECTION_FORM
                || screen == Screen.NOTE;
    }

    private void handlePersonalDataBack() {
        if (screen == Screen.FAVORITES || screen == Screen.COLLECTION_LIST) {
            screen = Screen.LIBRARY;
        } else if (screen == Screen.COLLECTION_FORM) {
            if (collectionFormReturnScreen == Screen.COLLECTION
                    && selectedCollectionId <= 0
                    && !collectionBackStack.isEmpty()) {
                selectedCollectionId = collectionBackStack.remove(collectionBackStack.size() - 1);
            }
            screen = collectionFormReturnScreen;
        } else if (screen == Screen.COLLECTION) {
            if (collectionReturnScreen == Screen.COLLECTION && !collectionBackStack.isEmpty()) {
                selectedCollectionId = collectionBackStack.remove(collectionBackStack.size() - 1);
            } else {
                screen = collectionReturnScreen;
            }
        } else if (screen == Screen.NOTE) {
            screen = noteReturnScreen;
        } else {
            screen = Screen.DISCOVER;
        }
        render();
    }

    private DiscoverFragment activeDiscoverFragment() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(DiscoverFragment.class.getName());
        return fragment instanceof DiscoverFragment ? (DiscoverFragment) fragment : null;
    }

    private boolean isShoppingScreen() {
        return screen == Screen.SHOPPING || screen == Screen.SHOPPING_ITEM_FORM;
    }

    private ShoppingFragment activeShoppingFragment() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(ShoppingFragment.class.getName());
        return fragment instanceof ShoppingFragment ? (ShoppingFragment) fragment : null;
    }

    @Override
    public void onShoppingBack() {
        screen = shoppingReturnScreen == Screen.SHOPPING ? Screen.DISCOVER : shoppingReturnScreen;
        render();
    }

    @Override
    public void onShoppingNavigateTab(int tab) {
        if (tab == 0) screen = Screen.DISCOVER;
        else if (tab == 1) {
            screen = Screen.MEAL_PLAN;
            mealPlanViewModel.refresh();
        } else if (tab == 2) return;
        else screen = Screen.LIBRARY;
        render();
    }

    @Override
    public void onShoppingOpenPantry() {
        pantryReturnScreen = Screen.SHOPPING;
        screen = Screen.PANTRY;
        pantryViewModel.refreshItems();
        render();
    }

    @Override
    public void onShoppingEditItem(String id) {
        openShoppingItemForm(id);
    }

    @Override
    public void onShoppingFilterChanged(String status) {
        shoppingStatusFilter = status;
    }

    @Override
    public void onShoppingFormClosed() {
        screen = Screen.SHOPPING;
    }

    @Override
    public void onShoppingFormSaved(String status) {
        shoppingStatusFilter = status;
        screen = Screen.SHOPPING;
    }

    @Override
    public void onDiscoverScreenChanged(
            Screen newScreen, String category, String recipeId, Screen detailReturn) {
        screen = newScreen;
        selectedCategory = category == null ? "" : category;
        if (recipeId != null) detailRecipeId = recipeId;
        detailReturnScreen = detailReturn == null ? Screen.DISCOVER : detailReturn;
    }

    @Override
    public void onDiscoverOpenNote(Recipe recipe) {
        openRecipeNote(recipe);
    }

    @Override
    public void onDiscoverOpenMealPlanPicker(Recipe recipe) {
        openMealPlanPicker();
    }

    @Override
    public void onDiscoverStartCooking(String canonicalRecipeId, int servings) {
        // N2-10 will open the cooking session with this canonical ID and selected servings.
        android.widget.Toast.makeText(this, "Chế độ nấu từng bước chưa khả dụng.",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDiscoverNavigateTab(int tab) {
        if (tab == 0) {
            screen = Screen.DISCOVER;
        } else if (tab == 1) {
            screen = Screen.MEAL_PLAN;
            mealPlanViewModel.refresh();
        } else if (tab == 2) {
            openShoppingList(Screen.DISCOVER);
            return;
        } else {
            screen = Screen.LIBRARY;
        }
        render();
    }

    @Override
    public void onDiscoverBack() {
        finish();
    }

    @Override
    public void onDiscoverExitTo(Screen destination) {
        screen = destination;
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER)
            mealPlanViewModel.refresh();
        render();
    }

    @Override
    public void onMealPlanOpenPicker() {
        openMealPlanPicker();
    }

    @Override
    public void onMealPlanBackFromPicker() {
        screen = Screen.MEAL_PLAN;
        render();
    }

    @Override
    public void onMealPlanSelectionSaved() {
        screen = Screen.MEAL_PLAN;
        render();
    }

    @Override
    public void onMealPlanOpenShopping() {
        openShoppingList(Screen.MEAL_PLAN);
    }

    @Override
    public void onMealPlanOpenRecipe(Recipe recipe) {
        openRecipeDetail(recipe);
    }

    @Override
    public void onMealPlanNavigateTab(int tab) {
        if (tab == 0) screen = Screen.DISCOVER;
        else if (tab == 1) {
            mealPlanViewModel.refresh();
            return;
        } else if (tab == 2) {
            openShoppingList(Screen.MEAL_PLAN);
            return;
        } else screen = Screen.LIBRARY;
        render();
    }

    private String personalDataSection() {
        if (screen == Screen.FAVORITES) return "FAVORITES";
        if (screen == Screen.COLLECTION_LIST) return "COLLECTIONS";
        if (screen == Screen.COLLECTION) return "COLLECTION";
        if (screen == Screen.COLLECTION_FORM) return "COLLECTION_FORM";
        if (screen == Screen.NOTE) return "NOTE";
        return "LIBRARY";
    }

    @Override
    public void onPersonalOpenFavorites() {
        screen = Screen.FAVORITES;
        render();
    }

    @Override
    public void onPersonalOpenCollections() {
        collectionReturnScreen = screen;
        collectionBackStack.clear();
        screen = Screen.COLLECTION_LIST;
        render();
    }

    @Override
    public void onPersonalCreateCollection() {
        openCollectionForm(null);
    }

    @Override
    public void onPersonalEditCollection(long collectionId) {
        openCollectionForm(collectionId);
    }

    @Override
    public void onPersonalDeleteCollection(long collectionId) {
        // The deletion publishes state immediately, so leave the deleted collection's screen first.
        returnFromCollection();
        personalDataViewModel.deleteCollection(collectionId);
    }

    private void returnFromCollection() {
        if (collectionReturnScreen == Screen.COLLECTION && !collectionBackStack.isEmpty()) {
            selectedCollectionId = collectionBackStack.remove(collectionBackStack.size() - 1);
            screen = Screen.COLLECTION;
        } else {
            screen = collectionReturnScreen;
            selectedCollectionId = -1L;
        }
        render();
    }

    @Override
    public void onPersonalCollectionSaved(long collectionId) {
        selectedCollectionId = collectionId;
        screen = Screen.COLLECTION;
        render();
    }

    @Override
    public void onPersonalOpenCollection(long collectionId) {
        if (screen == Screen.COLLECTION && selectedCollectionId > 0 && selectedCollectionId != collectionId) {
            collectionBackStack.add(selectedCollectionId);
            collectionReturnScreen = Screen.COLLECTION;
        } else {
            collectionBackStack.clear();
            collectionReturnScreen = screen == Screen.COLLECTION_LIST
                    ? Screen.COLLECTION_LIST : Screen.LIBRARY;
        }
        selectedCollectionId = collectionId;
        screen = Screen.COLLECTION;
        render();
    }

    @Override
    public void onPersonalOpenRecipe(Recipe recipe) {
        openRecipeDetail(recipe);
    }

    @Override
    public Recipe onPersonalResolveRecipe(String recipeId) {
        return recipeById(recipeId);
    }

    @Override
    public void onPersonalOpenRecipeLogs(String recipeId) {
        openCookingLogs(recipeId, Screen.NOTE);
    }

    @Override
    public void onPersonalCreateCookingLog(String recipeId) {
        openCookingLogForm(null, recipeId);
    }

    @Override
    public void onPersonalOpenJournal() {
        openAllCookingLogs(Screen.LIBRARY);
    }

    @Override
    public void onPersonalNavigateTab(int tab) {
        if (tab == 0) screen = Screen.DISCOVER;
        else if (tab == 1) {
            screen = Screen.MEAL_PLAN;
            mealPlanViewModel.refresh();
        } else if (tab == 2) {
            openShoppingList(Screen.LIBRARY);
            return;
        } else screen = Screen.LIBRARY;
        render();
    }

    @Override
    public void onPersonalBack() {
        handlePersonalDataBack();
    }

    @Override
    public Recipe onJournalResolveRecipe(String recipeId) {
        return recipeById(recipeId);
    }

    @Override
    public void onJournalBack(String currentScreen) {
        if (Screen.COOKING_LOG_FORM.name().equals(currentScreen))
            screen = cookingLogFormReturnScreen;
        else if (Screen.COOKING_JOURNAL.name().equals(currentScreen)
                || Screen.COOKING_LOGS.name().equals(currentScreen))
            screen = cookingLogReturnScreen;
        else screen = Screen.DISCOVER;
        render();
    }

    @Override
    public void onJournalNavigateTab(int tab) {
        if (tab == 0) screen = Screen.DISCOVER;
        else if (tab == 1) {
            screen = Screen.MEAL_PLAN;
            mealPlanViewModel.refresh();
        } else if (tab == 2) {
            openShoppingList(Screen.COOKING_JOURNAL);
            return;
        } else screen = Screen.LIBRARY;
        render();
    }

    @Override
    public void onJournalScreenChanged(String route, String recipeId) {
        try {
            screen = Screen.valueOf(route);
        } catch (IllegalArgumentException invalid) {
            screen = Screen.COOKING_JOURNAL;
        }
        cookingLogRecipeId = recipeId;
        if (!isCookingJournalScreen()) render();
    }

    private CookingJournalFragment activeCookingJournalFragment() {
        Fragment fragment =
                getSupportFragmentManager()
                        .findFragmentByTag(CookingJournalFragment.class.getName());
        return fragment instanceof CookingJournalFragment
                ? (CookingJournalFragment) fragment
                : null;
    }

    private boolean isCookingJournalScreen() {
        return screen == Screen.COOKING_JOURNAL
                || screen == Screen.COOKING_LOGS
                || screen == Screen.COOKING_LOG_FORM;
    }

    private Bundle createPantryFragmentArguments() {
        Bundle args = new Bundle();
        args.putBoolean("pantry.initialScreen", screen == Screen.PANTRY_FORM);
        return args;
    }

    @Override
    public void onPantryClose() {
        screen = pantryReturnScreen;
        render();
    }

    @Override
    public void onPantryScreenChanged(boolean showingForm) {
        screen = showingForm ? Screen.PANTRY_FORM : Screen.PANTRY;
    }

    @Override
    public void onPantryNavigateTab(int selectedTab) {
        if (selectedTab == 0) screen = Screen.DISCOVER;
        else if (selectedTab == 1) {
            screen = Screen.MEAL_PLAN;
            mealPlanViewModel.refresh();
        } else if (selectedTab == 2) {
            openShoppingList(Screen.PANTRY);
            return;
        } else screen = Screen.LIBRARY;
        render();
    }

    private void openCookingLogs(String recipeId, Screen returnScreen) {
        cookingLogRecipeId = recipeId;
        cookingLogReturnScreen = returnScreen;
        screen = Screen.COOKING_LOGS;
        render();
    }

    private void openCookingLogForm(String logId, String recipeId) {
        cookingLogFormReturnScreen = screen;
        cookingLogRecipeId = recipeId;
        editingCookingLogId = logId;
        screen = Screen.COOKING_LOG_FORM;
        render();
    }

    private void openAllCookingLogs(Screen returnScreen) {
        cookingLogRecipeId = null;
        cookingLogReturnScreen = returnScreen;
        screen = Screen.COOKING_JOURNAL;
        render();
    }
    private void openRecipeDetail(Recipe recipe) {
        if (recipe.details == null) personalDataViewModel.recordViewedRecipe(recipe);
        detailReturnScreen = screen;
        detailRecipeId = recipe.id;
        screen = Screen.RECIPE_DETAIL;
        render();
    }
    private void openCollectionForm(Long collectionId) {
        collectionFormReturnScreen = collectionId == null ? screen : Screen.COLLECTION;
        if (collectionId == null) {
            if (screen == Screen.COLLECTION && selectedCollectionId > 0) {
                collectionBackStack.add(selectedCollectionId);
                collectionReturnScreen = Screen.COLLECTION;
            } else {
                collectionReturnScreen = screen;
            }
        }
        selectedCollectionId = collectionId == null ? -1L : collectionId;
        String name = "";
        List<Recipe> recipes = new ArrayList<>();
        if (collectionId != null) {
            RecipeCollection row = findCollection(collectionId);
            if (row != null) name = row.name;
            recipes.addAll(personalDataViewModel.getCollectionRecipes(collectionId));
        }
        personalDataViewModel.beginCollectionForm(name, recipes);
        screen = Screen.COLLECTION_FORM;
        render();
    }

    private void openRecipeNote(Recipe recipe) {
        personalDataViewModel.recordViewedRecipe(recipe);
        noteReturnScreen = screen;
        noteRecipeId = recipe.id;
        screen = Screen.NOTE;
        render();
    }
    private RecipeCollection findCollection(long id) {
        for (RecipeCollection row : personalDataViewModel.getCollections())
            if (row.id == id) return row;
        return null;
    }
    private Recipe recipeById(String id) {
        for (Recipe recipe : PreviewRecipeCatalog.designRecipes())
            if (recipe.id.equals(id)) return recipe;
        return PreviewRecipeCatalog.designRecipes().get(0);
    }
}
