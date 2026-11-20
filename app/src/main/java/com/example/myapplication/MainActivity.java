package com.example.myapplication;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Module 5: local favorites, collections, history and personal notes. */
public class MainActivity extends AppCompatActivity {
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

    private PersonalDataDbHelper data;
    private LinearLayout appRoot;
    private Screen screen = Screen.LIBRARY;
    private Screen noteReturnScreen = Screen.LIBRARY;
    private long selectedCollectionId = -1L;
    private String editingCollectionName = "";
    private String noteRecipeId = DESIGN_RECIPES.get(0).id;
    private EditText collectionNameInput;
    private TextView collectionError;
    private TextView collectionSaveButton;
    private boolean collectionValidationAttempted;
    private final List<String> selectedRecipeIds = new ArrayList<>();

    enum Screen { LIBRARY, FAVORITES, COLLECTION, COLLECTION_FORM, NOTE }

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
                    screen = selectedCollectionId > 0 ? Screen.COLLECTION : Screen.LIBRARY;
                } else if (screen == Screen.NOTE) {
                    screen = noteReturnScreen;
                } else {
                    screen = Screen.LIBRARY;
                }
                render();
            }
        });
        data = new PersonalDataDbHelper(this);
        data.seedDesignPreviewIfNeeded(DESIGN_RECIPES);
        render();
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

    @Override
    protected void onDestroy() {
        if (data != null) data.close();
        super.onDestroy();
    }

    private void render() {
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
        else if (screen == Screen.COLLECTION) renderCollection();
        else if (screen == Screen.COLLECTION_FORM) renderCollectionForm();
        else renderNote();
        setContentView(frame);
        ViewCompat.requestApplyInsets(frame);
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
        List<RecipePreview> favoriteRows = data.getFavorites();
        addRecipeGrid(content, favoriteRows.subList(0, Math.min(2, favoriteRows.size())), true, false);

        addText(content, "Bộ sưu tập", 19, TEXT, true, 0, 14, 0, 12);
        List<PersonalDataDbHelper.CollectionRow> collections = data.getCollections();
        if (collections.isEmpty()) addEmptyCard(content, "Chưa có bộ sưu tập",
                "Tạo bộ sưu tập đầu tiên của bạn.", () -> openCollectionForm(null));
        else addCollectionRow(content, collections.get(0), 0);

        addText(content, "Đã xem gần đây", 19, TEXT, true, 0, 24, 0, 12);
        List<RecipePreview> history = data.getHistory();
        for (int i = 0; i < Math.min(2, history.size()); i++) addHistoryRow(content, history.get(i));

        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation();
    }

    private void renderFavorites() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(24));
        addBackTitle(page, "Yêu thích");
        addText(page, "Những công thức bạn đã lưu.", 14, MUTED, false, 0, 0, 0, 34);
        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(content);
        List<RecipePreview> favorites = data.getFavorites();
        if (favorites.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            ImageView star = iconView(R.drawable.ic_star_outline, GREEN);
            star.setPadding(dp(24), dp(24), dp(24), dp(24));
            star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(star, new LinearLayout.LayoutParams(dp(84), dp(84)));
            addText(empty, "Chưa lưu món nào", 22, TEXT, true, 0, 18, 0, 0);
            TextView hint = text("Lưu một công thức để tìm lại nhanh hơn.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            empty.addView(hint);
            TextView explore = text("Khám phá món", 15, WHITE, true);
            explore.setGravity(Gravity.CENTER);
            explore.setBackground(roundRect(GREEN, GREEN, 14));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(49));
            buttonParams.setMargins(dp(24), dp(16), dp(24), 0);
            empty.addView(explore, buttonParams);
        } else {
            addRecipeGrid(content, favorites, true, true);
            addInfoBanner(content, "Chạm biểu tượng lưu để bỏ yêu thích.", 22);
        }
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        appRoot.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
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

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(selected.name, 19, TEXT, true);
        titleLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView edit = iconView(R.drawable.ic_edit_outline, GREEN);
        edit.setPadding(dp(14), dp(14), dp(14), dp(14));
        edit.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 16));
        edit.setContentDescription("Chỉnh sửa bộ sưu tập");
        titleLine.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(48)));
        edit.setOnClickListener(v -> openCollectionForm(selectedCollectionId));
        page.addView(titleLine);
        addText(page, selected.recipeCount + " món · Chỉnh sửa bộ sưu tập", 14, MUTED, false, 0, 8, 0, 24);

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        scroll.addView(content);
        List<RecipePreview> collectionRecipes = data.getCollectionRecipes(selectedCollectionId);
        if (collectionRecipes.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setGravity(Gravity.CENTER);
            ImageView folder = iconView(R.drawable.ic_folder_outline, GREEN);
            folder.setPadding(dp(24), dp(24), dp(24), dp(24));
            folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
            empty.addView(folder, new LinearLayout.LayoutParams(dp(84), dp(84)));
            addText(empty, "Bộ sưu tập đang trống", 22, TEXT, true, 0, 10, 0, 0);
            TextView hint = text("Thêm món đã lưu vào bộ sưu tập này.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            empty.addView(hint);
            TextView add = text("Thêm món", 15, WHITE, true);
            add.setGravity(Gravity.CENTER);
            add.setBackground(roundRect(GREEN, GREEN, 14));
            empty.addView(add, new LinearLayout.LayoutParams(-1, dp(49)));
            content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            add.setOnClickListener(v -> openCollectionForm(selectedCollectionId));
        } else addRecipeGrid(content, collectionRecipes, true, true);
        List<PersonalDataDbHelper.CollectionRow> otherCollections = data.getCollections();
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
        collectionValidationAttempted = false;

        ScrollView scroll = scrollContent();
        LinearLayout content = vertical();
        scroll.addView(content);
        addText(content, "Tên bộ sưu tập", 13, TEXT, true, 0, 20, 0, 10);
        collectionNameInput = editField(editingCollectionName, 16, 49);
        collectionNameInput.setSingleLine(true);
        collectionNameInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        content.addView(collectionNameInput, new LinearLayout.LayoutParams(-1, dp(49)));
        collectionError = text("", 12, ERROR, false);
        content.addView(collectionError);
        addText(content, "Chọn món để thêm", 19, TEXT, true, 0, 34, 0, 18);
        for (RecipePreview recipe : DESIGN_RECIPES) addSelectableRecipeRow(content, recipe);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        collectionSaveButton = addButton(page, "Lưu bộ sưu tập", true, this::saveCollectionForm, 18);
        collectionNameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
        labels.addView(text("Ghi chú chỉ lưu trên thiết bị", 12, MUTED, false));
        recipeRow.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams recipeParams = new LinearLayout.LayoutParams(-1, dp(64));
        recipeParams.topMargin = dp(30);
        content.addView(recipeRow, recipeParams);

        addText(content, "Ghi chú sau khi nấu", 13, TEXT, true, 0, 48, 0, 16);
        EditText noteInput = editField(data.getNote(recipe.id), 16, -1);
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        noteInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density);
        int noteHeightDp = Math.max(160, Math.min(220, Math.round(screenHeightDp * 0.215f)));
        content.addView(noteInput, new LinearLayout.LayoutParams(-1, dp(noteHeightDp)));
        addInfoBanner(content, "Ghi chú đã lưu", 24);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "Lưu ghi chú", true, () -> {
            data.saveNote(recipe, noteInput.getText().toString());
            noteInput.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(noteInput.getWindowToken(), 0);
            screen = noteReturnScreen;
            render();
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
            boolean favorite = data.isFavorite(recipe.id);
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
                if (data.isFavorite(recipe.id)) data.removeFavorite(recipe.id); else data.addFavorite(recipe);
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
            selectedCollectionId = row.id;
            screen = Screen.COLLECTION;
            render();
        });
        parent.addView(item, new LinearLayout.LayoutParams(-1, dp(62)));
        if (top > 0) item.setTranslationY(dp(0));
    }

    private void addRelatedCollectionRow(LinearLayout parent, PersonalDataDbHelper.CollectionRow row) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(7), dp(5), dp(12), dp(5));
        item.setBackground(roundRect(WHITE, BORDER, 14));
        List<RecipePreview> recipes = data.getCollectionRecipes(row.id);
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
        item.addView(text("›", 23, GREEN, false));
        item.setOnClickListener(v -> {
            openRecipeNote(recipe);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.bottomMargin = dp(9);
        parent.addView(item, params);
    }

    private void addSelectableRecipeRow(LinearLayout parent, RecipePreview recipe) {
        boolean selected = selectedRecipeIds.contains(recipe.id);
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
            if (selectedRecipeIds.contains(recipe.id)) selectedRecipeIds.remove(recipe.id);
            else selectedRecipeIds.add(recipe.id);
            render();
        });
    }

    private void addBottomNavigation() {
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
            ImageView glyph = iconView(icons[i], i == 3 ? GREEN : MUTED);
            if (i == 3) glyph.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
            tab.addView(glyph, new LinearLayout.LayoutParams(dp(52), dp(30)));
            TextView label = text(names[i], 10, i == 3 ? GREEN : MUTED, i == 3);
            label.setGravity(Gravity.CENTER);
            tab.addView(label);
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(58), 1));
            tab.setOnClickListener(v -> {
                if (index == 3) {
                    screen = Screen.LIBRARY;
                    render();
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
        collectionValidationAttempted = true;
        if (name.trim().isEmpty()) {
            updateCollectionFormValidation();
            return;
        }
        Long currentId = selectedCollectionId > 0 ? selectedCollectionId : null;
        if (!data.isCollectionNameAvailable(name, currentId)) {
            collectionError.setText("Tên này đã được sử dụng.");
            collectionNameInput.setBackground(roundRect(WHITE, ERROR, 14));
            updateCollectionSaveButton(false);
            return;
        }
        List<RecipePreview> selected = new ArrayList<>();
        for (String id : selectedRecipeIds) selected.add(recipeById(id));
        boolean saved = data.saveCollection(name, selected, currentId);
        if (!saved) {
            collectionError.setText("Tên này đã được sử dụng.");
            collectionNameInput.setBackground(roundRect(WHITE, ERROR, 14));
            updateCollectionSaveButton(false);
            return;
        }
        boolean editing = selectedCollectionId > 0;
        screen = editing ? Screen.COLLECTION : Screen.LIBRARY;
        if (!editing) selectedCollectionId = -1L;
        render();
    }

    private void updateCollectionFormValidation() {
        String name = collectionNameInput.getText().toString();
        boolean nonEmpty = !name.trim().isEmpty();
        Long currentId = selectedCollectionId > 0 ? selectedCollectionId : null;
        boolean available = nonEmpty && data.isCollectionNameAvailable(name, currentId);
        if (collectionValidationAttempted) {
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
        updateCollectionSaveButton(collectionValidationAttempted ? available : nonEmpty);
    }

    private void updateCollectionSaveButton(boolean enabled) {
        if (collectionSaveButton == null) return;
        collectionSaveButton.setEnabled(enabled);
        collectionSaveButton.setTextColor(enabled ? WHITE : MUTED);
        collectionSaveButton.setBackground(roundRect(enabled ? GREEN : PALE_GREEN,
                enabled ? GREEN : PALE_GREEN, 14));
    }

    private void openCollectionForm(Long collectionId) {
        selectedCollectionId = collectionId == null ? -1L : collectionId;
        editingCollectionName = "";
        selectedRecipeIds.clear();
        if (collectionId != null) {
            PersonalDataDbHelper.CollectionRow row = findCollection(collectionId);
            if (row != null) editingCollectionName = row.name;
            for (RecipePreview recipe : data.getCollectionRecipes(collectionId)) selectedRecipeIds.add(recipe.id);
        }
        screen = Screen.COLLECTION_FORM;
        render();
    }

    private void openRecipeNote(RecipePreview recipe) {
        noteReturnScreen = screen;
        noteRecipeId = recipe.id;
        screen = Screen.NOTE;
        render();
    }

    private PersonalDataDbHelper.CollectionRow findCollection(long id) {
        for (PersonalDataDbHelper.CollectionRow row : data.getCollections()) if (row.id == id) return row;
        return null;
    }

    private void addInfoBanner(LinearLayout parent, String label, int top) {
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
