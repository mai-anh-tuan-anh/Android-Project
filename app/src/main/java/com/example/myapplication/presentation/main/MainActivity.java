package com.example.myapplication.presentation.main;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.*;
import com.example.myapplication.domain.model.*;
import com.example.myapplication.presentation.cookingjournal.CookingJournalViewModel;
import com.example.myapplication.presentation.discover.DiscoverSearchViewModel;
import com.example.myapplication.presentation.discover.DiscoverViewModel;
import com.example.myapplication.presentation.mealplan.MealPlanViewModel;
import com.example.myapplication.presentation.pantry.PantryViewModel;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;
import com.example.myapplication.presentation.shopping.ShoppingViewModel;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.BaseInputConnection;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.text.Normalizer;

/** Local personal data, meal planning, shopping, pantry, and cooking journal. */
public class MainActivity extends AppCompatActivity {
    private static final String STATE_APP_SCREEN = "app.currentScreen";
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
    private static final String STATE_PICKER_SCROLL_Y = "module6.pickerScrollY";
    private static final String STATE_PLAN_SCROLL_Y = "module6.planScrollY";
    private static final String STATE_SHOPPING_SCREEN = "module7.screen";
    private static final String STATE_SHOPPING_ITEM_ID = "module7.itemId";
    private static final String STATE_SHOPPING_NAME = "module7.name";
    private static final String STATE_SHOPPING_AMOUNT = "module7.amount";
    private static final String STATE_SHOPPING_UNIT = "module7.unit";
    private static final String STATE_SHOPPING_UNIT_ID = "module7.unitId";
    private static final String STATE_SHOPPING_CATEGORY = "module7.category";
    private static final String STATE_SHOPPING_FILTER = "module7.filter";
    private static final String STATE_SHOPPING_RETURN_SCREEN = "module7.returnScreen";
    private static final String STATE_PANTRY_SCREEN = "pantry.screen";
    private static final String STATE_PANTRY_ID = "pantry.id";
    private static final String STATE_PANTRY_NAME = "pantry.name";
    private static final String STATE_PANTRY_AMOUNT = "pantry.amount";
    private static final String STATE_PANTRY_UNIT = "pantry.unit";
    private static final String STATE_PANTRY_CATEGORY = "pantry.category";
    private static final String STATE_PANTRY_EXPIRY = "pantry.expiry";
    private static final String STATE_PANTRY_LOCATION = "pantry.location";
    private static final String STATE_PANTRY_PURCHASE_DATE = "pantry.purchaseDate";
    private static final String STATE_PANTRY_FILTER = "pantry.filter";
    private static final String STATE_PANTRY_QUERY = "pantry.query";
    private static final String STATE_LOG_SCREEN = "journal.screen";
    private static final String STATE_LOG_RECIPE_ID = "journal.recipeId";
    private static final String STATE_LOG_ID = "journal.id";
    private static final String STATE_LOG_DATE = "journal.date";
    private static final String STATE_LOG_SERVINGS = "journal.servings";
    private static final String STATE_LOG_DURATION = "journal.duration";
    private static final String STATE_LOG_RATING = "journal.rating";
    private static final String STATE_LOG_NOTE = "journal.note";
    private static final String STATE_LOG_PHOTO = "journal.photo";
    private static final String STATE_LOG_RETURN_SCREEN = "journal.returnScreen";
    private static final String STATE_LOG_FORM_RETURN_SCREEN = "journal.formReturnScreen";
    private static final String STATE_LOG_SEARCH = "journal.search";
    private static final String STATE_LOG_FROM_DATE = "journal.fromDate";
    private static final String STATE_LOG_TO_DATE = "journal.toDate";
    private static final String STATE_LOG_FILTER_RATING = "journal.filterRating";
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
    private CookingJournalViewModel cookingJournalViewModel;
    private DiscoverSearchViewModel searchViewModel;
    private final DiscoverViewModel discoverViewModel = new DiscoverViewModel();
    private ActivityResultLauncher<String> cookingPhotoPicker;
    private LinearLayout appRoot;
    private View bottomNavigationBar;
    private Screen screen = Screen.DISCOVER;
    private boolean animateNextScreen;
    private Screen noteReturnScreen = Screen.LIBRARY;
    private Screen collectionReturnScreen = Screen.LIBRARY;
    private Screen detailReturnScreen = Screen.DISCOVER;
    private String detailRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private String selectedCategory = "";
    private String discoverSearchQuery = "";
    private long selectedCollectionId = -1L;
    private String noteRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private String favoritesSearchQuery = "";
    private String collectionDetailSearchQuery = "";
    private long collectionDetailSearchOwnerId = -1L;
    private String collectionFormSearchQuery = "";
    private EditText collectionNameInput;
    private TextView collectionError;
    private TextView collectionSaveButton;
    private LinearLayout planPickerRows;
    private LinearLayout collectionRecipeResults;
    private LinearLayout collectionFormRecipeRows;
    private TextView planPickerSourceButton;
    private EditText planPickerSearchInput;
    private ScrollView planPickerScrollView;
    private ScrollView mealPlanScrollView;
    private int restoredPickerScrollY;
    private int restoredMealPlanScrollY;
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
    private String editingPantryItemId;
    private String pantryDraftName = "";
    private String pantryDraftAmount = "";
    private String pantryDraftUnit = "";
    private String pantryDraftUnitId = "";
    private String pantryDraftCategory = ShoppingCategoryCatalog.OTHER;
    private String pantryDraftExpiry = "";
    private String pantryDraftLocation = "";
    private String pantryDraftPurchaseDate = "";
    private String pantryFilter = "ALL";
    private String pantrySearchQuery = "";
    private EditText pantryNameInput;
    private EditText pantryAmountInput;
    private EditText pantryUnitInput;
    private EditText pantryLocationInput;
    private TextView pantryCategoryInput;
    private TextView pantryExpiryInput;
    private TextView pantryPurchaseDateInput;
    private final Handler pantryExpiryHandler = new Handler(Looper.getMainLooper());
    private Runnable pantryExpiryRefresh;
    private Screen cookingLogReturnScreen = Screen.NOTE;
    private Screen cookingLogFormReturnScreen = Screen.COOKING_LOGS;
    private String cookingLogRecipeId;
    private String editingCookingLogId;
    private long cookingLogCookedAt = System.currentTimeMillis();
    private int cookingLogServings = 1;
    private String cookingLogDuration = "";
    private Integer cookingLogRating;
    private String cookingLogAdjustment = "";
    private String cookingLogPhotoPath = "";
    private String originalCookingLogPhotoPath = "";
    private EditText cookingLogServingsInput;
    private EditText cookingLogDurationInput;
    private EditText cookingLogAdjustmentInput;
    private TextView cookingLogDateInput;
    private LinearLayout cookingLogRatingRow;
    private TextView cookingLogPhotoButton;
    private String cookingJournalSearch = "";
    private long cookingJournalFromDate;
    private long cookingJournalToDate;
    private int cookingJournalRatingFilter;
    private List<CookingLogEntry> cookingJournalAllLogs = new ArrayList<>();

    public enum Screen { DISCOVER, CATEGORY_LIST, CATEGORY_RECIPES, NEW_RECIPES, SEARCH, SEARCH_FILTER, SEARCH_RESULTS,
        WHAT_TO_COOK_INGREDIENTS, WHAT_TO_COOK_RESULTS, RECIPE_DETAIL,
        LIBRARY, FAVORITES, COLLECTION_LIST, COLLECTION, COLLECTION_FORM, NOTE, MEAL_PLAN,
        MEAL_PLAN_PICKER, SHOPPING, SHOPPING_ITEM_FORM, PANTRY, PANTRY_FORM, COOKING_JOURNAL,
        COOKING_LOGS, COOKING_LOG_FORM }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (screen == Screen.DISCOVER) {
                    finish();
                    return;
                } else if (screen == Screen.SEARCH_FILTER || screen == Screen.SEARCH_RESULTS) {
                    screen = Screen.SEARCH;
                } else if (screen == Screen.WHAT_TO_COOK_RESULTS) {
                    screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                } else if (screen == Screen.CATEGORY_RECIPES || screen == Screen.NEW_RECIPES
                        || screen == Screen.CATEGORY_LIST || screen == Screen.SEARCH
                        || screen == Screen.WHAT_TO_COOK_INGREDIENTS) {
                    screen = Screen.DISCOVER;
                } else if (screen == Screen.CATEGORY_RECIPES) {
                    screen = Screen.CATEGORY_LIST;
                } else if (screen == Screen.RECIPE_DETAIL) {
                    screen = detailReturnScreen == Screen.RECIPE_DETAIL ? Screen.DISCOVER : detailReturnScreen;
                } else if (screen == Screen.PANTRY_FORM) {
                    screen = Screen.PANTRY;
                } else if (screen == Screen.PANTRY) {
                    screen = pantryReturnScreen;
                } else if (screen == Screen.COOKING_LOG_FORM) {
                    discardCookingLogDraftPhoto();
                    screen = cookingLogFormReturnScreen;
                } else if (screen == Screen.COOKING_JOURNAL) {
                    clearCookingJournalDateRange();
                    screen = cookingLogReturnScreen;
                } else if (screen == Screen.COOKING_LOGS) {
                    screen = cookingLogReturnScreen;
                } else if (screen == Screen.COLLECTION_FORM) {
                    screen = selectedCollectionId > 0 ? Screen.COLLECTION : collectionReturnScreen;
                } else if (screen == Screen.COLLECTION) {
                    screen = collectionReturnScreen;
                } else if (screen == Screen.NOTE) {
                    screen = noteReturnScreen;
                } else if (screen == Screen.MEAL_PLAN_PICKER) {
                    screen = Screen.MEAL_PLAN;
                } else if (screen == Screen.SHOPPING_ITEM_FORM) {
                    screen = Screen.SHOPPING;
                } else if (screen == Screen.SHOPPING) {
                    screen = shoppingReturnScreen == Screen.SHOPPING ? Screen.DISCOVER : shoppingReturnScreen;
                } else if (screen == Screen.MEAL_PLAN || screen == Screen.LIBRARY) {
                    screen = Screen.DISCOVER;
                } else {
                    screen = Screen.DISCOVER;
                }
                render();
            }
        });
        AppViewModelFactory viewModelFactory = new AppViewModelFactory((BepNhaApplication) getApplication());
        personalDataViewModel = new ViewModelProvider(this, viewModelFactory).get(PersonalDataViewModel.class);
        mealPlanViewModel = new ViewModelProvider(this, viewModelFactory).get(MealPlanViewModel.class);
        shoppingViewModel = new ViewModelProvider(this, viewModelFactory).get(ShoppingViewModel.class);
        pantryViewModel = new ViewModelProvider(this, viewModelFactory).get(PantryViewModel.class);
        cookingJournalViewModel = new ViewModelProvider(this, viewModelFactory).get(CookingJournalViewModel.class);
        searchViewModel = new ViewModelProvider(this, viewModelFactory).get(DiscoverSearchViewModel.class);
        cookingPhotoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || screen != Screen.COOKING_LOG_FORM) return;
            PhotoSaveResult result = cookingJournalViewModel.savePhoto(uri.toString());
            String copied = result.filename;
            if (copied != null) {
                if (!originalCookingLogPhotoPath.equals(cookingLogPhotoPath)) cookingJournalViewModel.deletePhoto(cookingLogPhotoPath);
                cookingLogPhotoPath = copied;
                render();
            } else {
                Toast.makeText(this, result.tooLarge ? "Ảnh cần nhỏ hơn 10 MB." : "Không thể lưu ảnh đã chọn.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        personalDataViewModel.seedDesignPreviewIfNeeded(PreviewRecipeCatalog.designRecipes());
        restoreAppState(savedInstanceState);
        restoreMealPlanState(savedInstanceState);
        restoreOfflineFeatureState(savedInstanceState);
        render();
    }

    private void restoreAppState(Bundle state) {
        if (state == null) return;
        selectedCategory = state.getString(STATE_SELECTED_CATEGORY, "");
        discoverSearchQuery = state.getString(STATE_DISCOVER_SEARCH_QUERY, "");
        detailRecipeId = state.getString(STATE_DETAIL_RECIPE_ID, PreviewRecipeCatalog.designRecipes().get(0).id);
        String savedDetailReturn = state.getString(STATE_DETAIL_RETURN_SCREEN, Screen.DISCOVER.name());
        try {
            detailReturnScreen = Screen.valueOf(savedDetailReturn);
        } catch (IllegalArgumentException e) {
            detailReturnScreen = Screen.DISCOVER;
        }
        String savedScreen = state.getString(STATE_APP_SCREEN);
        if (savedScreen != null) {
            try {
                screen = Screen.valueOf(savedScreen);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen == Screen.PANTRY && appRoot != null) render();
    }

    @Override
    protected void onPause() {
        if (pantryExpiryRefresh != null) pantryExpiryHandler.removeCallbacks(pantryExpiryRefresh);
        super.onPause();
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
            shoppingDraftCategory = state.getString(STATE_SHOPPING_CATEGORY, ShoppingCategoryCatalog.OTHER);
            String restoredShoppingFilter = state.getString(STATE_SHOPPING_FILTER);
            shoppingStatusFilter = ShoppingViewModel.AVAILABLE.equals(restoredShoppingFilter)
                    ? ShoppingViewModel.AVAILABLE : ShoppingViewModel.NEED_TO_BUY;
            try {
                shoppingReturnScreen = Screen.valueOf(state.getString(STATE_SHOPPING_RETURN_SCREEN,
                        Screen.MEAL_PLAN.name()));
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
            String savedSourceName = state.getString(STATE_PICKER_SOURCE,
                    MealPlanViewModel.PickerSource.ALL.name());
            try {
                savedSource = MealPlanViewModel.PickerSource.valueOf(savedSourceName == null
                        ? MealPlanViewModel.PickerSource.ALL.name() : savedSourceName);
            } catch (IllegalArgumentException invalidSource) {
                savedSource = MealPlanViewModel.PickerSource.ALL;
            }
            mealPlanViewModel.restorePickerDraft(savedIds,
                    state.getInt(STATE_PICKER_SERVINGS, MealPlanViewModel.MIN_SERVINGS),
                    state.getString(STATE_SEARCH_QUERY, ""), savedSource,
                    state.getLong(STATE_PICKER_COLLECTION_ID, -1L));
            restoredPickerScrollY = Math.max(0, state.getInt(STATE_PICKER_SCROLL_Y, 0));
        } else {
            restoredMealPlanScrollY = Math.max(0, state.getInt(STATE_PLAN_SCROLL_Y, 0));
        }
    }

    private void restoreOfflineFeatureState(Bundle state) {
        if (state == null) return;
        String savedPantryScreen = state.getString(STATE_PANTRY_SCREEN);
        if (Screen.PANTRY.name().equals(savedPantryScreen) || Screen.PANTRY_FORM.name().equals(savedPantryScreen)) {
            screen = Screen.valueOf(savedPantryScreen);
            editingPantryItemId = state.getString(STATE_PANTRY_ID);
            pantryDraftName = state.getString(STATE_PANTRY_NAME, "");
            pantryDraftAmount = state.getString(STATE_PANTRY_AMOUNT, "");
            pantryDraftUnit = state.getString(STATE_PANTRY_UNIT, "");
            pantryDraftCategory = state.getString(STATE_PANTRY_CATEGORY, ShoppingCategoryCatalog.OTHER);
            pantryDraftExpiry = state.getString(STATE_PANTRY_EXPIRY, "");
            pantryDraftLocation = state.getString(STATE_PANTRY_LOCATION, "");
            pantryDraftPurchaseDate = state.getString(STATE_PANTRY_PURCHASE_DATE, "");
            pantryFilter = state.getString(STATE_PANTRY_FILTER, "ALL");
            pantrySearchQuery = state.getString(STATE_PANTRY_QUERY, "");
            pantryReturnScreen = Screen.SHOPPING;
        }
        String savedLogScreen = state.getString(STATE_LOG_SCREEN);
        if (Screen.COOKING_JOURNAL.name().equals(savedLogScreen)
                || Screen.COOKING_LOGS.name().equals(savedLogScreen)
                || Screen.COOKING_LOG_FORM.name().equals(savedLogScreen)) {
            screen = Screen.valueOf(savedLogScreen);
            cookingLogRecipeId = state.getString(STATE_LOG_RECIPE_ID);
            editingCookingLogId = state.getString(STATE_LOG_ID);
            cookingLogCookedAt = state.getLong(STATE_LOG_DATE, System.currentTimeMillis());
            cookingLogServings = Math.max(1, Math.min(999, state.getInt(STATE_LOG_SERVINGS, 1)));
            cookingLogDuration = state.getString(STATE_LOG_DURATION, "");
            int rating = state.getInt(STATE_LOG_RATING, 0);
            cookingLogRating = rating >= 1 && rating <= 5 ? rating : null;
            cookingLogAdjustment = state.getString(STATE_LOG_NOTE, "");
            cookingLogPhotoPath = state.getString(STATE_LOG_PHOTO, "");
            CookingLogEntry original = cookingJournalViewModel.getLog(editingCookingLogId);
            originalCookingLogPhotoPath = original == null ? "" : original.photoLocalPath;
            String savedReturn = state.getString(STATE_LOG_RETURN_SCREEN, Screen.NOTE.name());
            try { cookingLogReturnScreen = Screen.valueOf(savedReturn); }
            catch (IllegalArgumentException invalid) { cookingLogReturnScreen = Screen.NOTE; }
            String savedFormReturn = state.getString(STATE_LOG_FORM_RETURN_SCREEN, Screen.COOKING_LOGS.name());
            try { cookingLogFormReturnScreen = Screen.valueOf(savedFormReturn); }
            catch (IllegalArgumentException invalid) { cookingLogFormReturnScreen = Screen.COOKING_LOGS; }
            cookingJournalSearch = state.getString(STATE_LOG_SEARCH, "");
            cookingJournalFromDate = state.getLong(STATE_LOG_FROM_DATE, 0L);
            cookingJournalToDate = state.getLong(STATE_LOG_TO_DATE, 0L);
            cookingJournalRatingFilter = state.getInt(STATE_LOG_FILTER_RATING, 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (screen == Screen.MEAL_PLAN || screen == Screen.MEAL_PLAN_PICKER) {
            outState.putString(STATE_SCREEN, screen.name());
            if (screen == Screen.MEAL_PLAN_PICKER) {
                outState.putStringArrayList(STATE_PICKED_RECIPE_IDS,
                        new ArrayList<>(mealPlanViewModel.getSelectedRecipeIds()));
                outState.putInt(STATE_PICKER_SERVINGS, mealPlanViewModel.getPickerServings());
                outState.putString(STATE_PICKER_SOURCE, mealPlanViewModel.getPickerSource().name());
                outState.putLong(STATE_PICKER_COLLECTION_ID, mealPlanViewModel.getPickerCollectionId());
                String query = planPickerSearchInput == null
                        ? mealPlanViewModel.getSearchQuery() : planPickerSearchInput.getText().toString();
                outState.putString(STATE_SEARCH_QUERY, query);
                outState.putInt(STATE_PICKER_SCROLL_Y,
                        planPickerScrollView == null ? 0 : planPickerScrollView.getScrollY());
            } else {
                outState.putInt(STATE_PLAN_SCROLL_Y,
                        mealPlanScrollView == null ? 0 : mealPlanScrollView.getScrollY());
            }
        }
        searchViewModel.setSearchQuery(discoverSearchQuery);
        if (screen == Screen.SHOPPING || screen == Screen.SHOPPING_ITEM_FORM) {
            outState.putString(STATE_SHOPPING_SCREEN, screen.name());
            outState.putString(STATE_SHOPPING_ITEM_ID, editingShoppingItemId);
            outState.putString(STATE_SHOPPING_NAME, shoppingNameInput == null
                    ? shoppingDraftName : shoppingNameInput.getText().toString());
            outState.putString(STATE_SHOPPING_AMOUNT, shoppingAmountInput == null
                    ? shoppingDraftAmount : shoppingAmountInput.getText().toString());
            outState.putString(STATE_SHOPPING_UNIT, shoppingUnitInput == null
                    ? shoppingDraftUnit : shoppingUnitInput.getText().toString());
            outState.putString(STATE_SHOPPING_UNIT_ID, shoppingDraftUnitId);
            outState.putString(STATE_SHOPPING_CATEGORY, shoppingDraftCategory);
            outState.putString(STATE_SHOPPING_FILTER, shoppingStatusFilter);
            outState.putString(STATE_SHOPPING_RETURN_SCREEN, shoppingReturnScreen.name());
        }
        if (screen == Screen.PANTRY || screen == Screen.PANTRY_FORM) {
            outState.putString(STATE_PANTRY_SCREEN, screen.name());
            outState.putString(STATE_PANTRY_ID, editingPantryItemId);
            outState.putString(STATE_PANTRY_NAME, pantryNameInput == null ? pantryDraftName : pantryNameInput.getText().toString());
            outState.putString(STATE_PANTRY_AMOUNT, pantryAmountInput == null ? pantryDraftAmount : pantryAmountInput.getText().toString());
            outState.putString(STATE_PANTRY_UNIT, pantryUnitInput == null ? pantryDraftUnit : pantryUnitInput.getText().toString());
            outState.putString(STATE_PANTRY_CATEGORY, pantryDraftCategory);
            outState.putString(STATE_PANTRY_EXPIRY, pantryDraftExpiry);
            outState.putString(STATE_PANTRY_LOCATION, pantryLocationInput == null ? pantryDraftLocation : pantryLocationInput.getText().toString());
            outState.putString(STATE_PANTRY_PURCHASE_DATE, pantryDraftPurchaseDate);
            outState.putString(STATE_PANTRY_FILTER, pantryFilter);
            outState.putString(STATE_PANTRY_QUERY, pantrySearchQuery);
        }
        if (screen == Screen.COOKING_JOURNAL || screen == Screen.COOKING_LOGS || screen == Screen.COOKING_LOG_FORM) {
            outState.putString(STATE_LOG_SCREEN, screen.name());
            outState.putString(STATE_LOG_RECIPE_ID, cookingLogRecipeId);
            outState.putString(STATE_LOG_ID, editingCookingLogId);
            outState.putLong(STATE_LOG_DATE, cookingLogCookedAt);
            outState.putInt(STATE_LOG_SERVINGS, cookingLogServingsInput == null ? cookingLogServings
                    : parseInteger(cookingLogServingsInput.getText().toString(), cookingLogServings));
            outState.putString(STATE_LOG_DURATION, cookingLogDurationInput == null ? cookingLogDuration
                    : cookingLogDurationInput.getText().toString());
            outState.putInt(STATE_LOG_RATING, cookingLogRating == null ? 0 : cookingLogRating);
            outState.putString(STATE_LOG_NOTE, cookingLogAdjustmentInput == null ? cookingLogAdjustment
                    : cookingLogAdjustmentInput.getText().toString());
            outState.putString(STATE_LOG_PHOTO, cookingLogPhotoPath);
            outState.putString(STATE_LOG_RETURN_SCREEN, cookingLogReturnScreen.name());
            outState.putString(STATE_LOG_FORM_RETURN_SCREEN, cookingLogFormReturnScreen.name());
            outState.putString(STATE_LOG_SEARCH, cookingJournalSearch);
            outState.putLong(STATE_LOG_FROM_DATE, cookingJournalFromDate);
            outState.putLong(STATE_LOG_TO_DATE, cookingJournalToDate);
            outState.putInt(STATE_LOG_FILTER_RATING, cookingJournalRatingFilter);
        }
        outState.putString(STATE_APP_SCREEN, screen.name());
        outState.putString(STATE_SELECTED_CATEGORY, selectedCategory);
        outState.putString(STATE_DISCOVER_SEARCH_QUERY, searchViewModel == null
                ? discoverSearchQuery : searchViewModel.getSearchQuery());
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
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void render() {
        planPickerSearchInput = null;
        planPickerSourceButton = null;
        planPickerScrollView = null;
        mealPlanScrollView = null;
        bottomNavigationBar = null;
        FrameLayout frame = new FrameLayout(this);
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(CREAM);
        frame.addView(appRoot, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout root = appRoot;
        ViewCompat.setOnApplyWindowInsetsListener(frame, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(systemBars.left, systemBars.top, systemBars.right,
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
        else if (screen == Screen.LIBRARY) renderLibrary();
        else if (screen == Screen.FAVORITES) renderFavorites();
        else if (screen == Screen.COLLECTION_LIST) renderCollections();
        else if (screen == Screen.COLLECTION) renderCollection();
        else if (screen == Screen.COLLECTION_FORM) renderCollectionForm();
        else if (screen == Screen.NOTE) renderNote();
        else if (screen == Screen.MEAL_PLAN) renderMealPlan();
        else if (screen == Screen.MEAL_PLAN_PICKER) renderMealPlanPicker();
        else if (screen == Screen.SHOPPING) renderShoppingList();
        else if (screen == Screen.SHOPPING_ITEM_FORM) renderShoppingItemForm();
        else if (screen == Screen.PANTRY) renderPantry();
        else if (screen == Screen.PANTRY_FORM) renderPantryForm();
        else if (screen == Screen.COOKING_JOURNAL) renderAllCookingLogs();
        else if (screen == Screen.COOKING_LOGS) renderCookingLogs();
        else renderCookingLogForm();
        setContentView(frame);
        if (animateNextScreen) {
            animateNextScreen = false;
            frame.setAlpha(0f);
            frame.setTranslationY(dp(6));
            frame.animate().alpha(1f).translationY(0f).setDuration(160L).start();
        }
        ViewCompat.requestApplyInsets(frame);
        if (screen == Screen.MEAL_PLAN && mealPlanScrollView != null && restoredMealPlanScrollY > 0) {
            int scrollY = restoredMealPlanScrollY;
            mealPlanScrollView.post(() -> mealPlanScrollView.scrollTo(0, scrollY));
            restoredMealPlanScrollY = 0;
        } else if (screen == Screen.MEAL_PLAN_PICKER && planPickerScrollView != null
                && restoredPickerScrollY > 0) {
            int scrollY = restoredPickerScrollY;
            planPickerScrollView.post(() -> planPickerScrollView.scrollTo(0, scrollY));
            restoredPickerScrollY = 0;
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

        TextView bannerDesc = text("Gợi ý món ngon từ các nguyên liệu sẵn có trong tủ lạnh của bạn.", 13, TEXT, false);
        bannerDesc.setPadding(0, dp(6), 0, dp(12));
        banner.addView(bannerDesc);

        TextView bannerButton = text("Khám phá ngay ›", 13, GREEN, true);
        banner.addView(bannerButton);
        banner.setOnClickListener(v -> {
            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
            render();
        });
        content.addView(banner, bannerParams);

        addSectionHeader(content, "Danh mục món ăn", "Xem tất cả ›", () -> {
            screen = Screen.CATEGORY_LIST;
            render();
        }, 24);

        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] categories = {"Món Việt", "Món Á", "Món Âu", "Bữa sáng", "Món chay", "Món nhanh", "Dễ làm"};
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

        addSectionHeader(content, "Món mới công bố", "Xem tất cả ›", () -> {
            screen = Screen.NEW_RECIPES;
            render();
        }, 24);
        List<Recipe> newRecipes = discoverViewModel.newlyPublished(
                PreviewRecipeCatalog.designRecipes(), recipe -> recipe.publishedAtMillis);
        if (newRecipes.isEmpty()) {
            addText(content, "Chưa có món mới công bố.", 13, MUTED, false, 0, 0, 0, 0);
        } else {
            addRecipeGrid(content, newRecipes, true, true);
        }

        addSectionHeader(content, "Món nhanh & Dễ làm", "", () -> {}, 24);
        List<Recipe> quickRecipes = discoverViewModel.quickAndEasy(
                PreviewRecipeCatalog.designRecipes(), recipe -> recipe.durationMinutes, recipe -> recipe.difficulty);
        if (quickRecipes.isEmpty()) addText(content, "Chưa có món vừa nhanh vừa dễ.", 13, MUTED, false, 0, 0, 0, 0);
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
            row.addView(createCategoryTile(categories[i][0], categories[i][1]),
                    new LinearLayout.LayoutParams(0, -2, 1));
            View gap = new View(this);
            row.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
            if (i + 1 < categories.length) {
                row.addView(createCategoryTile(categories[i + 1][0], categories[i + 1][1]),
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
        List<Recipe> matches = discoverViewModel.recipesInCategory(
                PreviewRecipeCatalog.designRecipes(), selectedCategory, recipe -> recipe.categories);
        if (matches.isEmpty()) addText(page, "Danh mục này hiện chưa có món nào.", 14, MUTED, false, 0, 8, 0, 0);
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
        List<Recipe> newRecipes = discoverViewModel.newlyPublished(
                PreviewRecipeCatalog.designRecipes(), recipe -> recipe.publishedAtMillis);
        if (newRecipes.isEmpty()) {
            addEmptyCard(page, "Chưa có món mới công bố",
                    "Món sẽ xuất hiện tại đây sau khi kho công thức cung cấp thời điểm công bố.", () -> {
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
        searchInput.setHint("Tìm công thức, nguyên liệu...");
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(10), 0, dp(10), 0);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView clearIcon = iconView(R.drawable.ic_close_outline, MUTED);
        clearIcon.setPadding(dp(4), dp(4), dp(4), dp(4));
        clearIcon.setVisibility(searchViewModel.getSearchQuery().isEmpty() ? View.GONE : View.VISIBLE);
        clearIcon.setOnClickListener(v -> {
            searchViewModel.setSearchQuery("");
            searchInput.setText("");
            render();
        });
        searchBox.addView(clearIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        page.addView(searchBox, new LinearLayout.LayoutParams(-1, -2));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
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
            chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
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
            chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
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
            chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? GREEN : BORDER, 16));
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
        incInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchViewModel.setDraftIncludeIngredient(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        page.addView(incInput, new LinearLayout.LayoutParams(-1, -2));

        addText(page, "Nguyên liệu cần loại trừ", 14, TEXT, true, 0, 16, 0, 8);
        EditText excInput = editField(searchViewModel.getDraftExcludeIngredient(), 14, 46);
        excInput.setHint("Ví dụ: Đậu phộng, hải sản...");
        excInput.setBackground(roundRect(WHITE, BORDER, 14));
        excInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        excInput.setSingleLine(true);
        excInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchViewModel.setDraftExcludeIngredient(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
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
        clearIcon.setVisibility(searchViewModel.getSearchQuery().isEmpty() ? View.GONE : View.VISIBLE);
        clearIcon.setOnClickListener(v -> {
            searchViewModel.setSearchQuery("");
            searchInput.setText("");
            render();
        });
        searchBox.addView(clearIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
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
            iconBg.addView(searchEmptyIcon, new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER));
            empty.addView(iconBg, new LinearLayout.LayoutParams(dp(72), dp(72)));

            TextView emptyTitle = text("Không tìm thấy món", 20, TEXT, true);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(-1, -2);
            etParams.topMargin = dp(20);
            empty.addView(emptyTitle, etParams);

            TextView emptyDesc = text("Thử từ khóa khác hoặc bỏ bớt bộ lọc để mở rộng kết quả.", 14, MUTED, false);
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
        addText(page, "Chọn nguyên liệu bạn đang có để nhận gợi ý món ăn.", 14, MUTED, false, 0, 0, 0, 16);

        String[][] ingredientGroups = {
                {"Thịt / Hải sản", "Thịt bò", "Thịt lợn", "Thịt gà", "Cá basa"},
                {"Rau củ", "Cà chua", "Dứa", "Đậu bắp", "Giá đỗ", "Xà lách", "Dưa chuột", "Hành tây", "Hành lá"},
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
        addText(page, selectedIngredients.isEmpty() ? "Chưa chọn nguyên liệu"
                : "Đã chọn " + selectedIngredients.size() + " nguyên liệu", 13, MUTED, false, 0, 12, 0, 0);
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
        TextView availableBadge = text("✓ Đã có " + availableCount + "/" + totalCount, 11, GREEN, true);
        availableBadge.setPadding(dp(8), dp(5), dp(8), dp(5));
        availableBadge.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 12));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2);
        badgeLp.topMargin = dp(8);
        content.addView(availableBadge, badgeLp);

        TextView missingBadge = text(missing.isEmpty() ? "Đủ nguyên liệu"
                : "Thiếu " + missing.size() + " nguyên liệu", 11, missing.isEmpty() ? GREEN : TEXT, true);
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
            addEmptyCard(page, "Chưa chọn nguyên liệu",
                    "Quay lại để chọn ít nhất một nguyên liệu bạn đang có.", () -> {
                        screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                        render();
                    });
        } else {
            List<Recipe> matches = searchViewModel.suggestRecipes(PreviewRecipeCatalog.designRecipes());
            if (!searchViewModel.hasCompleteRecipeIngredients(PreviewRecipeCatalog.designRecipes())) {
                addEmptyCard(page, "Chưa có đủ dữ liệu nguyên liệu",
                        "Gợi ý sẽ xuất hiện khi kho công thức cung cấp đầy đủ nguyên liệu cho món.", () -> {
                            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                            render();
                        });
            } else if (matches.isEmpty()) {
                addEmptyCard(page, "Không có món phù hợp",
                        "Thử chọn thêm nguyên liệu để tìm được nhiều món hơn.", () -> {
                            screen = Screen.WHAT_TO_COOK_INGREDIENTS;
                            render();
                        });
            } else {
                addText(page, "Món đủ nguyên liệu đứng trước; các món còn lại xếp theo số nguyên liệu thiếu.", 13, MUTED, false, 0, 0, 0, 16);
                if (searchViewModel.hasIncompleteRecipeIngredients(PreviewRecipeCatalog.designRecipes())) {
                    addText(page, "Một số món chưa đủ dữ liệu nguyên liệu nên chưa được xét.", 12, MUTED, false, 0, 0, 0, 12);
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
        ImageView star = iconView(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline, GREEN);
        star.setBackground(roundRect(WHITE, WHITE, 18));
        star.setPadding(dp(8), dp(8), dp(8), dp(8));
        FrameLayout.LayoutParams starParams = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.END | Gravity.TOP);
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
        TextView metaText = text("⏱ " + recipe.duration + "   •   📊 Độ khó: " + recipe.difficulty + "   •   👥 2-4 người", 13, TEXT, false);
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
        addText(ingCard, "Định lượng nguyên liệu chi tiết theo khẩu phần (Module 3 của Người 2 đang hoàn thiện).", 13, MUTED, false, 0, 0, 0, 0);
        page.addView(ingCard, new LinearLayout.LayoutParams(-1, -2));

        addSectionHeader(page, "Các bước nấu", "", () -> {}, 24);
        LinearLayout stepCard = vertical();
        stepCard.setBackground(roundRect(WHITE, BORDER, 14));
        stepCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addText(stepCard, "Hướng dẫn từng bước kèm hẹn giờ nấu (Module 4 của Người 2 đang hoàn thiện).", 13, MUTED, false, 0, 0, 0, 0);
        page.addView(stepCard, new LinearLayout.LayoutParams(-1, -2));

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void renderLibrary() {
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(content);

        addText(content, "Kho của tôi", 23, TEXT, true, 0, 0, 0, 0);
        addText(content, "Các món và ghi chú lưu trên thiết bị.", 14, MUTED, false, 0, 22, 0, 0);

        addSectionHeader(content, "Yêu thích", "Xem tất cả ›", () -> {
            screen = Screen.FAVORITES;
            render();
        }, 30);
        List<Recipe> favoriteRows = personalDataViewModel.getFavorites();
        if (favoriteRows.isEmpty()) addEmptyFavoriteCard(content);
        else addRecipeGrid(content, favoriteRows.subList(0, Math.min(2, favoriteRows.size())), true, true);

        addSectionHeader(content, "Bộ sưu tập", "Xem tất cả ›", () -> {
            screen = Screen.COLLECTION_LIST;
            render();
        }, 14);
        List<RecipeCollection> collections = personalDataViewModel.getCollections();
        if (collections.isEmpty()) addEmptyCard(content, "Chưa có bộ sưu tập",
                "Tạo bộ sưu tập đầu tiên của bạn.", () -> openCollectionForm(null));
        else {
            for (int i = 0; i < Math.min(5, collections.size()); i++) addCollectionRow(content, collections.get(i), 0);
        }

        addText(content, "Đã xem gần đây", 19, TEXT, true, 0, 24, 0, 12);
        List<Recipe> history = personalDataViewModel.getHistory();
        for (int i = 0; i < Math.min(10, history.size()); i++) addHistoryRow(content, history.get(i));

        addSectionHeader(content, "Nhật ký nấu", "Xem tất cả ›", () -> openAllCookingLogs(Screen.LIBRARY), 24);
        addText(content, cookingJournalViewModel.getAllLogs().size() + " lần nấu đã ghi", 13, MUTED,
                false, 0, 0, 0, 8);

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(3);
    }

    private void renderFavorites() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Yêu thích");
        addText(page, "Những công thức bạn đã lưu.", 14, MUTED, false, 0, 0, 0, 16);
        List<Recipe> favorites = personalDataViewModel.getFavorites();
        LinearLayout content = vertical();
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        if (!favorites.isEmpty()) {
            EditText search = editField(favoritesSearchQuery, 16, 49);
            search.setInputType(InputType.TYPE_CLASS_TEXT);
            search.setSingleLine(true);
            search.setHint("Tìm món yêu thích");
            search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            search.setBackgroundColor(Color.TRANSPARENT);
            search.setPadding(dp(48), dp(10), dp(12), dp(10));
            FrameLayout searchBox = new FrameLayout(this);
            searchBox.setBackground(roundRect(WHITE, BORDER, 16));
            ImageView searchIcon = iconView(R.drawable.ic_search_outline, GREEN);
            searchIcon.setPadding(dp(12), dp(12), dp(12), dp(12));
            searchBox.addView(searchIcon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
            searchBox.addView(search, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
            searchParams.bottomMargin = dp(18);
            page.addView(searchBox, searchParams);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    favoritesSearchQuery = s.toString();
                    populateFavoriteResults(content, favorites);
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }
        ScrollView scroll = scrollContent();
        scroll.addView(content);
        if (favorites.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            ImageView star = iconView(R.drawable.ic_star_outline, GREEN);
            star.setPadding(dp(24), dp(24), dp(24), dp(24));
            star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(star, new LinearLayout.LayoutParams(dp(84), dp(84)));
            TextView emptyTitle = text("Chưa lưu món nào", 22, TEXT, true);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams emptyTitleParams = new LinearLayout.LayoutParams(-1, -2);
            emptyTitleParams.topMargin = dp(18);
            emptyTitleParams.bottomMargin = dp(8);
            empty.addView(emptyTitle, emptyTitleParams);
            TextView hint = text("Lưu một công thức để tìm lại nhanh hơn.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
            hintParams.bottomMargin = dp(16);
            empty.addView(hint, hintParams);
            TextView explore = text("Khám phá món", 15, WHITE, true);
            explore.setGravity(Gravity.CENTER);
            explore.setBackground(roundRect(GREEN, GREEN, 14));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(49));
            buttonParams.setMargins(dp(24), dp(16), dp(24), 0);
            empty.addView(explore, buttonParams);
        } else {
            populateFavoriteResults(content, favorites);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void populateFavoriteResults(LinearLayout content, List<Recipe> favorites) {
        content.removeAllViews();
        String query = normalizeRecipeSearch(favoritesSearchQuery);
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : favorites) {
            if (query.isEmpty() || normalizeRecipeSearch(recipe.title).contains(query)) matches.add(recipe);
        }
        if (matches.isEmpty()) {
            TextView noResults = text("Không tìm thấy món yêu thích phù hợp.", 14, MUTED, false);
            noResults.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(96));
            content.addView(noResults, params);
            return;
        }
        addRecipeGrid(content, matches, true, true);
        addInfoBanner(content, "Chạm biểu tượng lưu để bỏ yêu thích.", 22);
    }

    private static String normalizeRecipeSearch(String value) {
        String decomposed = java.text.Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT),
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

    private void populateCollectionRecipeResults(LinearLayout results, List<Recipe> recipes) {
        if (results == null) return;
        results.removeAllViews();
        String query = normalizeRecipeSearch(collectionDetailSearchQuery);
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (query.isEmpty() || normalizeRecipeSearch(recipe.title).contains(query)) matches.add(recipe);
        }
        if (matches.isEmpty()) {
            TextView noResults = text("Không tìm thấy món trong bộ sưu tập.", 14, MUTED, false);
            noResults.setGravity(Gravity.CENTER);
            results.addView(noResults, new LinearLayout.LayoutParams(-1, dp(80)));
            return;
        }
        addRecipeGrid(results, matches, true, true);
    }

    private void populateCollectionFormRecipeRows(LinearLayout rows) {
        if (rows == null) return;
        rows.removeAllViews();
        String query = normalizeRecipeSearch(collectionFormSearchQuery);
        int matches = 0;
        for (Recipe recipe : PreviewRecipeCatalog.designRecipes()) {
            if (query.isEmpty() || normalizeRecipeSearch(recipe.title).contains(query)) {
                addSelectableRecipeRow(rows, recipe);
                matches++;
            }
        }
        if (matches == 0) {
            TextView noResults = text("Không tìm thấy món phù hợp.", 14, MUTED, false);
            noResults.setGravity(Gravity.CENTER);
            rows.addView(noResults, new LinearLayout.LayoutParams(-1, dp(72)));
        }
    }

    private void renderCollections() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Bộ sưu tập");
        addText(page, "Các bộ sưu tập bạn đã tạo.", 14, MUTED, false, 0, 0, 0, 20);

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        scroll.addView(content);
        List<RecipeCollection> collections = personalDataViewModel.getCollections();
        if (collections.isEmpty()) {
            addEmptyCard(content, "Chưa có bộ sưu tập", "Tạo bộ sưu tập đầu tiên của bạn.",
                    () -> openCollectionForm(null));
        } else {
            for (RecipeCollection collection : collections) addCollectionRow(content, collection, 0);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "+     Tạo bộ sưu tập", true, () -> openCollectionForm(null), 18);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderMealPlan() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(16));
        TextView heading = text("Nấu hôm nay", 28, TEXT, true);
        page.addView(heading);

        List<PlannedRecipe> planned = mealPlanViewModel.getMealPlanRecipes();
        if (!planned.isEmpty()) {
            LinearLayout summary = vertical();
            summary.setPadding(dp(14), dp(12), dp(14), dp(12));
            summary.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 16));
            summary.addView(text(planned.size() + " món cho bữa cơm nhà.", 15, TEXT, true));
            MealPlanViewModel.MealDurationEstimate estimate = mealPlanViewModel.estimateTotalDuration(planned);
            String durationLabel;
            if (estimate.unknownRecipes == 0) {
                durationLabel = "Tổng thời gian ước tính: " + estimate.knownMinutes + " phút";
            } else if (estimate.knownRecipes > 0) {
                durationLabel = "Ít nhất " + estimate.knownMinutes + " phút · "
                        + estimate.unknownRecipes + " món chưa có thời lượng";
            } else {
                durationLabel = "Chưa đủ dữ liệu thời lượng để ước tính";
            }
            TextView duration = text(durationLabel, 13, GREEN, true);
            LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(-1, -2);
            durationParams.topMargin = dp(5);
            summary.addView(duration, durationParams);
            TextView estimateNote = text("Cộng thời lượng từng món; chưa tính việc nấu song song.", 11, MUTED, false);
            LinearLayout.LayoutParams estimateNoteParams = new LinearLayout.LayoutParams(-1, -2);
            estimateNoteParams.topMargin = dp(3);
            summary.addView(estimateNote, estimateNoteParams);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
            summaryParams.topMargin = dp(18);
            summaryParams.bottomMargin = dp(22);
            page.addView(summary, summaryParams);
        }

        ScrollView scroll = scrollContent();
        mealPlanScrollView = scroll;
        if (planned.isEmpty()) scroll.setFillViewport(true);
        LinearLayout content = vertical();
        scroll.addView(content);
        if (planned.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            content.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(-1, -2);
            emptyParams.topMargin = dp(144);
            content.addView(empty, emptyParams);
            ImageView icon = iconView(R.drawable.ic_calendar_outline, GREEN);
            icon.setPadding(dp(24), dp(24), dp(24), dp(24));
            icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(icon, new LinearLayout.LayoutParams(dp(84), dp(84)));
            TextView title = text("Bạn chưa chọn món", 22, TEXT, true);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
            titleParams.topMargin = dp(18);
            titleParams.bottomMargin = dp(8);
            empty.addView(title, titleParams);
            TextView hint = text("Thêm món và đặt khẩu phần để chuẩn bị bữa ăn.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            hint.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
            hintParams.bottomMargin = dp(20);
            empty.addView(hint, hintParams);
            TextView choose = primaryButton("Chọn món", () -> openMealPlanPicker());
            empty.addView(choose, new LinearLayout.LayoutParams(-1, dp(49)));
        } else {
            for (int i = 0; i < planned.size(); i++) addPlannedRecipeCard(content, planned.get(i), i, planned.size());
            TextView addMore = text("＋     Thêm món khác", 16, GREEN, true);
            addMore.setGravity(Gravity.CENTER);
            addMore.setBackground(roundRect(WHITE, BORDER, 15));
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(52));
            addParams.topMargin = dp(14);
            addParams.bottomMargin = dp(14);
            content.addView(addMore, addParams);
            addMore.setOnClickListener(v -> openMealPlanPicker());
        }

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (!planned.isEmpty()) {
            FrameLayout shopping = new FrameLayout(this);
            shopping.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
            ImageView cart = iconView(R.drawable.ic_cart_outline, GREEN);
            cart.setPadding(dp(14), dp(10), dp(10), dp(10));
            shopping.addView(cart, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START | Gravity.CENTER_VERTICAL));
            TextView shoppingLabel = text("Tạo danh sách mua sắm", 15, GREEN, true);
            shoppingLabel.setGravity(Gravity.CENTER);
            shopping.addView(shoppingLabel, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout.LayoutParams shoppingParams = new LinearLayout.LayoutParams(-1, dp(48));
            shoppingParams.topMargin = dp(10);
            page.addView(shopping, shoppingParams);
            shopping.setOnClickListener(v -> openShoppingList(Screen.MEAL_PLAN));

            TextView startCooking = primaryButton("Bắt đầu nấu", () -> Toast.makeText(this,
                    "Màn Nấu từng bước sẽ được kết nối khi hoàn thành module 4.", Toast.LENGTH_SHORT).show());
            LinearLayout.LayoutParams cookParams = new LinearLayout.LayoutParams(-1, dp(49));
            cookParams.topMargin = dp(10);
            page.addView(startCooking, cookParams);
        }
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(1);
    }

    private void renderShoppingList() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(20), dp(24), dp(16));
        addShoppingTitleWithPantry(page);

        List<ShoppingListItem> items = shoppingViewModel.getItems();
        if (items.isEmpty()) {
            ScrollView emptyScroll = scrollContent();
            emptyScroll.setFillViewport(true);
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), 0, dp(24), 0);
            emptyScroll.addView(empty, new ScrollView.LayoutParams(-1, -1));
            ImageView icon = iconView(R.drawable.ic_cart_outline, GREEN);
            icon.setPadding(dp(24), dp(24), dp(24), dp(24));
            icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(icon, new LinearLayout.LayoutParams(dp(84), dp(84)));
            TextView title = text("Danh sách đang trống", 22, TEXT, true);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
            titleParams.topMargin = dp(18);
            titleParams.bottomMargin = dp(8);
            empty.addView(title, titleParams);
            TextView hint = text("Chưa có dữ liệu nguyên liệu công thức. Bạn có thể thêm thủ công.",
                    14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            hint.setLineSpacing(dp(3), 1f);
            empty.addView(hint, new LinearLayout.LayoutParams(-1, -2));
            TextView add = primaryButton("Thêm nguyên liệu", () -> openShoppingItemForm(null));
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(49));
            addParams.topMargin = dp(44);
            empty.addView(add, addParams);
            page.addView(emptyScroll, new LinearLayout.LayoutParams(-1, 0, 1));
            appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
            addBottomNavigation(2);
            return;
        }

        TextView summary = text("Danh sách nguyên liệu lưu trên thiết bị.", 14, MUTED, false);
        summary.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.topMargin = dp(14);
        page.addView(summary, summaryParams);

        int availableCount = 0;
        for (ShoppingListItem item : items) {
            if (ShoppingViewModel.AVAILABLE.equals(item.status)) availableCount++;
        }
        android.widget.ProgressBar progress = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, items.size()));
        progress.setProgress(availableCount);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BORDER));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
        progressParams.topMargin = dp(24);
        page.addView(progress, progressParams);
        addText(page, availableCount + "/" + items.size() + " nguyên liệu đã có", 14, MUTED,
                false, 0, 8, 0, 0);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        addShoppingFilterChip(statusRow, "Cần mua", ShoppingViewModel.NEED_TO_BUY);
        addShoppingFilterChip(statusRow, "Đã có", ShoppingViewModel.AVAILABLE);
        HorizontalScrollView statusScroll = new HorizontalScrollView(this);
        statusScroll.setHorizontalScrollBarEnabled(false);
        statusScroll.addView(statusRow, new HorizontalScrollView.LayoutParams(-2, -2));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(44));
        statusParams.topMargin = dp(24);
        statusParams.bottomMargin = dp(20);
        page.addView(statusScroll, statusParams);

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(0, 0, 0, dp(12));
        scroll.addView(content);
        Map<String, List<ShoppingListItem>> groupedItems = new LinkedHashMap<>();
        for (String category : ShoppingCategoryCatalog.ORDER) groupedItems.put(category, new ArrayList<>());
        for (ShoppingListItem item : items) {
            if (shoppingStatusFilter != null && !shoppingStatusFilter.equals(item.status)) continue;
            String category = ShoppingCategoryCatalog.contains(item.category)
                    ? item.category : ShoppingCategoryCatalog.OTHER;
            groupedItems.get(category).add(item);
        }
        int renderedCategoryCount = 0;
        for (Map.Entry<String, List<ShoppingListItem>> group : groupedItems.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            addText(content, group.getKey(), 20, TEXT, true, 0,
                    renderedCategoryCount == 0 ? 0 : 18, 0, 0);
            renderedCategoryCount++;
            for (ShoppingListItem item : group.getValue()) {
                addShoppingItemRow(content, item);
            }
        }
        if (shoppingStatusFilter != null && filteredShoppingCount(items) == 0) {
            TextView noItems = text("Không có nguyên liệu ở trạng thái này.", 14, MUTED, false);
            noItems.setPadding(0, dp(18), 0, 0);
            content.addView(noItems);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(49));
        actionsParams.topMargin = dp(10);
        page.addView(actions, actionsParams);
        TextView add = text("＋   Thêm nguyên liệu", 15, WHITE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(roundRect(GREEN, GREEN, 14));
        actions.addView(add, new LinearLayout.LayoutParams(0, -1, 1.6f));
        add.setOnClickListener(v -> openShoppingItemForm(null));
        TextView clear = text("Xóa toàn bộ", 15, ERROR, true);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(roundRect(WHITE, BORDER, 14));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, -1, 1);
        clearParams.leftMargin = dp(12);
        actions.addView(clear, clearParams);
        clear.setOnClickListener(v -> confirmClearShoppingList(items.size()));
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(2);
    }

    private void schedulePantryExpiryRefresh() {
        if (pantryExpiryRefresh != null) pantryExpiryHandler.removeCallbacks(pantryExpiryRefresh);
        if (screen != Screen.PANTRY) return;
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_YEAR, 1);
        nextDay.set(Calendar.HOUR_OF_DAY, 0);
        nextDay.set(Calendar.MINUTE, 0);
        nextDay.set(Calendar.SECOND, 0);
        nextDay.set(Calendar.MILLISECOND, 0);
        long delay = Math.max(1000L, nextDay.getTimeInMillis() - System.currentTimeMillis());
        pantryExpiryRefresh = () -> {
            pantryExpiryRefresh = null;
            if (screen == Screen.PANTRY) render();
        };
        pantryExpiryHandler.postDelayed(pantryExpiryRefresh, delay);
    }

    private void addShoppingFilterChip(LinearLayout parent, String label, String status) {
        boolean selected = status.equals(shoppingStatusFilter == null
                ? ShoppingViewModel.NEED_TO_BUY : shoppingStatusFilter);
        TextView chip = text(label, 14, selected ? GREEN : MUTED, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(13), 0, dp(13), 0);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE,
                selected ? PALE_GREEN : BORDER, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(42));
        params.rightMargin = dp(8);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            shoppingStatusFilter = status;
            render();
        });
    }

    private int filteredShoppingCount(List<ShoppingListItem> items) {
        int count = 0;
        for (ShoppingListItem item : items) {
            if (shoppingStatusFilter != null && shoppingStatusFilter.equals(item.status)) count++;
        }
        return count;
    }

    private void addShoppingItemRow(LinearLayout parent, ShoppingListItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(WHITE, BORDER, 16));
        LinearLayout details = vertical();
        details.addView(text(item.name, 16, TEXT, true));
        String state = shoppingStatusLabel(item.status);
        TextView amount = text(item.amount + " " + KitchenUnitCatalog.label(item.unit) + " · " + state,
                14, MUTED, false);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, -2);
        amountParams.topMargin = dp(5);
        details.addView(amount, amountParams);
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        boolean available = ShoppingViewModel.AVAILABLE.equals(item.status);
        TextView check = text(available ? "✓" : "", 22, available ? WHITE : GREEN, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(roundRect(available ? GREEN : WHITE, GREEN, 10));
        check.setContentDescription(available ? "Đánh dấu cần mua: " + item.name
                : "Đánh dấu đã có: " + item.name);
        card.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(12);
        parent.addView(card, cardParams);
        card.setOnClickListener(v -> openShoppingItemForm(item.id));
        check.setOnClickListener(v -> {
            String newStatus = available
                    ? ShoppingViewModel.NEED_TO_BUY : ShoppingViewModel.AVAILABLE;
            String previousStatus = item.status;
            shoppingViewModel.setStatus(item.id, newStatus);
            render();
            Snackbar snackbar = Snackbar.make(appRoot,
                    item.name + " · " + shoppingStatusLabel(newStatus), Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác", view -> {
                        shoppingViewModel.setStatus(item.id, previousStatus);
                        render();
                    });
            if (bottomNavigationBar != null) snackbar.setAnchorView(bottomNavigationBar);
            snackbar.show();
        });
        card.setOnLongClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa nguyên liệu?")
                    .setMessage("Xóa “" + item.name + "” khỏi danh sách mua sắm?")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        shoppingViewModel.deleteItem(item.id);
                        render();
                    }).show();
            return true;
        });
    }

    private String shoppingStatusLabel(String status) {
        if (ShoppingViewModel.AVAILABLE.equals(status)) return "Đã có";
        return "Cần mua";
    }

    private void confirmClearShoppingList(int itemCount) {
        if (itemCount <= 0) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa toàn bộ danh sách?")
                .setMessage("Toàn bộ " + itemCount + " nguyên liệu sẽ bị xóa khỏi danh sách mua sắm. Không thể hoàn tác thao tác này.")
                .setNegativeButton("Giữ danh sách", null)
                .setPositiveButton("Xóa toàn bộ", (dialog, which) -> {
                    shoppingViewModel.clearItems();
                    shoppingStatusFilter = ShoppingViewModel.NEED_TO_BUY;
                    render();
                })
                .show();
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
                .setSingleChoiceItems(categories, Math.max(0, selected), (dialog, which) -> {
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
        String canonicalUnit = shoppingDraftUnit.equals(shoppingUnitInput.getText().toString())
                && !shoppingDraftUnitId.isEmpty()
                ? shoppingDraftUnitId : KitchenUnitCatalog.canonicalId(shoppingDraftUnit);
        String status;
        if (editingShoppingItemId == null) {
            status = shoppingViewModel.initialStatusForPantry(null, shoppingDraftName,
                    shoppingDraftAmount, canonicalUnit);
        } else {
            ShoppingListItem existing = shoppingViewModel.findItem(editingShoppingItemId);
            status = existing == null ? ShoppingViewModel.NEED_TO_BUY : existing.status;
        }
        if (!shoppingViewModel.saveItem(editingShoppingItemId, shoppingDraftName, shoppingDraftAmount,
                canonicalUnit, shoppingDraftCategory, status)) {
            Toast.makeText(this, "Vui lòng nhập tên, số lượng hợp lệ và đơn vị.", Toast.LENGTH_SHORT).show();
            return;
        }
        shoppingStatusFilter = status;
        screen = Screen.SHOPPING;
        render();
    }

    private void confirmDeleteShoppingItem(String itemId) {
        ShoppingListItem item = shoppingViewModel.findItem(itemId);
        if (item == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa nguyên liệu?")
                .setMessage("Xóa “" + item.name + "” khỏi danh sách mua sắm?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    shoppingViewModel.deleteItem(itemId);
                    screen = Screen.SHOPPING;
                    render();
                }).show();
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

        TextView description = text(update.message +
                " Chưa có dữ liệu định lượng trong công thức; cập nhật sẽ ghi nhận kế hoạch mới và giữ nguyên các mục mua thủ công.",
                15, MUTED, false);
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
            Toast.makeText(this, "Đã ghi nhận kế hoạch mới; mục nhập tay được giữ nguyên.",
                    Toast.LENGTH_SHORT).show();
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void openShoppingList(Screen returnScreen) {
        ShoppingViewModel.PlanUpdate update = returnScreen == Screen.MEAL_PLAN
                ? shoppingViewModel.checkForPlanChanges() : null;
        shoppingReturnScreen = returnScreen == Screen.SHOPPING ? Screen.MEAL_PLAN : returnScreen;
        shoppingStatusFilter = ShoppingViewModel.NEED_TO_BUY;
        screen = Screen.SHOPPING;
        render();
        if (update != null) showShoppingPlanUpdate(update);
    }

    private void addPlannedRecipeCard(LinearLayout parent, PlannedRecipe item,
                                      int position, int totalCount) {
        boolean compact = getResources().getConfiguration().screenWidthDp < 360
                || getResources().getConfiguration().fontScale >= 1.15f;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        card.setGravity(compact ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
        int cardPadding = compact ? 8 : 11;
        card.setPadding(dp(cardPadding), dp(cardPadding), dp(cardPadding), dp(cardPadding));
        card.setBackground(roundRect(WHITE, BORDER, 20));

        LinearLayout contentRow = card;
        if (compact) {
            contentRow = new LinearLayout(this);
            contentRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(contentRow, new LinearLayout.LayoutParams(-1, -2));
        }
        ImageView image = recipeImage(item.recipe);
        int imageSize = compact ? 64 : 78;
        contentRow.addView(image, new LinearLayout.LayoutParams(dp(imageSize), dp(imageSize)));

        LinearLayout details = vertical();
        details.setPadding(dp(compact ? 8 : 12), 0, 0, 0);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(item.recipe.title, 18, TEXT, true);
        title.setMaxLines(2);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView remove = text("×", 28, GREEN, false);
        remove.setGravity(Gravity.CENTER);
        remove.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
        remove.setContentDescription("Bỏ " + item.recipe.title + " khỏi kế hoạch");
        titleRow.addView(remove, new LinearLayout.LayoutParams(dp(44), dp(44)));
        details.addView(titleRow);
        details.addView(text(item.recipe.duration + " · " + item.recipe.difficulty, 14, MUTED, false));
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, -2, 1);
        if (!compact) detailsParams.leftMargin = dp(1);
        contentRow.addView(details, detailsParams);

        addMealPlanOrderControls(compact ? card : details, item, position, totalCount);

        LinearLayout servings = new LinearLayout(this);
        servings.setGravity(Gravity.CENTER_VERTICAL);
        servings.setPadding(dp(4), 0, dp(4), 0);
        servings.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
        TextView minus = stepButton("−", "Giảm khẩu phần", () -> changeServings(item, -1));
        servings.addView(minus, new LinearLayout.LayoutParams(dp(40), dp(42)));
        TextView count = text(String.valueOf(item.servings), 19, TEXT, true);
        count.setGravity(Gravity.CENTER);
        servings.addView(count, new LinearLayout.LayoutParams(dp(40), dp(42)));
        TextView plus = stepButton("＋", "Tăng khẩu phần", () -> changeServings(item, 1));
        servings.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(42)));
        TextView person = text("người", 14, MUTED, false);
        person.setSingleLine(true);
        LinearLayout.LayoutParams personParams = new LinearLayout.LayoutParams(-2, -2);
        personParams.leftMargin = dp(5);
        servings.addView(person, personParams);
        LinearLayout.LayoutParams servingsParams = new LinearLayout.LayoutParams(-1, -2);
        if (compact) {
            servingsParams.topMargin = dp(8);
            card.addView(servings, servingsParams);
        } else {
            servingsParams.topMargin = dp(8);
            details.addView(servings, servingsParams);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(16);
        parent.addView(card, cardParams);
        remove.setOnClickListener(v -> removePlannedRecipe(item));
    }

    private void addMealPlanOrderControls(LinearLayout card, PlannedRecipe item,
                                          int position, int totalCount) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("Thứ tự nấu · " + (position + 1) + "/" + totalCount, 12, MUTED, false);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView up = mealPlanOrderButton("↑", "Đưa " + item.recipe.title + " lên trước",
                position > 0, () -> movePlannedRecipe(item.recipe.id, -1));
        TextView down = mealPlanOrderButton("↓", "Đưa " + item.recipe.title + " xuống sau",
                position < totalCount - 1, () -> movePlannedRecipe(item.recipe.id, 1));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        buttonParams.leftMargin = dp(4);
        row.addView(up, buttonParams);
        LinearLayout.LayoutParams downParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        downParams.leftMargin = dp(4);
        row.addView(down, downParams);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(6);
        card.addView(row, rowParams);
    }

    private TextView mealPlanOrderButton(String glyph, String description, boolean enabled, Runnable action) {
        TextView button = text(glyph, 20, enabled ? GREEN : MUTED, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(enabled ? PALE_GREEN : CREAM, enabled ? PALE_GREEN : BORDER, 14));
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setEnabled(enabled);
        if (enabled) button.setOnClickListener(v -> action.run());
        return button;
    }

    private void movePlannedRecipe(String recipeId, int direction) {
        if (mealPlanViewModel.moveMealPlanRecipe(recipeId, direction)) render();
    }

    private void removePlannedRecipe(PlannedRecipe item) {
        MealPlanViewModel.RemovedPlanRecipe removed = mealPlanViewModel.removeMealPlanRecipeForUndo(item.recipe.id);
        if (removed == null) return;
        render();
        Snackbar snackbar = Snackbar.make(appRoot, "Đã bỏ " + item.recipe.title + " khỏi kế hoạch.",
                Snackbar.LENGTH_LONG).setDuration(5000).setAction("Hoàn tác", view -> {
                    if (mealPlanViewModel.restoreMealPlanRecipe(removed)) render();
                    else Toast.makeText(this, "Món này đã có lại trong kế hoạch.", Toast.LENGTH_SHORT).show();
                });
        if (bottomNavigationBar != null) snackbar.setAnchorView(bottomNavigationBar);
        snackbar.show();
    }

    private void changeServings(PlannedRecipe item, int delta) {
        int servings = item.servings + delta;
        if (servings < MealPlanViewModel.MIN_SERVINGS || servings > MealPlanViewModel.MAX_SERVINGS) return;
        mealPlanViewModel.setMealPlanRecipeServings(item.recipe.id, servings);
        render();
    }

    private void renderMealPlanPicker() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Thêm món vào kế hoạch");

        EditText search = editField(mealPlanViewModel.getSearchQuery(), 16, 49);
        planPickerSearchInput = search;
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setSingleLine(true);
        search.setHint("Tìm món hoặc nguyên liệu");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(48), dp(10), dp(12), dp(10));
        FrameLayout searchBox = new FrameLayout(this);
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView searchIcon = iconView(R.drawable.ic_search_outline, GREEN);
        searchIcon.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchBox.addView(searchIcon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
        searchBox.addView(search, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
        searchParams.topMargin = dp(14);
        searchParams.bottomMargin = dp(10);
        page.addView(searchBox, searchParams);

        planPickerSourceButton = text("", 14, GREEN, true);
        planPickerSourceButton.setGravity(Gravity.CENTER_VERTICAL);
        planPickerSourceButton.setSingleLine(true);
        planPickerSourceButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        planPickerSourceButton.setMaxWidth(Math.round(getResources().getDisplayMetrics().widthPixels * 0.68f));
        planPickerSourceButton.setPadding(dp(14), 0, dp(14), 0);
        planPickerSourceButton.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-2, dp(40));
        sourceParams.bottomMargin = dp(14);
        page.addView(planPickerSourceButton, sourceParams);
        planPickerSourceButton.setOnClickListener(v -> showMealPlanSourceMenu(v));
        updatePickerSourceButton();

        ScrollView scroll = scrollContent();
        planPickerScrollView = scroll;
        planPickerRows = vertical();
        scroll.addView(planPickerRows);
        populateMealPlanPickerRows();
        Runnable[] pendingSearch = new Runnable[1];
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (BaseInputConnection.getComposingSpanStart((Spannable) s) >= 0) return;
                if (pendingSearch[0] != null) search.removeCallbacks(pendingSearch[0]);
                pendingSearch[0] = () -> {
                    if (BaseInputConnection.getComposingSpanStart(search.getText()) < 0) {
                        applyMealPlanSearch(search.getText().toString());
                    }
                };
                search.postDelayed(pendingSearch[0], 1000L);
            }
        });
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                if (pendingSearch[0] != null) search.removeCallbacks(pendingSearch[0]);
                applyMealPlanSearch(search.getText().toString());
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView selectedTitle = text("Khẩu phần món đã chọn", 20, TEXT, true);
        LinearLayout.LayoutParams selectedTitleParams = new LinearLayout.LayoutParams(-1, -2);
        selectedTitleParams.topMargin = dp(14);
        selectedTitleParams.bottomMargin = dp(8);
        page.addView(selectedTitle, selectedTitleParams);
        LinearLayout stepper = new LinearLayout(this);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        stepper.setPadding(dp(4), 0, dp(4), 0);
        stepper.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
        TextView minus = stepButton("−", "Giảm khẩu phần món đã chọn", () -> {
            int servings = mealPlanViewModel.getPickerServings();
            if (servings > MealPlanViewModel.MIN_SERVINGS) {
                mealPlanViewModel.setPickerServings(servings - 1);
                updatePickerServingCount();
            }
        });
        stepper.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView servingCount = text(String.valueOf(mealPlanViewModel.getPickerServings()), 20, TEXT, true);
        servingCount.setGravity(Gravity.CENTER);
        servingCount.setTag("plan-serving-count");
        stepper.addView(servingCount, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView plus = stepButton("＋", "Tăng khẩu phần món đã chọn", () -> {
            int servings = mealPlanViewModel.getPickerServings();
            if (servings < MealPlanViewModel.MAX_SERVINGS) {
                mealPlanViewModel.setPickerServings(servings + 1);
                updatePickerServingCount();
            }
        });
        stepper.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(stepper, new LinearLayout.LayoutParams(-2, dp(48)));

        TextView confirm = primaryButton("Thêm vào Nấu hôm nay", this::saveMealPlanSelection);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(-1, dp(52));
        confirmParams.topMargin = dp(14);
        page.addView(confirm, confirmParams);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void populateMealPlanPickerRows() {
        if (planPickerRows == null) return;
        planPickerRows.removeAllViews();
        List<Recipe> sourceRecipes = getCurrentMealPlanPickerRecipes();
        List<Recipe> matches = mealPlanViewModel.filterRecipes(sourceRecipes);
        if (matches.isEmpty()) {
            String message;
            if (!mealPlanViewModel.getSearchQuery().trim().isEmpty()) {
                message = "Không tìm thấy món phù hợp.";
            } else if (mealPlanViewModel.getPickerSource() == MealPlanViewModel.PickerSource.FAVORITES) {
                message = "Chưa có món yêu thích nào.";
            } else if (mealPlanViewModel.getPickerSource() == MealPlanViewModel.PickerSource.COLLECTION
                    && mealPlanViewModel.getPickerCollectionId() > 0) {
                message = "Bộ sưu tập này chưa có món nào.";
            } else if (mealPlanViewModel.getPickerSource() == MealPlanViewModel.PickerSource.COLLECTION) {
                message = "Chọn một bộ sưu tập để xem món.";
            } else {
                message = "Không tìm thấy món phù hợp.";
            }
            TextView empty = text(message, 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            planPickerRows.addView(empty, new LinearLayout.LayoutParams(-1, dp(72)));
            return;
        }
        for (Recipe recipe : matches) {
            boolean planned = mealPlanViewModel.isRecipeInPlan(recipe.id);
            boolean selected = mealPlanViewModel.isSelected(recipe.id);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(7), dp(6), dp(14), dp(6));
            row.setBackground(roundRect(WHITE, BORDER, 15));
            ImageView image = recipeImage(recipe);
            row.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
            LinearLayout labels = vertical();
            labels.setPadding(dp(12), 0, 0, 0);
            labels.addView(text(recipe.title, 15, TEXT, true));
            String subtitle = planned
                    ? (selected ? "Đã có trong kế hoạch" : "Sẽ bỏ khỏi kế hoạch")
                    : (selected ? "Đang chọn" : recipe.duration + " · " + recipe.difficulty);
            labels.addView(text(subtitle, 13, MUTED, false));
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            FrameLayout toggleTarget = new FrameLayout(this);
            toggleTarget.setFocusable(true);
            toggleTarget.setClickable(true);
            toggleTarget.setContentDescription((selected ? "Bỏ chọn " : "Chọn ") + recipe.title);
            TextView mark = text(selected ? "✓" : "+", 21, selected ? WHITE : GREEN, false);
            mark.setGravity(Gravity.CENTER);
            if (selected) mark.setBackground(roundRect(GREEN, GREEN, 8));
            toggleTarget.addView(mark, new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER));
            row.addView(toggleTarget, new LinearLayout.LayoutParams(dp(48), dp(48)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(64));
            rowParams.bottomMargin = dp(10);
            planPickerRows.addView(row, rowParams);
            row.setContentDescription("Xem chi tiết món " + recipe.title);
            row.setFocusable(true);
            row.setOnClickListener(v -> openRecipeDetail(recipe));
            toggleTarget.setOnClickListener(v -> togglePlanPickerRecipe(recipe));
        }
    }

    private void applyMealPlanSearch(String query) {
        mealPlanViewModel.setSearchQuery(query);
        populateMealPlanPickerRows();
    }

    private List<Recipe> getCurrentMealPlanPickerRecipes() {
        return mealPlanViewModel.getPickerRecipes(PreviewRecipeCatalog.mealPlanPreviews());
    }

    private void showMealPlanSourceMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Tất cả món");
        menu.getMenu().add(0, 2, 1, "Món yêu thích");
        Map<Integer, Long> collectionIdsByMenuId = new LinkedHashMap<>();
        List<RecipeCollection> collections = mealPlanViewModel.getRecipeCollections();
        if (collections.isEmpty()) {
            menu.getMenu().add(0, 3, 2, "Chưa có bộ sưu tập").setEnabled(false);
        } else {
            int menuId = 10;
            int order = 2;
            for (RecipeCollection collection : collections) {
                menu.getMenu().add(0, menuId, order++, "Bộ sưu tập: " + collection.name);
                collectionIdsByMenuId.put(menuId, collection.id);
                menuId++;
            }
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                mealPlanViewModel.setPickerSource(MealPlanViewModel.PickerSource.ALL, -1L);
            } else if (item.getItemId() == 2) {
                mealPlanViewModel.setPickerSource(MealPlanViewModel.PickerSource.FAVORITES, -1L);
            } else {
                Long collectionId = collectionIdsByMenuId.get(item.getItemId());
                if (collectionId == null) return false;
                mealPlanViewModel.setPickerSource(MealPlanViewModel.PickerSource.COLLECTION, collectionId);
            }
            updatePickerSourceButton();
            populateMealPlanPickerRows();
            if (planPickerScrollView != null) planPickerScrollView.scrollTo(0, 0);
            return true;
        });
        menu.show();
    }

    private void updatePickerSourceButton() {
        if (planPickerSourceButton == null || mealPlanViewModel == null) return;
        String label;
        switch (mealPlanViewModel.getPickerSource()) {
            case FAVORITES:
                label = "Yêu thích";
                break;
            case COLLECTION:
                label = "Chọn bộ sưu tập";
                for (RecipeCollection collection : mealPlanViewModel.getRecipeCollections()) {
                    if (collection.id == mealPlanViewModel.getPickerCollectionId()) {
                        label = collection.name;
                        break;
                    }
                }
                break;
            case ALL:
            default:
                label = "Tất cả món";
                break;
        }
        planPickerSourceButton.setText(label + "  ▾");
        planPickerSourceButton.setContentDescription("Nguồn món: " + label + ". Chạm để đổi nguồn.");
    }

    private void openMealPlanPicker() {
        mealPlanViewModel.beginPicker();
        restoredPickerScrollY = 0;
        screen = Screen.MEAL_PLAN_PICKER;
        render();
    }

    private void togglePlanPickerRecipe(Recipe recipe) {
        mealPlanViewModel.toggleRecipe(recipe.id);
        populateMealPlanPickerRows();
    }

    private void saveMealPlanSelection() {
        if (!mealPlanViewModel.saveSelection(mealPlanViewModel.getAllSelectableRecipes(PreviewRecipeCatalog.designRecipes()))) {
            Toast.makeText(this, "Chọn ít nhất một món.", Toast.LENGTH_SHORT).show();
            return;
        }
        screen = Screen.MEAL_PLAN;
        render();
    }

    private void updatePickerServingCount() {
        if (appRoot == null) return;
        TextView count = appRoot.findViewWithTag("plan-serving-count");
        if (count != null) count.setText(String.valueOf(mealPlanViewModel.getPickerServings()));
    }

    private TextView stepButton(String label, String description, Runnable action) {
        TextView button = text(label, 21, GREEN, false);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private TextView primaryButton(String label, Runnable action) {
        TextView button = text(label, 15, WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(GREEN, GREEN, 14));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void renderCollection() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Bộ sưu tập");
        RecipeCollection selected = findCollection(selectedCollectionId);
        if (selected == null) {
            screen = Screen.LIBRARY;
            render();
            return;
        }
        if (collectionDetailSearchOwnerId != selected.id) {
            collectionDetailSearchOwnerId = selected.id;
            collectionDetailSearchQuery = "";
        }

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(selected.name, 19, TEXT, true);
        titleLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout delete = collectionActionButton("Xóa", R.drawable.ic_delete_outline, ERROR, WHITE,
                () -> confirmDeleteCollection(selected));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, dp(48));
        deleteParams.setMarginEnd(dp(8));
        titleLine.addView(delete, deleteParams);
        LinearLayout edit = collectionActionButton("Chỉnh sửa", R.drawable.ic_edit_outline, GREEN, PALE_GREEN,
                () -> openCollectionForm(selectedCollectionId));
        titleLine.addView(edit, new LinearLayout.LayoutParams(-2, dp(48)));
        page.addView(titleLine);
        addText(page, selected.recipeCount + " món", 14, MUTED, false, 0, 8, 0, 24);

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        scroll.addView(content);
        List<Recipe> collectionRecipes = personalDataViewModel.getCollectionRecipes(selectedCollectionId);
        if (collectionRecipes.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER);
            ImageView folder = iconView(R.drawable.ic_folder_outline, GREEN);
            folder.setPadding(dp(24), dp(24), dp(24), dp(24));
            folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(folder, new LinearLayout.LayoutParams(dp(84), dp(84)));
            TextView emptyTitle = text("Bộ sưu tập đang trống", 22, TEXT, true);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams emptyTitleParams = new LinearLayout.LayoutParams(-1, -2);
            emptyTitleParams.topMargin = dp(18);
            emptyTitleParams.bottomMargin = dp(8);
            empty.addView(emptyTitle, emptyTitleParams);
            TextView hint = text("Thêm món đã lưu vào bộ sưu tập này.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
            hintParams.bottomMargin = dp(16);
            empty.addView(hint, hintParams);
            TextView add = text("Thêm món", 15, WHITE, true);
            add.setGravity(Gravity.CENTER);
            add.setBackground(roundRect(GREEN, GREEN, 14));
            empty.addView(add, new LinearLayout.LayoutParams(-1, dp(49)));
            content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            add.setOnClickListener(v -> openCollectionForm(selectedCollectionId));
        } else {
            content.addView(createRecipeSearchBox(collectionDetailSearchQuery, "Tìm món trong bộ sưu tập",
                    new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            collectionDetailSearchQuery = s.toString();
                            populateCollectionRecipeResults(collectionRecipeResults, collectionRecipes);
                        }
                        @Override public void afterTextChanged(Editable s) { }
                    }), searchLayoutParams(0, 14));
            collectionRecipeResults = vertical();
            content.addView(collectionRecipeResults);
            populateCollectionRecipeResults(collectionRecipeResults, collectionRecipes);
        }
        List<RecipeCollection> otherCollections = personalDataViewModel.getCollections();
        boolean hasOtherCollections = false;
        for (RecipeCollection row : otherCollections) {
            if (row.id == selectedCollectionId) continue;
            if (!hasOtherCollections) {
                addText(content, "Bộ sưu tập khác", 19, TEXT, true, 0, 22, 0, 12);
                hasOtherCollections = true;
            }
            addRelatedCollectionRow(content, row);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addButton(page, "+     Tạo bộ sưu tập", true, () -> openCollectionForm(null), 18);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderCollectionForm() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Tạo bộ sưu tập");

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        scroll.addView(content);
        addText(content, "Tên bộ sưu tập", 13, TEXT, true, 0, 20, 0, 10);
        collectionNameInput = editField(personalDataViewModel.getCollectionNameDraft(), 16, 49);
        collectionNameInput.setSingleLine(true);
        collectionNameInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        content.addView(collectionNameInput, new LinearLayout.LayoutParams(-1, dp(49)));
        collectionError = text("", 12, ERROR, false);
        content.addView(collectionError);
        addText(content, "Chọn món để thêm", 19, TEXT, true, 0, 34, 0, 18);
        content.addView(createRecipeSearchBox(collectionFormSearchQuery, "Tìm món để thêm",
                new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        collectionFormSearchQuery = s.toString();
                        populateCollectionFormRecipeRows(collectionFormRecipeRows);
                    }
                    @Override public void afterTextChanged(Editable s) { }
                }), searchLayoutParams(0, 14));
        collectionFormRecipeRows = vertical();
        content.addView(collectionFormRecipeRows);
        populateCollectionFormRecipeRows(collectionFormRecipeRows);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        collectionSaveButton = addButton(page, "Lưu bộ sưu tập", true, this::saveCollectionForm, 18);
        collectionNameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                personalDataViewModel.setCollectionNameDraft(s.toString());
                updateCollectionFormValidation();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        updateCollectionFormValidation();
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderNote() {
        Recipe recipe = recipeById(noteRecipeId);
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));

        // Scroll the form when the available viewport shrinks (small phone or IME open).
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(0, 0, 0, dp(8));
        scroll.addView(content);
        addBackTitle(content, "Ghi chú cá nhân");

        LinearLayout recipeRow = new LinearLayout(this);
        recipeRow.setGravity(Gravity.CENTER_VERTICAL);
        recipeRow.setPadding(dp(8), dp(6), dp(12), dp(6));
        recipeRow.setBackground(roundRect(WHITE, BORDER, 14));
        ImageView image = recipeImage(recipe);
        recipeRow.addView(image, new LinearLayout.LayoutParams(dp(60), dp(52)));
        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(recipe.title, 14, TEXT, true));
        labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
        recipeRow.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        recipeRow.addView(favoriteToggleButton(recipe), new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout.LayoutParams recipeParams = new LinearLayout.LayoutParams(-1, dp(64));
        recipeParams.topMargin = dp(30);
        content.addView(recipeRow, recipeParams);

        addText(content, "Ghi chú sau khi nấu", 13, TEXT, true, 0, 48, 0, 16);
        EditText noteInput = editField(personalDataViewModel.getNote(recipe.id), 16, -1);
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        noteInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density);
        int noteHeightDp = Math.max(160, Math.min(220, Math.round(screenHeightDp * 0.215f)));
        content.addView(noteInput, new LinearLayout.LayoutParams(-1, dp(noteHeightDp)));
        LinearLayout savedBanner = addInfoBanner(content, "Ghi chú đã lưu", 24);
        savedBanner.setVisibility(View.GONE);

        addCookingJournalPreview(content, recipe);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "Lưu ghi chú", true, () -> {
            personalDataViewModel.saveNote(recipe, noteInput.getText().toString());
            savedBanner.setVisibility(View.VISIBLE);
            noteInput.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(noteInput.getWindowToken(), 0);
        }, 8);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void openPantry(Screen returnScreen) {
        pantryReturnScreen = returnScreen == Screen.PANTRY ? Screen.SHOPPING : returnScreen;
        pantryFilter = "ALL";
        screen = Screen.PANTRY;
        render();
    }

    private void renderPantry() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Tủ nguyên liệu");
        List<PantryIngredient> items = pantryViewModel.getItems();
        int usableCount = 0;
        int expiringCount = 0;
        int expiredCount = 0;
        for (PantryIngredient item : items) {
            String state = pantryViewModel.expiryState(item.expiryDate);
            if ("EXPIRED".equals(state)) expiredCount++;
            else if ("EXPIRING".equals(state)) expiringCount++;
            else usableCount++;
        }
        addText(page, usableCount + " sử dụng được · " + expiringCount + " sắp hết hạn · " + expiredCount + " hết hạn",
                14, MUTED, false, 0, 8, 0, 12);

        EditText search = editField(pantrySearchQuery, 15, 48);
        search.setSingleLine(true);
        search.setHint("Tìm nguyên liệu trong tủ");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(48), dp(8), dp(12), dp(8));
        FrameLayout searchBox = new FrameLayout(this);
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView searchIcon = iconView(R.drawable.ic_search_outline, GREEN);
        searchIcon.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchBox.addView(searchIcon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
        searchBox.addView(search, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
        searchParams.bottomMargin = dp(12);
        page.addView(searchBox, searchParams);

        LinearLayout filters = new LinearLayout(this);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        addPantryFilterChip(filters, "Tất cả", "ALL");
        addPantryFilterChip(filters, "Sử dụng được", "USABLE");
        addPantryFilterChip(filters, "Sắp hết hạn", "EXPIRING");
        addPantryFilterChip(filters, "Hết hạn", "EXPIRED");
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.addView(filters, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(filterScroll, new LinearLayout.LayoutParams(-1, dp(46)));

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(content);
        populatePantryRows(content, items);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pantrySearchQuery = s.toString();
                populatePantryRows(content, items);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView add = text("+   Thêm nguyên liệu", 14, WHITE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(roundRect(GREEN, GREEN, 14));
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(48), 1));
        add.setOnClickListener(v -> openPantryForm(null));
        TextView clear = text("Xóa tất cả", 14, ERROR, true);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(dp(10), 0, dp(10), 0);
        clear.setBackground(roundRect(WHITE, BORDER, 14));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-2, dp(48));
        clearParams.leftMargin = dp(10);
        actions.addView(clear, clearParams);
        clear.setEnabled(!items.isEmpty());
        clear.setAlpha(items.isEmpty() ? 0.5f : 1f);
        clear.setOnClickListener(v -> confirmClearPantryItems(items.size()));
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.topMargin = dp(8);
        page.addView(actions, actionsParams);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(2);
        schedulePantryExpiryRefresh();
    }

    private void populatePantryRows(LinearLayout content, List<PantryIngredient> allItems) {
        content.removeAllViews();
        List<PantryIngredient> matches = new ArrayList<>();
        String query = normalizeSearchText(pantrySearchQuery);
        for (PantryIngredient item : allItems) {
            String expiryState = pantryViewModel.expiryState(item.expiryDate);
            if ("USABLE".equals(pantryFilter) && !expiryState.isEmpty()) continue;
            if ("EXPIRING".equals(pantryFilter) && !"EXPIRING".equals(expiryState)) continue;
            if ("EXPIRED".equals(pantryFilter) && !"EXPIRED".equals(expiryState)) continue;
            if (!query.isEmpty() && !normalizeSearchText(item.name + " " + item.category + " " + item.storageLocation)
                    .contains(query)) continue;
            matches.add(item);
        }
        if (matches.isEmpty()) {
            String title = allItems.isEmpty() ? "Tủ nguyên liệu đang trống" : "Không tìm thấy nguyên liệu";
            String subtitle = allItems.isEmpty()
                    ? "Thêm những nguyên liệu bạn đang có để quản lý hạn sử dụng."
                    : "Thử từ khóa khác hoặc chọn bộ lọc Tất cả.";
            addEmptyCard(content, title, subtitle, allItems.isEmpty() ? () -> openPantryForm(null) : () -> {
                pantryFilter = "ALL";
                pantrySearchQuery = "";
                render();
            });
            return;
        }
        Map<String, List<PantryIngredient>> groups = new LinkedHashMap<>();
        for (String category : ShoppingCategoryCatalog.ORDER) groups.put(category, new ArrayList<>());
        for (PantryIngredient item : matches) {
            String category = ShoppingCategoryCatalog.contains(item.category)
                    ? item.category : ShoppingCategoryCatalog.OTHER;
            groups.get(category).add(item);
        }
        int groupIndex = 0;
        for (Map.Entry<String, List<PantryIngredient>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            addText(content, group.getKey(), 18, TEXT, true, 0, groupIndex == 0 ? 4 : 20, 0, 0);
            groupIndex++;
            for (PantryIngredient item : group.getValue()) addPantryItemCard(content, item);
        }
    }

    private void addPantryFilterChip(LinearLayout parent, String label, String filter) {
        boolean selected = filter.equals(pantryFilter);
        TextView chip = text(label, 13, selected ? GREEN : MUTED, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? PALE_GREEN : BORDER, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38));
        params.rightMargin = dp(8);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            pantryFilter = filter;
            render();
        });
    }

    private void addPantryItemCard(LinearLayout parent, PantryIngredient item) {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(WHITE, BORDER, 16));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(item.name, 16, TEXT, true);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView edit = text("Sửa", 13, GREEN, true);
        edit.setGravity(Gravity.CENTER);
        edit.setPadding(dp(10), 0, dp(10), 0);
        top.addView(edit, new LinearLayout.LayoutParams(-2, dp(40)));
        card.addView(top);
        String quantity = item.amountDecimal == null ? "Chưa biết lượng" :
                item.amountDecimal + (item.unitId == null || item.unitId.isEmpty() ? "" : " " + KitchenUnitCatalog.label(item.unitId));
        String availability = pantryViewModel.availabilityLabel(item.expiryDate);
        String detailText = quantity + " · " + availability + " · " + item.category;
        TextView detail = text(detailText,
                13, "Hết hạn".equals(availability) ? ERROR
                        : "Sắp hết hạn".equals(availability) ? Color.rgb(180, 112, 30) : MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(4);
        card.addView(detail, detailParams);
        String expiryState = pantryViewModel.expiryState(item.expiryDate);
        if (item.expiryDate != null || !item.storageLocation.isEmpty() || item.purchaseDate != null) {
            LinearLayout meta = new LinearLayout(this);
            meta.setGravity(Gravity.CENTER_VERTICAL);
            if (item.expiryDate != null) {
                int color = "EXPIRED".equals(expiryState) ? ERROR
                        : "EXPIRING".equals(expiryState) ? Color.rgb(180, 112, 30) : MUTED;
                String label = "EXPIRED".equals(expiryState) ? "Hạn đã qua · "
                        : "EXPIRING".equals(expiryState) ? "Hạn sắp đến · " : "Dùng trước · ";
                meta.addView(text(label + displayIsoDate(item.expiryDate), 13, color, true),
                        new LinearLayout.LayoutParams(0, -2, 1));
            }
            if (!item.storageLocation.isEmpty()) {
                meta.addView(text(item.storageLocation, 12, MUTED, false));
            }
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
            metaParams.topMargin = dp(5);
            card.addView(meta, metaParams);
        }
        if (item.purchaseDate != null) {
            TextView purchased = text("Mua ngày " + displayIsoDate(item.purchaseDate), 12, MUTED, false);
            LinearLayout.LayoutParams purchasedParams = new LinearLayout.LayoutParams(-1, -2);
            purchasedParams.topMargin = dp(4);
            card.addView(purchased, purchasedParams);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(9);
        parent.addView(card, cardParams);
        card.setOnClickListener(v -> openPantryForm(item.id));
        edit.setOnClickListener(v -> openPantryForm(item.id));
        card.setOnLongClickListener(v -> {
            confirmDeletePantryItem(item);
            return true;
        });
    }

    private void openPantryForm(String itemId) {
        editingPantryItemId = itemId;
        PantryIngredient item = pantryViewModel.getItem(itemId);
        pantryDraftName = item == null ? "" : item.name;
        pantryDraftAmount = item == null || item.amountDecimal == null ? "" : item.amountDecimal;
        pantryDraftUnitId = item == null || item.unitId == null ? "" : item.unitId;
        pantryDraftUnit = pantryDraftUnitId.isEmpty() ? "" : KitchenUnitCatalog.label(pantryDraftUnitId);
        pantryDraftCategory = item == null ? ShoppingCategoryCatalog.OTHER : item.category;
        pantryDraftExpiry = item == null || item.expiryDate == null ? "" : item.expiryDate;
        pantryDraftLocation = item == null ? "" : item.storageLocation;
        pantryDraftPurchaseDate = item == null || item.purchaseDate == null ? "" : item.purchaseDate;
        screen = Screen.PANTRY_FORM;
        render();
    }

    private void renderPantryForm() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(16));
        addBackTitle(page, editingPantryItemId == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu");
        ScrollView scroll = scrollContent();
        LinearLayout fields = vertical();
        fields.setPadding(0, dp(18), 0, dp(20));
        scroll.addView(fields);

        addText(fields, "Tên nguyên liệu", 14, TEXT, true, 0, 0, 0, 0);
        pantryNameInput = editField(pantryDraftName, 16, 52);
        pantryNameInput.setSingleLine(true);
        pantryNameInput.setHint("Ví dụ: Cà chua");
        pantryNameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        addFieldView(fields, pantryNameInput, 8, 20);

        addText(fields, "Số lượng (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        pantryAmountInput = editField(pantryDraftAmount, 16, 52);
        pantryAmountInput.setSingleLine(true);
        pantryAmountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pantryAmountInput.setHint("Để trống nếu chưa biết lượng");
        addFieldView(fields, pantryAmountInput, 8, 20);

        addText(fields, "Đơn vị", 14, TEXT, true, 0, 0, 0, 0);
        pantryUnitInput = editField(pantryDraftUnit, 16, 52);
        pantryUnitInput.setSingleLine(true);
        pantryUnitInput.setHint("Ví dụ: quả, g, ml, muỗng cà phê");
        addFieldView(fields, pantryUnitInput, 8, 20);

        addText(fields, "Danh mục", 14, TEXT, true, 0, 0, 0, 0);
        pantryCategoryInput = selectionField(pantryDraftCategory + "     ▾", 15, 52);
        addFieldView(fields, pantryCategoryInput, 8, 20);
        pantryCategoryInput.setOnClickListener(v -> showPantryCategoryPicker());

        addText(fields, "Hạn sử dụng (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        LinearLayout expiryRow = new LinearLayout(this);
        expiryRow.setGravity(Gravity.CENTER_VERTICAL);
        pantryExpiryInput = text(pantryDraftExpiry.isEmpty() ? "Không đặt" : displayIsoDate(pantryDraftExpiry),
                15, pantryDraftExpiry.isEmpty() ? MUTED : TEXT, false);
        pantryExpiryInput.setPadding(dp(15), 0, dp(15), 0);
        pantryExpiryInput.setBackground(roundRect(WHITE, BORDER, 14));
        expiryRow.addView(pantryExpiryInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView clearDate = text("Xóa", 13, GREEN, true);
        clearDate.setGravity(Gravity.CENTER);
        expiryRow.addView(clearDate, new LinearLayout.LayoutParams(dp(48), dp(48)));
        addFieldView(fields, expiryRow, 8, 20);
        pantryExpiryInput.setOnClickListener(v -> showPantryExpiryPicker());
        clearDate.setOnClickListener(v -> {
            pantryDraftExpiry = "";
            pantryExpiryInput.setText("Không đặt");
            pantryExpiryInput.setTextColor(MUTED);
        });

        addText(fields, "Ngày mua (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        LinearLayout purchaseRow = new LinearLayout(this);
        purchaseRow.setGravity(Gravity.CENTER_VERTICAL);
        pantryPurchaseDateInput = text(pantryDraftPurchaseDate.isEmpty() ? "Không đặt" : displayIsoDate(pantryDraftPurchaseDate),
                15, pantryDraftPurchaseDate.isEmpty() ? MUTED : TEXT, false);
        pantryPurchaseDateInput.setPadding(dp(15), 0, dp(15), 0);
        pantryPurchaseDateInput.setBackground(roundRect(WHITE, BORDER, 14));
        purchaseRow.addView(pantryPurchaseDateInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView clearPurchaseDate = text("Xóa", 13, GREEN, true);
        clearPurchaseDate.setGravity(Gravity.CENTER);
        purchaseRow.addView(clearPurchaseDate, new LinearLayout.LayoutParams(dp(48), dp(48)));
        addFieldView(fields, purchaseRow, 8, 20);
        pantryPurchaseDateInput.setOnClickListener(v -> showPantryPurchaseDatePicker());
        clearPurchaseDate.setOnClickListener(v -> {
            pantryDraftPurchaseDate = "";
            pantryPurchaseDateInput.setText("Không đặt");
            pantryPurchaseDateInput.setTextColor(MUTED);
        });

        addText(fields, "Nơi cất (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        pantryLocationInput = editField(pantryDraftLocation, 16, 52);
        pantryLocationInput.setSingleLine(true);
        pantryLocationInput.setHint("Ví dụ: Ngăn mát");
        addFieldView(fields, pantryLocationInput, 8, 14);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "Lưu nguyên liệu", true, this::savePantryForm, 8);
        if (editingPantryItemId != null) {
            TextView remove = text("Xóa nguyên liệu", 14, ERROR, true);
            remove.setGravity(Gravity.CENTER);
            page.addView(remove, new LinearLayout.LayoutParams(-1, dp(44)));
            remove.setOnClickListener(v -> confirmDeletePantryItem(pantryViewModel.getItem(editingPantryItemId)));
        }
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showPantryCategoryPicker() {
        String[] categories = ShoppingCategoryCatalog.ORDER.toArray(new String[0]);
        int selected = ShoppingCategoryCatalog.ORDER.indexOf(pantryDraftCategory);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Chọn danh mục")
                .setSingleChoiceItems(categories, Math.max(0, selected), (dialog, which) -> {
                    pantryDraftCategory = categories[which];
                    pantryCategoryInput.setText(pantryDraftCategory + "     ▾");
                    dialog.dismiss();
                }).setNegativeButton("Hủy", null).show();
    }

    private void showPantryExpiryPicker() {
        Calendar selected = Calendar.getInstance();
        if (!pantryDraftExpiry.isEmpty()) {
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                Date date = format.parse(pantryDraftExpiry);
                if (date != null) selected.setTime(date);
            } catch (java.text.ParseException ignored) { }
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.clear();
            date.set(year, month, day);
            pantryDraftExpiry = formatIsoDate(date.getTimeInMillis());
            pantryExpiryInput.setText(displayIsoDate(pantryDraftExpiry));
            pantryExpiryInput.setTextColor(TEXT);
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showPantryPurchaseDatePicker() {
        Calendar selected = Calendar.getInstance();
        if (!pantryDraftPurchaseDate.isEmpty()) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                Date date = format.parse(pantryDraftPurchaseDate);
                if (date != null) selected.setTime(date);
            } catch (java.text.ParseException ignored) { }
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.clear();
            date.set(year, month, day);
            pantryDraftPurchaseDate = formatIsoDate(date.getTimeInMillis());
            pantryPurchaseDateInput.setText(displayIsoDate(pantryDraftPurchaseDate));
            pantryPurchaseDateInput.setTextColor(TEXT);
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void capturePantryDraftFields() {
        if (pantryNameInput != null) pantryDraftName = pantryNameInput.getText().toString();
        if (pantryAmountInput != null) pantryDraftAmount = pantryAmountInput.getText().toString();
        if (pantryUnitInput != null) pantryDraftUnit = pantryUnitInput.getText().toString();
        if (pantryLocationInput != null) pantryDraftLocation = pantryLocationInput.getText().toString();
        pantryDraftUnitId = pantryDraftAmount.trim().isEmpty() ? "" : KitchenUnitCatalog.canonicalId(pantryDraftUnit);
    }

    private void savePantryForm() {
        capturePantryDraftFields();
        if (!pantryDraftAmount.trim().isEmpty() && pantryDraftUnitId.isEmpty()) {
            Toast.makeText(this, "Nhập đơn vị cho số lượng.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pantryViewModel.saveItem(editingPantryItemId, pantryDraftName, pantryDraftAmount,
                pantryDraftUnitId, pantryDraftCategory, pantryDraftExpiry, pantryDraftLocation,
                pantryDraftPurchaseDate)) {
            Toast.makeText(this, "Vui lòng kiểm tra tên, số lượng và đơn vị.", Toast.LENGTH_SHORT).show();
            return;
        }
        screen = Screen.PANTRY;
        render();
    }

    private void confirmDeletePantryItem(PantryIngredient item) {
        if (item == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Xóa nguyên liệu khỏi tủ?")
                .setMessage("Xóa “" + item.name + "” khỏi tủ nguyên liệu?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    pantryViewModel.deleteItem(item.id);
                    screen = Screen.PANTRY;
                    render();
                }).show();
    }

    private void confirmClearPantryItems(int itemCount) {
        if (itemCount <= 0) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa tất cả nguyên liệu trong tủ?")
                .setMessage("Xóa toàn bộ " + itemCount + " nguyên liệu đã lưu trong tủ? Thao tác này không thể hoàn tác.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    pantryViewModel.deleteAllItems();
                    pantryFilter = "ALL";
                    pantrySearchQuery = "";
                    screen = Screen.PANTRY;
                    render();
                }).show();
    }

    private void addCookingJournalPreview(LinearLayout parent, Recipe recipe) {
        List<CookingLogEntry> logs = cookingJournalViewModel.getLogs(recipe.id);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(24);
        titleParams.bottomMargin = dp(6);
        parent.addView(titleRow, titleParams);
        TextView heading = text("Nhật ký nấu", 18, TEXT, true);
        titleRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = text("Xem tất cả ›", 12, GREEN, true);
        titleRow.addView(all);
        all.setOnClickListener(v -> openCookingLogs(recipe.id, Screen.NOTE));
        TextView count = text("Đã nấu " + logs.size() + (logs.size() == 1 ? " lần" : " lần"),
                13, MUTED, false);
        parent.addView(count);
        if (logs.isEmpty()) {
            addText(parent, "Ghi lại trải nghiệm mỗi lần bạn nấu món này.", 13, MUTED, false, 0, 5, 0, 0);
        } else {
            for (int i = 0; i < Math.min(2, logs.size()); i++) addCookingLogPreviewRow(parent, logs.get(i));
        }
        addButton(parent, "+   Ghi lại lần nấu", false, () -> openCookingLogForm(null, recipe.id), 10);
    }

    private void openCookingLogs(String recipeId, Screen returnScreen) {
        cookingLogRecipeId = recipeId;
        cookingLogReturnScreen = returnScreen;
        screen = Screen.COOKING_LOGS;
        render();
    }

    private void openAllCookingLogs(Screen returnScreen) {
        cookingLogRecipeId = null;
        cookingLogReturnScreen = returnScreen;
        screen = Screen.COOKING_JOURNAL;
        render();
    }

    private void renderAllCookingLogs() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Nhật ký nấu của tôi");
        addText(page, "Tìm lại những lần bạn đã nấu.", 14, MUTED, false, 0, 0, 0, 14);

        List<CookingLogEntry> allLogs = cookingJournalViewModel.getAllLogs();
        cookingJournalAllLogs = allLogs;
        addJournalStatsCard(page, cookingJournalViewModel.getStats());

        FrameLayout searchBox = createRecipeSearchBox(cookingJournalSearch, "Tìm món hoặc ghi chú",
                new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        cookingJournalSearch = s.toString();
                        populateAllCookingLogs(logRows, allLogs.size());
                    }
                    @Override public void afterTextChanged(Editable s) { }
                });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
        searchParams.topMargin = dp(16);
        searchParams.bottomMargin = dp(10);
        page.addView(searchBox, searchParams);

        LinearLayout dateFilters = new LinearLayout(this);
        dateFilters.setGravity(Gravity.CENTER_VERTICAL);
        dateFilters.addView(createJournalDateFilter(true), new LinearLayout.LayoutParams(0, dp(54), 1));
        View dateGap = new View(this);
        dateFilters.addView(dateGap, new LinearLayout.LayoutParams(dp(8), 1));
        dateFilters.addView(createJournalDateFilter(false), new LinearLayout.LayoutParams(0, dp(54), 1));
        page.addView(dateFilters);

        LinearLayout ratingFilters = new LinearLayout(this);
        addJournalRatingChip(ratingFilters, "Tất cả", 0);
        for (int rating = 5; rating >= 1; rating--) addJournalRatingChip(ratingFilters, rating + " ★", rating);
        HorizontalScrollView ratingScroll = new HorizontalScrollView(this);
        ratingScroll.setHorizontalScrollBarEnabled(false);
        ratingScroll.addView(ratingFilters, new HorizontalScrollView.LayoutParams(-2, -2));
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(-1, dp(46));
        ratingParams.topMargin = dp(8);
        page.addView(ratingScroll, ratingParams);

        if (cookingJournalFromDate > 0 || cookingJournalToDate > 0 || cookingJournalRatingFilter > 0
                || !cookingJournalSearch.trim().isEmpty()) {
            TextView clearFilters = text("Xóa bộ lọc", 13, GREEN, true);
            clearFilters.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, dp(36));
            clearParams.bottomMargin = dp(4);
            page.addView(clearFilters, clearParams);
            clearFilters.setOnClickListener(v -> {
                cookingJournalSearch = "";
                cookingJournalFromDate = 0;
                cookingJournalToDate = 0;
                cookingJournalRatingFilter = 0;
                render();
            });
        }

        ScrollView scroll = scrollContent();
        logRows = vertical();
        logRows.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(logRows);
        populateAllCookingLogs(logRows, allLogs.size());
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(3);
    }

    private LinearLayout logRows;

    private void addJournalStatsCard(LinearLayout parent, CookingJournalViewModel.JournalStats stats) {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 16));
        addJournalStatRow(card, "Món nấu nhiều nhất", stats.mostCookedCount == 0
                ? "Chưa có" : stats.mostCookedRecipe + " · " + stats.mostCookedCount + " lần");
        addJournalStatRow(card, "Lần nấu gần nhất", stats.lastCookedAt == null
                ? "Chưa có" : formatDate(stats.lastCookedAt));
        addJournalStatRow(card, "Đánh giá gần nhất", stats.latestRating == null
                ? "Chưa đánh giá" : ratingStars(stats.latestRating));
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addJournalStatRow(LinearLayout card, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(label, 12, MUTED, false);
        row.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView valueView = text(value, 13, TEXT, true);
        valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        valueView.setMaxLines(2);
        valueView.setMaxWidth(dp(200));
        row.addView(valueView, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row);
        if (!"Đánh giá gần nhất".equals(label)) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(211, 226, 212));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(7);
            dividerParams.bottomMargin = dp(7);
            card.addView(divider, dividerParams);
        }
    }

    private LinearLayout createJournalDateFilter(boolean startDate) {
        long selectedDate = startDate ? cookingJournalFromDate : cookingJournalToDate;
        String title = startDate ? "Từ ngày" : "Đến ngày";
        TextView field = text(title + "\n" + (selectedDate == 0 ? "Chọn ngày" : formatDate(selectedDate)),
                12, selectedDate == 0 ? MUTED : TEXT, true);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(12), dp(5), dp(6), dp(5));
        field.setOnClickListener(v -> showJournalDatePicker(startDate));
        LinearLayout container = new LinearLayout(this);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setBackground(roundRect(WHITE, BORDER, 14));
        container.addView(field, new LinearLayout.LayoutParams(0, -1, 1));
        if (selectedDate > 0) {
            TextView clear = text("×", 20, MUTED, true);
            clear.setGravity(Gravity.CENTER);
            clear.setContentDescription(startDate ? "Xóa ngày bắt đầu" : "Xóa ngày kết thúc");
            container.addView(clear, new LinearLayout.LayoutParams(dp(38), dp(44)));
            clear.setOnClickListener(v -> {
                if (startDate) cookingJournalFromDate = 0;
                else cookingJournalToDate = 0;
                render();
            });
        }
        return container;
    }

    private void clearCookingJournalDateRange() {
        cookingJournalFromDate = 0;
        cookingJournalToDate = 0;
    }

    private void showJournalDatePicker(boolean startDate) {
        long selectedDate = startDate ? cookingJournalFromDate : cookingJournalToDate;
        Calendar selected = Calendar.getInstance();
        if (selectedDate > 0) selected.setTimeInMillis(selectedDate);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.clear();
            picked.set(year, month, day);
            long value = picked.getTimeInMillis();
            if (startDate) cookingJournalFromDate = value;
            else cookingJournalToDate = value;
            render();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addJournalRatingChip(LinearLayout parent, String label, int rating) {
        boolean selected = cookingJournalRatingFilter == rating;
        TextView chip = text(label, 12, selected ? GREEN : MUTED, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(11), 0, dp(11), 0);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? PALE_GREEN : BORDER, 20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(34));
        params.rightMargin = dp(7);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            cookingJournalRatingFilter = rating;
            render();
        });
    }

    private void populateAllCookingLogs(LinearLayout rows, int totalCount) {
        if (rows == null) return;
        rows.removeAllViews();
        long fromInclusive = cookingJournalFromDate;
        long toExclusive = 0;
        if (cookingJournalToDate > 0) {
            Calendar nextDay = Calendar.getInstance();
            nextDay.setTimeInMillis(cookingJournalToDate);
            nextDay.add(Calendar.DAY_OF_YEAR, 1);
            toExclusive = nextDay.getTimeInMillis();
        }
        if (fromInclusive > 0 && toExclusive > 0 && fromInclusive >= toExclusive) {
            addEmptyCard(rows, "Khoảng ngày chưa hợp lệ", "Ngày bắt đầu phải trước hoặc cùng ngày kết thúc.", () -> {
                cookingJournalFromDate = 0;
                cookingJournalToDate = 0;
                render();
            });
            return;
        }
        List<CookingLogEntry> filtered = cookingJournalViewModel.filterLogs(
                cookingJournalAllLogs, cookingJournalSearch, fromInclusive, toExclusive, cookingJournalRatingFilter);
        addText(rows, filtered.size() + " lần nấu",
                13, MUTED, false, 0, 4, 0, 8);
        if (filtered.isEmpty()) {
            if (totalCount == 0) {
                addEmptyCard(rows, "Chưa có lần nấu nào", "Mở ghi chú của một món để ghi lại lần nấu đầu tiên.",
                        () -> { screen = Screen.LIBRARY; render(); });
            } else {
                addEmptyCard(rows, "Không tìm thấy lần nấu phù hợp", "Thử đổi từ khóa hoặc điều kiện lọc.", () -> {
                    cookingJournalSearch = "";
                    cookingJournalFromDate = 0;
                    cookingJournalToDate = 0;
                    cookingJournalRatingFilter = 0;
                    render();
                });
            }
            return;
        }
        for (CookingLogEntry log : filtered) addCookingLogCard(rows, log, true);
    }

    private void renderCookingLogs() {
        Recipe recipe = recipeById(cookingLogRecipeId);
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Nhật ký nấu");
        LinearLayout recipeCard = new LinearLayout(this);
        recipeCard.setGravity(Gravity.CENTER_VERTICAL);
        recipeCard.setPadding(dp(8), dp(7), dp(12), dp(7));
        recipeCard.setBackground(roundRect(WHITE, BORDER, 14));
        recipeCard.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(58), dp(52)));
        LinearLayout recipeLabels = vertical();
        recipeLabels.setPadding(dp(12), 0, 0, 0);
        recipeLabels.addView(text(recipe.title, 14, TEXT, true));
        recipeLabels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
        recipeCard.addView(recipeLabels, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams recipeParams = new LinearLayout.LayoutParams(-1, -2);
        recipeParams.topMargin = dp(18);
        page.addView(recipeCard, recipeParams);

        List<CookingLogEntry> logs = cookingJournalViewModel.getLogs(recipe.id);
        addText(page, "Đã nấu " + logs.size() + (logs.size() == 1 ? " lần" : " lần"),
                14, MUTED, false, 0, 12, 0, 10);
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(0, 0, 0, dp(12));
        scroll.addView(content);
        if (logs.isEmpty()) {
            addEmptyCard(content, "Chưa có lần nấu nào", "Lưu lại cách bạn đã nấu để dễ xem lại lần sau.",
                    () -> openCookingLogForm(null, recipe.id));
        } else {
            for (CookingLogEntry log : logs) addCookingLogCard(content, log);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "+   Ghi lại lần nấu", true, () -> openCookingLogForm(null, recipe.id), 8);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void addCookingLogPreviewRow(LinearLayout parent, CookingLogEntry log) {
        LinearLayout card = vertical();
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(roundRect(WHITE, BORDER, 14));
        String summary = formatDate(log.cookedAt) + " · " + log.servings + " người" +
                (log.rating == null ? "" : " · " + ratingStars(log.rating));
        card.addView(text(summary, 12, MUTED, true));
        if (!log.adjustmentNote.isEmpty()) {
            TextView note = text(log.adjustmentNote, 13, TEXT, false);
            note.setMaxLines(2);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
            noteParams.topMargin = dp(4);
            card.addView(note, noteParams);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        parent.addView(card, params);
        card.setOnClickListener(v -> openCookingLogs(log.recipeId, Screen.NOTE));
    }

    private void addCookingLogCard(LinearLayout parent, CookingLogEntry log) {
        addCookingLogCard(parent, log, false);
    }

    private void addCookingLogCard(LinearLayout parent, CookingLogEntry log, boolean showRecipeTitle) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView photo = new ImageView(this);
        File imageFile = cookingJournalViewModel.getPhoto(log.photoLocalPath);
        if (imageFile != null && imageFile.isFile()) photo.setImageBitmap(decodeCookingPhoto(imageFile, dp(64), dp(64)));
        else photo.setImageResource(recipeDrawable(log.recipeId));
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackground(roundRect(BORDER, BORDER, 12));
        photo.setClipToOutline(true);
        card.addView(photo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout details = vertical();
        details.setPadding(dp(10), 0, dp(4), 0);
        if (showRecipeTitle) details.addView(text(log.title, 14, TEXT, true));
        details.addView(text(formatDate(log.cookedAt), showRecipeTitle ? 12 : 14,
                showRecipeTitle ? MUTED : TEXT, !showRecipeTitle));
        String info = log.servings + " người" + (log.actualDurationMinutes == null ? ""
                : " · " + log.actualDurationMinutes + " phút");
        TextView detail = text(info, 12, MUTED, false);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(3);
        details.addView(detail, infoParams);
        if (log.rating != null) {
            TextView stars = text(ratingStars(log.rating), 12, GREEN, true);
            LinearLayout.LayoutParams starsParams = new LinearLayout.LayoutParams(-1, -2);
            starsParams.topMargin = dp(2);
            details.addView(stars, starsParams);
        }
        if (!log.adjustmentNote.isEmpty()) {
            TextView note = text(log.adjustmentNote, 12, MUTED, false);
            note.setMaxLines(2);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
            noteParams.topMargin = dp(3);
            details.addView(note, noteParams);
        }
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        TextView delete = text("×", 24, ERROR, false);
        delete.setGravity(Gravity.CENTER);
        card.addView(delete, new LinearLayout.LayoutParams(dp(40), dp(48)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        parent.addView(card, params);
        card.setOnClickListener(v -> openCookingLogForm(log.id, log.recipeId));
        delete.setContentDescription("Xóa lần nấu ngày " + formatDate(log.cookedAt));
        delete.setOnClickListener(v -> confirmDeleteCookingLog(log));
    }

    private void openCookingLogForm(String logId, String recipeId) {
        if (screen == Screen.NOTE) cookingLogReturnScreen = Screen.NOTE;
        cookingLogFormReturnScreen = screen == Screen.COOKING_JOURNAL
                ? Screen.COOKING_JOURNAL : Screen.COOKING_LOGS;
        cookingLogRecipeId = recipeId;
        editingCookingLogId = logId;
        CookingLogEntry log = cookingJournalViewModel.getLog(logId);
        if (log == null) {
            cookingLogCookedAt = System.currentTimeMillis();
            cookingLogServings = 1;
            cookingLogDuration = "";
            cookingLogRating = null;
            cookingLogAdjustment = "";
            cookingLogPhotoPath = "";
            originalCookingLogPhotoPath = "";
        } else {
            cookingLogRecipeId = log.recipeId;
            cookingLogCookedAt = log.cookedAt;
            cookingLogServings = log.servings;
            cookingLogDuration = log.actualDurationMinutes == null ? "" : String.valueOf(log.actualDurationMinutes);
            cookingLogRating = log.rating;
            cookingLogAdjustment = log.adjustmentNote;
            cookingLogPhotoPath = log.photoLocalPath;
            originalCookingLogPhotoPath = log.photoLocalPath;
        }
        screen = Screen.COOKING_LOG_FORM;
        render();
    }

    private void renderCookingLogForm() {
        Recipe recipe = recipeById(cookingLogRecipeId);
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(14));
        addBackTitle(page, editingCookingLogId == null ? "Ghi lại lần nấu" : "Sửa lần nấu");
        ScrollView scroll = scrollContent();
        LinearLayout fields = vertical();
        fields.setPadding(0, dp(16), 0, dp(18));
        scroll.addView(fields);
        LinearLayout recipeCard = new LinearLayout(this);
        recipeCard.setGravity(Gravity.CENTER_VERTICAL);
        recipeCard.setPadding(dp(8), dp(6), dp(10), dp(6));
        recipeCard.setBackground(roundRect(WHITE, BORDER, 14));
        recipeCard.addView(recipeImage(recipeById(cookingLogRecipeId)), new LinearLayout.LayoutParams(dp(56), dp(50)));
        TextView title = text(recipe.title, 14, TEXT, true);
        title.setPadding(dp(10), 0, 0, 0);
        recipeCard.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        fields.addView(recipeCard);

        addText(fields, "Ngày nấu", 14, TEXT, true, 0, 20, 0, 7);
        cookingLogDateInput = text(formatDate(cookingLogCookedAt) + "     ▾", 15, TEXT, false);
        cookingLogDateInput.setPadding(dp(14), 0, dp(14), 0);
        cookingLogDateInput.setBackground(roundRect(WHITE, BORDER, 14));
        fields.addView(cookingLogDateInput, new LinearLayout.LayoutParams(-1, dp(50)));
        cookingLogDateInput.setOnClickListener(v -> showCookingLogDatePicker());

        addText(fields, "Khẩu phần", 14, TEXT, true, 0, 18, 0, 7);
        cookingLogServingsInput = editField(String.valueOf(cookingLogServings), 16, 50);
        cookingLogServingsInput.setSingleLine(true);
        cookingLogServingsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        cookingLogServingsInput.setHint("Số người");
        fields.addView(cookingLogServingsInput, new LinearLayout.LayoutParams(-1, -2));

        addText(fields, "Thời gian thực tế (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
        cookingLogDurationInput = editField(cookingLogDuration, 16, 50);
        cookingLogDurationInput.setSingleLine(true);
        cookingLogDurationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        cookingLogDurationInput.setHint("Số phút");
        fields.addView(cookingLogDurationInput, new LinearLayout.LayoutParams(-1, -2));

        addText(fields, "Bạn đánh giá món này (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
        cookingLogRatingRow = new LinearLayout(this);
        cookingLogRatingRow.setGravity(Gravity.CENTER_VERTICAL);
        fields.addView(cookingLogRatingRow, new LinearLayout.LayoutParams(-1, dp(48)));
        populateCookingLogRating();

        addText(fields, "Điều chỉnh lần này (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
        cookingLogAdjustmentInput = editField(cookingLogAdjustment, 15, 100);
        cookingLogAdjustmentInput.setGravity(Gravity.TOP | Gravity.START);
        cookingLogAdjustmentInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        cookingLogAdjustmentInput.setHint("Ví dụ: giảm me, thêm rau thơm...");
        fields.addView(cookingLogAdjustmentInput, new LinearLayout.LayoutParams(-1, dp(104)));

        addText(fields, "Ảnh món đã nấu (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
        cookingLogPhotoButton = text(cookingLogPhotoPath.isEmpty() ? "+   Thêm ảnh" : "Đổi ảnh", 14, GREEN, true);
        cookingLogPhotoButton.setGravity(Gravity.CENTER);
        cookingLogPhotoButton.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        fields.addView(cookingLogPhotoButton, new LinearLayout.LayoutParams(-1, dp(48)));
        cookingLogPhotoButton.setOnClickListener(v -> {
            captureCookingLogDraft();
            cookingPhotoPicker.launch("image/*");
        });
        if (!cookingLogPhotoPath.isEmpty()) {
            ImageView preview = new ImageView(this);
            File photo = cookingJournalViewModel.getPhoto(cookingLogPhotoPath);
            if (photo != null && photo.isFile()) preview.setImageBitmap(decodeCookingPhoto(photo, dp(800), dp(320)));
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setBackground(roundRect(BORDER, BORDER, 14));
            preview.setClipToOutline(true);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(160));
            previewParams.topMargin = dp(8);
            fields.addView(preview, previewParams);
            TextView removePhoto = text("Xóa ảnh", 13, ERROR, true);
            removePhoto.setGravity(Gravity.CENTER);
            fields.addView(removePhoto, new LinearLayout.LayoutParams(-1, dp(40)));
            removePhoto.setOnClickListener(v -> {
                captureCookingLogDraft();
                if (!originalCookingLogPhotoPath.equals(cookingLogPhotoPath)) cookingJournalViewModel.deletePhoto(cookingLogPhotoPath);
                cookingLogPhotoPath = "";
                render();
            });
        }

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, editingCookingLogId == null ? "Lưu lần nấu" : "Lưu thay đổi", true,
                this::saveCookingLogForm, 8);
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void populateCookingLogRating() {
        if (cookingLogRatingRow == null) return;
        cookingLogRatingRow.removeAllViews();
        for (int value = 1; value <= 5; value++) {
            final int rating = value;
            TextView star = text(value <= (cookingLogRating == null ? 0 : cookingLogRating) ? "★" : "☆",
                    27, value <= (cookingLogRating == null ? 0 : cookingLogRating) ? GREEN : MUTED, true);
            star.setGravity(Gravity.CENTER);
            cookingLogRatingRow.addView(star, new LinearLayout.LayoutParams(dp(44), dp(44)));
            star.setContentDescription("Đánh giá " + value + " trên 5 sao");
            star.setOnClickListener(v -> {
                captureCookingLogDraft();
                cookingLogRating = cookingLogRating != null && cookingLogRating == rating ? null : rating;
                render();
            });
        }
        if (cookingLogRating != null) {
            TextView clear = text("Bỏ", 12, MUTED, false);
            clear.setGravity(Gravity.CENTER);
            cookingLogRatingRow.addView(clear, new LinearLayout.LayoutParams(dp(42), dp(44)));
            clear.setOnClickListener(v -> {
                captureCookingLogDraft();
                cookingLogRating = null;
                render();
            });
        }
    }

    private void captureCookingLogDraft() {
        if (cookingLogServingsInput != null) cookingLogServings = parseInteger(cookingLogServingsInput.getText().toString(), cookingLogServings);
        if (cookingLogDurationInput != null) cookingLogDuration = cookingLogDurationInput.getText().toString();
        if (cookingLogAdjustmentInput != null) cookingLogAdjustment = cookingLogAdjustmentInput.getText().toString();
    }

    private void showCookingLogDatePicker() {
        captureCookingLogDraft();
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(cookingLogCookedAt);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.clear();
            date.set(year, month, day);
            cookingLogCookedAt = date.getTimeInMillis();
            render();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveCookingLogForm() {
        captureCookingLogDraft();
        if (cookingLogServings < 1 || cookingLogServings > 999) {
            Toast.makeText(this, "Khẩu phần phải từ 1 đến 999.", Toast.LENGTH_SHORT).show();
            return;
        }
        Integer duration = null;
        String durationText = cookingLogDuration.trim();
        if (!durationText.isEmpty()) {
            try {
                duration = Integer.parseInt(durationText);
                if (duration < 1) throw new NumberFormatException();
            } catch (NumberFormatException invalid) {
                Toast.makeText(this, "Thời gian phải là số phút lớn hơn 0.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Recipe recipe = recipeById(cookingLogRecipeId);
        if (!cookingJournalViewModel.save(editingCookingLogId, recipe, cookingLogCookedAt,
                cookingLogServings, duration, cookingLogRating, cookingLogAdjustment, cookingLogPhotoPath)) {
            Toast.makeText(this, "Không thể lưu lần nấu. Vui lòng kiểm tra thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!originalCookingLogPhotoPath.isEmpty() && !originalCookingLogPhotoPath.equals(cookingLogPhotoPath)) {
            cookingJournalViewModel.deletePhoto(originalCookingLogPhotoPath);
        }
        originalCookingLogPhotoPath = cookingLogPhotoPath;
        editingCookingLogId = null;
        screen = cookingLogFormReturnScreen;
        render();
        Toast.makeText(this, "Đã lưu lần nấu.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteCookingLog(CookingLogEntry log) {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Xóa lần nấu?")
                .setMessage("Xóa nhật ký nấu ngày " + formatDate(log.cookedAt) + "? Công thức và ghi chú chung vẫn được giữ.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    cookingJournalViewModel.delete(log.id);
                    cookingJournalViewModel.deletePhoto(log.photoLocalPath);
                    render();
                }).show();
    }

    private android.graphics.Bitmap decodeCookingPhoto(File file, int requestedWidth, int requestedHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (bounds.outWidth / options.inSampleSize > requestedWidth * 2
                || bounds.outHeight / options.inSampleSize > requestedHeight * 2) {
            options.inSampleSize *= 2;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private void deleteCookingLogPhoto(String filename) {
        cookingJournalViewModel.deletePhoto(filename);
    }

    private void discardCookingLogDraftPhoto() {
        if (!cookingLogPhotoPath.isEmpty() && !cookingLogPhotoPath.equals(originalCookingLogPhotoPath)) {
            deleteCookingLogPhoto(cookingLogPhotoPath);
        }
    }

    private String formatIsoDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(millis));
    }

    private String displayIsoDate(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(isoDate);
            return date == null ? isoDate : new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(date);
        } catch (java.text.ParseException invalid) {
            return isoDate;
        }
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(millis));
    }

    private String ratingStars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) stars.append(i <= rating ? '★' : '☆');
        return stars.toString();
    }

    private String normalizeSearchText(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return decomposed.replaceAll("\\s+", " ").trim();
    }

    private int parseInteger(String value, int fallback) {
        try { return Integer.parseInt(value == null ? "" : value.trim()); }
        catch (NumberFormatException invalid) { return fallback; }
    }

    private void addShoppingTitleWithPantry(LinearLayout page) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Danh sách mua sắm", 23, TEXT, true);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pantry = text("Tủ nguyên liệu", 12, GREEN, true);
        pantry.setGravity(Gravity.CENTER);
        pantry.setPadding(dp(8), 0, dp(8), 0);
        pantry.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
        row.addView(pantry, new LinearLayout.LayoutParams(-2, dp(40)));
        pantry.setOnClickListener(v -> openPantry(Screen.SHOPPING));
        page.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addRecipeGrid(LinearLayout parent, List<Recipe> recipes, boolean showFavorite, boolean removable) {
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
            ImageView star = iconView(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline, GREEN);
            star.setBackground(roundRect(WHITE, WHITE, 18));
            FrameLayout starTouchTarget = new FrameLayout(this);
            starTouchTarget.setFocusable(true);
            starTouchTarget.setContentDescription(favorite ? "Bỏ yêu thích" : "Lưu yêu thích");
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER);
            starTouchTarget.addView(star, iconParams);
            FrameLayout.LayoutParams touchParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
            touchParams.setMargins(0, dp(2), dp(2), 0);
            imageFrame.addView(starTouchTarget, touchParams);
            starTouchTarget.setOnClickListener(v -> {
                if (personalDataViewModel.isFavorite(recipe.id)) personalDataViewModel.removeFavorite(recipe.id);
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
        if (removable) card.setOnClickListener(v -> {
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

    private void addSectionHeader(LinearLayout parent, String title, String action, Runnable onAction, int top) {
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

    private void addEmptyFavoriteCard(LinearLayout parent) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(9), dp(10), dp(14), dp(10));
        item.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView star = iconView(R.drawable.ic_star_outline, GREEN);
        star.setPadding(dp(10), dp(10), dp(10), dp(10));
        star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 13));
        item.addView(star, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text("Chưa có món yêu thích nào", 13, TEXT, true));
        labels.addView(text("Lưu món để tìm lại nhanh hơn.", 11, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(text("›", 23, GREEN, false));
        item.setFocusable(true);
        item.setContentDescription("Chưa có món yêu thích nào. Mở danh sách yêu thích.");
        item.setOnClickListener(v -> {
            screen = Screen.FAVORITES;
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(62));
        parent.addView(item, params);
    }

    private LinearLayout collectionActionButton(String label, int icon, int tint, int background, Runnable action) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(roundRect(background, background == WHITE ? BORDER : background, 14));
        ImageView image = iconView(icon, tint);
        button.addView(image, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView caption = text(label, 12, tint, true);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-2, -2);
        captionParams.setMarginStart(dp(5));
        button.addView(caption, captionParams);
        button.setContentDescription(label + " bộ sưu tập");
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void addRelatedCollectionRow(LinearLayout parent, RecipeCollection row) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(7), dp(5), dp(12), dp(5));
        item.setBackground(roundRect(WHITE, BORDER, 14));
        List<Recipe> recipes = personalDataViewModel.getCollectionRecipes(row.id);
        if (recipes.isEmpty()) {
            ImageView folder = iconView(R.drawable.ic_folder_outline, GREEN);
            folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 12));
            item.addView(folder, new LinearLayout.LayoutParams(dp(52), dp(52)));
        } else {
            item.addView(recipeImage(recipes.get(0)), new LinearLayout.LayoutParams(dp(52), dp(52)));
        }
        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(row.name, 14, TEXT, true));
        labels.addView(text(row.recipeCount + " món đã lưu", 12, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(text("›", 23, GREEN, false));
        item.setOnClickListener(v -> {
            selectedCollectionId = row.id;
            screen = Screen.COLLECTION;
            render();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.bottomMargin = dp(9);
        parent.addView(item, params);
    }

    private void addHistoryRow(LinearLayout parent, Recipe recipe) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(7), dp(5), dp(12), dp(5));
        item.setBackground(roundRect(WHITE, BORDER, 14));
        ImageView image = recipeImage(recipe);
        item.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout labels = vertical();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(recipe.title, 14, TEXT, true));
        labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams favoriteParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        favoriteParams.setMarginEnd(dp(8));
        item.addView(favoriteToggleButton(recipe), favoriteParams);
        item.addView(text("›", 23, GREEN, false));
        item.setOnClickListener(v -> {
            openRecipeDetail(recipe);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.bottomMargin = dp(9);
        parent.addView(item, params);
    }

    private FrameLayout favoriteToggleButton(Recipe recipe) {
        boolean favorite = personalDataViewModel.isFavorite(recipe.id);
        FrameLayout button = new FrameLayout(this);
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView star = iconView(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline, GREEN);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER);
        button.addView(star, iconParams);
        updateFavoriteToggleDescription(button, favorite, recipe);
        button.setOnClickListener(v -> {
            boolean nowFavorite = !personalDataViewModel.isFavorite(recipe.id);
            if (nowFavorite) personalDataViewModel.addFavorite(recipe);
            else personalDataViewModel.removeFavorite(recipe.id);
            star.setImageResource(nowFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            star.setColorFilter(GREEN);
            updateFavoriteToggleDescription(button, nowFavorite, recipe);
        });
        return button;
    }

    private static void updateFavoriteToggleDescription(FrameLayout button, boolean favorite, Recipe recipe) {
        button.setContentDescription((favorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
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
        int[] icons = {R.drawable.ic_home_outline, R.drawable.ic_calendar_outline,
                R.drawable.ic_cart_outline, R.drawable.ic_star_outline};
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
                    clearCookingJournalDateRangeIfLeavingJournal();
                    screen = Screen.DISCOVER;
                    render();
                } else if (index == 3) {
                    clearCookingJournalDateRangeIfLeavingJournal();
                    screen = Screen.LIBRARY;
                    render();
                } else if (index == 1) {
                    clearCookingJournalDateRangeIfLeavingJournal();
                    screen = Screen.MEAL_PLAN;
                    render();
                } else if (index == 2) {
                    clearCookingJournalDateRangeIfLeavingJournal();
                    openShoppingList(screen);
                }
            });
        }
        appRoot.addView(nav, new LinearLayout.LayoutParams(-1, dp(64)));
    }

    private void clearCookingJournalDateRangeIfLeavingJournal() {
        if (screen == Screen.COOKING_JOURNAL) clearCookingJournalDateRange();
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
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density;
        float availableWidthDp = Math.max(160, screenWidthDp - 108);
        float titleWidthDp = heading.getPaint().measureText(title) /
                getResources().getDisplayMetrics().density;
        if (titleWidthDp > availableWidthDp) {
            heading.setTextSize(Math.max(17, 23 * availableWidthDp / titleWidthDp));
        }
        heading.setPadding(dp(12), 0, 0, 0);
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private TextView addButton(LinearLayout parent, String label, boolean primary, Runnable action, int top) {
        TextView button = text(label, 15, primary ? WHITE : GREEN, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(49));
        params.topMargin = dp(top);
        parent.addView(button, params);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void saveCollectionForm() {
        String name = collectionNameInput.getText().toString();
        personalDataViewModel.setCollectionNameDraft(name);
        personalDataViewModel.markCollectionSubmitAttempted();
        if (name.trim().isEmpty()) {
            updateCollectionFormValidation();
            return;
        }
        Long currentId = selectedCollectionId > 0 ? selectedCollectionId : null;
        Long savedCollectionId = personalDataViewModel.saveCollectionForm(currentId, PreviewRecipeCatalog.designRecipes());
        if (savedCollectionId == null) {
            collectionError.setText("Tên này đã được sử dụng.");
            collectionNameInput.setBackground(roundRect(WHITE, ERROR, 14));
            updateCollectionSaveButton(false);
            return;
        }
        selectedCollectionId = savedCollectionId;
        screen = Screen.COLLECTION;
        render();
    }

    private void updateCollectionFormValidation() {
        String name = collectionNameInput.getText().toString();
        personalDataViewModel.setCollectionNameDraft(name);
        boolean nonEmpty = !name.trim().isEmpty();
        Long currentId = selectedCollectionId > 0 ? selectedCollectionId : null;
        boolean available = nonEmpty && personalDataViewModel.isCollectionNameAvailable(name, currentId);
        if (personalDataViewModel.wasCollectionSubmitAttempted()) {
            if (!nonEmpty) {
                collectionError.setText("");
                collectionNameInput.setBackground(roundRect(WHITE, BORDER, 14));
            } else if (!available) {
                collectionError.setText("Tên này đã được sử dụng.");
                collectionNameInput.setBackground(roundRect(WHITE, ERROR, 14));
            } else {
                collectionError.setText("");
                collectionNameInput.setBackground(roundRect(WHITE, BORDER, 14));
            }
        }
        updateCollectionSaveButton(personalDataViewModel.wasCollectionSubmitAttempted() ? available : nonEmpty);
    }

    private void updateCollectionSaveButton(boolean enabled) {
        if (collectionSaveButton == null) return;
        collectionSaveButton.setEnabled(enabled);
        collectionSaveButton.setTextColor(enabled ? WHITE : MUTED);
        collectionSaveButton.setBackground(roundRect(enabled ? GREEN : PALE_GREEN,
                enabled ? GREEN : PALE_GREEN, 14));
    }

    private void openCollectionForm(Long collectionId) {
        if (collectionId == null && screen != Screen.COLLECTION) collectionReturnScreen = screen;
        collectionFormSearchQuery = "";
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

    private void confirmDeleteCollection(RecipeCollection collection) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa bộ sưu tập?")
                .setMessage("Bộ sưu tập “" + collection.name + "” và danh sách món trong đó sẽ bị xóa. Yêu thích và ghi chú của các món vẫn được giữ.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    personalDataViewModel.deleteCollection(collection.id);
                    selectedCollectionId = -1L;
                    screen = collectionReturnScreen;
                    render();
                })
                .show();
    }

    private RecipeCollection findCollection(long id) {
        for (RecipeCollection row : personalDataViewModel.getCollections()) if (row.id == id) return row;
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
            case "sample-canh-chua-ca": return R.drawable.recipe_canh_chua;
            case "sample-ga-chien-nuoc-mam": return R.drawable.recipe_ga_chien;
            case "sample-mi-y-sot-bo-bam": return R.drawable.recipe_mi_y;
            case "sample-com-rang-dua-bo": return R.drawable.recipe_com_rang;
            case "sample-salad-rau-cu": return R.drawable.recipe_salad;
            default: return R.drawable.recipe_canh_chua;
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

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold,
                         int left, int top, int right, int bottom) {
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

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private Recipe recipeById(String id) {
        for (Recipe recipe : PreviewRecipeCatalog.designRecipes()) if (recipe.id.equals(id)) return recipe;
        return PreviewRecipeCatalog.designRecipes().get(0);
    }
}
