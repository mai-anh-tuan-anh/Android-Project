package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Local personal data, the current meal plan, and shopping (modules 5–7). */
public class MainActivity extends AppCompatActivity {
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
    private static final String STATE_SHOPPING_STATUS = "module7.status";
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

    static final List<RecipePreview> DESIGN_RECIPES = Arrays.asList(
            new RecipePreview("sample-canh-chua-ca", "Canh chua cá", "recipe_canh_chua", "30 phút", "Dễ"),
            new RecipePreview("sample-ga-chien-nuoc-mam", "Gà chiên nước mắm", "recipe_ga_chien", "25 phút", "Dễ"),
            new RecipePreview("sample-mi-y-sot-bo-bam", "Mì Ý sốt bò bằm", "recipe_mi_y", "35 phút", "Vừa"),
            new RecipePreview("sample-com-rang-dua-bo", "Cơm rang dưa bò", "recipe_com_rang", "20 phút", "Dễ"),
            new RecipePreview("sample-salad-rau-cu", "Salad rau củ", "recipe_salad", "15 phút", "Dễ")
    );
    private static final List<RecipePreview> MEAL_PLAN_PREVIEWS = Arrays.asList(
            DESIGN_RECIPES.get(0), DESIGN_RECIPES.get(1), DESIGN_RECIPES.get(3));

    private PersonalDataViewModel personalDataViewModel;
    private MealPlanViewModel mealPlanViewModel;
    private ShoppingViewModel shoppingViewModel;
    private LinearLayout appRoot;
    private Screen screen = Screen.LIBRARY;
    private Screen noteReturnScreen = Screen.LIBRARY;
    private Screen collectionReturnScreen = Screen.LIBRARY;
    private long selectedCollectionId = -1L;
    private String noteRecipeId = DESIGN_RECIPES.get(0).id;
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
    private Screen shoppingReturnScreen = Screen.MEAL_PLAN;
    private String editingShoppingItemId;
    private String shoppingFormStatus = ShoppingViewModel.NEED_TO_BUY;
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

    enum Screen { LIBRARY, FAVORITES, COLLECTION_LIST, COLLECTION, COLLECTION_FORM, NOTE, MEAL_PLAN,
        MEAL_PLAN_PICKER, SHOPPING, SHOPPING_ITEM_FORM }

    static final class RecipePreview {
        final String id;
        final String title;
        final String imageName;
        final String duration;
        final String difficulty;
        RecipePreview(String id, String title, String imageName, String duration, String difficulty) {
            this.id = id;
            this.title = title;
            this.imageName = imageName;
            this.duration = duration;
            this.difficulty = difficulty;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (screen == Screen.LIBRARY) {
                    finish();
                    return;
                }
                if (screen == Screen.COLLECTION_FORM) {
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
                    screen = shoppingReturnScreen;
                } else {
                    screen = Screen.LIBRARY;
                }
                render();
            }
        });
        personalDataViewModel = new ViewModelProvider(this).get(PersonalDataViewModel.class);
        mealPlanViewModel = new ViewModelProvider(this).get(MealPlanViewModel.class);
        shoppingViewModel = new ViewModelProvider(this).get(ShoppingViewModel.class);
        personalDataViewModel.seedDesignPreviewIfNeeded(DESIGN_RECIPES);
        restoreMealPlanState(savedInstanceState);
        render();
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
            shoppingFormStatus = state.getString(STATE_SHOPPING_STATUS, ShoppingViewModel.NEED_TO_BUY);
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
            outState.putString(STATE_SHOPPING_STATUS, shoppingFormStatus);
            outState.putString(STATE_SHOPPING_FILTER, shoppingStatusFilter);
            outState.putString(STATE_SHOPPING_RETURN_SCREEN, shoppingReturnScreen.name());
        }
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

        if (screen == Screen.LIBRARY) renderLibrary();
        else if (screen == Screen.FAVORITES) renderFavorites();
        else if (screen == Screen.COLLECTION_LIST) renderCollections();
        else if (screen == Screen.COLLECTION) renderCollection();
        else if (screen == Screen.COLLECTION_FORM) renderCollectionForm();
        else if (screen == Screen.NOTE) renderNote();
        else if (screen == Screen.MEAL_PLAN) renderMealPlan();
        else if (screen == Screen.MEAL_PLAN_PICKER) renderMealPlanPicker();
        else if (screen == Screen.SHOPPING) renderShoppingList();
        else renderShoppingItemForm();
        setContentView(frame);
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
        List<RecipePreview> favoriteRows = personalDataViewModel.getFavorites();
        if (favoriteRows.isEmpty()) addEmptyFavoriteCard(content);
        else addRecipeGrid(content, favoriteRows.subList(0, Math.min(2, favoriteRows.size())), true, true);

        addSectionHeader(content, "Bộ sưu tập", "Xem tất cả ›", () -> {
            screen = Screen.COLLECTION_LIST;
            render();
        }, 14);
        List<PersonalDataDbHelper.CollectionRow> collections = personalDataViewModel.getCollections();
        if (collections.isEmpty()) addEmptyCard(content, "Chưa có bộ sưu tập",
                "Tạo bộ sưu tập đầu tiên của bạn.", () -> openCollectionForm(null));
        else {
            for (int i = 0; i < Math.min(5, collections.size()); i++) addCollectionRow(content, collections.get(i), 0);
        }

        addText(content, "Đã xem gần đây", 19, TEXT, true, 0, 24, 0, 12);
        List<RecipePreview> history = personalDataViewModel.getHistory();
        for (int i = 0; i < Math.min(10, history.size()); i++) addHistoryRow(content, history.get(i));

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(3);
    }

    private void renderFavorites() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Yêu thích");
        addText(page, "Những công thức bạn đã lưu.", 14, MUTED, false, 0, 0, 0, 16);
        List<RecipePreview> favorites = personalDataViewModel.getFavorites();
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

    private void populateFavoriteResults(LinearLayout content, List<RecipePreview> favorites) {
        content.removeAllViews();
        String query = normalizeRecipeSearch(favoritesSearchQuery);
        List<RecipePreview> matches = new ArrayList<>();
        for (RecipePreview recipe : favorites) {
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

    private void populateCollectionRecipeResults(LinearLayout results, List<RecipePreview> recipes) {
        if (results == null) return;
        results.removeAllViews();
        String query = normalizeRecipeSearch(collectionDetailSearchQuery);
        List<RecipePreview> matches = new ArrayList<>();
        for (RecipePreview recipe : recipes) {
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
        for (RecipePreview recipe : DESIGN_RECIPES) {
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
        List<PersonalDataDbHelper.CollectionRow> collections = personalDataViewModel.getCollections();
        if (collections.isEmpty()) {
            addEmptyCard(content, "Chưa có bộ sưu tập", "Tạo bộ sưu tập đầu tiên của bạn.",
                    () -> openCollectionForm(null));
        } else {
            for (PersonalDataDbHelper.CollectionRow collection : collections) addCollectionRow(content, collection, 0);
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

        List<PersonalDataDbHelper.PlannedRecipe> planned = mealPlanViewModel.getMealPlanRecipes();
        if (!planned.isEmpty()) {
            TextView summary = text(planned.size() + " món cho bữa cơm nhà.", 15, MUTED, false);
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
            for (PersonalDataDbHelper.PlannedRecipe item : planned) addPlannedRecipeCard(content, item);
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
        addText(page, "Danh sách mua sắm", 23, TEXT, true, 0, 0, 0, 0);

        List<PersonalDataDbHelper.ShoppingItem> items = shoppingViewModel.getItems();
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
        for (PersonalDataDbHelper.ShoppingItem item : items) {
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
        Map<String, List<PersonalDataDbHelper.ShoppingItem>> groupedItems = new LinkedHashMap<>();
        for (String category : ShoppingCategoryCatalog.ORDER) groupedItems.put(category, new ArrayList<>());
        for (PersonalDataDbHelper.ShoppingItem item : items) {
            if (shoppingStatusFilter != null && !shoppingStatusFilter.equals(item.status)) continue;
            String category = ShoppingCategoryCatalog.contains(item.category)
                    ? item.category : ShoppingCategoryCatalog.OTHER;
            groupedItems.get(category).add(item);
        }
        for (Map.Entry<String, List<PersonalDataDbHelper.ShoppingItem>> group : groupedItems.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            addText(content, group.getKey(), 20, TEXT, true, 0, 0, 0, 0);
            for (PersonalDataDbHelper.ShoppingItem item : group.getValue()) {
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

    private int filteredShoppingCount(List<PersonalDataDbHelper.ShoppingItem> items) {
        int count = 0;
        for (PersonalDataDbHelper.ShoppingItem item : items) {
            if (shoppingStatusFilter != null && shoppingStatusFilter.equals(item.status)) count++;
        }
        return count;
    }

    private void addShoppingItemRow(LinearLayout parent, PersonalDataDbHelper.ShoppingItem item) {
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
            shoppingViewModel.setStatus(item.id, newStatus);
            shoppingStatusFilter = newStatus;
            render();
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
        PersonalDataDbHelper.ShoppingItem item = shoppingViewModel.findItem(itemId);
        shoppingDraftName = item == null ? "" : item.name;
        shoppingDraftAmount = item == null ? "" : item.amount;
        shoppingDraftUnit = item == null ? "" : KitchenUnitCatalog.label(item.unit);
        shoppingDraftUnitId = item == null ? "" : item.unit;
        shoppingDraftCategory = item == null ? ShoppingCategoryCatalog.OTHER : item.category;
        shoppingFormStatus = item == null ? ShoppingViewModel.NEED_TO_BUY : item.status;
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
        shoppingCategoryInput = text(shoppingDraftCategory + "     ▾", 16, TEXT, false);
        shoppingCategoryInput.setGravity(Gravity.CENTER_VERTICAL);
        shoppingCategoryInput.setPadding(dp(15), 0, dp(15), 0);
        shoppingCategoryInput.setBackground(roundRect(WHITE, BORDER, 14));
        addFieldView(fields, shoppingCategoryInput, 10, 26);
        shoppingCategoryInput.setOnClickListener(v -> showShoppingCategoryPicker());

        addText(fields, "Trạng thái", 20, TEXT, true, 0, 4, 0, 12);
        LinearLayout statuses = new LinearLayout(this);
        statuses.setOrientation(LinearLayout.HORIZONTAL);
        statuses.setGravity(Gravity.CENTER_VERTICAL);
        addShoppingFormStatus(statuses, "Cần mua", ShoppingViewModel.NEED_TO_BUY);
        addShoppingFormStatus(statuses, "Đã có", ShoppingViewModel.AVAILABLE);
        fields.addView(statuses);
        TextView statusHint = text("Đã có nghĩa là nguyên liệu hiện đang có sẵn ở nhà.",
                13, MUTED, false);
        statusHint.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams statusHintParams = new LinearLayout.LayoutParams(-1, -2);
        statusHintParams.topMargin = dp(8);
        fields.addView(statusHint, statusHintParams);

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

    private void addShoppingFormStatus(LinearLayout parent, String label, String status) {
        boolean selected = status.equals(shoppingFormStatus);
        TextView chip = text(label, 14, selected ? GREEN : MUTED, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, BORDER, 24));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44));
        params.rightMargin = dp(8);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            shoppingDraftName = shoppingNameInput.getText().toString();
            shoppingDraftAmount = shoppingAmountInput.getText().toString();
            shoppingDraftUnit = shoppingUnitInput.getText().toString();
            shoppingFormStatus = status;
            render();
        });
    }

    private void saveShoppingForm() {
        shoppingDraftName = shoppingNameInput.getText().toString();
        shoppingDraftAmount = shoppingAmountInput.getText().toString();
        shoppingDraftUnit = shoppingUnitInput.getText().toString();
        String canonicalUnit = shoppingDraftUnit.equals(shoppingUnitInput.getText().toString())
                && !shoppingDraftUnitId.isEmpty()
                ? shoppingDraftUnitId : KitchenUnitCatalog.canonicalId(shoppingDraftUnit);
        if (!shoppingViewModel.saveItem(editingShoppingItemId, shoppingDraftName, shoppingDraftAmount,
                canonicalUnit, shoppingDraftCategory, shoppingFormStatus)) {
            Toast.makeText(this, "Vui lòng nhập tên, số lượng hợp lệ và đơn vị.", Toast.LENGTH_SHORT).show();
            return;
        }
        shoppingStatusFilter = shoppingFormStatus;
        screen = Screen.SHOPPING;
        render();
    }

    private void confirmDeleteShoppingItem(String itemId) {
        PersonalDataDbHelper.ShoppingItem item = shoppingViewModel.findItem(itemId);
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

    private void addPlannedRecipeCard(LinearLayout parent, PersonalDataDbHelper.PlannedRecipe item) {
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
        remove.setOnClickListener(v -> {
            mealPlanViewModel.removeMealPlanRecipe(item.recipe.id);
            render();
        });
    }

    private void changeServings(PersonalDataDbHelper.PlannedRecipe item, int delta) {
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
        List<RecipePreview> sourceRecipes = getCurrentMealPlanPickerRecipes();
        List<RecipePreview> matches = mealPlanViewModel.filterRecipes(sourceRecipes);
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
        for (RecipePreview recipe : matches) {
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
            row.setContentDescription("Xem ghi chú món " + recipe.title);
            row.setFocusable(true);
            row.setOnClickListener(v -> openRecipeNote(recipe));
            toggleTarget.setOnClickListener(v -> togglePlanPickerRecipe(recipe));
        }
    }

    private void applyMealPlanSearch(String query) {
        mealPlanViewModel.setSearchQuery(query);
        populateMealPlanPickerRows();
    }

    private List<RecipePreview> getCurrentMealPlanPickerRecipes() {
        return mealPlanViewModel.getPickerRecipes(MEAL_PLAN_PREVIEWS);
    }

    private void showMealPlanSourceMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Tất cả món");
        menu.getMenu().add(0, 2, 1, "Món yêu thích");
        Map<Integer, Long> collectionIdsByMenuId = new LinkedHashMap<>();
        List<PersonalDataDbHelper.CollectionRow> collections = mealPlanViewModel.getRecipeCollections();
        if (collections.isEmpty()) {
            menu.getMenu().add(0, 3, 2, "Chưa có bộ sưu tập").setEnabled(false);
        } else {
            int menuId = 10;
            int order = 2;
            for (PersonalDataDbHelper.CollectionRow collection : collections) {
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
                for (PersonalDataDbHelper.CollectionRow collection : mealPlanViewModel.getRecipeCollections()) {
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

    private void togglePlanPickerRecipe(RecipePreview recipe) {
        mealPlanViewModel.toggleRecipe(recipe.id);
        populateMealPlanPickerRows();
    }

    private void saveMealPlanSelection() {
        if (!mealPlanViewModel.saveSelection(mealPlanViewModel.getAllSelectableRecipes(DESIGN_RECIPES))) {
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
        PersonalDataDbHelper.CollectionRow selected = findCollection(selectedCollectionId);
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
        List<RecipePreview> collectionRecipes = personalDataViewModel.getCollectionRecipes(selectedCollectionId);
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
        List<PersonalDataDbHelper.CollectionRow> otherCollections = personalDataViewModel.getCollections();
        boolean hasOtherCollections = false;
        for (PersonalDataDbHelper.CollectionRow row : otherCollections) {
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
        RecipePreview recipe = recipeById(noteRecipeId);
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

    private void addRecipeGrid(LinearLayout parent, List<RecipePreview> recipes, boolean showFavorite, boolean removable) {
        if (recipes.isEmpty()) return;
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        int index = 0;
        for (RecipePreview recipe : recipes) {
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

    private LinearLayout recipeCard(RecipePreview recipe, boolean showFavorite, boolean removable) {
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
            openRecipeNote(recipe);
        });
        return card;
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

    private void addCollectionRow(LinearLayout parent, PersonalDataDbHelper.CollectionRow row, int top) {
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

    private void addRelatedCollectionRow(LinearLayout parent, PersonalDataDbHelper.CollectionRow row) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(7), dp(5), dp(12), dp(5));
        item.setBackground(roundRect(WHITE, BORDER, 14));
        List<RecipePreview> recipes = personalDataViewModel.getCollectionRecipes(row.id);
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

    private void addHistoryRow(LinearLayout parent, RecipePreview recipe) {
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
            openRecipeNote(recipe);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.bottomMargin = dp(9);
        parent.addView(item, params);
    }

    private FrameLayout favoriteToggleButton(RecipePreview recipe) {
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

    private static void updateFavoriteToggleDescription(FrameLayout button, boolean favorite, RecipePreview recipe) {
        button.setContentDescription((favorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    }

    private void addSelectableRecipeRow(LinearLayout parent, RecipePreview recipe) {
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
                if (index == 3) {
                    screen = Screen.LIBRARY;
                    render();
                } else if (index == 1) {
                    screen = Screen.MEAL_PLAN;
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
        Long savedCollectionId = personalDataViewModel.saveCollectionForm(currentId, DESIGN_RECIPES);
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
        List<RecipePreview> recipes = new ArrayList<>();
        if (collectionId != null) {
            PersonalDataDbHelper.CollectionRow row = findCollection(collectionId);
            if (row != null) name = row.name;
            recipes.addAll(personalDataViewModel.getCollectionRecipes(collectionId));
        }
        personalDataViewModel.beginCollectionForm(name, recipes);
        screen = Screen.COLLECTION_FORM;
        render();
    }

    private void openRecipeNote(RecipePreview recipe) {
        personalDataViewModel.recordViewedRecipe(recipe);
        noteReturnScreen = screen;
        noteRecipeId = recipe.id;
        screen = Screen.NOTE;
        render();
    }

    private void confirmDeleteCollection(PersonalDataDbHelper.CollectionRow collection) {
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

    private PersonalDataDbHelper.CollectionRow findCollection(long id) {
        for (PersonalDataDbHelper.CollectionRow row : personalDataViewModel.getCollections()) if (row.id == id) return row;
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
        empty.setContentDescription("Tạo bộ sưu tập đầu tiên");
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

    private ImageView recipeImage(RecipePreview recipe) {
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

    private RecipePreview recipeById(String id) {
        for (RecipePreview recipe : DESIGN_RECIPES) if (recipe.id.equals(id)) return recipe;
        return DESIGN_RECIPES.get(0);
    }
}
