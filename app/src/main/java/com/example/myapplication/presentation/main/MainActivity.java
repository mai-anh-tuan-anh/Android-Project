package com.example.myapplication.presentation.main;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.*;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.presentation.cookingjournal.CookingJournalFragment;
import com.example.myapplication.presentation.discover.DiscoverSearchViewModel;
import com.example.myapplication.presentation.discover.DiscoverViewModel;
import com.example.myapplication.presentation.mealplan.MealPlanFragment;
import com.example.myapplication.presentation.mealplan.MealPlanViewModel;
import com.example.myapplication.presentation.pantry.PantryFragment;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import com.example.myapplication.presentation.personaldata.PersonalDataFragment;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;
import com.example.myapplication.presentation.shopping.ShoppingFragment;
import com.example.myapplication.presentation.shopping.ShoppingViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local personal data, meal planning, shopping, pantry, and cooking journal. */
public class MainActivity extends AppCompatActivity
        implements PantryFragment.Host,
                ShoppingFragment.Host,
                MealPlanFragment.Host,
                PersonalDataFragment.Host,
                CookingJournalFragment.Host {
    private static final String STATE_APP_SCREEN = "app.currentScreen";
    private static final String STATE_JOURNAL_RETURN_SCREEN = "journal.returnScreen";
    private static final String STATE_JOURNAL_RECIPE_ID = "journal.recipeId";
    private static final String STATE_JOURNAL_LOG_ID = "journal.logId";
    private static final String STATE_JOURNAL_FORM_RETURN = "journal.formReturn";
    private static final String STATE_SELECTED_CATEGORY = "discover.selectedCategory";
    private static final String STATE_DISCOVER_SEARCH_QUERY = "discover.searchQuery";
    private static final String STATE_DETAIL_RECIPE_ID = "discover.detailRecipeId";
    private static final String STATE_DETAIL_RETURN_SCREEN = "discover.detailReturnScreen";
    private static final String STATE_SCREEN = "module6.screen";
    private static final String STATE_PICKED_RECIPE_IDS = "module6.pickedRecipeIds";
    private static final String STATE_PICKER_SERVINGS = "module6.pickerServings";
    private static final String STATE_SEARCH_QUERY = "module6.searchQuery";
    private static final String STATE_PICKER_SOURCE = "module6.pickerSource";
    private static final String STATE_PICKER_COLLECTION_ID = "module6.pickerCollectionId";
    private static final String STATE_SHOPPING_SCREEN = "module7.screen";
    private static final String STATE_SHOPPING_ITEM_ID = "module7.itemId";
    private static final String STATE_SHOPPING_NAME = "module7.name";
    private static final String STATE_SHOPPING_AMOUNT = "module7.amount";
    private static final String STATE_SHOPPING_UNIT = "module7.unit";
    private static final String STATE_SHOPPING_UNIT_ID = "module7.unitId";
    private static final String STATE_SHOPPING_CATEGORY = "module7.category";
    private static final String STATE_SHOPPING_FILTER = "module7.filter";
    private static final String STATE_SHOPPING_RETURN_SCREEN = "module7.returnScreen";
    private static final int CREAM = Color.rgb(250, 249, 246);
    private static final int GREEN = Color.rgb(50, 108, 79);
    private static final int PALE_GREEN = Color.rgb(232, 242, 233);
    private static final int TEXT = Color.rgb(37, 53, 45);
    private static final int MUTED = Color.rgb(88, 102, 92);
    private static final int BORDER = Color.rgb(220, 228, 220);
    private static final int ERROR = Color.rgb(169, 54, 54);
    private static final int WHITE = Color.WHITE;

    private PersonalDataViewModel personalDataViewModel;
    private MealPlanViewModel mealPlanViewModel;
    private ShoppingViewModel shoppingViewModel;
    private PantryViewModel pantryViewModel;
    private DiscoverSearchViewModel searchViewModel;
    private DiscoverViewModel discoverViewModel;
    private LinearLayout appRoot;
    private FrameLayout mainContent;
    private FrameLayout fragmentContainer;
    private PantryFragment pantryFragment;
    private ShoppingFragment shoppingFragment;
    private MealPlanFragment mealPlanFragment;
    private PersonalDataFragment personalDataFragment;
    private CookingJournalFragment cookingJournalFragment;
    private Screen cookingLogReturnScreen = Screen.NOTE;
    private Screen cookingLogFormReturnScreen = Screen.COOKING_LOGS;
    private String cookingLogRecipeId;
    private String editingCookingLogId;
    private View bottomNavigationBar;
    private Screen screen = Screen.DISCOVER;
    private boolean animateNextScreen;
    private boolean renderPending;
    private Screen noteReturnScreen = Screen.LIBRARY;
    private Screen collectionReturnScreen = Screen.LIBRARY;
    private Screen detailReturnScreen = Screen.DISCOVER;
    private String detailRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private String selectedCategory = "";
    private String discoverSearchQuery = "";
    private long selectedCollectionId = -1L;
    private String noteRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private String favoritesSearchQuery = "";
    private EditText discoverSearchInput;
    private Screen shoppingReturnScreen = Screen.DISCOVER;
    private String editingShoppingItemId;
    private EditText shoppingNameInput;
    private EditText shoppingAmountInput;
    private EditText shoppingUnitInput;
    private TextView shoppingCategoryInput;
    private String shoppingDraftName = "";
    private String shoppingDraftAmount = "";
    private String shoppingDraftUnit = "";
    private String shoppingDraftUnitId = "";
    private String shoppingDraftCategory = ShoppingCategoryCatalog.OTHER;
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
                                } else if (isPantryScreen()) {
                                    PantryFragment fragment = activePantryFragment();
                                    if (fragment != null) {
                                        fragment.handleBackPressed();
                                        return;
                                    }
                                } else if (screen == Screen.SEARCH_FILTER
                                        || screen == Screen.SEARCH_RESULTS) {
                                    screen = Screen.SEARCH;
                                } else if (screen == Screen.WHAT_TO_COOK_RESULTS) {
                                    screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                                } else if (screen == Screen.CATEGORY_RECIPES
                                        || screen == Screen.NEW_RECIPES
                                        || screen == Screen.CATEGORY_LIST
                                        || screen == Screen.SEARCH
                                        || screen == Screen.WHAT_TO_COOK_INGREDIENTS) {
                                    screen = Screen.DISCOVER;
                                } else if (screen == Screen.CATEGORY_RECIPES) {
                                    screen = Screen.CATEGORY_LIST;
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
                                    screen =
                                            selectedCollectionId > 0
                                                    ? Screen.COLLECTION
                                                    : collectionReturnScreen;
                                } else if (screen == Screen.COLLECTION) {
                                    screen = collectionReturnScreen;
                                } else if (screen == Screen.NOTE) {
                                    screen = noteReturnScreen;
                                } else if (screen == Screen.MEAL_PLAN_PICKER) {
                                    screen = Screen.MEAL_PLAN;
                                } else if (screen == Screen.SHOPPING_ITEM_FORM) {
                                    screen = Screen.SHOPPING;
                                } else if (screen == Screen.SHOPPING) {
                                    screen =
                                            shoppingReturnScreen == Screen.SHOPPING
                                                    ? Screen.DISCOVER
                                                    : shoppingReturnScreen;
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
        discoverViewModel =
                new ViewModelProvider(this, viewModelFactory).get(DiscoverViewModel.class);
        searchViewModel
                .recentSearchState()
                .observe(
                        this,
                        state -> {
                            if (state != null
                                    && !state.loading
                                    && screen == Screen.SEARCH
                                    && (discoverSearchInput == null
                                            || !discoverSearchInput.hasFocus())) render();
                        });
        personalDataViewModel
                .personalDataState()
                .observe(
                        this,
                        state -> {
                            if (state == null || state.loading) return;
                            if (screen == Screen.RECIPE_DETAIL) {
                                render();
                            }
                        });
        shoppingViewModel
                .saveResult()
                .observe(
                        this,
                        result -> {
                            if (result == null || screen != Screen.SHOPPING_ITEM_FORM) return;
                            shoppingViewModel.clearSaveResult();
                            if (result.success) {
                                shoppingStatusFilter = result.status;
                                screen = Screen.SHOPPING;
                                render();
                            } else {
                                Toast.makeText(
                                                this,
                                                result.error == null
                                                        ? "Không thể lưu nguyên liệu."
                                                        : result.error,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
        shoppingViewModel
                .planUpdate()
                .observe(
                        this,
                        update -> {
                            if (update == null || screen != Screen.SHOPPING) return;
                            shoppingViewModel.clearPlanUpdate();
                            if (!update.changes.isEmpty()) showShoppingPlanUpdate(update);
                            else if (update.message != null && !update.message.isEmpty()) {
                                Toast.makeText(this, update.message, Toast.LENGTH_SHORT).show();
                            }
                        });
        personalDataViewModel.seedDesignPreviewIfNeeded(PreviewRecipeCatalog.designRecipes());
        restoreAppState(savedInstanceState);
        restoreMealPlanState(savedInstanceState);
        if (screen == Screen.PANTRY) pantryViewModel.refreshItems();
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER)
            mealPlanViewModel.refresh();
        if (screen == Screen.SHOPPING) shoppingViewModel.refreshItems();
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
        detailRecipeId =
                state.getString(
                        STATE_DETAIL_RECIPE_ID, PreviewRecipeCatalog.designRecipes().get(0).id);
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
            shoppingDraftName = state.getString(STATE_SHOPPING_NAME, "");
            shoppingDraftAmount = state.getString(STATE_SHOPPING_AMOUNT, "");
            shoppingDraftUnit = state.getString(STATE_SHOPPING_UNIT, "");
            shoppingDraftUnitId = state.getString(STATE_SHOPPING_UNIT_ID, "");
            shoppingDraftCategory =
                    state.getString(STATE_SHOPPING_CATEGORY, ShoppingCategoryCatalog.OTHER);
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
        searchViewModel.setSearchQuery(discoverSearchQuery);
        if (screen == Screen.SHOPPING || screen == Screen.SHOPPING_ITEM_FORM) {
            outState.putString(STATE_SHOPPING_SCREEN, screen.name());
            outState.putString(STATE_SHOPPING_ITEM_ID, editingShoppingItemId);
            outState.putString(
                    STATE_SHOPPING_NAME,
                    shoppingNameInput == null
                            ? shoppingDraftName
                            : shoppingNameInput.getText().toString());
            outState.putString(
                    STATE_SHOPPING_AMOUNT,
                    shoppingAmountInput == null
                            ? shoppingDraftAmount
                            : shoppingAmountInput.getText().toString());
            outState.putString(
                    STATE_SHOPPING_UNIT,
                    shoppingUnitInput == null
                            ? shoppingDraftUnit
                            : shoppingUnitInput.getText().toString());
            outState.putString(STATE_SHOPPING_UNIT_ID, shoppingDraftUnitId);
            outState.putString(STATE_SHOPPING_CATEGORY, shoppingDraftCategory);
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
        if (isCookingJournalScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager()
                            .findFragmentByTag(CookingJournalFragment.class.getName());
            if (restored instanceof CookingJournalFragment) {
                cookingJournalFragment = (CookingJournalFragment) restored;
                if (screen == Screen.COOKING_LOG_FORM && !cookingJournalFragment.isFormShowing())
                    cookingJournalFragment.openLogForm(editingCookingLogId, cookingLogRecipeId);
                else cookingJournalFragment.showScreen(screen.name(), cookingLogRecipeId);
                return;
            }
            String initialJournalScreen =
                    screen == Screen.COOKING_LOG_FORM
                            ? cookingLogFormReturnScreen.name()
                            : screen.name();
            cookingJournalFragment =
                    CookingJournalFragment.newInstance(initialJournalScreen, cookingLogRecipeId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            cookingJournalFragment,
                            CookingJournalFragment.class.getName())
                    .commitNow();
            if (screen == Screen.COOKING_LOG_FORM)
                cookingJournalFragment.openLogForm(editingCookingLogId, cookingLogRecipeId);
            return;
        }
        if (isPantryScreen()) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(PantryFragment.class.getName());
            if (restored instanceof PantryFragment) {
                pantryFragment = (PantryFragment) restored;
                return;
            }
            pantryFragment = PantryFragment.newInstance(createPantryFragmentArguments());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            pantryFragment,
                            PantryFragment.class.getName())
                    .commitNow();
            return;
        }
        if (screen == Screen.SHOPPING) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(ShoppingFragment.class.getName());
            if (restored instanceof ShoppingFragment) {
                shoppingFragment = (ShoppingFragment) restored;
                return;
            }
            shoppingFragment =
                    ShoppingFragment.newInstance(
                            shoppingStatusFilter == null
                                    ? ShoppingViewModel.NEED_TO_BUY
                                    : shoppingStatusFilter);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            shoppingFragment,
                            ShoppingFragment.class.getName())
                    .commitNow();
            return;
        }
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER) {
            removeLegacyContent();
            fragmentContainer.setVisibility(View.VISIBLE);
            Fragment restored =
                    getSupportFragmentManager().findFragmentByTag(MealPlanFragment.class.getName());
            if (restored instanceof MealPlanFragment) {
                mealPlanFragment = (MealPlanFragment) restored;
                mealPlanFragment.showPicker(screen == Screen.MEAL_PLAN_PICKER);
                return;
            }
            mealPlanFragment = MealPlanFragment.newInstance(screen == Screen.MEAL_PLAN_PICKER);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            mealPlanFragment,
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
                personalDataFragment = (PersonalDataFragment) restored;
                personalDataFragment.showScreen(
                        personalDataSection(), selectedCollectionId, noteRecipeId);
                return;
            }
            personalDataFragment =
                    PersonalDataFragment.newInstance(
                            personalDataSection(), selectedCollectionId, noteRecipeId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main_fragment_container,
                            personalDataFragment,
                            PersonalDataFragment.class.getName())
                    .commitNow();
            return;
        }
        Fragment existingPantry =
                getSupportFragmentManager().findFragmentByTag(PantryFragment.class.getName());
        if (existingPantry != null) {
            getSupportFragmentManager().beginTransaction().remove(existingPantry).commitNow();
            pantryFragment = null;
        }
        Fragment existingShopping =
                getSupportFragmentManager().findFragmentByTag(ShoppingFragment.class.getName());
        if (existingShopping != null) {
            getSupportFragmentManager().beginTransaction().remove(existingShopping).commitNow();
            shoppingFragment = null;
        }
        Fragment existingMealPlan =
                getSupportFragmentManager().findFragmentByTag(MealPlanFragment.class.getName());
        if (existingMealPlan != null) {
            getSupportFragmentManager().beginTransaction().remove(existingMealPlan).commitNow();
            mealPlanFragment = null;
        }
        Fragment existingJournal =
                getSupportFragmentManager()
                        .findFragmentByTag(CookingJournalFragment.class.getName());
        if (existingJournal != null) {
            getSupportFragmentManager().beginTransaction().remove(existingJournal).commitNow();
            cookingJournalFragment = null;
        }
        Fragment existingPersonalData =
                getSupportFragmentManager().findFragmentByTag(PersonalDataFragment.class.getName());
        if (existingPersonalData != null) {
            getSupportFragmentManager().beginTransaction().remove(existingPersonalData).commitNow();
            personalDataFragment = null;
        }
        fragmentContainer.setVisibility(View.GONE);
        removeLegacyContent();
        bottomNavigationBar = null;
        FrameLayout frame = new FrameLayout(this);
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(CREAM);
        frame.addView(appRoot, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout root = appRoot;
        ViewCompat.setOnApplyWindowInsetsListener(
                frame,
                (view, windowInsets) -> {
                    Insets systemBars =
                            windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                    root.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            Math.max(systemBars.bottom, ime.bottom));
                    return windowInsets;
                });

        if (screen == Screen.DISCOVER) renderDiscover();
        else if (screen == Screen.CATEGORY_LIST) renderCategoryList();
        else if (screen == Screen.CATEGORY_RECIPES) renderCategoryRecipes();
        else if (screen == Screen.NEW_RECIPES) renderNewRecipes();
        else if (screen == Screen.SEARCH) renderSearch();
        else if (screen == Screen.SEARCH_FILTER) renderSearchFilter();
        else if (screen == Screen.SEARCH_RESULTS) renderSearchResults();
        else if (screen == Screen.WHAT_TO_COOK_INGREDIENTS) renderWhatToCookIngredients();
        else if (screen == Screen.WHAT_TO_COOK_RESULTS) renderWhatToCookResults();
        else if (screen == Screen.RECIPE_DETAIL) renderRecipeDetail();
        else if (screen == Screen.SHOPPING_ITEM_FORM) renderShoppingItemForm();
        else renderDiscover();
        if (mainContent != null) mainContent.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        if (animateNextScreen) {
            animateNextScreen = false;
            frame.setAlpha(0f);
            frame.setTranslationY(dp(6));
            frame.animate().alpha(1f).translationY(0f).setDuration(160L).start();
        }
        ViewCompat.requestApplyInsets(frame);
    }

    private void removeLegacyContent() {
        if (mainContent == null) return;
        for (int i = mainContent.getChildCount() - 1; i >= 0; i--) {
            View child = mainContent.getChildAt(i);
            if (child != fragmentContainer) mainContent.removeViewAt(i);
        }
    }

    private void renderDiscover() {
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(content);

        addText(content, "Bếp nhà", 23, TEXT, true, 0, 0, 0, 0);
        addText(content, "Hôm nay bạn muốn nấu món gì?", 14, MUTED, false, 0, 4, 0, 16);

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setBackground(roundRect(WHITE, BORDER, 16));
        searchBar.setPadding(dp(14), dp(12), dp(14), dp(12));
        ImageView searchIcon = iconView(R.drawable.ic_search_outline, MUTED);
        searchBar.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView hint = text("Tìm công thức, nguyên liệu...", 14, MUTED, false);
        hint.setPadding(dp(10), 0, 0, 0);
        searchBar.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        searchBar.setOnClickListener(v -> {
            screen = Screen.SEARCH;
            render();
        });
        content.addView(searchBar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout banner = vertical();
        banner.setBackground(roundRect(PALE_GREEN, GREEN, 16));
        banner.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, -2);
        bannerParams.setMargins(0, dp(18), 0, 0);

        LinearLayout bannerHeader = new LinearLayout(this);
        bannerHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView bannerTitle = text("🍳  Tối nay nấu gì?", 16, GREEN, true);
        bannerHeader.addView(bannerTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView bannerBadge = text("MỚI", 11, WHITE, true);
        bannerBadge.setBackground(roundRect(GREEN, GREEN, 8));
        bannerBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        bannerHeader.addView(bannerBadge);
        banner.addView(bannerHeader);

        TextView bannerDesc =
                text(
                        "Gợi ý món ngon từ các nguyên liệu sẵn có trong tủ lạnh của bạn.",
                        13,
                        TEXT,
                        false);
        bannerDesc.setPadding(0, dp(6), 0, dp(12));
        banner.addView(bannerDesc);

        TextView bannerButton = text("Khám phá ngay ›", 13, GREEN, true);
        banner.addView(bannerButton);
        banner.setOnClickListener(v -> {
            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
            render();
        });
        content.addView(banner, bannerParams);

        addSectionHeader(
                content,
                "Danh mục món ăn",
                "Xem tất cả ›",
                () -> {
                    screen = Screen.CATEGORY_LIST;
                    render();
                },
                24);

        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] categories = {
            "Món Việt", "Món Á", "Món Âu", "Bữa sáng", "Món chay", "Món nhanh", "Dễ làm"
        };
        for (String cat : categories) {
            TextView chip = text(cat, 13, TEXT, true);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackground(roundRect(WHITE, BORDER, 18));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
            chipParams.setMargins(0, 0, dp(8), 0);
            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                screen = Screen.CATEGORY_RECIPES;
                render();
            });
            catRow.addView(chip, chipParams);
        }
        catScroll.addView(catRow);
        content.addView(catScroll, new LinearLayout.LayoutParams(-1, -2));

        addSectionHeader(
                content,
                "Món mới công bố",
                "Xem tất cả ›",
                () -> {
                    screen = Screen.NEW_RECIPES;
                    render();
                },
                24);
        List<Recipe> newRecipes =
                discoverViewModel.newlyPublished(
                        PreviewRecipeCatalog.designRecipes(), recipe -> recipe.publishedAtMillis);
        if (newRecipes.isEmpty()) {
            addText(content, "Chưa có món mới công bố.", 13, MUTED, false, 0, 0, 0, 0);
        } else {
            addRecipeGrid(content, newRecipes, true, true);
        }

        addSectionHeader(content, "Món nhanh & Dễ làm", "", () -> {}, 24);
        List<Recipe> quickRecipes =
                discoverViewModel.quickAndEasy(
                        PreviewRecipeCatalog.designRecipes(),
                        recipe -> recipe.durationMinutes,
                        recipe -> recipe.difficulty);
        if (quickRecipes.isEmpty())
            addText(content, "Chưa có món vừa nhanh vừa dễ.", 13, MUTED, false, 0, 0, 0, 0);
        else addRecipeGrid(content, quickRecipes, true, true);

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void renderCategoryList() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Danh mục món ăn");
        addText(page, "Chọn nhóm món bạn muốn khám phá.", 14, MUTED, false, 0, 0, 0, 20);

        String[][] categories = {
            {"Món Việt", "category_vietnamese"},
            {"Món chính", "category_main"},
            {"Canh & súp", "category_soup"},
            {"Món nhanh", "category_quick"},
            {"Món chay", "category_vegetarian"},
            {"Bữa sáng", "category_breakfast"}
        };
        for (int i = 0; i < categories.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(20);
            row.addView(
                    createCategoryTile(categories[i][0], categories[i][1]),
                    new LinearLayout.LayoutParams(0, -2, 1));
            View gap = new View(this);
            row.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
            if (i + 1 < categories.length) {
                row.addView(
                        createCategoryTile(categories[i + 1][0], categories[i + 1][1]),
                        new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            }
            page.addView(row, rowParams);
        }
        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private View createCategoryTile(String title, String imageName) {
        LinearLayout card = vertical();
        card.setBackground(roundRect(WHITE, BORDER, 17));
        card.setClipToOutline(true);
        ImageView image = new ImageView(this);
        int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        if (imageId != 0) image.setImageResource(imageId);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(130)));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(12), dp(10), dp(12), dp(10));
        titleRow.addView(text(title, 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        titleRow.addView(text("›", 22, GREEN, false));
        card.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));
        card.setOnClickListener(v -> {
            selectedCategory = title;
            screen = Screen.CATEGORY_RECIPES;
            render();
        });
        return card;
    }

    private void renderCategoryRecipes() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, selectedCategory.isEmpty() ? "Danh mục món ăn" : selectedCategory);
        addText(page, "Các công thức phù hợp thuộc danh mục.", 14, MUTED, false, 0, 0, 0, 16);
        List<Recipe> matches =
                discoverViewModel.recipesInCategory(
                        PreviewRecipeCatalog.designRecipes(),
                        selectedCategory,
                        recipe -> recipe.categories);
        if (matches.isEmpty())
            addText(page, "Danh mục này hiện chưa có món nào.", 14, MUTED, false, 0, 8, 0, 0);
        else addRecipeGrid(page, matches, true, true);
        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void renderNewRecipes() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addText(page, "Món mới", 28, TEXT, true, 0, 0, 0, 0);
        addText(page, "Sắp xếp theo ngày công bố gần nhất.", 14, MUTED, false, 0, 4, 0, 20);
        List<Recipe> newRecipes =
                discoverViewModel.newlyPublished(
                        PreviewRecipeCatalog.designRecipes(), recipe -> recipe.publishedAtMillis);
        if (newRecipes.isEmpty()) {
            addEmptyCard(
                    page,
                    "Chưa có món mới công bố",
                    "Món sẽ xuất hiện tại đây sau khi kho công thức cung cấp thời điểm công bố.",
                    () -> {
                        screen = Screen.DISCOVER;
                        render();
                    });
        } else {
            addRecipeGrid(page, newRecipes, true, true);
        }
        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void addSearchDishItem(LinearLayout parent, Recipe recipe) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), dp(7), dp(14), dp(7));
        item.setBackground(roundRect(WHITE, BORDER, 15));
        item.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, dp(8), 0);
        labels.addView(text(recipe.title, 14, TEXT, true));
        labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("›", 22, MUTED, false);
        item.addView(arrow);

        item.setOnClickListener(v -> openRecipeDetail(recipe));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        parent.addView(item, lp);
    }

    private void renderSearch() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));

        addBackTitle(page, "Tìm kiếm");

        // Search Input Box
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        searchBox.setPadding(dp(14), dp(10), dp(12), dp(10));

        ImageView searchIcon = iconView(R.drawable.ic_search_outline, MUTED);
        searchBox.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        EditText searchInput = editField(searchViewModel.getSearchQuery(), 15, 44);
        discoverSearchInput = searchInput;
        searchInput.setHint("Tìm công thức, nguyên liệu...");
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(10), 0, dp(10), 0);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView clearIcon = iconView(R.drawable.ic_close_outline, MUTED);
        clearIcon.setPadding(dp(4), dp(4), dp(4), dp(4));
        clearIcon.setVisibility(
                searchViewModel.getSearchQuery().isEmpty() ? View.GONE : View.VISIBLE);
        clearIcon.setOnClickListener(v -> {
            searchViewModel.setSearchQuery("");
            searchInput.setText("");
            render();
        });
        searchBox.addView(clearIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        page.addView(searchBox, new LinearLayout.LayoutParams(-1, -2));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                String q = searchInput.getText().toString();
                searchViewModel.submitSearch(q);
                screen = Screen.SEARCH_RESULTS;
                render();
                return true;
            }
            return false;
        });

        // Section "Tìm kiếm gần đây" with Filter button on the right
        LinearLayout recentHeader = new LinearLayout(this);
        recentHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rhParams = new LinearLayout.LayoutParams(-1, -2);
        rhParams.topMargin = dp(20);
        rhParams.bottomMargin = dp(12);

        TextView recentTitle = text("Tìm kiếm gần đây", 18, TEXT, true);
        recentHeader.addView(recentTitle, new LinearLayout.LayoutParams(0, -2, 1));

        List<String> recentList = searchViewModel.getRecentSearches();
        if (!recentList.isEmpty()) {
            TextView clearRecentBtn = text("Xóa", 12, MUTED, false);
            clearRecentBtn.setPadding(dp(8), dp(4), dp(12), dp(4));
            clearRecentBtn.setOnClickListener(v -> {
                searchViewModel.clearRecentSearches();
                render();
            });
            recentHeader.addView(clearRecentBtn);
        }

        FrameLayout filterBtn = new FrameLayout(this);
        filterBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView filterIcon = iconView(R.drawable.ic_filter_outline, GREEN);
        filterBtn.addView(filterIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        filterBtn.setOnClickListener(v -> {
            searchViewModel.beginFilterDraft();
            screen = Screen.SEARCH_FILTER;
            render();
        });
        recentHeader.addView(filterBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(recentHeader, rhParams);

        // Recent search chips
        if (!recentList.isEmpty()) {
            HorizontalScrollView recentScroll = new HorizontalScrollView(this);
            recentScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout recentRow = new LinearLayout(this);
            recentRow.setOrientation(LinearLayout.HORIZONTAL);
            for (String term : recentList) {
                TextView chip = text(term, 13, TEXT, false);
                chip.setPadding(dp(16), dp(8), dp(16), dp(8));
                chip.setBackground(roundRect(WHITE, BORDER, 18));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = dp(8);
                chip.setOnClickListener(v -> {
                    searchViewModel.submitSearch(term);
                    screen = Screen.SEARCH_RESULTS;
                    render();
                });
                recentRow.addView(chip, lp);
            }
            recentScroll.addView(recentRow);
            page.addView(recentScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        // Section "Gợi ý"
        addSectionHeader(page, "Gợi ý", "", () -> {}, 20);
        HorizontalScrollView sugScroll = new HorizontalScrollView(this);
        sugScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout sugRow = new LinearLayout(this);
        sugRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String sug : searchViewModel.getSuggestions()) {
            TextView chip = text(sug, 13, TEXT, false);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackground(roundRect(WHITE, BORDER, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.submitSearch(sug);
                screen = Screen.SEARCH_RESULTS;
                render();
            });
            sugRow.addView(chip, lp);
        }
        sugScroll.addView(sugRow);
        page.addView(sugScroll, new LinearLayout.LayoutParams(-1, -2));

        // Section "Món phù hợp"
        addSectionHeader(page, "Món phù hợp", "", () -> {}, 20);
        List<Recipe> matches = searchViewModel.filterRecipes(PreviewRecipeCatalog.designRecipes());
        for (Recipe r : matches) {
            addSearchDishItem(page, r);
        }

        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderSearchFilter() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(12), dp(24), dp(24));

        View handle = new View(this);
        handle.setBackground(roundRect(BORDER, BORDER, 3));
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(44), dp(5));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dp(14);
        page.addView(handle, handleLp);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Bộ lọc nâng cao", 22, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        FrameLayout closeBtn = new FrameLayout(this);
        closeBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView closeIcon = iconView(R.drawable.ic_close_outline, GREEN);
        closeBtn.addView(closeIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        closeBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        header.addView(closeBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
        page.addView(header);

        addText(page, "Thời gian nấu", 15, TEXT, true, 0, 20, 0, 10);
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] times = {15, 30, -1};
        String[] timeLabels = {"< 15 phút", "15–30 phút", "> 30 phút"};
        Integer currentRange = searchViewModel.getDraftDurationRange();
        for (int i = 0; i < times.length; i++) {
            final int t = times[i];
            boolean selected = currentRange != null && currentRange == t;
            TextView chip = text(timeLabels[i], 13, selected ? GREEN : TEXT, selected);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackground(
                    roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.setDraftDurationRange(t);
                render();
            });
            timeRow.addView(chip, lp);
        }
        page.addView(timeRow);

        addText(page, "Độ khó", 15, TEXT, true, 0, 20, 0, 10);
        LinearLayout diffRow = new LinearLayout(this);
        diffRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] diffs = {"Dễ", "Vừa", "Khó"};
        String currentDiff = searchViewModel.getDraftDifficulty();
        for (String d : diffs) {
            boolean selected = d.equalsIgnoreCase(currentDiff);
            TextView chip = text(d, 13, selected ? GREEN : TEXT, selected);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackground(
                    roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.setDraftDifficulty(d);
                render();
            });
            diffRow.addView(chip, lp);
        }
        page.addView(diffRow);

        addText(page, "Chế độ ăn", 15, TEXT, true, 0, 20, 0, 10);
        LinearLayout dietRow = new LinearLayout(this);
        dietRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] diets = {"Món chay", "Không giới hạn"};
        String currentDiet = searchViewModel.getDraftDiet();
        for (String dt : diets) {
            boolean selected = dt.equalsIgnoreCase(currentDiet);
            TextView chip = text(dt, 13, selected ? GREEN : TEXT, selected);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackground(
                    roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.setDraftDiet(dt);
                render();
            });
            dietRow.addView(chip, lp);
        }
        page.addView(dietRow);

        addText(page, "Nguyên liệu muốn dùng", 14, TEXT, true, 0, 20, 0, 8);
        EditText incInput = editField(searchViewModel.getDraftIncludeIngredient(), 14, 46);
        incInput.setHint("Ví dụ: Cà chua, thịt gà...");
        incInput.setBackground(roundRect(WHITE, BORDER, 14));
        incInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        incInput.setSingleLine(true);
        incInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchViewModel.setDraftIncludeIngredient(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
        page.addView(incInput, new LinearLayout.LayoutParams(-1, -2));

        addText(page, "Nguyên liệu cần loại trừ", 14, TEXT, true, 0, 16, 0, 8);
        EditText excInput = editField(searchViewModel.getDraftExcludeIngredient(), 14, 46);
        excInput.setHint("Ví dụ: Đậu phộng, hải sản...");
        excInput.setBackground(roundRect(WHITE, BORDER, 14));
        excInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        excInput.setSingleLine(true);
        excInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchViewModel.setDraftExcludeIngredient(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
        page.addView(excInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams arParams = new LinearLayout.LayoutParams(-1, -2);
        arParams.topMargin = dp(28);
        arParams.bottomMargin = dp(16);

        TextView resetBtn = text("Đặt lại", 14, TEXT, true);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setBackground(roundRect(WHITE, BORDER, 14));
        resetBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        resetBtn.setOnClickListener(v -> {
            searchViewModel.resetDraftFilter();
            render();
        });
        actionRow.addView(resetBtn, new LinearLayout.LayoutParams(0, -2, 35));

        View gap = new View(this);
        actionRow.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));

        TextView applyBtn = text("Áp dụng bộ lọc", 14, WHITE, true);
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setBackground(roundRect(GREEN, GREEN, 14));
        applyBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        applyBtn.setOnClickListener(v -> {
            searchViewModel.applyDraftFilter();
            screen = Screen.SEARCH_RESULTS;
            render();
        });
        actionRow.addView(applyBtn, new LinearLayout.LayoutParams(0, -2, 65));
        page.addView(actionRow, arParams);

        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderSearchResults() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));

        addBackTitle(page, "Kết quả tìm kiếm");

        // Search Input Box
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        searchBox.setPadding(dp(14), dp(10), dp(12), dp(10));

        ImageView searchIcon = iconView(R.drawable.ic_search_outline, MUTED);
        searchBox.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        EditText searchInput = editField(searchViewModel.getSearchQuery(), 15, 44);
        searchInput.setHint("Tìm công thức, nguyên liệu...");
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(10), 0, dp(10), 0);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView clearIcon = iconView(R.drawable.ic_close_outline, MUTED);
        clearIcon.setPadding(dp(4), dp(4), dp(4), dp(4));
        clearIcon.setVisibility(
                searchViewModel.getSearchQuery().isEmpty() ? View.GONE : View.VISIBLE);
        clearIcon.setOnClickListener(v -> {
            searchViewModel.setSearchQuery("");
            searchInput.setText("");
            render();
        });
        searchBox.addView(clearIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                String q = searchInput.getText().toString();
                searchViewModel.submitSearch(q);
                render();
                return true;
            }
            return false;
        });

        LinearLayout.LayoutParams sbParams = new LinearLayout.LayoutParams(-1, -2);
        sbParams.bottomMargin = dp(14);
        page.addView(searchBox, sbParams);

        // Active Filter row
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams frParams = new LinearLayout.LayoutParams(-1, -2);
        frParams.bottomMargin = dp(14);

        HorizontalScrollView badgeScroll = new HorizontalScrollView(this);
        badgeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);

        List<String> activeBadges = searchViewModel.getActiveFilterBadges();
        for (String badge : activeBadges) {
            TextView chip = text(badge + " ✕", 12, GREEN, true);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setBackground(roundRect(PALE_GREEN, GREEN, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.removeFilterBadge(badge);
                render();
            });
            badgeRow.addView(chip, lp);
        }
        badgeScroll.addView(badgeRow);
        filterRow.addView(badgeScroll, new LinearLayout.LayoutParams(0, -2, 1));

        FrameLayout filterBtn = new FrameLayout(this);
        filterBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView filterIcon = iconView(R.drawable.ic_filter_outline, GREEN);
        filterBtn.addView(filterIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        filterBtn.setOnClickListener(v -> {
            searchViewModel.beginFilterDraft();
            screen = Screen.SEARCH_FILTER;
            render();
        });
        filterRow.addView(filterBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
        page.addView(filterRow, frParams);

        // Search matches
        List<Recipe> matches = searchViewModel.filterRecipes(PreviewRecipeCatalog.designRecipes());
        if (!matches.isEmpty()) {
            TextView countText = text(matches.size() + " món phù hợp", 13, MUTED, false);
            countText.setPadding(0, 0, 0, dp(12));
            page.addView(countText);

            for (Recipe r : matches) {
                addSearchDishItem(page, r);
            }
        } else {
            // Empty state matching states/khong-ket-qua.png
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(dp(24), dp(40), dp(24), dp(24));

            FrameLayout iconBg = new FrameLayout(this);
            iconBg.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 24));
            ImageView searchEmptyIcon = iconView(R.drawable.ic_search_outline, GREEN);
            iconBg.addView(
                    searchEmptyIcon, new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER));
            empty.addView(iconBg, new LinearLayout.LayoutParams(dp(72), dp(72)));

            TextView emptyTitle = text("Không tìm thấy món", 20, TEXT, true);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(-1, -2);
            etParams.topMargin = dp(20);
            empty.addView(emptyTitle, etParams);

            TextView emptyDesc =
                    text(
                            "Thử từ khóa khác hoặc bỏ bớt bộ lọc để mở rộng kết quả.",
                            14,
                            MUTED,
                            false);
            emptyDesc.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams edParams = new LinearLayout.LayoutParams(-1, -2);
            edParams.topMargin = dp(8);
            edParams.bottomMargin = dp(24);
            empty.addView(emptyDesc, edParams);

            TextView editFilterBtn = text("Chỉnh bộ lọc", 14, WHITE, true);
            editFilterBtn.setGravity(Gravity.CENTER);
            editFilterBtn.setBackground(roundRect(GREEN, GREEN, 14));
            editFilterBtn.setPadding(dp(28), dp(14), dp(28), dp(14));
            editFilterBtn.setOnClickListener(v -> {
                searchViewModel.beginFilterDraft();
                screen = Screen.SEARCH_FILTER;
                render();
            });
            empty.addView(editFilterBtn, new LinearLayout.LayoutParams(-1, -2));

            page.addView(empty);
        }

        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderWhatToCookIngredients() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Có gì nấu nấy");
        addText(
                page,
                "Chọn nguyên liệu bạn đang có để nhận gợi ý món ăn.",
                14,
                MUTED,
                false,
                0,
                0,
                0,
                16);

        String[][] ingredientGroups = {
            {"Thịt / Hải sản", "Thịt bò", "Thịt lợn", "Thịt gà", "Cá basa"},
            {
                "Rau củ",
                "Cà chua",
                "Dứa",
                "Đậu bắp",
                "Giá đỗ",
                "Xà lách",
                "Dưa chuột",
                "Hành tây",
                "Hành lá"
            },
            {"Trứng / Đậu", "Trứng gà", "Đậu phụ"},
            {"Gia vị", "Nước mắm", "Tỏi", "Đường", "Hành khô"}
        };
        LinearLayout ingSection = vertical();
        ingSection.setBackground(roundRect(WHITE, BORDER, 16));
        ingSection.setPadding(dp(16), dp(12), dp(16), dp(12));
        for (int groupIndex = 0; groupIndex < ingredientGroups.length; groupIndex++) {
            String[] group = ingredientGroups[groupIndex];
            addText(ingSection, group[0], 14, TEXT, true, 0, groupIndex == 0 ? 0 : 12, 0, 8);
            LinearLayout chipRow = new LinearLayout(this);
            chipRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 1; i < group.length; i++) {
                String ingredient = group[i];
                TextView chip = text(ingredient, 12, TEXT, false);
                styleCookingIngredientChip(chip, ingredient);
                chip.setPadding(dp(12), dp(8), dp(12), dp(8));
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
                chipParams.rightMargin = dp(8);
                chipRow.addView(chip, chipParams);
            }
            HorizontalScrollView chipScroll = new HorizontalScrollView(this);
            chipScroll.setHorizontalScrollBarEnabled(false);
            chipScroll.addView(chipRow);
            ingSection.addView(chipScroll, new LinearLayout.LayoutParams(-1, -2));
        }
        page.addView(ingSection, new LinearLayout.LayoutParams(-1, -2));

        Set<String> selectedIngredients = searchViewModel.getSelectedCookingIngredients();
        addText(
                page,
                selectedIngredients.isEmpty()
                        ? "Chưa chọn nguyên liệu"
                        : "Đã chọn " + selectedIngredients.size() + " nguyên liệu",
                13,
                MUTED,
                false,
                0,
                12,
                0,
                0);
        if (!selectedIngredients.isEmpty()) {
            TextView clearSelection = text("Bỏ chọn tất cả", 12, GREEN, true);
            clearSelection.setPadding(0, dp(6), 0, dp(4));
            clearSelection.setOnClickListener(v -> {
                searchViewModel.clearCookingIngredients();
                render();
            });
            page.addView(clearSelection);
        }

        TextView findRecipesBtn = text("Xem gợi ý món ăn ›", 14, WHITE, true);
        findRecipesBtn.setGravity(Gravity.CENTER);
        findRecipesBtn.setBackground(roundRect(GREEN, GREEN, 14));
        findRecipesBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, -2);
        btnParams.topMargin = dp(24);
        findRecipesBtn.setOnClickListener(v -> {
            screen = Screen.WHAT_TO_COOK_RESULTS;
            render();
        });
        page.addView(findRecipesBtn, btnParams);

        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void styleCookingIngredientChip(TextView chip, String ingredient) {
        boolean selected = searchViewModel.isCookingIngredientSelected(ingredient);
        chip.setTextColor(selected ? GREEN : TEXT);
        chip.setTypeface(chip.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 14));
        chip.setOnClickListener(v -> {
            searchViewModel.toggleCookingIngredient(ingredient);
            render();
        });
    }

    private String joinNames(List<String> names) {
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            if (result.length() > 0) result.append(", ");
            result.append(name);
        }
        return result.toString();
    }

    private void addSuggestionCard(LinearLayout parent, Recipe recipe) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(12), dp(10));
        card.setBackground(roundRect(WHITE, BORDER, 16));
        card.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(76), dp(76)));

        LinearLayout content = vertical();
        content.setPadding(dp(12), 0, 0, 0);
        content.addView(text(recipe.title, 15, TEXT, true));
        content.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));

        int availableCount = searchViewModel.availableIngredientCount(recipe);
        int totalCount = searchViewModel.totalIngredientCount(recipe);
        List<String> missing = searchViewModel.missingIngredients(recipe);
        TextView availableBadge =
                text("✓ Đã có " + availableCount + "/" + totalCount, 11, GREEN, true);
        availableBadge.setPadding(dp(8), dp(5), dp(8), dp(5));
        availableBadge.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 12));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2);
        badgeLp.topMargin = dp(8);
        content.addView(availableBadge, badgeLp);

        TextView missingBadge =
                text(
                        missing.isEmpty()
                                ? "Đủ nguyên liệu"
                                : "Thiếu " + missing.size() + " nguyên liệu",
                        11,
                        missing.isEmpty() ? GREEN : TEXT,
                        true);
        missingBadge.setPadding(dp(8), dp(5), dp(8), dp(5));
        missingBadge.setBackground(roundRect(missing.isEmpty() ? PALE_GREEN : CREAM, BORDER, 12));
        LinearLayout.LayoutParams missingLp = new LinearLayout.LayoutParams(-2, -2);
        missingLp.topMargin = dp(6);
        content.addView(missingBadge, missingLp);
        if (!missing.isEmpty()) {
            TextView missingNames = text(joinNames(missing), 11, MUTED, false);
            missingNames.setMaxLines(2);
            missingNames.setPadding(0, dp(4), 0, 0);
            content.addView(missingNames);
        }
        card.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
        card.setOnClickListener(v -> openRecipeDetail(recipe));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(10);
        parent.addView(card, cardLp);
    }

    private void renderWhatToCookResults() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Món gợi ý phù hợp");
        Set<String> available = searchViewModel.getSelectedCookingIngredients();
        if (available.isEmpty()) {
            addEmptyCard(
                    page,
                    "Chưa chọn nguyên liệu",
                    "Quay lại để chọn ít nhất một nguyên liệu bạn đang có.",
                    () -> {
                        screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                        render();
                    });
        } else {
            List<Recipe> matches =
                    searchViewModel.suggestRecipes(PreviewRecipeCatalog.designRecipes());
            if (!searchViewModel.hasCompleteRecipeIngredients(
                    PreviewRecipeCatalog.designRecipes())) {
                addEmptyCard(
                        page,
                        "Chưa có đủ dữ liệu nguyên liệu",
                        "Gợi ý sẽ xuất hiện khi kho công thức cung cấp đầy đủ nguyên liệu cho món.",
                        () -> {
                            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                            render();
                        });
            } else if (matches.isEmpty()) {
                addEmptyCard(
                        page,
                        "Không có món phù hợp",
                        "Thử chọn thêm nguyên liệu để tìm được nhiều món hơn.",
                        () -> {
                            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                            render();
                        });
            } else {
                addText(
                        page,
                        "Món đủ nguyên liệu đứng trước; các món còn lại xếp theo số nguyên liệu"
                            + " thiếu.",
                        13,
                        MUTED,
                        false,
                        0,
                        0,
                        0,
                        16);
                if (searchViewModel.hasIncompleteRecipeIngredients(
                        PreviewRecipeCatalog.designRecipes())) {
                    addText(
                            page,
                            "Một số món chưa đủ dữ liệu nguyên liệu nên chưa được xét.",
                            12,
                            MUTED,
                            false,
                            0,
                            0,
                            0,
                            12);
                }
                for (Recipe recipe : matches) {
                    addSuggestionCard(page, recipe);
                }
            }
        }
        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void renderRecipeDetail() {
        Recipe recipe = recipeById(detailRecipeId);
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        scroll.addView(page);

        addBackTitle(page, recipe.title);

        LinearLayout card = vertical();
        card.setBackground(roundRect(WHITE, BORDER, 16));
        card.setClipToOutline(true);

        FrameLayout imgFrame = new FrameLayout(this);
        ImageView img = recipeImage(recipe);
        imgFrame.addView(img, new FrameLayout.LayoutParams(-1, dp(180)));

        boolean favorite = personalDataViewModel.isFavorite(recipe.id);
        ImageView star =
                iconView(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline, GREEN);
        star.setBackground(roundRect(WHITE, WHITE, 18));
        star.setPadding(dp(8), dp(8), dp(8), dp(8));
        FrameLayout.LayoutParams starParams =
                new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.END | Gravity.TOP);
        starParams.setMargins(0, dp(12), dp(12), 0);
        imgFrame.addView(star, starParams);
        star.setOnClickListener(v -> {
            if (personalDataViewModel.isFavorite(recipe.id)) {
                personalDataViewModel.removeFavorite(recipe.id);
            } else {
                personalDataViewModel.addFavorite(recipe);
            }
            render();
        });
        card.addView(imgFrame, new LinearLayout.LayoutParams(-1, dp(180)));

        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setPadding(dp(16), dp(14), dp(16), dp(14));
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView metaText =
                text(
                        "⏱ "
                                + recipe.duration
                                + "   •   📊 Độ khó: "
                                + recipe.difficulty
                                + "   •   👥 2-4 người",
                        13,
                        TEXT,
                        false);
        infoRow.addView(metaText);
        card.addView(infoRow);
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(-1, -2);
        actionRowParams.topMargin = dp(16);

        TextView addPlanBtn = text("＋ Nấu hôm nay", 13, WHITE, true);
        addPlanBtn.setGravity(Gravity.CENTER);
        addPlanBtn.setBackground(roundRect(GREEN, GREEN, 14));
        addPlanBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        addPlanBtn.setOnClickListener(v -> {
            openMealPlanPicker();
        });
        actionRow.addView(addPlanBtn, new LinearLayout.LayoutParams(0, -2, 1));

        View btnGap = new View(this);
        actionRow.addView(btnGap, new LinearLayout.LayoutParams(dp(10), 1));

        TextView noteBtn = text("📝 Ghi chú", 13, GREEN, true);
        noteBtn.setGravity(Gravity.CENTER);
        noteBtn.setBackground(roundRect(PALE_GREEN, GREEN, 14));
        noteBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        noteBtn.setOnClickListener(v -> openRecipeNote(recipe));
        actionRow.addView(noteBtn, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(actionRow, actionRowParams);

        addSectionHeader(page, "Nguyên liệu", "", () -> {}, 24);
        LinearLayout ingCard = vertical();
        ingCard.setBackground(roundRect(WHITE, BORDER, 14));
        ingCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addText(
                ingCard,
                "Định lượng nguyên liệu chi tiết theo khẩu phần (Module 3 của Người 2 đang hoàn"
                    + " thiện).",
                13,
                MUTED,
                false,
                0,
                0,
                0,
                0);
        page.addView(ingCard, new LinearLayout.LayoutParams(-1, -2));

        addSectionHeader(page, "Các bước nấu", "", () -> {}, 24);
        LinearLayout stepCard = vertical();
        stepCard.setBackground(roundRect(WHITE, BORDER, 14));
        stepCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addText(
                stepCard,
                "Hướng dẫn từng bước kèm hẹn giờ nấu (Module 4 của Người 2 đang hoàn thiện).",
                13,
                MUTED,
                false,
                0,
                0,
                0,
                0);
        page.addView(stepCard, new LinearLayout.LayoutParams(-1, -2));

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private static String normalizeRecipeSearch(String value) {
        String decomposed =
                java.text.Normalizer.normalize(
                        value == null ? "" : value.trim().toLowerCase(Locale.ROOT),
                        java.text.Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private FrameLayout createRecipeSearchBox(String value, String hint, TextWatcher watcher) {
        EditText search = editField(value, 16, 49);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setSingleLine(true);
        search.setHint(hint);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(48), dp(10), dp(12), dp(10));
        FrameLayout searchBox = new FrameLayout(this);
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView searchIcon = iconView(R.drawable.ic_search_outline, GREEN);
        searchIcon.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchBox.addView(searchIcon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
        searchBox.addView(search, new FrameLayout.LayoutParams(-1, -1));
        search.addTextChangedListener(watcher);
        return searchBox;
    }

    private LinearLayout.LayoutParams searchLayoutParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(49));
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private void openShoppingItemForm(String itemId) {
        editingShoppingItemId = itemId;
        ShoppingListItem item = shoppingViewModel.findItem(itemId);
        shoppingDraftName = item == null ? "" : item.name;
        shoppingDraftAmount = item == null ? "" : item.amount;
        shoppingDraftUnit = item == null ? "" : KitchenUnitCatalog.label(item.unit);
        shoppingDraftUnitId = item == null ? "" : item.unit;
        shoppingDraftCategory = item == null ? ShoppingCategoryCatalog.OTHER : item.category;
        screen = Screen.SHOPPING_ITEM_FORM;
        render();
    }

    private void renderShoppingItemForm() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(20), dp(24), dp(16));
        addBackTitle(page, editingShoppingItemId == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu");
        ScrollView scroll = scrollContent();
        LinearLayout fields = vertical();
        fields.setPadding(0, dp(20), 0, dp(20));
        scroll.addView(fields);

        addText(fields, "Tên nguyên liệu", 15, TEXT, true, 0, 0, 0, 0);
        shoppingNameInput = editField(shoppingDraftName, 16, 56);
        shoppingNameInput.setSingleLine(true);
        shoppingNameInput.setHint("Ví dụ: Cà chua");
        addFieldView(fields, shoppingNameInput, 10, 22);

        addText(fields, "Số lượng", 15, TEXT, true, 0, 0, 0, 0);
        shoppingAmountInput = editField(shoppingDraftAmount, 16, 56);
        shoppingAmountInput.setSingleLine(true);
        shoppingAmountInput.setHint("Ví dụ: 2 hoặc vừa đủ");
        addFieldView(fields, shoppingAmountInput, 10, 22);

        addText(fields, "Đơn vị", 15, TEXT, true, 0, 0, 0, 0);
        shoppingUnitInput = editField(shoppingDraftUnit, 16, 56);
        shoppingUnitInput.setSingleLine(true);
        shoppingUnitInput.setHint("Ví dụ: quả, g, ml, muỗng cà phê");
        addFieldView(fields, shoppingUnitInput, 10, 26);

        addText(fields, "Danh mục", 15, TEXT, true, 0, 0, 0, 0);
        shoppingCategoryInput = selectionField(shoppingDraftCategory + "     ▾", 16, 56);
        addFieldView(fields, shoppingCategoryInput, 10, 26);
        shoppingCategoryInput.setOnClickListener(v -> showShoppingCategoryPicker());

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "Lưu nguyên liệu", true, this::saveShoppingForm, 10);
        if (editingShoppingItemId != null) {
            TextView remove = text("Xóa nguyên liệu", 14, ERROR, true);
            remove.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(-1, dp(42));
            removeParams.topMargin = dp(6);
            page.addView(remove, removeParams);
            remove.setOnClickListener(v -> confirmDeleteShoppingItem(editingShoppingItemId));
        }
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void addFieldView(LinearLayout parent, View field, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        parent.addView(field, params);
    }

    private TextView selectionField(String value, int textSize, int minHeight) {
        TextView field = text(value, textSize, TEXT, false);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(15), dp(8), dp(15), dp(8));
        field.setMinHeight(dp(minHeight));
        field.setBackground(roundRect(WHITE, BORDER, 14));
        field.setFocusable(true);
        field.setClickable(true);
        return field;
    }

    private void showShoppingCategoryPicker() {
        String[] categories = ShoppingCategoryCatalog.ORDER.toArray(new String[0]);
        int selected = ShoppingCategoryCatalog.ORDER.indexOf(shoppingDraftCategory);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn danh mục")
                .setSingleChoiceItems(
                        categories,
                        Math.max(0, selected),
                        (dialog, which) -> {
                            shoppingDraftCategory = categories[which];
                            shoppingCategoryInput.setText(shoppingDraftCategory + "     ▾");
                            dialog.dismiss();
                        })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveShoppingForm() {
        shoppingDraftName = shoppingNameInput.getText().toString();
        shoppingDraftAmount = shoppingAmountInput.getText().toString();
        shoppingDraftUnit = shoppingUnitInput.getText().toString();
        String canonicalUnit =
                shoppingDraftUnit.equals(shoppingUnitInput.getText().toString())
                                && !shoppingDraftUnitId.isEmpty()
                        ? shoppingDraftUnitId
                        : KitchenUnitCatalog.canonicalId(shoppingDraftUnit);
        String status;
        if (editingShoppingItemId == null) {
            status = null; // The ViewModel checks known pantry stock asynchronously.
        } else {
            ShoppingListItem existing = shoppingViewModel.findItem(editingShoppingItemId);
            status = existing == null ? ShoppingViewModel.NEED_TO_BUY : existing.status;
        }
        if (!shoppingViewModel.saveItem(
                editingShoppingItemId,
                shoppingDraftName,
                shoppingDraftAmount,
                canonicalUnit,
                shoppingDraftCategory,
                status)) {
            return;
        }
    }

    private void confirmDeleteShoppingItem(String itemId) {
        ShoppingListItem item = shoppingViewModel.findItem(itemId);
        if (item == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa nguyên liệu?")
                .setMessage("Xóa “" + item.name + "” khỏi danh sách mua sắm?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) -> {
                            shoppingViewModel.deleteItem(itemId);
                            screen = Screen.SHOPPING;
                            render();
                        })
                .show();
    }

    private void showShoppingPlanUpdate(ShoppingViewModel.PlanUpdate update) {
        if (update == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        LinearLayout sheet = vertical();
        sheet.setPadding(dp(24), dp(12), dp(24), dp(24));
        sheet.setBackground(roundRect(WHITE, WHITE, 26));
        View handle = new View(this);
        handle.setBackground(roundRect(BORDER, BORDER, 4));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(72), dp(6));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        sheet.addView(handle, handleParams);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Cập nhật danh sách?", 23, TEXT, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = text("×", 28, GREEN, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 20));
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        close.setOnClickListener(v -> dialog.dismiss());
        sheet.addView(titleRow);

        TextView description =
                text(
                        update.message
                                + " Chưa có dữ liệu định lượng trong công thức; cập nhật sẽ ghi"
                                + " nhận kế hoạch mới và giữ nguyên các mục mua thủ công.",
                        15,
                        MUTED,
                        false);
        description.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.topMargin = dp(10);
        descriptionParams.bottomMargin = dp(18);
        sheet.addView(description, descriptionParams);

        ScrollView changesScroll = scrollContent();
        changesScroll.setFillViewport(false);
        changesScroll.setVerticalScrollBarEnabled(false);
        LinearLayout changes = vertical();
        changesScroll.addView(changes);
        for (ShoppingViewModel.PlanUpdateRow change : update.changes) {
            LinearLayout card = vertical();
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(roundRect(WHITE, BORDER, 15));
            card.addView(text(change.title, 16, TEXT, true));
            TextView quantityChange = text(change.servingChangeLabel(), 14, MUTED, false);
            LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(-1, -2);
            quantityParams.topMargin = dp(5);
            card.addView(quantityChange, quantityParams);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(10);
            changes.addView(card, cardParams);
        }
        LinearLayout.LayoutParams changesParams = new LinearLayout.LayoutParams(-1, -2);
        changesParams.height = Math.min(dp(300), dp(88 * update.changes.size()));
        sheet.addView(changesScroll, changesParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(52));
        actionsParams.topMargin = dp(18);
        sheet.addView(actions, actionsParams);
        TextView keep = text("Giữ danh sách cũ", 14, GREEN, true);
        keep.setGravity(Gravity.CENTER);
        keep.setBackground(roundRect(WHITE, BORDER, 14));
        actions.addView(keep, new LinearLayout.LayoutParams(0, -1, 1));
        keep.setOnClickListener(v -> dialog.dismiss());
        TextView refresh = text("Cập nhật", 15, WHITE, true);
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(roundRect(GREEN, GREEN, 14));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, -1, 1);
        refreshParams.leftMargin = dp(10);
        actions.addView(refresh, refreshParams);
        refresh.setOnClickListener(v -> {
            shoppingViewModel.acceptPlanUpdate(update);
            dialog.dismiss();
            Toast.makeText(
                            this,
                            "Đã ghi nhận kế hoạch mới; mục nhập tay được giữ nguyên.",
                            Toast.LENGTH_SHORT)
                    .show();
        });
        dialog.setContentView(sheet);
        dialog.show();
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
        return fragment instanceof PantryFragment ? (PantryFragment) fragment : pantryFragment;
    }

    @Override
    public void onShoppingBack() {
        screen = shoppingReturnScreen == Screen.SHOPPING ? Screen.DISCOVER : shoppingReturnScreen;
        render();
    }

    @Override
    public void onShoppingNavigateTab(int tab) {
        animateNextScreen = true;
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
        animateNextScreen = true;
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
        personalDataViewModel.deleteCollection(collectionId);
        selectedCollectionId = -1L;
        screen = collectionReturnScreen;
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
        selectedCollectionId = collectionId;
        collectionReturnScreen = Screen.LIBRARY;
        screen = Screen.COLLECTION;
        render();
    }

    @Override
    public void onPersonalOpenRecipe(Recipe recipe) {
        openRecipeDetail(recipe);
    }

    @Override
    public void onPersonalOpenNote(Recipe recipe) {
        openRecipeNote(recipe);
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
        animateNextScreen = true;
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
        getOnBackPressedDispatcher().onBackPressed();
    }

    @Override
    public Recipe onJournalResolveRecipe(String recipeId) {
        return recipeById(recipeId);
    }

    @Override
    public void onJournalBack(String currentScreen) {
        if (Screen.COOKING_JOURNAL.name().equals(currentScreen)) screen = cookingLogReturnScreen;
        else screen = Screen.NOTE;
        render();
    }

    @Override
    public void onJournalOpenRecipeNote(String recipeId) {
        noteRecipeId = recipeId;
        noteReturnScreen = Screen.LIBRARY;
        screen = Screen.NOTE;
        render();
    }

    @Override
    public void onJournalNavigateTab(int tab) {
        animateNextScreen = true;
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
        animateNextScreen = true;
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

    private String normalizeSearchText(String value) {
        String decomposed =
                Normalizer.normalize(
                                value == null ? "" : value.toLowerCase(Locale.ROOT),
                                Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .replace('đ', 'd');
        return decomposed.replaceAll("\\s+", " ").trim();
    }

    private int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    private void addRecipeGrid(
            LinearLayout parent, List<Recipe> recipes, boolean showFavorite, boolean removable) {
        if (recipes.isEmpty()) return;
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        int index = 0;
        for (Recipe recipe : recipes) {
            LinearLayout card = recipeCard(recipe, showFavorite, removable);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(index % 2, 1f);
            params.setMargins(0, 0, index % 2 == 0 ? dp(9) : 0, dp(12));
            grid.addView(card, params);
            index++;
        }
        parent.addView(grid, new LinearLayout.LayoutParams(-1, -2));
    }

    private LinearLayout recipeCard(Recipe recipe, boolean showFavorite, boolean removable) {
        LinearLayout card = vertical();
        card.setBackground(roundRect(WHITE, BORDER, 15));
        card.setClipToOutline(true);
        FrameLayout imageFrame = new FrameLayout(this);
        ImageView image = recipeImage(recipe);
        imageFrame.addView(image, new FrameLayout.LayoutParams(-1, dp(96)));
        if (showFavorite) {
            boolean favorite = personalDataViewModel.isFavorite(recipe.id);
            ImageView star =
                    iconView(
                            favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline,
                            GREEN);
            star.setBackground(roundRect(WHITE, WHITE, 18));
            FrameLayout starTouchTarget = new FrameLayout(this);
            starTouchTarget.setFocusable(true);
            starTouchTarget.setContentDescription(favorite ? "Bỏ yêu thích" : "Lưu yêu thích");
            FrameLayout.LayoutParams iconParams =
                    new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER);
            starTouchTarget.addView(star, iconParams);
            FrameLayout.LayoutParams touchParams =
                    new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
            touchParams.setMargins(0, dp(2), dp(2), 0);
            imageFrame.addView(starTouchTarget, touchParams);
            starTouchTarget.setOnClickListener(v -> {
                if (personalDataViewModel.isFavorite(recipe.id))
                    personalDataViewModel.removeFavorite(recipe.id);
                else personalDataViewModel.addFavorite(recipe);
                render();
            });
        }
        card.addView(imageFrame);
        TextView title = text(recipe.title, 13, TEXT, true);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(dp(9), dp(7), dp(8), 0);
        card.addView(title);
        TextView meta = text(recipe.duration + " · " + recipe.difficulty, 11, MUTED, false);
        meta.setPadding(dp(9), dp(6), dp(8), dp(11));
        card.addView(meta);
        if (removable)
            card.setOnClickListener(v -> {
                openRecipeDetail(recipe);
            });
        return card;
    }

    private void openRecipeDetail(Recipe recipe) {
        personalDataViewModel.recordViewedRecipe(recipe);
        detailReturnScreen = screen;
        detailRecipeId = recipe.id;
        screen = Screen.RECIPE_DETAIL;
        render();
    }

    private void addSectionHeader(
            LinearLayout parent, String title, String action, Runnable onAction, int top) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 19, TEXT, true);
        View before = new View(this);
        parent.addView(before, new LinearLayout.LayoutParams(1, dp(top == 30 ? 22 : top)));
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView link = text(action, 12, GREEN, true);
        row.addView(link);
        link.setOnClickListener(v -> onAction.run());
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(16)));
    }

    private void addCollectionRow(LinearLayout parent, RecipeCollection row, int top) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(9), dp(10), dp(14), dp(10));
        item.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView folder = iconView(R.drawable.ic_folder_outline, GREEN);
        folder.setPadding(dp(10), dp(10), dp(10), dp(10));
        folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 13));
        item.addView(folder, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout labels = vertical();
        labels.setPadding(dp(18), 0, 0, 0);
        labels.addView(text(row.name, 14, TEXT, true));
        labels.addView(text(row.recipeCount + " món đã lưu", 12, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(text("›", 23, GREEN, false));
        item.setOnClickListener(v -> {
            collectionReturnScreen = screen;
            selectedCollectionId = row.id;
            screen = Screen.COLLECTION;
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(62));
        params.bottomMargin = dp(9);
        parent.addView(item, params);
        if (top > 0) item.setTranslationY(dp(0));
    }

    private static void updateFavoriteToggleDescription(
            FrameLayout button, boolean favorite, Recipe recipe) {
        button.setContentDescription(
                (favorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    }

    private void addSelectableRecipeRow(LinearLayout parent, Recipe recipe) {
        boolean selected = personalDataViewModel.isCollectionRecipeSelected(recipe.id);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(7), dp(6), dp(14), dp(6));
        row.setBackground(roundRect(WHITE, BORDER, 14));
        ImageView image = recipeImage(recipe);
        row.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(recipe.title, 14, TEXT, true));
        labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView check = text(selected ? "✓" : "+", 21, selected ? WHITE : GREEN, false);
        check.setGravity(Gravity.CENTER);
        if (selected) check.setBackground(roundRect(GREEN, GREEN, 7));
        row.addView(check, new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.bottomMargin = dp(9);
        parent.addView(row, params);
        row.setOnClickListener(v -> {
            personalDataViewModel.toggleCollectionRecipe(recipe.id);
            render();
        });
    }

    private void addBottomNavigation(int selectedTab) {
        LinearLayout nav = new LinearLayout(this);
        bottomNavigationBar = nav;
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackgroundColor(WHITE);
        nav.setPadding(dp(7), dp(5), dp(7), dp(2));
        View topLine = new View(this);
        topLine.setBackgroundColor(BORDER);
        appRoot.addView(topLine, new LinearLayout.LayoutParams(-1, dp(1)));
        String[] names = {"Khám phá", "Nấu hôm nay", "Mua sắm", "Kho của tôi"};
        int[] icons = {
            R.drawable.ic_home_outline,
            R.drawable.ic_calendar_outline,
            R.drawable.ic_cart_outline,
            R.drawable.ic_star_outline
        };
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            LinearLayout tab = vertical();
            tab.setGravity(Gravity.CENTER);
            ImageView glyph = iconView(icons[i], i == selectedTab ? GREEN : MUTED);
            if (i == selectedTab) glyph.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
            tab.addView(glyph, new LinearLayout.LayoutParams(dp(52), dp(30)));
            TextView label = text(names[i], 10, i == selectedTab ? GREEN : MUTED, i == selectedTab);
            label.setGravity(Gravity.CENTER);
            tab.addView(label);
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(58), 1));
            tab.setOnClickListener(v -> {
                animateNextScreen = true;
                if (index == 0) {
                    screen = Screen.DISCOVER;
                    render();
                } else if (index == 3) {
                    screen = Screen.LIBRARY;
                    render();
                } else if (index == 1) {
                    screen = Screen.MEAL_PLAN;
                    mealPlanViewModel.refresh();
                    render();
                } else if (index == 2) {
                    openShoppingList(screen);
                }
            });
        }
        appRoot.addView(nav, new LinearLayout.LayoutParams(-1, dp(64)));
    }

    private void addBackTitle(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = iconView(R.drawable.ic_arrow_back_outline, GREEN);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        back.setBackground(roundRect(WHITE, WHITE, 15));
        back.setContentDescription("Quay lại");
        row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        TextView heading = text(title, 23, TEXT, true);
        float screenWidthDp =
                getResources().getDisplayMetrics().widthPixels
                        / getResources().getDisplayMetrics().density;
        float availableWidthDp = Math.max(160, screenWidthDp - 108);
        float titleWidthDp =
                heading.getPaint().measureText(title) / getResources().getDisplayMetrics().density;
        if (titleWidthDp > availableWidthDp) {
            heading.setTextSize(Math.max(17, 23 * availableWidthDp / titleWidthDp));
        }
        heading.setPadding(dp(12), 0, 0, 0);
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private TextView addButton(
            LinearLayout parent, String label, boolean primary, Runnable action, int top) {
        TextView button = text(label, 15, primary ? WHITE : GREEN, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(49));
        params.topMargin = dp(top);
        parent.addView(button, params);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void openCollectionForm(Long collectionId) {
        if (collectionId == null && screen != Screen.COLLECTION) collectionReturnScreen = screen;
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

    private LinearLayout addInfoBanner(LinearLayout parent, String label, int top) {
        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(14), dp(10), dp(14), dp(10));
        banner.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 15));
        TextView info = text("ⓘ", 18, GREEN, false);
        banner.addView(info);
        TextView content = text(label, 13, TEXT, true);
        content.setPadding(dp(12), 0, 0, 0);
        banner.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(top);
        params.bottomMargin = dp(12);
        parent.addView(banner, params);
        return banner;
    }

    private void addEmptyCard(LinearLayout parent, String title, String subtitle, Runnable action) {
        LinearLayout empty = vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(24), dp(20), dp(24));
        empty.setBackground(roundRect(WHITE, BORDER, 15));
        empty.setFocusable(true);
        empty.setContentDescription(title + ". " + subtitle);
        empty.setOnClickListener(v -> action.run());
        empty.addView(text(title, 15, TEXT, true));
        empty.addView(text(subtitle, 13, MUTED, false));
        parent.addView(empty, new LinearLayout.LayoutParams(-1, -2));
    }

    private ScrollView scrollContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        return scroll;
    }

    private EditText editField(String value, int textSize, int minHeight) {
        EditText field = new EditText(this);
        field.setText(value);
        field.setTextSize(textSize);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        field.setSingleLine(false);
        field.setBackground(roundRect(WHITE, BORDER, 14));
        field.setPadding(dp(15), dp(10), dp(15), dp(10));
        if (minHeight > 0) field.setMinHeight(dp(minHeight));
        return field;
    }

    private ImageView recipeImage(Recipe recipe) {
        ImageView image = new ImageView(this);
        int resource = recipeDrawable(recipe.id);
        image.setImageResource(resource);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(roundRect(BORDER, BORDER, 12));
        image.setClipToOutline(true);
        return image;
    }

    private int recipeDrawable(String recipeId) {
        switch (recipeId) {
            case "sample-canh-chua-ca":
                return R.drawable.recipe_canh_chua;
            case "sample-ga-chien-nuoc-mam":
                return R.drawable.recipe_ga_chien;
            case "sample-mi-y-sot-bo-bam":
                return R.drawable.recipe_mi_y;
            case "sample-com-rang-dua-bo":
                return R.drawable.recipe_com_rang;
            case "sample-salad-rau-cu":
                return R.drawable.recipe_salad;
            default:
                return R.drawable.recipe_canh_chua;
        }
    }

    private ImageView iconView(int resourceId, int tint) {
        ImageView image = new ImageView(this);
        image.setImageResource(resourceId);
        image.setColorFilter(tint);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return image;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private void addText(
            LinearLayout parent,
            String value,
            int size,
            int color,
            boolean bold,
            int left,
            int top,
            int right,
            int bottom) {
        TextView text = text(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        parent.addView(text, params);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Recipe recipeById(String id) {
        for (Recipe recipe : PreviewRecipeCatalog.designRecipes())
            if (recipe.id.equals(id)) return recipe;
        return PreviewRecipeCatalog.designRecipes().get(0);
    }
}
