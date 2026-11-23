package com.example.myapplication.presentation.mealplan;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.core.ui.BottomNavigationBar;
import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.model.PlannedRecipe;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.google.android.material.snackbar.Snackbar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nấu hôm nay list presentation; recipe selection remains coordinated by the Activity for now. */
public final class MealPlanFragment extends Fragment {
  private static final String STATE_SCROLL = "mealplan.fragment.scroll";
  private static final String STATE_PICKER_QUERY = "mealplan.fragment.pickerQuery";
  private static final String STATE_PICKER_SCROLL = "mealplan.fragment.pickerScroll";
  private static final String STATE_PICKER_MODE = "mealplan.fragment.pickerMode";
  private static final int CREAM = Color.rgb(250, 249, 246),
      GREEN = Color.rgb(50, 108, 79),
      PALE_GREEN = Color.rgb(232, 242, 233);
  private static final int TEXT = Color.rgb(37, 53, 45),
      MUTED = Color.rgb(88, 102, 92),
      BORDER = Color.rgb(220, 228, 220),
      WHITE = Color.WHITE;

  public interface Host {
    void onMealPlanOpenPicker();

    void onMealPlanOpenShopping();

    void onMealPlanNavigateTab(int tab);

    void onMealPlanOpenRecipe(Recipe recipe);

    void onMealPlanBackFromPicker();

    void onMealPlanSelectionSaved();
  }

  private MealPlanViewModel viewModel;
  private Host host;
  private LinearLayout root;
  private View bottomNavigation;
  private int restoredScroll;
  private int restoredPickerScroll;
  private boolean pickerMode;
  private String pickerQueryDraft = "";
  private boolean pickerQueryInitialized;
  private EditText pickerSearch;
  private ScrollView pickerScroll;
  private LinearLayout pickerRows;
  private TextView pickerSourceButton;
  private TextView pickerServingCount;
  private Runnable pendingSearch;

  public static MealPlanFragment newInstance(boolean pickerMode) {
    MealPlanFragment fragment = new MealPlanFragment();
    Bundle args = new Bundle();
    args.putBoolean("mealplan.pickerMode", pickerMode);
    fragment.setArguments(args);
    return fragment;
  }

  public void showPicker(boolean pickerMode) {
    if (pickerMode && !this.pickerMode) {
      pickerQueryDraft = viewModel == null ? "" : viewModel.getSearchQuery();
      pickerQueryInitialized = true;
    }
    this.pickerMode = pickerMode;
    render();
  }

  @Override
  public void onAttach(@NonNull android.content.Context context) {
    super.onAttach(context);
    if (!(context instanceof Host))
      throw new IllegalStateException("Host Activity must implement MealPlanFragment.Host");
    host = (Host) context;
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    if (state != null) {
      restoredScroll = Math.max(0, state.getInt(STATE_SCROLL));
      restoredPickerScroll = Math.max(0, state.getInt(STATE_PICKER_SCROLL));
      pickerMode =
          state.getBoolean(
              STATE_PICKER_MODE,
              getArguments() != null && getArguments().getBoolean("mealplan.pickerMode", false));
      if (state.containsKey(STATE_PICKER_QUERY)) {
        pickerQueryDraft = state.getString(STATE_PICKER_QUERY, "");
        pickerQueryInitialized = true;
      }
    } else if (getArguments() != null) {
      pickerMode = getArguments().getBoolean("mealplan.pickerMode", false);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    if (root != null && root.getChildCount() > 0) {
      View page = root.getChildAt(0);
      if (page instanceof LinearLayout) {
        View scroll = ((LinearLayout) page).findViewWithTag("mealplan-scroll");
        if (scroll instanceof ScrollView) restoredScroll = ((ScrollView) scroll).getScrollY();
        View picker = ((LinearLayout) page).findViewWithTag("mealplan-picker-scroll");
        if (picker instanceof ScrollView) restoredPickerScroll = ((ScrollView) picker).getScrollY();
      }
    }
    out.putInt(STATE_SCROLL, restoredScroll);
    out.putInt(STATE_PICKER_SCROLL, restoredPickerScroll);
    out.putBoolean(STATE_PICKER_MODE, pickerMode);
    out.putString(
        STATE_PICKER_QUERY,
        pickerSearch == null ? pickerQueryDraft : pickerSearch.getText().toString());
    super.onSaveInstanceState(out);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    AppViewModelFactory factory =
        new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication());
    viewModel = new ViewModelProvider(requireActivity(), factory).get(MealPlanViewModel.class);
    if (!pickerQueryInitialized) {
      pickerQueryDraft = viewModel.getSearchQuery();
      pickerQueryInitialized = true;
    }
    root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(CREAM);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          view.setPadding(
              system.left, system.top, system.right, Math.max(system.bottom, ime.bottom));
          return insets;
        });
    viewModel
        .planState()
        .observe(
            getViewLifecycleOwner(),
            stateValue -> {
              if (stateValue != null && !stateValue.loading) render();
            });
    render();
    return root;
  }

  private void render() {
    if (root == null || !isAdded()) return;
    captureScrollPositions();
    root.removeAllViews();
    if (pickerMode) {
      renderPicker();
      return;
    }
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(16));
    TextView heading = text("Nấu hôm nay", 28, TEXT, true);
    page.addView(heading);
    List<PlannedRecipe> planned = viewModel.getMealPlanRecipes();
    if (!planned.isEmpty()) {
      LinearLayout summary = vertical();
      summary.setPadding(dp(14), dp(12), dp(14), dp(12));
      summary.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 16));
      summary.addView(text(planned.size() + " món cho bữa cơm nhà.", 15, TEXT, true));
      MealPlanViewModel.MealDurationEstimate estimate = viewModel.estimateTotalDuration(planned);
      String duration;
      if (estimate.unknownRecipes == 0)
        duration = "Tổng thời gian ước tính: " + estimate.knownMinutes + " phút";
      else if (estimate.knownRecipes > 0)
        duration =
            "Ít nhất "
                + estimate.knownMinutes
                + " phút · "
                + estimate.unknownRecipes
                + " món chưa có thời lượng";
      else duration = "Chưa đủ dữ liệu thời lượng để ước tính";
      TextView durationView = text(duration, 13, GREEN, true);
      LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
      dp.topMargin = dp(5);
      summary.addView(durationView, dp);
      TextView note =
          text("Cộng thời lượng từng món; chưa tính việc nấu song song.", 11, MUTED, false);
      LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
      np.topMargin = dp(3);
      summary.addView(note, np);
      LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
      sp.topMargin = dp(18);
      sp.bottomMargin = dp(22);
      page.addView(summary, sp);
    }
    ScrollView scroll = new ScrollView(requireContext());
    scroll.setTag("mealplan-scroll");
    if (planned.isEmpty()) scroll.setFillViewport(true);
    LinearLayout content = vertical();
    scroll.addView(content);
    if (planned.isEmpty()) {
      LinearLayout empty = vertical();
      empty.setGravity(Gravity.CENTER_HORIZONTAL);
      content.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
      LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
      ep.topMargin = dp(144);
      content.addView(empty, ep);
      MealPlanViewModel.PlanState state = viewModel.planState().getValue();
      if (state != null && state.loading) {
        empty.addView(
            new android.widget.ProgressBar(requireContext()),
            new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView loading = text("Đang tải kế hoạch…", 14, MUTED, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        empty.addView(loading, lp);
      } else {
        ImageView icon = icon(R.drawable.ic_calendar_outline, GREEN);
        icon.setPadding(dp(24), dp(24), dp(24), dp(24));
        icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
        empty.addView(icon, new LinearLayout.LayoutParams(dp(84), dp(84)));
        TextView title = text("Bạn chưa chọn món", 22, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(18);
        tp.bottomMargin = dp(8);
        empty.addView(title, tp);
        TextView hint = text("Thêm món và đặt khẩu phần để chuẩn bị bữa ăn.", 14, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = dp(20);
        empty.addView(hint, hp);
        TextView choose = button("Chọn món", true);
        empty.addView(choose, new LinearLayout.LayoutParams(-1, dp(49)));
        choose.setOnClickListener(v -> host.onMealPlanOpenPicker());
      }
    } else {
      for (int i = 0; i < planned.size(); i++)
        addRecipeCard(content, planned.get(i), i, planned.size());
      TextView more = button("＋     Thêm món khác", false);
      more.setTextColor(GREEN);
      LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, dp(52));
      mp.topMargin = dp(14);
      mp.bottomMargin = dp(14);
      content.addView(more, mp);
      more.setOnClickListener(v -> host.onMealPlanOpenPicker());
    }
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (!planned.isEmpty()) {
      FrameLayout shop = new FrameLayout(requireContext());
      shop.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
      ImageView cart = icon(R.drawable.ic_cart_outline, GREEN);
      cart.setPadding(dp(14), dp(10), dp(10), dp(10));
      shop.addView(
          cart, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START | Gravity.CENTER_VERTICAL));
      TextView label = text("Tạo danh sách mua sắm", 15, GREEN, true);
      label.setGravity(Gravity.CENTER);
      shop.addView(label, new FrameLayout.LayoutParams(-1, -1));
      LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(48));
      sp.topMargin = dp(10);
      page.addView(shop, sp);
      shop.setOnClickListener(v -> host.onMealPlanOpenShopping());
      TextView cook = button("Bắt đầu nấu", true);
      LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(49));
      cp.topMargin = dp(10);
      page.addView(cook, cp);
      cook.setOnClickListener(v ->
          Toast.makeText(
                  requireContext(),
                  "Màn Nấu từng bước sẽ được kết nối khi hoàn thành module 4.",
                  Toast.LENGTH_SHORT)
              .show());
    }
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    addBottomNavigation();
    if (restoredScroll > 0) {
      int y = restoredScroll;
      scroll.post(() -> scroll.scrollTo(0, y));
      restoredScroll = 0;
    }
  }

  private void renderPicker() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(12));
    addPickerHeader(page);

    if (!pickerQueryDraft.equals(viewModel.getSearchQuery()))
      viewModel.setSearchQuery(pickerQueryDraft);

    pickerSearch = new EditText(requireContext());
    pickerSearch.setText(
        pickerQueryDraft.isEmpty() ? viewModel.getSearchQuery() : pickerQueryDraft);
    pickerSearch.setSingleLine(true);
    pickerSearch.setTextSize(16);
    pickerSearch.setTextColor(TEXT);
    pickerSearch.setHintTextColor(MUTED);
    pickerSearch.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    pickerSearch.setHint("Tìm món hoặc nguyên liệu");
    pickerSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    pickerSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    pickerSearch.setPadding(dp(48), dp(10), dp(12), dp(10));
    FrameLayout searchBox = new FrameLayout(requireContext());
    searchBox.setBackground(roundRect(WHITE, BORDER, 16));
    ImageView searchIcon = icon(R.drawable.ic_search_outline, GREEN);
    searchIcon.setPadding(dp(12), dp(12), dp(12), dp(12));
    searchBox.addView(searchIcon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
    searchBox.addView(pickerSearch, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
    searchParams.topMargin = dp(14);
    searchParams.bottomMargin = dp(10);
    page.addView(searchBox, searchParams);

    pickerSourceButton = text("", 14, GREEN, true);
    pickerSourceButton.setGravity(Gravity.CENTER_VERTICAL);
    pickerSourceButton.setSingleLine(true);
    pickerSourceButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
    pickerSourceButton.setMaxWidth(
        Math.round(getResources().getDisplayMetrics().widthPixels * .68f));
    pickerSourceButton.setPadding(dp(14), 0, dp(14), 0);
    pickerSourceButton.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-2, dp(40));
    sourceParams.bottomMargin = dp(14);
    page.addView(pickerSourceButton, sourceParams);
    pickerSourceButton.setOnClickListener(v -> showPickerSourceMenu(v));
    updatePickerSourceButton();

    pickerScroll = new ScrollView(requireContext());
    pickerScroll.setTag("mealplan-picker-scroll");
    pickerRows = vertical();
    pickerScroll.addView(pickerRows);
    populatePickerRows();
    page.addView(pickerScroll, new LinearLayout.LayoutParams(-1, 0, 1));

    Runnable[] delayed = new Runnable[1];
    pickerSearch.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable editable) {
            pickerQueryDraft = editable.toString();
            if (BaseInputConnection.getComposingSpanStart((Spannable) editable) >= 0) return;
            if (delayed[0] != null) pickerSearch.removeCallbacks(delayed[0]);
            delayed[0] =
                () -> {
                  if (pickerSearch != null
                      && BaseInputConnection.getComposingSpanStart(pickerSearch.getText()) < 0) {
                    applyPickerSearch(pickerSearch.getText().toString());
                  }
                };
            pendingSearch = delayed[0];
            pickerSearch.postDelayed(delayed[0], 1000L);
          }
        });
    pickerSearch.setOnEditorActionListener((view, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH
          || actionId == EditorInfo.IME_ACTION_DONE
          || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
        if (delayed[0] != null) pickerSearch.removeCallbacks(delayed[0]);
        applyPickerSearch(pickerSearch.getText().toString());
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager)
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(pickerSearch.getWindowToken(), 0);
        return true;
      }
      return false;
    });

    TextView selectedTitle = text("Khẩu phần món đã chọn", 20, TEXT, true);
    LinearLayout.LayoutParams selectedTitleParams = new LinearLayout.LayoutParams(-1, -2);
    selectedTitleParams.topMargin = dp(14);
    selectedTitleParams.bottomMargin = dp(8);
    page.addView(selectedTitle, selectedTitleParams);
    LinearLayout stepper = new LinearLayout(requireContext());
    stepper.setGravity(Gravity.CENTER_VERTICAL);
    stepper.setPadding(dp(4), 0, dp(4), 0);
    stepper.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
    stepper.addView(
        pickerStep("−", "Giảm khẩu phần món đã chọn", -1),
        new LinearLayout.LayoutParams(dp(48), dp(48)));
    pickerServingCount = text(String.valueOf(viewModel.getPickerServings()), 20, TEXT, true);
    pickerServingCount.setGravity(Gravity.CENTER);
    pickerServingCount.setTag("plan-serving-count");
    stepper.addView(pickerServingCount, new LinearLayout.LayoutParams(dp(48), dp(48)));
    stepper.addView(
        pickerStep("＋", "Tăng khẩu phần món đã chọn", 1),
        new LinearLayout.LayoutParams(dp(48), dp(48)));
    page.addView(stepper, new LinearLayout.LayoutParams(-2, dp(48)));

    TextView confirm = button("Thêm vào Nấu hôm nay", true);
    LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(-1, dp(52));
    confirmParams.topMargin = dp(14);
    page.addView(confirm, confirmParams);
    confirm.setOnClickListener(v -> savePickerSelection());
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    if (restoredPickerScroll > 0) {
      int y = restoredPickerScroll;
      pickerScroll.post(() -> pickerScroll.scrollTo(0, y));
      restoredPickerScroll = 0;
    }
  }

  private void addPickerHeader(LinearLayout page) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    ImageView back = icon(R.drawable.ic_arrow_back_outline, GREEN);
    back.setPadding(dp(12), dp(12), dp(12), dp(12));
    back.setBackground(roundRect(WHITE, WHITE, 15));
    back.setContentDescription("Quay lại");
    row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
    back.setOnClickListener(v -> host.onMealPlanBackFromPicker());
    TextView title = text("Thêm món vào kế hoạch", 20, TEXT, true);
    title.setPadding(dp(12), 0, 0, 0);
    row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    page.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private void populatePickerRows() {
    if (pickerRows == null) return;
    pickerRows.removeAllViews();
    List<Recipe> recipes = viewModel.getPickerRecipes(PreviewRecipeCatalog.mealPlanPreviews());
    List<Recipe> matches = viewModel.filterRecipes(recipes);
    if (matches.isEmpty()) {
      String message = "Không tìm thấy món phù hợp.";
      if (viewModel.getSearchQuery().trim().isEmpty()) {
        if (viewModel.getPickerSource() == MealPlanViewModel.PickerSource.FAVORITES)
          message = "Chưa có món yêu thích nào.";
        else if (viewModel.getPickerSource() == MealPlanViewModel.PickerSource.COLLECTION
            && viewModel.getPickerCollectionId() > 0) message = "Bộ sưu tập này chưa có món nào.";
        else if (viewModel.getPickerSource() == MealPlanViewModel.PickerSource.COLLECTION)
          message = "Chọn một bộ sưu tập để xem món.";
      }
      TextView empty = text(message, 14, MUTED, false);
      empty.setGravity(Gravity.CENTER);
      pickerRows.addView(empty, new LinearLayout.LayoutParams(-1, dp(72)));
      return;
    }
    for (Recipe recipe : matches) {
      boolean planned = viewModel.isRecipeInPlan(recipe.id);
      boolean selected = viewModel.isSelected(recipe.id);
      LinearLayout row = new LinearLayout(requireContext());
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(dp(7), dp(6), dp(14), dp(6));
      row.setBackground(roundRect(WHITE, BORDER, 15));
      row.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(52), dp(52)));
      LinearLayout labels = vertical();
      labels.setPadding(dp(12), 0, 0, 0);
      labels.addView(text(recipe.title, 15, TEXT, true));
      String subtitle =
          planned
              ? (selected ? "Đã có trong kế hoạch" : "Sẽ bỏ khỏi kế hoạch")
              : (selected ? "Đang chọn" : recipe.duration + " · " + recipe.difficulty);
      labels.addView(text(subtitle, 13, MUTED, false));
      row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
      FrameLayout toggle = new FrameLayout(requireContext());
      toggle.setFocusable(true);
      toggle.setClickable(true);
      toggle.setContentDescription((selected ? "Bỏ chọn " : "Chọn ") + recipe.title);
      TextView mark = text(selected ? "✓" : "+", 21, selected ? WHITE : GREEN, false);
      mark.setGravity(Gravity.CENTER);
      if (selected) mark.setBackground(roundRect(GREEN, GREEN, 8));
      toggle.addView(mark, new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER));
      row.addView(toggle, new LinearLayout.LayoutParams(dp(48), dp(48)));
      LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(64));
      rowParams.bottomMargin = dp(10);
      pickerRows.addView(row, rowParams);
      row.setContentDescription("Xem chi tiết món " + recipe.title);
      row.setFocusable(true);
      row.setOnClickListener(v -> host.onMealPlanOpenRecipe(recipe));
      toggle.setOnClickListener(v -> {
        viewModel.toggleRecipe(recipe.id);
        populatePickerRows();
      });
    }
  }

  private void applyPickerSearch(String query) {
    pickerQueryDraft = query;
    viewModel.setSearchQuery(query);
    populatePickerRows();
  }

  private void showPickerSourceMenu(View anchor) {
    PopupMenu menu = new PopupMenu(requireContext(), anchor);
    menu.getMenu().add(0, 1, 0, "Tất cả món");
    menu.getMenu().add(0, 2, 1, "Món yêu thích");
    Map<Integer, Long> collectionIds = new LinkedHashMap<>();
    List<RecipeCollection> collections = viewModel.getRecipeCollections();
    if (collections.isEmpty()) menu.getMenu().add(0, 3, 2, "Chưa có bộ sưu tập").setEnabled(false);
    else {
      int id = 10;
      int order = 2;
      for (RecipeCollection collection : collections) {
        menu.getMenu().add(0, id, order++, "Bộ sưu tập: " + collection.name);
        collectionIds.put(id++, collection.id);
      }
    }
    menu.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == 1) viewModel.setPickerSource(MealPlanViewModel.PickerSource.ALL, -1L);
      else if (item.getItemId() == 2)
        viewModel.setPickerSource(MealPlanViewModel.PickerSource.FAVORITES, -1L);
      else {
        Long id = collectionIds.get(item.getItemId());
        if (id == null) return false;
        viewModel.setPickerSource(MealPlanViewModel.PickerSource.COLLECTION, id);
      }
      updatePickerSourceButton();
      populatePickerRows();
      if (pickerScroll != null) pickerScroll.scrollTo(0, 0);
      return true;
    });
    menu.show();
  }

  private void updatePickerSourceButton() {
    if (pickerSourceButton == null) return;
    String label = "Tất cả món";
    if (viewModel.getPickerSource() == MealPlanViewModel.PickerSource.FAVORITES)
      label = "Yêu thích";
    else if (viewModel.getPickerSource() == MealPlanViewModel.PickerSource.COLLECTION) {
      label = "Chọn bộ sưu tập";
      for (RecipeCollection collection : viewModel.getRecipeCollections()) {
        if (collection.id == viewModel.getPickerCollectionId()) {
          label = collection.name;
          break;
        }
      }
    }
    pickerSourceButton.setText(label + "  ▾");
    pickerSourceButton.setContentDescription("Nguồn món: " + label + ". Chạm để đổi nguồn.");
  }

  private TextView pickerStep(String label, String description, int delta) {
    TextView step = text(label, 20, GREEN, true);
    step.setGravity(Gravity.CENTER);
    step.setContentDescription(description);
    step.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    step.setOnClickListener(v -> {
      int servings = viewModel.getPickerServings() + delta;
      if (servings < MealPlanViewModel.MIN_SERVINGS || servings > MealPlanViewModel.MAX_SERVINGS)
        return;
      viewModel.setPickerServings(servings);
      if (pickerServingCount != null) pickerServingCount.setText(String.valueOf(servings));
    });
    return step;
  }

  private void savePickerSelection() {
    if (!viewModel.saveSelection(
        viewModel.getAllSelectableRecipes(PreviewRecipeCatalog.designRecipes()))) {
      Toast.makeText(requireContext(), "Chọn ít nhất một món.", Toast.LENGTH_SHORT).show();
      return;
    }
    host.onMealPlanSelectionSaved();
  }

  private void addRecipeCard(LinearLayout parent, PlannedRecipe item, int position, int total) {
    boolean compact =
        getResources().getConfiguration().screenWidthDp < 360
            || getResources().getConfiguration().fontScale >= 1.15f;
    LinearLayout card = new LinearLayout(requireContext());
    card.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
    card.setGravity(compact ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
    int padding = compact ? 8 : 11;
    card.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
    card.setBackground(roundRect(WHITE, BORDER, 20));
    LinearLayout contentRow = card;
    if (compact) {
      contentRow = new LinearLayout(requireContext());
      contentRow.setGravity(Gravity.CENTER_VERTICAL);
      card.addView(contentRow, new LinearLayout.LayoutParams(-1, -2));
    }
    ImageView image = recipeImage(item.recipe);
    int imageSize = compact ? 64 : 78;
    contentRow.addView(image, new LinearLayout.LayoutParams(dp(imageSize), dp(imageSize)));
    LinearLayout details = vertical();
    details.setPadding(dp(compact ? 8 : 12), 0, 0, 0);
    LinearLayout titleRow = new LinearLayout(requireContext());
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
    addOrderControls(compact ? card : details, item, position, total);
    LinearLayout servings = new LinearLayout(requireContext());
    servings.setGravity(Gravity.CENTER_VERTICAL);
    servings.setPadding(dp(4), 0, dp(4), 0);
    servings.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
    TextView minus = step("−", "Giảm khẩu phần", () -> changeServings(item, -1));
    servings.addView(minus, new LinearLayout.LayoutParams(dp(40), dp(42)));
    TextView count = text(String.valueOf(item.servings), 19, TEXT, true);
    count.setGravity(Gravity.CENTER);
    servings.addView(count, new LinearLayout.LayoutParams(dp(40), dp(42)));
    TextView plus = step("＋", "Tăng khẩu phần", () -> changeServings(item, 1));
    servings.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(42)));
    TextView person = text("người", 14, MUTED, false);
    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
    pp.leftMargin = dp(5);
    servings.addView(person, pp);
    LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(-1, -2);
    svp.topMargin = dp(8);
    if (compact) card.addView(servings, svp);
    else details.addView(servings, svp);
    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
    cp.bottomMargin = dp(16);
    parent.addView(card, cp);
    remove.setOnClickListener(v -> removeRecipe(item));
    image.setOnClickListener(v -> host.onMealPlanOpenRecipe(item.recipe));
    title.setOnClickListener(v -> host.onMealPlanOpenRecipe(item.recipe));
  }

  private void addOrderControls(LinearLayout card, PlannedRecipe item, int position, int total) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView label = text("Thứ tự nấu · " + (position + 1) + "/" + total, 12, MUTED, false);
    row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
    TextView up =
        orderButton(
            "↑",
            "Đưa " + item.recipe.title + " lên trước",
            position > 0,
            () -> viewModel.moveMealPlanRecipe(item.recipe.id, -1));
    TextView down =
        orderButton(
            "↓",
            "Đưa " + item.recipe.title + " xuống sau",
            position < total - 1,
            () -> viewModel.moveMealPlanRecipe(item.recipe.id, 1));
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(48), dp(48));
    bp.leftMargin = dp(4);
    row.addView(up, bp);
    LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(dp(48), dp(48));
    dp.leftMargin = dp(4);
    row.addView(down, dp);
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
    rp.topMargin = dp(6);
    card.addView(row, rp);
  }

  private TextView orderButton(String glyph, String description, boolean enabled, Runnable action) {
    TextView b = text(glyph, 20, enabled ? GREEN : MUTED, true);
    b.setGravity(Gravity.CENTER);
    b.setBackground(roundRect(enabled ? PALE_GREEN : CREAM, enabled ? PALE_GREEN : BORDER, 14));
    b.setContentDescription(description);
    b.setFocusable(true);
    b.setEnabled(enabled);
    if (enabled) b.setOnClickListener(v -> action.run());
    return b;
  }

  private void removeRecipe(PlannedRecipe item) {
    MealPlanViewModel.RemovedPlanRecipe removed =
        viewModel.removeMealPlanRecipeForUndo(item.recipe.id);
    if (removed == null) return;
    Snackbar snackbar =
        Snackbar.make(root, "Đã bỏ " + item.recipe.title + " khỏi kế hoạch.", Snackbar.LENGTH_LONG)
            .setDuration(5000)
            .setAction(
                "Hoàn tác",
                v -> {
                  if (!viewModel.restoreMealPlanRecipe(removed))
                    Toast.makeText(
                            requireContext(),
                            "Món này đã có lại trong kế hoạch.",
                            Toast.LENGTH_SHORT)
                        .show();
                });
    if (bottomNavigation != null) snackbar.setAnchorView(bottomNavigation);
    snackbar.show();
  }

  private void changeServings(PlannedRecipe item, int delta) {
    int servings = item.servings + delta;
    if (servings < MealPlanViewModel.MIN_SERVINGS || servings > MealPlanViewModel.MAX_SERVINGS)
      return;
    viewModel.setMealPlanRecipeServings(item.recipe.id, servings);
  }

  private void addBottomNavigation() {
    BottomNavigationBar navigation =
        new BottomNavigationBar(requireContext(), 1, host::onMealPlanNavigateTab);
    bottomNavigation = navigation.getNavigationStrip();
    root.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
  }

  private void captureScrollPositions() {
    if (root == null || root.getChildCount() == 0) return;
    View page = root.getChildAt(0);
    if (!(page instanceof LinearLayout)) return;
    View list = ((LinearLayout) page).findViewWithTag("mealplan-scroll");
    if (list instanceof ScrollView) restoredScroll = ((ScrollView) list).getScrollY();
    View picker = ((LinearLayout) page).findViewWithTag("mealplan-picker-scroll");
    if (picker instanceof ScrollView) restoredPickerScroll = ((ScrollView) picker).getScrollY();
  }

  private TextView step(String value, String description, Runnable action) {
    TextView b = text(value, 20, GREEN, true);
    b.setGravity(Gravity.CENTER);
    b.setContentDescription(description);
    b.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private TextView button(String value, boolean primary) {
    TextView b = text(value, 15, primary ? WHITE : GREEN, true);
    b.setGravity(Gravity.CENTER);
    b.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
    return b;
  }

  private ImageView recipeImage(Recipe recipe) {
    int image = R.drawable.recipe_canh_chua;
    switch (recipe.id) {
      case "sample-ga-chien-nuoc-mam":
        image = R.drawable.recipe_ga_chien;
        break;
      case "sample-mi-y-sot-bo-bam":
        image = R.drawable.recipe_mi_y;
        break;
      case "sample-com-rang-dua-bo":
        image = R.drawable.recipe_com_rang;
        break;
      case "sample-salad-rau-cu":
        image = R.drawable.recipe_salad;
        break;
    }
    ImageView view = icon(image, TEXT);
    view.clearColorFilter();
    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
    view.setBackground(roundRect(BORDER, BORDER, 12));
    view.setClipToOutline(true);
    return view;
  }

  private ImageView icon(int id, int tint) {
    ImageView image = new ImageView(requireContext());
    image.setImageResource(id);
    image.setColorFilter(tint);
    image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    return image;
  }

  private LinearLayout vertical() {
    LinearLayout l = new LinearLayout(requireContext());
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private TextView text(String value, int size, int color, boolean bold) {
    TextView t = new TextView(requireContext());
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(
        t.getTypeface(), bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    t.setGravity(Gravity.CENTER_VERTICAL);
    return t;
  }

  private GradientDrawable roundRect(int fill, int stroke, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(fill);
    d.setCornerRadius(dp(radius));
    d.setStroke(dp(1), stroke);
    return d;
  }

  private int dp(float value) {
    return (int) (value * getResources().getDisplayMetrics().density + .5f);
  }
}
