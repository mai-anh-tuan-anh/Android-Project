package com.example.myapplication.presentation.recipedetail;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.data.image.RecipeImageLoader;
import com.example.myapplication.domain.model.CookingStep;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.Draft;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.IngredientRow;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.State;
import com.example.myapplication.presentation.recipedetail.RecipeDetailViewModel.Status;

/** Renders the catalog detail, its ingredient sheet, and the read-only step preview. */
public final class RecipeDetailView extends FrameLayout {
    public interface Actions {
        void back();
        void discover();
        void openNote();
        void openSavedNote();
        void toggleFavorite();
        void openMealPlan();
        void startCooking(String canonicalId, int servings);
    }

    private static final int CREAM = 0xfffaf9f6;
    private static final int GREEN = 0xff326c4f;
    private static final int PALE = 0xffe8f2e9;
    private static final int TEXT = 0xff25352d;
    private static final int MUTED = 0xff58665c;
    private static final int BORDER = 0xffdce4dc;
    private static final int ERROR = 0xffa93636;
    private static final int WHITE = Color.WHITE;

    private final RecipeDetailViewModel model;
    private final RecipeImageLoader images;
    private final Actions actions;
    private final Recipe legacySource;
    private State state;
    private boolean favorite;
    private boolean stepsShown;
    private ScrollView scroll;
    private ImageView star;
    private FrameLayout servingOverlay;
    private LinearLayout sheetPanel;
    private LinearLayout comparison;
    private TextView inputError;
    private TextView apply;
    private TextView minus;
    private TextView plus;
    private EditText number;
    private boolean bindingNumber;

    public RecipeDetailView(Context context, RecipeDetailViewModel model, RecipeImageLoader images, Actions actions,
                            Recipe legacySource) {
        super(context);
        this.model = model;
        this.images = images;
        this.actions = actions;
        this.legacySource = legacySource;
        setBackgroundColor(CREAM);
    }

    public void display(State value, boolean isFavorite) {
        favorite = isFavorite;
        if (value == state) {
            updateStar();
            return;
        }
        int previousScroll = state != null && state.status == Status.AVAILABLE && scroll != null
                ? scroll.getScrollY() : 0;
        state = value;
        if (state == null || state.recipe == null || state.recipe.details == null
                || state.recipe.details.steps.isEmpty()) stepsShown = false;
        drawPage();
        if (previousScroll > 0) {
            ScrollView current = scroll;
            current.post(() -> current.scrollTo(0, previousScroll));
        }
    }

    public boolean handleBack() {
        if (servingOverlay != null) {
            model.cancelAdjustment();
            return true;
        }
        return goBackFromSteps();
    }

    private boolean goBackFromSteps() {
        if (!stepsShown) return false;
        stepsShown = false;
        drawPage();
        return true;
    }

    private void drawPage() {
        if (getChildCount() > 0) removeViewAt(0);
        star = null;
        LinearLayout page = column();
        addView(page, 0, new LayoutParams(-1, -1));
        scroll = scrollView();
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout contents = column();
        scroll.addView(contents, new ScrollView.LayoutParams(-1, -2));
        if (state == null || state.status != Status.AVAILABLE) {
            unavailable(contents);
        } else if (stepsShown) {
            steps(contents);
            addFooter(page, true);
        } else {
            detail(contents);
            addFooter(page, false);
        }
        if (servingOverlay != null) {
            page.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }

    private void detail(LinearLayout contents) {
        Recipe recipe = state.recipe;
        Recipe.Details details = recipe.details;
        FrameLayout hero = new FrameLayout(getContext());
        ImageView cover = cover(recipe);
        hero.addView(cover, new LayoutParams(-1, -1));
        ImageView back = icon(R.drawable.ic_arrow_back_outline, "Quay lại", actions::back);
        LayoutParams backParams = new LayoutParams(dp(48), dp(48), Gravity.START | Gravity.TOP);
        backParams.setMargins(dp(16), dp(12), dp(16), 0);
        hero.addView(back, backParams);
        star = icon(R.drawable.ic_star_outline, "Yêu thích", actions::toggleFavorite);
        LayoutParams starParams = new LayoutParams(dp(48), dp(48), Gravity.END | Gravity.TOP);
        starParams.setMargins(dp(16), dp(12), dp(16), 0);
        hero.addView(star, starParams);
        updateStar();
        int widthDp = getResources().getConfiguration().screenWidthDp;
        contents.addView(hero, new LinearLayout.LayoutParams(-1, dp(Math.max(160, Math.min(250, widthDp * 59 / 100)))));

        LinearLayout body = column();
        body.setPadding(dp(24), dp(10), dp(24), dp(8));
        contents.addView(body);
        TextView title = label(recipe.title, 29, TEXT, true);
        title.setTypeface(Typeface.create("serif", Typeface.BOLD));
        body.addView(title);
        if (details != null && hasText(details.description)) {
            add(body, label(details.description, 14, MUTED, false), 8);
        }
        LinearLayout meta = row();
        if (details != null) {
            stat(meta, "Chuẩn bị", minutes(details.prepMinutes));
            stat(meta, "Nấu", minutes(details.cookMinutes));
        }
        stat(meta, "Độ khó", recipe.difficulty);
        if (meta.getChildCount() > 0) add(body, meta, 12);
        if (details != null && details.durationMinutes != null) {
            add(body, label("Tổng thời gian · " + minutes(details.durationMinutes), 12, MUTED, false), 5);
        }

        LinearLayout ingredientHeader = row();
        ingredientHeader.addView(label("Nguyên liệu", 20, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (state.servings > 0) {
            TextView portion = button(state.servings + " người", GREEN, WHITE, BORDER, model::beginAdjustment);
            portion.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_people_outline, 0, 0, 0);
            portion.setCompoundDrawableTintList(ColorStateList.valueOf(GREEN));
            portion.setCompoundDrawablePadding(dp(8));
            portion.setEnabled(state.canAdjust);
            ingredientHeader.addView(portion, new LinearLayout.LayoutParams(-2, -2));
        }
        add(body, ingredientHeader, 8);
        ingredientRows(body, state.ingredients, false, state.servings);
        if (!state.canAdjust) {
            boolean unknown = details == null || details.ingredientDataStatus == Recipe.IngredientDataStatus.UNKNOWN;
            String message = unknown ? "Chưa có dữ liệu định lượng để đổi khẩu phần."
                    : "Chưa đủ dữ liệu định lượng để đổi khẩu phần.";
            add(body, label(message, 13, MUTED, false), 8);
        }
        if (details != null) {
            if (!details.tools.isEmpty()) info(body, "Dụng cụ", TextUtils.join(", ", details.tools), null);
            if (!details.steps.isEmpty()) {
                info(body, "Cách làm", details.steps.size() + " bước · Xem hướng dẫn   ›", () -> {
                    stepsShown = true;
                    drawPage();
                });
            }
            if (hasText(details.tip)) info(body, "Mẹo", details.tip, null);
        }
        TextView note = label("Ghi chú cá nhân ›", 14, GREEN, true);
        note.setGravity(Gravity.CENTER_VERTICAL);
        note.setMinimumHeight(dp(48));
        note.setOnClickListener(view -> actions.openNote());
        body.addView(note);
    }

    private void updateStar() {
        if (star == null) return;
        star.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        star.setContentDescription(favorite ? "Bỏ yêu thích" : "Yêu thích");
    }

    private void steps(LinearLayout contents) {
        contents.setPadding(dp(24), dp(8), dp(24), dp(12));
        header(contents, "Cách làm", this::goBackFromSteps);
        LinearLayout summary = row();
        summary.setPadding(dp(8), dp(8), dp(12), dp(8));
        summary.setBackground(shape(WHITE, BORDER, 14));
        ImageView image = cover(state.recipe);
        image.setBackground(shape(PALE, PALE, 10));
        image.setClipToOutline(true);
        summary.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout title = column();
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(label(state.recipe.title, 16, TEXT, true));
        add(title, label(state.recipe.details.steps.size() + " bước · " + state.servings + " người",
                13, MUTED, false), 4);
        summary.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        add(contents, summary, 18);
        int position = 0;
        for (CookingStep step : state.recipe.details.steps) {
            LinearLayout item = column();
            item.setPadding(dp(16), dp(12), dp(16), dp(12));
            item.setBackground(shape(WHITE, BORDER, 14));
            item.addView(label(++position + ". " + step.title, 16, TEXT, true));
            add(item, label(step.instruction, 14, MUTED, false), 4);
            add(contents, item, 10);
        }
    }

    private void addFooter(LinearLayout page, boolean onlyCook) {
        LinearLayout footer = row();
        footer.setPadding(dp(24), dp(6), dp(24), dp(12));
        TextView cook = button("Bắt đầu nấu", WHITE, GREEN, GREEN, () -> model.startCooking(actions::startCooking));
        cook.setEnabled(state.recipe.details != null && !state.recipe.details.steps.isEmpty());
        if (!onlyCook && legacySource != null) {
            TextView plan = button("Thêm vào Nấu hôm nay", GREEN, PALE, PALE, actions::openMealPlan);
            LinearLayout.LayoutParams planLayout = new LinearLayout.LayoutParams(0, -1, 1.15f);
            planLayout.setMarginEnd(dp(9));
            footer.addView(plan, planLayout);
        }
        footer.addView(cook, new LinearLayout.LayoutParams(0, -1, 1));
        page.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void unavailable(LinearLayout contents) {
        contents.setPadding(dp(24), dp(8), dp(24), dp(24));
        header(contents, "Chi tiết công thức", actions::back);
        Status status = state == null ? Status.LOADING : state.status;
        if (status == Status.REMOVED) {
            removed(contents);
            return;
        }
        String title;
        String message;
        switch (status) {
            case EMPTY:
                title = "Kho công thức đang trống";
                message = "Hiện chưa có món nào được công bố.";
                break;
            case MISSING:
                title = "Không tìm thấy công thức";
                message = "Món này không có trong kho công thức đã tải.";
                break;
            case DATABASE_ERROR:
                title = "Không thể đọc kho công thức";
                message = "Thử lại để mở dữ liệu đã lưu trên thiết bị.";
                break;
            case REMOTE_ERROR:
            case NOT_LOADED:
                title = "Chưa có công thức đã tải";
                message = "Kiểm tra kết nối Internet rồi thử tải kho món lần đầu.";
                break;
            default:
                title = "Đang chuẩn bị kho món";
                message = "Món sẽ xuất hiện sau khi tải xong lần đầu.";
        }
        TextView heading = label(title, 23, TEXT, true);
        heading.setGravity(Gravity.CENTER);
        if (status == Status.LOADING) {
            ProgressBar progress = new ProgressBar(getContext());
            progress.setIndeterminateTintList(ColorStateList.valueOf(GREEN));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(46));
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.topMargin = dp(82);
            contents.addView(progress, params);
            add(contents, heading, 18);
        } else {
            add(contents, heading, 100);
        }
        TextView description = label(message, 15, MUTED, false);
        description.setGravity(Gravity.CENTER);
        add(contents, description, 12);
        if (state != null && state.canRetry()) {
            add(contents, button("Thử lại", WHITE, GREEN, GREEN, model::retry), 24);
        } else if (status != Status.LOADING) {
            add(contents, button("Về Khám phá", WHITE, GREEN, GREEN, actions::discover), 24);
        }
    }

    private void removed(LinearLayout contents) {
        LinearLayout body = column();
        body.setGravity(Gravity.CENTER);
        body.setPadding(dp(24), dp(28), dp(24), dp(46));
        contents.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        ImageView info = new ImageView(getContext());
        info.setImageResource(R.drawable.ic_catalog_info);
        info.setPadding(dp(24), dp(24), dp(24), dp(24));
        info.setBackground(shape(PALE, PALE, 24));
        body.addView(info, new LinearLayout.LayoutParams(dp(84), dp(84)));
        TextView title = label("Công thức đã được gỡ", 23, TEXT, true);
        title.setGravity(Gravity.CENTER);
        add(body, title, 18);
        add(body, label("Bạn có thể tìm món khác. Ghi chú cá nhân của bạn vẫn được lưu.", 15, MUTED, false), 12);
        add(body, button("Về Khám phá", WHITE, GREEN, GREEN, actions::discover), 32);
        add(body, button("Xem ghi chú đã lưu", GREEN, WHITE, BORDER, actions::openSavedNote), 12);
    }

    public void displayDraft(Draft draft) {
        if (draft == null) {
            dismissSheet();
            return;
        }
        if (state == null || !state.canAdjust) return;
        if (servingOverlay == null) createSheet();
        if (!draft.input.contentEquals(number.getText())) {
            bindingNumber = true;
            number.setText(draft.input);
            number.setSelection(number.length());
            bindingNumber = false;
        }
        boolean valid = draft.valid();
        minus.setEnabled(valid && draft.servings > 1);
        plus.setEnabled(valid && draft.servings < 999);
        apply.setEnabled(valid);
        apply.setTextColor(valid ? WHITE : MUTED);
        apply.setBackground(clickable(valid ? GREEN : BORDER, valid ? GREEN : BORDER, 14));
        number.setBackground(shape(PALE, valid ? PALE : ERROR, 10));
        inputError.setVisibility(valid ? GONE : VISIBLE);
        comparison.removeAllViews();
        if (valid) ingredientRows(comparison, draft.ingredients, true, draft.servings);
    }

    private void createSheet() {
        servingOverlay = new FrameLayout(getContext());
        servingOverlay.setOnClickListener(view -> model.cancelAdjustment());
        LinearLayout layout = column();
        sheetPanel = layout;
        GradientDrawable sheetBackground = shape(WHITE, WHITE, 0);
        float corner = dp(26);
        sheetBackground.setCornerRadii(new float[]{corner, corner, corner, corner, 0, 0, 0, 0});
        layout.setBackground(sheetBackground);
        layout.setPadding(dp(24), dp(10), dp(24), dp(12));
        layout.setFocusableInTouchMode(true);
        layout.setClickable(true);
        View handle = new View(getContext());
        handle.setBackground(shape(BORDER, BORDER, 4));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(52), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(handle, handleParams);
        LinearLayout titleRow = row();
        LinearLayout titles = column();
        titles.addView(label("Điều chỉnh khẩu phần", 23, TEXT, true));
        titles.addView(label("Từ " + state.recipe.details.baseServings + " người", 13, MUTED, false));
        titleRow.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView close = icon(R.drawable.ic_close_outline, "Đóng", model::cancelAdjustment);
        close.setBackground(clickable(PALE, PALE, 16));
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        add(layout, titleRow, 8);

        ScrollView sheetScroll = scrollView();
        LinearLayout body = column();
        sheetScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        layout.addView(sheetScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout control = row();
        LinearLayout counter = row();
        counter.setBackground(shape(PALE, PALE, 16));
        minus = button("−", GREEN, PALE, PALE, () -> model.stepServings(-1));
        plus = button("+", GREEN, PALE, PALE, () -> model.stepServings(1));
        minus.setTextSize(23);
        plus.setTextSize(23);
        minus.setPadding(0, 0, 0, 0);
        plus.setPadding(0, 0, 0, 0);
        minus.setContentDescription("Giảm khẩu phần");
        plus.setContentDescription("Tăng khẩu phần");
        number = new EditText(getContext());
        number.setTextColor(TEXT);
        number.setTextSize(19);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setPadding(0, 0, 0, 0);
        number.setSingleLine(true);
        number.setGravity(Gravity.CENTER);
        number.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        number.setContentDescription("Số người ăn");
        number.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        number.setSelectAllOnFocus(true);
        counter.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        counter.addView(number, new LinearLayout.LayoutParams(dp(54), dp(48)));
        counter.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        control.addView(counter);
        TextView people = label("người ăn", 14, MUTED, false);
        people.setPadding(dp(12), 0, 0, 0);
        control.addView(people, new LinearLayout.LayoutParams(0, -2, 1));
        add(body, control, 16);
        inputError = label("Nhập số nguyên từ 1 đến 999.", 13, ERROR, false);
        inputError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(body, inputError, 6);
        comparison = column();
        add(body, comparison, 18);
        apply = button("Áp dụng khẩu phần", WHITE, GREEN, GREEN, model::applyServings);
        add(layout, apply, 10);
        number.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!bindingNumber) model.editServings(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        number.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            model.applyServings();
            return true;
        });

        servingOverlay.addView(layout, new LayoutParams(-1, sheetHeight(getHeight()), Gravity.BOTTOM));
        getChildAt(0).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        addView(servingOverlay, new LayoutParams(-1, -1));
        layout.requestFocus();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (sheetPanel == null || height == oldHeight) return;
        ViewGroup.LayoutParams params = sheetPanel.getLayoutParams();
        params.height = sheetHeight(height);
        sheetPanel.setLayoutParams(params);
    }

    private int sheetHeight(int height) {
        int available = height > 0 ? height : dp(getResources().getConfiguration().screenHeightDp);
        return Math.min(available, Math.max(dp(380), available * 81 / 100));
    }

    public void dismissSheet() {
        if (servingOverlay == null) return;
        if (number.hasFocus()) {
            InputMethodManager input = getContext().getSystemService(InputMethodManager.class);
            if (input != null) input.hideSoftInputFromWindow(number.getWindowToken(), 0);
        }
        removeView(servingOverlay);
        servingOverlay = null;
        sheetPanel = null;
        getChildAt(0).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    private void ingredientRows(LinearLayout container, Iterable<IngredientRow> rows,
                                boolean compare, int servings) {
        for (IngredientRow ingredient : rows) {
            LinearLayout line = row();
            line.setMinimumHeight(dp(compare ? 44 : 37));
            line.setPadding(0, dp(5), 0, dp(5));
            line.addView(label(ingredient.name, 14, TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
            String value = ingredient.amount;
            if (compare && !ingredient.qualitative && servings != state.recipe.details.baseServings) {
                value = ingredient.original + " → " + ingredient.amount;
            }
            TextView amount = label(value == null ? "Chưa có định lượng" : value,
                    value == null ? 12 : 14, value == null ? MUTED : GREEN, value != null);
            amount.setGravity(Gravity.END);
            line.addView(amount, new LinearLayout.LayoutParams(0, -2, compare ? 1.4f : 1));
            container.addView(line);
            View separator = new View(getContext());
            separator.setBackgroundColor(BORDER);
            container.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private void header(LinearLayout contents, String text, Runnable backAction) {
        LinearLayout header = row();
        header.addView(icon(R.drawable.ic_arrow_back_outline, "Quay lại", backAction),
                new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = label(text, 23, TEXT, true);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        contents.addView(header);
    }

    private void stat(LinearLayout meta, String title, String value) {
        if (!hasText(value)) return;
        LinearLayout card = column();
        card.setPadding(dp(10), dp(8), dp(8), dp(8));
        card.setBackground(shape(PALE, PALE, 13));
        card.addView(label(title, 11, MUTED, false));
        card.addView(label(value, 14, TEXT, true));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        if (meta.getChildCount() > 0) params.setMarginStart(dp(6));
        meta.addView(card, params);
    }

    private void info(LinearLayout contents, String title, String value, Runnable click) {
        LinearLayout info = row();
        info.setMinimumHeight(dp(click == null ? 31 : 48));
        info.addView(label(title, 14, TEXT, true), new LinearLayout.LayoutParams(0, -2, .39f));
        info.addView(label(value, 13, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        if (click != null) info.setOnClickListener(view -> click.run());
        contents.addView(info);
    }

    private ImageView cover(Recipe recipe) {
        ImageView image = new ImageView(getContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(PALE);
        image.setContentDescription("Ảnh " + recipe.title);
        images.bind(image, recipe.details == null ? null : recipe.details.imagePath);
        return image;
    }

    private ImageView icon(int resource, String description, Runnable click) {
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(resource);
        icon.setImageTintList(ColorStateList.valueOf(GREEN));
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setContentDescription(description);
        icon.setBackground(clickable(WHITE, WHITE, 16));
        icon.setOnClickListener(view -> click.run());
        return icon;
    }

    private TextView button(String title, int textColor, int fill, int border, Runnable click) {
        TextView button = label(title, 14, textColor, true);
        button.setGravity(Gravity.CENTER);
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(10), dp(6), dp(10), dp(6));
        button.setBackground(clickable(fill, border, 14));
        button.setOnClickListener(view -> click.run());
        return button;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView label = new TextView(getContext());
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return label;
    }

    private void add(LinearLayout parent, View child, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(gap);
        parent.addView(child, params);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private ScrollView scrollView() {
        ScrollView view = new ScrollView(getContext());
        view.setFillViewport(true);
        view.setVerticalScrollBarEnabled(false);
        return view;
    }

    private RippleDrawable clickable(int fill, int border, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x19326c4f), shape(fill, border, radius), null);
    }

    private GradientDrawable shape(int fill, int border, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (fill != border) shape.setStroke(dp(1), border);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    private static String minutes(Integer value) { return value == null ? null : value + " phút"; }
}
