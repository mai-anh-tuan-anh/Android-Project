package com.example.myapplication.presentation.discover;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.core.ui.BottomNavigationBar;
import com.example.myapplication.core.ui.DebouncedSearchTextWatcher;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.presentation.main.MainActivity.Screen;
import com.example.myapplication.presentation.personaldata.PersonalDataViewModel;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Owns discovery, search, ingredient suggestions, and recipe detail UI. */
public final class DiscoverFragment extends Fragment {
    public interface Host {
        void onDiscoverScreenChanged(
                Screen screen, String category, String recipeId, Screen detailReturn);

        void onDiscoverOpenNote(Recipe recipe);

        void onDiscoverOpenMealPlanPicker(Recipe recipe);

        void onDiscoverNavigateTab(int tab);

        void onDiscoverBack();

        void onDiscoverExitTo(Screen screen);
    }

    private static final int CREAM = Color.rgb(250, 249, 246);
    private static final int GREEN = Color.rgb(50, 108, 79);
    private static final int PALE_GREEN = Color.rgb(232, 242, 233);
    private static final int TEXT = Color.rgb(37, 53, 45);
    private static final int MUTED = Color.rgb(88, 102, 92);
    private static final int BORDER = Color.rgb(220, 228, 220);
    private static final int WHITE = Color.WHITE;
    private Host host;
    private FrameLayout root;
    private LinearLayout pageRoot;
    private DiscoverSearchViewModel searchViewModel;
    private DiscoverViewModel discoverViewModel;
    private PersonalDataViewModel personalDataViewModel;
    private Screen screen = Screen.DISCOVER;
    private Screen lastRenderedScreen;
    private Screen detailReturnScreen = Screen.DISCOVER;
    private String selectedCategory = "";
    private String detailRecipeId = PreviewRecipeCatalog.designRecipes().get(0).id;
    private EditText discoverSearchInput;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (!(context instanceof Host))
            throw new IllegalStateException("Host Activity must implement DiscoverFragment.Host");
        host = (Host) context;
    }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            try {
                screen = Screen.valueOf(state.getString("discover.screen", Screen.DISCOVER.name()));
            } catch (IllegalArgumentException ignored) {
                screen = Screen.DISCOVER;
            }
            selectedCategory = state.getString("discover.category", "");
            detailRecipeId = state.getString("discover.recipeId", detailRecipeId);
            try {
                detailReturnScreen =
                        Screen.valueOf(
                                state.getString("discover.detailReturn", Screen.DISCOVER.name()));
            } catch (IllegalArgumentException ignored) {
                detailReturnScreen = Screen.DISCOVER;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        out.putString("discover.screen", screen.name());
        out.putString("discover.category", selectedCategory);
        out.putString("discover.recipeId", detailRecipeId);
        out.putString("discover.detailReturn", detailReturnScreen.name());
        super.onSaveInstanceState(out);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {
        AppViewModelFactory factory =
                new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication());
        searchViewModel =
                new ViewModelProvider(requireActivity(), factory)
                        .get(DiscoverSearchViewModel.class);
        discoverViewModel =
                new ViewModelProvider(requireActivity(), factory).get(DiscoverViewModel.class);
        personalDataViewModel =
                new ViewModelProvider(requireActivity(), factory).get(PersonalDataViewModel.class);
        root = new FrameLayout(requireContext());
        root.setBackgroundColor(CREAM);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    view.setPadding(
                            system.left,
                            system.top,
                            system.right,
                            Math.max(system.bottom, ime.bottom));
                    return insets;
                });
        searchViewModel
                .recentSearchState()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            if (value != null
                                    && !value.loading
                                    && screen == Screen.SEARCH
                                    && (discoverSearchInput == null
                                            || !discoverSearchInput.hasFocus())) render();
                        });
        personalDataViewModel
                .personalDataState()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            if (value != null && !value.loading && screen == Screen.RECIPE_DETAIL)
                                render();
                        });
        render();
        return root;
    }

    public Screen getScreen() {
        return screen;
    }

    public void syncScreen(Screen value, String category, String recipeId, Screen detailReturn) {
        boolean changed =
                screen != value
                        || !selectedCategory.equals(category == null ? "" : category)
                        || (recipeId != null && !detailRecipeId.equals(recipeId))
                        || detailReturnScreen != detailReturn;
        screen = value;
        selectedCategory = category == null ? "" : category;
        if (recipeId != null) detailRecipeId = recipeId;
        detailReturnScreen = detailReturn == null ? Screen.DISCOVER : detailReturn;
        if (changed) render();
    }

    private void showScreen(Screen value) {
        screen = value;
        host.onDiscoverScreenChanged(screen, selectedCategory, detailRecipeId, detailReturnScreen);
    }

    public void showRecipeDetail(Recipe recipe, Screen returnScreen) {
        if (recipe == null) return;
        if (screen == Screen.SEARCH
                || screen == Screen.SEARCH_FILTER
                || screen == Screen.SEARCH_RESULTS) {
            searchViewModel.setSearchQuery("");
        }
        detailRecipeId = recipe.id;
        detailReturnScreen = returnScreen == Screen.RECIPE_DETAIL ? Screen.DISCOVER : returnScreen;
        personalDataViewModel.recordViewedRecipe(recipe);
        showScreen(Screen.RECIPE_DETAIL);
        render();
    }

    public void handleBackPressed() {
        if (screen == Screen.DISCOVER) {
            host.onDiscoverBack();
            return;
        }
        if (screen == Screen.SEARCH_FILTER || screen == Screen.SEARCH_RESULTS) {
            showScreen(Screen.SEARCH);
        } else if (screen == Screen.WHAT_TO_COOK_RESULTS) {
            showScreen(Screen.WHAT_TO_COOK_INGREDIENTS);
        } else if (screen == Screen.CATEGORY_RECIPES) {
            showScreen(Screen.CATEGORY_LIST);
        } else if (screen == Screen.RECIPE_DETAIL) {
            Screen destination =
                    detailReturnScreen == Screen.RECIPE_DETAIL
                            ? Screen.DISCOVER
                            : detailReturnScreen;
            if (isDiscoverRoute(destination)) {
                showScreen(destination);
                render();
            } else {
                host.onDiscoverExitTo(destination);
            }
            return;
        } else {
            if (screen == Screen.SEARCH) searchViewModel.setSearchQuery("");
            showScreen(Screen.DISCOVER);
        }
        render();
    }

    private boolean isDiscoverRoute(Screen value) {
        return value == Screen.DISCOVER
                || value == Screen.CATEGORY_LIST
                || value == Screen.CATEGORY_RECIPES
                || value == Screen.NEW_RECIPES
                || value == Screen.SEARCH
                || value == Screen.SEARCH_FILTER
                || value == Screen.SEARCH_RESULTS
                || value == Screen.WHAT_TO_COOK_INGREDIENTS
                || value == Screen.WHAT_TO_COOK_RESULTS
                || value == Screen.RECIPE_DETAIL;
    }

    private Recipe recipeById(String id) {
        for (Recipe recipe : PreviewRecipeCatalog.designRecipes())
            if (recipe.id.equals(id)) return recipe;
        return PreviewRecipeCatalog.designRecipes().isEmpty()
                ? null
                : PreviewRecipeCatalog.designRecipes().get(0);
    }

    private void render() {
        if (root == null || !isAdded()) return;
        boolean preservePosition = lastRenderedScreen == screen;
        ScrollView previousScroll = preservePosition ? findFirstScrollView(root) : null;
        int previousScrollY = previousScroll == null ? 0 : previousScroll.getScrollY();
        root.removeAllViews();
        pageRoot = new LinearLayout(requireContext());
        pageRoot.setOrientation(LinearLayout.VERTICAL);
        pageRoot.setBackgroundColor(CREAM);
        root.addView(pageRoot, new FrameLayout.LayoutParams(-1, -1));
        switch (screen) {
            case DISCOVER:
                renderDiscover();
                break;
            case CATEGORY_LIST:
                renderCategoryList();
                break;
            case CATEGORY_RECIPES:
                renderCategoryRecipes();
                break;
            case NEW_RECIPES:
                renderNewRecipes();
                break;
            case SEARCH:
                renderSearch();
                break;
            case SEARCH_FILTER:
                renderSearchFilter();
                break;
            case SEARCH_RESULTS:
                renderSearchResults();
                break;
            case WHAT_TO_COOK_INGREDIENTS:
                renderWhatToCookIngredients();
                break;
            case WHAT_TO_COOK_RESULTS:
                renderWhatToCookResults();
                break;
            case RECIPE_DETAIL:
                renderRecipeDetail();
                break;
            default:
                renderDiscover();
        }
        lastRenderedScreen = screen;
        if (preservePosition && previousScrollY > 0) {
            ScrollView currentScroll = findFirstScrollView(root);
            if (currentScroll != null)
                currentScroll.post(() -> currentScroll.scrollTo(0, previousScrollY));
        }
    }

    private ScrollView findFirstScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ScrollView found = findFirstScrollView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private void renderDiscover() {
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(content);

        addText(content, "Bếp nhà", 23, TEXT, true, 0, 0, 0, 0);
        addText(content, "Hôm nay bạn muốn nấu món gì?", 14, MUTED, false, 0, 4, 0, 16);

        LinearLayout searchBar = new LinearLayout(requireContext());
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setBackground(roundRect(WHITE, BORDER, 16));
        searchBar.setPadding(dp(14), dp(12), dp(14), dp(12));
        ImageView searchIcon = iconView(R.drawable.ic_search_outline, MUTED);
        searchBar.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView hint = text("Tìm công thức, nguyên liệu...", 14, MUTED, false);
        hint.setPadding(dp(10), 0, 0, 0);
        searchBar.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        searchBar.setOnClickListener(v -> {
            showScreen(Screen.SEARCH);
            render();
        });
        content.addView(searchBar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout banner = vertical();
        banner.setBackground(roundRect(PALE_GREEN, GREEN, 16));
        banner.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, -2);
        bannerParams.setMargins(0, dp(18), 0, 0);

        LinearLayout bannerHeader = new LinearLayout(requireContext());
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
            showScreen(Screen.WHAT_TO_COOK_INGREDIENTS);
            render();
        });
        content.addView(banner, bannerParams);

        addSectionHeader(
                content,
                "Danh mục món ăn",
                "Xem tất cả ›",
                () -> {
                    showScreen(Screen.CATEGORY_LIST);
                    render();
                },
                24);

        HorizontalScrollView catScroll = new HorizontalScrollView(requireContext());
        catScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout catRow = new LinearLayout(requireContext());
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
                showScreen(Screen.CATEGORY_RECIPES);
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
                    showScreen(Screen.NEW_RECIPES);
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

        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(20);
            row.addView(
                    createCategoryTile(categories[i][0], categories[i][1]),
                    new LinearLayout.LayoutParams(0, -2, 1));
            View gap = new View(requireContext());
            row.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
            if (i + 1 < categories.length) {
                row.addView(
                        createCategoryTile(categories[i + 1][0], categories[i + 1][1]),
                        new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                row.addView(new View(requireContext()), new LinearLayout.LayoutParams(0, 1, 1));
            }
            page.addView(row, rowParams);
        }
        scroll.addView(page);
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private View createCategoryTile(String title, String imageName) {
        LinearLayout card = vertical();
        card.setBackground(roundRect(WHITE, BORDER, 17));
        card.setClipToOutline(true);
        ImageView image = new ImageView(requireContext());
        int imageId =
                getResources()
                        .getIdentifier(imageName, "drawable", requireContext().getPackageName());
        if (imageId != 0) image.setImageResource(imageId);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(130)));

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(12), dp(10), dp(12), dp(10));
        titleRow.addView(text(title, 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        titleRow.addView(text("›", 22, GREEN, false));
        card.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));
        card.setOnClickListener(v -> {
            selectedCategory = title;
            showScreen(Screen.CATEGORY_RECIPES);
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
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void renderNewRecipes() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Món mới");
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
                        showScreen(Screen.DISCOVER);
                        render();
                    });
        } else {
            addRecipeGrid(page, newRecipes, true, true);
        }
        scroll.addView(page);
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void addSearchDishItem(LinearLayout parent, Recipe recipe) {
        LinearLayout item = new LinearLayout(requireContext());
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

        item.setOnClickListener(v -> showRecipeDetail(recipe, screen));
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
        LinearLayout searchBox = new LinearLayout(requireContext());
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
        searchInput.addTextChangedListener(
                new DebouncedSearchTextWatcher(searchInput, searchViewModel::setSearchQuery));

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
                showScreen(Screen.SEARCH_RESULTS);
                render();
                return true;
            }
            return false;
        });

        // Section "Tìm kiếm gần đây" with Filter button on the right
        LinearLayout recentHeader = new LinearLayout(requireContext());
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

        FrameLayout filterBtn = new FrameLayout(requireContext());
        filterBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView filterIcon = iconView(R.drawable.ic_filter_outline, GREEN);
        filterBtn.addView(filterIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        filterBtn.setOnClickListener(v -> {
            searchViewModel.beginFilterDraft();
            showScreen(Screen.SEARCH_FILTER);
            render();
        });
        recentHeader.addView(filterBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(recentHeader, rhParams);

        // Recent search chips
        if (!recentList.isEmpty()) {
            HorizontalScrollView recentScroll = new HorizontalScrollView(requireContext());
            recentScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout recentRow = new LinearLayout(requireContext());
            recentRow.setOrientation(LinearLayout.HORIZONTAL);
            for (String term : recentList) {
                TextView chip = text(term, 13, TEXT, false);
                chip.setPadding(dp(16), dp(8), dp(16), dp(8));
                chip.setBackground(roundRect(WHITE, BORDER, 18));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = dp(8);
                chip.setOnClickListener(v -> {
                    searchViewModel.submitSearch(term);
                    showScreen(Screen.SEARCH_RESULTS);
                    render();
                });
                recentRow.addView(chip, lp);
            }
            recentScroll.addView(recentRow);
            page.addView(recentScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        // Section "Gợi ý"
        addSectionHeader(page, "Gợi ý", "", () -> {}, 20);
        HorizontalScrollView sugScroll = new HorizontalScrollView(requireContext());
        sugScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout sugRow = new LinearLayout(requireContext());
        sugRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String sug : searchViewModel.getSuggestions()) {
            TextView chip = text(sug, 13, TEXT, false);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackground(roundRect(WHITE, BORDER, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                searchViewModel.submitSearch(sug);
                showScreen(Screen.SEARCH_RESULTS);
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
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderSearchFilter() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(12), dp(24), dp(24));

        View handle = new View(requireContext());
        handle.setBackground(roundRect(BORDER, BORDER, 3));
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(44), dp(5));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dp(14);
        page.addView(handle, handleLp);

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Bộ lọc nâng cao", 22, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        FrameLayout closeBtn = new FrameLayout(requireContext());
        closeBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView closeIcon = iconView(R.drawable.ic_close_outline, GREEN);
        closeBtn.addView(closeIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        closeBtn.setOnClickListener(v -> handleBackPressed());
        header.addView(closeBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
        page.addView(header);

        addText(page, "Thời gian nấu", 15, TEXT, true, 0, 20, 0, 10);
        LinearLayout timeRow = new LinearLayout(requireContext());
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
        LinearLayout diffRow = new LinearLayout(requireContext());
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
        LinearLayout dietRow = new LinearLayout(requireContext());
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

        LinearLayout actionRow = new LinearLayout(requireContext());
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

        View gap = new View(requireContext());
        actionRow.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));

        TextView applyBtn = text("Áp dụng bộ lọc", 14, WHITE, true);
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setBackground(roundRect(GREEN, GREEN, 14));
        applyBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        applyBtn.setOnClickListener(v -> {
            searchViewModel.applyDraftFilter();
            showScreen(Screen.SEARCH_RESULTS);
            render();
        });
        actionRow.addView(applyBtn, new LinearLayout.LayoutParams(0, -2, 65));
        page.addView(actionRow, arParams);

        scroll.addView(page);
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderSearchResults() {
        ScrollView scroll = scrollContent();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));

        addBackTitle(page, "Kết quả tìm kiếm");

        // Search Input Box
        LinearLayout searchBox = new LinearLayout(requireContext());
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
        LinearLayout matchesContainer = vertical();
        searchInput.addTextChangedListener(
                new DebouncedSearchTextWatcher(searchInput, query -> {
                    searchViewModel.setSearchQuery(query);
                    renderSearchMatches(matchesContainer);
                }));

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
        LinearLayout filterRow = new LinearLayout(requireContext());
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams frParams = new LinearLayout.LayoutParams(-1, -2);
        frParams.bottomMargin = dp(14);

        HorizontalScrollView badgeScroll = new HorizontalScrollView(requireContext());
        badgeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout badgeRow = new LinearLayout(requireContext());
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

        FrameLayout filterBtn = new FrameLayout(requireContext());
        filterBtn.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
        ImageView filterIcon = iconView(R.drawable.ic_filter_outline, GREEN);
        filterBtn.addView(filterIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        filterBtn.setOnClickListener(v -> {
            searchViewModel.beginFilterDraft();
            showScreen(Screen.SEARCH_FILTER);
            render();
        });
        filterRow.addView(filterBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
        page.addView(filterRow, frParams);

        renderSearchMatches(matchesContainer);
        page.addView(matchesContainer);

        scroll.addView(page);
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void renderSearchMatches(LinearLayout matchesContainer) {
        matchesContainer.removeAllViews();
        List<Recipe> matches = searchViewModel.filterRecipes(PreviewRecipeCatalog.designRecipes());
        if (!matches.isEmpty()) {
            TextView countText = text(matches.size() + " món phù hợp", 13, MUTED, false);
            countText.setPadding(0, 0, 0, dp(12));
            matchesContainer.addView(countText);

            for (Recipe r : matches) {
                addSearchDishItem(matchesContainer, r);
            }
        } else {
            // Empty state matching states/khong-ket-qua.png
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(dp(24), dp(40), dp(24), dp(24));

            FrameLayout iconBg = new FrameLayout(requireContext());
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
                showScreen(Screen.SEARCH_FILTER);
                render();
            });
            empty.addView(editFilterBtn, new LinearLayout.LayoutParams(-1, -2));

            matchesContainer.addView(empty);
        }
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
            LinearLayout chipRow = new LinearLayout(requireContext());
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
            HorizontalScrollView chipScroll = new HorizontalScrollView(requireContext());
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
            showScreen(Screen.WHAT_TO_COOK_RESULTS);
            render();
        });
        page.addView(findRecipesBtn, btnParams);

        scroll.addView(page);
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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
        LinearLayout card = new LinearLayout(requireContext());
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
        card.setOnClickListener(v -> showRecipeDetail(recipe, screen));
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
                        showScreen(Screen.WHAT_TO_COOK_INGREDIENTS);
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
                            showScreen(Screen.WHAT_TO_COOK_INGREDIENTS);
                            render();
                        });
            } else if (matches.isEmpty()) {
                addEmptyCard(
                        page,
                        "Không có món phù hợp",
                        "Thử chọn thêm nguyên liệu để tìm được nhiều món hơn.",
                        () -> {
                            showScreen(Screen.WHAT_TO_COOK_INGREDIENTS);
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
        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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

        FrameLayout imgFrame = new FrameLayout(requireContext());
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

        LinearLayout infoRow = new LinearLayout(requireContext());
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

        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(-1, -2);
        actionRowParams.topMargin = dp(16);

        TextView addPlanBtn = text("＋ Nấu hôm nay", 13, WHITE, true);
        addPlanBtn.setGravity(Gravity.CENTER);
        addPlanBtn.setBackground(roundRect(GREEN, GREEN, 14));
        addPlanBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        addPlanBtn.setOnClickListener(v -> {
            host.onDiscoverOpenMealPlanPicker(recipe);
        });
        actionRow.addView(addPlanBtn, new LinearLayout.LayoutParams(0, -2, 1));

        View btnGap = new View(requireContext());
        actionRow.addView(btnGap, new LinearLayout.LayoutParams(dp(10), 1));

        TextView noteBtn = text("📝 Ghi chú", 13, GREEN, true);
        noteBtn.setGravity(Gravity.CENTER);
        noteBtn.setBackground(roundRect(PALE_GREEN, GREEN, 14));
        noteBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        noteBtn.setOnClickListener(v -> host.onDiscoverOpenNote(recipe));
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

        pageRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation(0);
    }

    private void addRecipeGrid(
            LinearLayout parent, List<Recipe> recipes, boolean showFavorite, boolean removable) {
        if (recipes.isEmpty()) return;
        GridLayout grid = new GridLayout(requireContext());
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
        FrameLayout imageFrame = new FrameLayout(requireContext());
        ImageView image = recipeImage(recipe);
        imageFrame.addView(image, new FrameLayout.LayoutParams(-1, dp(96)));
        if (showFavorite) {
            boolean favorite = personalDataViewModel.isFavorite(recipe.id);
            ImageView star =
                    iconView(
                            favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline,
                            GREEN);
            star.setBackground(roundRect(WHITE, WHITE, 18));
            FrameLayout starTouchTarget = new FrameLayout(requireContext());
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
                showRecipeDetail(recipe, screen);
            });
        return card;
    }

    private void addSectionHeader(
            LinearLayout parent, String title, String action, Runnable onAction, int top) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 19, TEXT, true);
        View before = new View(requireContext());
        parent.addView(before, new LinearLayout.LayoutParams(1, dp(top == 30 ? 22 : top)));
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView link = text(action, 12, GREEN, true);
        row.addView(link);
        link.setOnClickListener(v -> onAction.run());
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        View spacer = new View(requireContext());
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(16)));
    }

    private void addBottomNavigation(int selectedTab) {
        BottomNavigationBar navigation =
                new BottomNavigationBar(
                        requireContext(),
                        selectedTab,
                        tab -> {
                            if (screen == Screen.SEARCH
                                    || screen == Screen.SEARCH_FILTER
                                    || screen == Screen.SEARCH_RESULTS) {
                                searchViewModel.setSearchQuery("");
                            }
                            host.onDiscoverNavigateTab(tab);
                        });
        pageRoot.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addBackTitle(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = iconView(R.drawable.ic_arrow_back_outline, GREEN);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        back.setBackground(roundRect(WHITE, WHITE, 15));
        back.setContentDescription("Quay lại");
        row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> handleBackPressed());
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

    private ImageView recipeImage(Recipe recipe) {
        ImageView image = new ImageView(requireContext());
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
        ImageView image = new ImageView(requireContext());
        image.setImageResource(resourceId);
        image.setColorFilter(tint);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return image;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(requireContext());
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
        LinearLayout layout = new LinearLayout(requireContext());
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

    private String normalizeSearchText(String value) {
        String decomposed =
                Normalizer.normalize(
                                value == null ? "" : value.toLowerCase(Locale.ROOT),
                                Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .replace('đ', 'd');
        return decomposed.replaceAll("\\s+", " ").trim();
    }

    private static void updateFavoriteToggleDescription(
            FrameLayout button, boolean favorite, Recipe recipe) {
        button.setContentDescription(
                (favorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    }

    private ScrollView scrollContent() {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        return scroll;
    }

    private EditText editField(String value, int textSize, int minHeight) {
        EditText field = new EditText(requireContext());
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
}
