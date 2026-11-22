package com.example.myapplication.presentation.shopping;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.core.ui.BottomNavigationBar;
import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.ShoppingCategoryCatalog;
import com.example.myapplication.domain.model.ShoppingListItem;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopping list and item-form presentation for module 7.
 */
public final class ShoppingFragment extends Fragment {
  private static final String STATE_FILTER = "shopping.fragment.filter";
  private static final String ARG_FILTER = "shopping.fragment.initialFilter";
  private static final int CREAM = Color.rgb(250, 249, 246);
  private static final int GREEN = Color.rgb(50, 108, 79);
  private static final int PALE_GREEN = Color.rgb(232, 242, 233);
  private static final int TEXT = Color.rgb(37, 53, 45);
  private static final int MUTED = Color.rgb(88, 102, 92);
  private static final int BORDER = Color.rgb(220, 228, 220);
  private static final int ERROR = Color.rgb(169, 54, 54);
  private static final int WHITE = Color.WHITE;

  public interface Host {
    void onShoppingBack();

    void onShoppingNavigateTab(int tab);

    void onShoppingOpenPantry();

    void onShoppingEditItem(String id);

    void onShoppingFilterChanged(String status);

    void onShoppingFormClosed();

    void onShoppingFormSaved(String status);
  }

  private ShoppingViewModel viewModel;
  private Host host;
  private LinearLayout root;
  private View bottomNavigation;
  private String statusFilter = ShoppingViewModel.NEED_TO_BUY;
  private boolean formShowing;
  private String editingItemId;
  private String draftName = "";
  private String draftAmount = "";
  private String draftUnit = "";
  private String draftUnitId = "";
  private String originalDraftUnit = "";
  private String draftCategory = ShoppingCategoryCatalog.OTHER;
  private EditText nameInput;
  private EditText amountInput;
  private EditText unitInput;
  private TextView categoryInput;

  public static ShoppingFragment newInstance(String initialFilter) {
    ShoppingFragment fragment = new ShoppingFragment();
    Bundle args = new Bundle();
    args.putString(ARG_FILTER, initialFilter);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(@NonNull android.content.Context context) {
    super.onAttach(context);
    if (!(context instanceof Host))
      throw new IllegalStateException("Host Activity must implement ShoppingFragment.Host");
    host = (Host) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      statusFilter = savedInstanceState.getString(STATE_FILTER, ShoppingViewModel.NEED_TO_BUY);
      formShowing = savedInstanceState.getBoolean("shopping.fragment.formShowing", false);
      editingItemId = savedInstanceState.getString("shopping.fragment.itemId");
      draftName = savedInstanceState.getString("shopping.fragment.name", "");
      draftAmount = savedInstanceState.getString("shopping.fragment.amount", "");
      draftUnit = savedInstanceState.getString("shopping.fragment.unit", "");
      draftUnitId = savedInstanceState.getString("shopping.fragment.unitId", "");
      draftCategory = savedInstanceState.getString("shopping.fragment.category", ShoppingCategoryCatalog.OTHER);
    } else if (getArguments() != null)
      statusFilter = getArguments().getString(ARG_FILTER, ShoppingViewModel.NEED_TO_BUY);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(STATE_FILTER, statusFilter);
    outState.putBoolean("shopping.fragment.formShowing", formShowing);
    outState.putString("shopping.fragment.itemId", editingItemId);
    outState.putString("shopping.fragment.name", currentText(nameInput, draftName));
    outState.putString("shopping.fragment.amount", currentText(amountInput, draftAmount));
    outState.putString("shopping.fragment.unit", currentText(unitInput, draftUnit));
    outState.putString("shopping.fragment.unitId", draftUnitId);
    outState.putString("shopping.fragment.category", draftCategory);
    super.onSaveInstanceState(outState);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    AppViewModelFactory factory =
        new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication());
    viewModel = new ViewModelProvider(requireActivity(), factory).get(ShoppingViewModel.class);
    root = vertical();
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
        .saveResult()
        .observe(
            getViewLifecycleOwner(),
            result -> {
              if (result == null) return;
              viewModel.clearSaveResult();
              if (result.success) {
                statusFilter = result.status;
                formShowing = false;
                render();
                host.onShoppingFormSaved(result.status);
              } else {
                Toast.makeText(
                        requireContext(),
                        result.error == null ? "Không thể lưu nguyên liệu." : result.error,
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });
    viewModel
        .planUpdate()
        .observe(
            getViewLifecycleOwner(),
            update -> {
              if (update == null) return;
              viewModel.clearPlanUpdate();
              if (!update.changes.isEmpty()) showPlanUpdate(update);
              else if (update.message != null && !update.message.isEmpty())
                Toast.makeText(requireContext(), update.message, Toast.LENGTH_SHORT).show();
            });
    viewModel
        .shoppingState()
        .observe(
            getViewLifecycleOwner(),
            state -> {
              if (state == null) return;
              if (!state.loading) {
                if (!formShowing) render();
                if (state.error != null && !state.error.isEmpty())
                  Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show();
              }
            });
    render();
    return root;
  }

  private void render() {
    if (root == null || !isAdded()) return;
    root.removeAllViews();
    if (formShowing) {
      renderForm();
      return;
    }
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(20), dp(24), dp(16));
    addShoppingTitle(page);
    List<ShoppingListItem> items = viewModel.getItems();
    if (items.isEmpty()) renderEmpty(page);
    else renderItems(page, items);
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    addBottomNavigation();
  }

  public boolean isFormShowing() {
    return formShowing;
  }

  public void showForm(String id) {
    if (viewModel == null) {
      editingItemId = id;
      formShowing = true;
      return;
    }
    editingItemId = id;
    ShoppingListItem item = viewModel.findItem(id);
    draftName = item == null ? "" : item.name;
    draftAmount = item == null ? "" : item.amount;
    draftUnit = item == null ? "" : com.example.myapplication.domain.catalog.KitchenUnitCatalog.label(item.unit);
    draftUnitId = item == null ? "" : item.unit;
    originalDraftUnit = draftUnit;
    draftCategory = item == null ? ShoppingCategoryCatalog.OTHER : item.category;
    formShowing = true;
    render();
  }

  public void showList() {
    formShowing = false;
    render();
  }

  public void handleBackPressed() {
    if (formShowing) {
      formShowing = false;
      render();
      host.onShoppingFormClosed();
    } else {
      host.onShoppingBack();
    }
  }

  private void renderForm() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(20), dp(24), dp(16));
    addFormBackTitle(page, editingItemId == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu");
    ScrollView scroll = new ScrollView(requireContext());
    scroll.setFillViewport(false);
    scroll.setClipToPadding(false);
    LinearLayout fields = vertical();
    fields.setPadding(0, dp(20), 0, dp(20));
    scroll.addView(fields);

    addText(fields, "Tên nguyên liệu", 15, TEXT, true, 0, 0, 0, 0);
    nameInput = editField(draftName, "Ví dụ: Cà chua");
    addFormField(fields, nameInput, 10, 22);

    addText(fields, "Số lượng", 15, TEXT, true, 0, 0, 0, 0);
    amountInput = editField(draftAmount, "Ví dụ: 2 hoặc vừa đủ");
    addFormField(fields, amountInput, 10, 22);

    addText(fields, "Đơn vị", 15, TEXT, true, 0, 0, 0, 0);
    unitInput = editField(draftUnit, "Ví dụ: quả, g, ml, muỗng cà phê");
    addFormField(fields, unitInput, 10, 26);

    addText(fields, "Danh mục", 15, TEXT, true, 0, 0, 0, 0);
    categoryInput = selectionField(draftCategory + "     ▾", 16, 56);
    addFormField(fields, categoryInput, 10, 26);
    categoryInput.setOnClickListener(v -> showCategoryPicker());

    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    addFormButton(page, "Lưu nguyên liệu", true, this::saveForm, 10);
    if (editingItemId != null) {
      TextView remove = text("Xóa nguyên liệu", 14, ERROR, true);
      remove.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(42));
      params.topMargin = dp(6);
      page.addView(remove, params);
      remove.setOnClickListener(v -> confirmDeleteFormItem());
    }
    root.addView(page, new LinearLayout.LayoutParams(-1, -1));
  }

  private void addFormBackTitle(LinearLayout parent, String title) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    ImageView back = new ImageView(requireContext());
    back.setImageResource(R.drawable.ic_arrow_back_outline);
    back.setColorFilter(GREEN);
    back.setPadding(dp(12), dp(12), dp(12), dp(12));
    back.setBackground(roundRect(WHITE, WHITE, 15));
    back.setContentDescription("Quay lại");
    row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
    back.setOnClickListener(v -> handleBackPressed());
    TextView heading = text(title, 23, TEXT, true);
    heading.setPadding(dp(12), 0, 0, 0);
    row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private EditText editField(String value, String hint) {
    EditText field = new EditText(requireContext());
    field.setText(value);
    field.setTextSize(16);
    field.setTextColor(TEXT);
    field.setHintTextColor(MUTED);
    field.setSingleLine(true);
    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    field.setHint(hint);
    field.setBackground(roundRect(WHITE, BORDER, 14));
    field.setPadding(dp(15), dp(10), dp(15), dp(10));
    field.setMinHeight(dp(56));
    return field;
  }

  private void addFormField(LinearLayout parent, View field, int top, int bottom) {
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

  private void addFormButton(LinearLayout parent, String label, boolean primary, Runnable action, int top) {
    TextView button = button(label, primary);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(49));
    params.topMargin = dp(top);
    parent.addView(button, params);
    button.setOnClickListener(v -> action.run());
  }

  private void showCategoryPicker() {
    String[] categories = ShoppingCategoryCatalog.ORDER.toArray(new String[0]);
    int selected = ShoppingCategoryCatalog.ORDER.indexOf(draftCategory);
    new AlertDialog.Builder(requireContext())
        .setTitle("Chọn danh mục")
        .setSingleChoiceItems(categories, Math.max(0, selected), (dialog, which) -> {
          draftCategory = categories[which];
          categoryInput.setText(draftCategory + "     ▾");
          dialog.dismiss();
        })
        .setNegativeButton("Hủy", null)
        .show();
  }

  private void saveForm() {
    draftName = nameInput.getText().toString();
    draftAmount = amountInput.getText().toString();
    String enteredUnit = unitInput.getText().toString();
    String canonicalUnit =
        enteredUnit.equals(originalDraftUnit) && !draftUnitId.isEmpty()
            ? draftUnitId
            : com.example.myapplication.domain.catalog.KitchenUnitCatalog.canonicalId(enteredUnit);
    draftUnit = enteredUnit;
    ShoppingListItem existing = viewModel.findItem(editingItemId);
    String status = editingItemId == null ? null
        : existing == null ? ShoppingViewModel.NEED_TO_BUY : existing.status;
    viewModel.saveItem(editingItemId, draftName, draftAmount, canonicalUnit, draftCategory, status);
  }

  private void confirmDeleteFormItem() {
    ShoppingListItem item = viewModel.findItem(editingItemId);
    if (item == null) return;
    new AlertDialog.Builder(requireContext())
        .setTitle("Xóa nguyên liệu?")
        .setMessage("Xóa “" + item.name + "” khỏi danh sách mua sắm?")
        .setNegativeButton("Hủy", null)
        .setPositiveButton("Xóa", (dialog, which) -> {
          viewModel.deleteItem(editingItemId);
          formShowing = false;
          editingItemId = null;
          render();
          host.onShoppingFormClosed();
        })
        .show();
  }

  private String currentText(EditText input, String fallback) {
    return input == null ? fallback : input.getText().toString();
  }

  private void showPlanUpdate(ShoppingViewModel.PlanUpdate update) {
        if (update == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        LinearLayout sheet = vertical();
        sheet.setPadding(dp(24), dp(12), dp(24), dp(24));
        sheet.setBackground(roundRect(WHITE, WHITE, 26));
        View handle = new View(requireContext());
        handle.setBackground(roundRect(BORDER, BORDER, 4));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(72), dp(6));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        sheet.addView(handle, handleParams);

        LinearLayout titleRow = new LinearLayout(requireContext());
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

        ScrollView changesScroll = new ScrollView(requireContext());
        changesScroll.setClipToPadding(false);
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

        LinearLayout actions = new LinearLayout(requireContext());
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
            viewModel.acceptPlanUpdate(update);
            dialog.dismiss();
            Toast.makeText(
                            requireContext(),
                            "Đã ghi nhận kế hoạch mới; mục nhập tay được giữ nguyên.",
                            Toast.LENGTH_SHORT)
                    .show();
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

  private void renderEmpty(LinearLayout page) {
    ScrollView emptyScroll = new ScrollView(requireContext());
    emptyScroll.setFillViewport(true);
    LinearLayout empty = vertical();
    empty.setGravity(Gravity.CENTER);
    empty.setPadding(dp(24), 0, dp(24), 0);
    emptyScroll.addView(empty, new ScrollView.LayoutParams(-1, -1));
    ImageView icon = new ImageView(requireContext());
    icon.setImageResource(R.drawable.ic_cart_outline);
    icon.setColorFilter(GREEN);
    icon.setPadding(dp(24), dp(24), dp(24), dp(24));
    icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
    empty.addView(icon, new LinearLayout.LayoutParams(dp(84), dp(84)));
    ShoppingViewModel.ShoppingState state = viewModel.shoppingState().getValue();
    if (state != null && state.loading) {
      android.widget.ProgressBar progress = new android.widget.ProgressBar(requireContext());
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(28), dp(28));
      params.topMargin = dp(16);
      empty.addView(progress, params);
      TextView loading = text("Đang tải danh sách mua sắm…", 14, MUTED, false);
      loading.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(-1, -2);
      loadingParams.topMargin = dp(12);
      empty.addView(loading, loadingParams);
    } else {
      TextView title = text("Danh sách đang trống", 22, TEXT, true);
      title.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
      titleParams.topMargin = dp(18);
      titleParams.bottomMargin = dp(8);
      empty.addView(title, titleParams);
      TextView hint =
          text(
              "Chưa có dữ liệu nguyên liệu công thức. Bạn có thể thêm thủ công.", 14, MUTED, false);
      hint.setGravity(Gravity.CENTER);
      hint.setLineSpacing(dp(3), 1f);
      empty.addView(hint);
    }
    TextView add = button("Thêm nguyên liệu", true);
    LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(49));
    addParams.topMargin = dp(44);
    empty.addView(add, addParams);
    add.setOnClickListener(v -> host.onShoppingEditItem(null));
    page.addView(emptyScroll, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void renderItems(LinearLayout page, List<ShoppingListItem> items) {
    TextView summary = text("Danh sách nguyên liệu lưu trên thiết bị.", 14, MUTED, false);
    summary.setLineSpacing(dp(3), 1f);
    LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
    summaryParams.topMargin = dp(14);
    page.addView(summary, summaryParams);
    int availableCount = 0;
    for (ShoppingListItem item : items)
      if (ShoppingViewModel.AVAILABLE.equals(item.status)) availableCount++;
    android.widget.ProgressBar progress =
        new android.widget.ProgressBar(
            requireContext(), null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(Math.max(1, items.size()));
    progress.setProgress(availableCount);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
    progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BORDER));
    LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
    progressParams.topMargin = dp(24);
    page.addView(progress, progressParams);
    addText(
        page,
        availableCount + "/" + items.size() + " nguyên liệu đã có",
        14,
        MUTED,
        false,
        0,
        8,
        0,
        0);
    LinearLayout filters = new LinearLayout(requireContext());
    filters.setGravity(Gravity.CENTER_VERTICAL);
    addFilterChip(filters, "Cần mua", ShoppingViewModel.NEED_TO_BUY);
    addFilterChip(filters, "Đã có", ShoppingViewModel.AVAILABLE);
    HorizontalScrollView filterScroll = new HorizontalScrollView(requireContext());
    filterScroll.setHorizontalScrollBarEnabled(false);
    filterScroll.addView(filters, new HorizontalScrollView.LayoutParams(-2, -2));
    LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(44));
    filterParams.topMargin = dp(24);
    filterParams.bottomMargin = dp(20);
    page.addView(filterScroll, filterParams);
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout rows = vertical();
    rows.setPadding(0, 0, 0, dp(12));
    scroll.addView(rows);
    Map<String, List<ShoppingListItem>> groups = new LinkedHashMap<>();
    for (String category : ShoppingCategoryCatalog.ORDER) groups.put(category, new ArrayList<>());
    for (ShoppingListItem item : items) {
      if (statusFilter != null && !statusFilter.equals(item.status)) continue;
      String category =
          ShoppingCategoryCatalog.contains(item.category)
              ? item.category
              : ShoppingCategoryCatalog.OTHER;
      groups.get(category).add(item);
    }
    int groupCount = 0;
    for (Map.Entry<String, List<ShoppingListItem>> group : groups.entrySet()) {
      if (group.getValue().isEmpty()) continue;
      addText(rows, group.getKey(), 20, TEXT, true, 0, groupCount++ == 0 ? 0 : 18, 0, 0);
      for (ShoppingListItem item : group.getValue()) addItemRow(rows, item);
    }
    if (filteredCount(items) == 0) {
      TextView none = text("Không có nguyên liệu ở trạng thái này.", 14, MUTED, false);
      none.setPadding(0, dp(18), 0, 0);
      rows.addView(none);
    }
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout actions = new LinearLayout(requireContext());
    LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(49));
    actionsParams.topMargin = dp(10);
    page.addView(actions, actionsParams);
    TextView add = button("＋   Thêm nguyên liệu", true);
    actions.addView(add, new LinearLayout.LayoutParams(0, -1, 1.6f));
    add.setOnClickListener(v -> host.onShoppingEditItem(null));
    TextView clear = button("Xóa toàn bộ", false);
    clear.setTextColor(ERROR);
    LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, -1, 1);
    clearParams.leftMargin = dp(12);
    actions.addView(clear, clearParams);
    clear.setOnClickListener(v -> confirmClear(items.size()));
  }

  private void addShoppingTitle(LinearLayout page) {
    TextView heading = text("Danh sách mua sắm", 23, TEXT, true);
    LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
    page.addView(heading, headingParams);
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView subtitle = text("Danh sách nguyên liệu lưu trên thiết bị.", 14, MUTED, false);
    row.addView(subtitle, new LinearLayout.LayoutParams(0, -2, 1));
    TextView pantry = text("Tủ nguyên liệu", 12, GREEN, true);
    pantry.setGravity(Gravity.CENTER);
    pantry.setPadding(dp(8), 0, dp(8), 0);
    pantry.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
    row.addView(pantry, new LinearLayout.LayoutParams(-2, dp(40)));
    pantry.setOnClickListener(v -> host.onShoppingOpenPantry());
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
    rowParams.topMargin = dp(8);
    page.addView(row, rowParams);
  }

  private void addFilterChip(LinearLayout parent, String label, String status) {
    boolean selected = status.equals(statusFilter);
    TextView chip = text(label, 14, selected ? GREEN : MUTED, true);
    chip.setGravity(Gravity.CENTER);
    chip.setPadding(dp(13), 0, dp(13), 0);
    chip.setBackground(
        roundRect(selected ? PALE_GREEN : WHITE, selected ? PALE_GREEN : BORDER, 22));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(42));
    params.rightMargin = dp(8);
    parent.addView(chip, params);
    chip.setOnClickListener(v -> {
      statusFilter = status;
      host.onShoppingFilterChanged(status);
      render();
    });
  }

  private int filteredCount(List<ShoppingListItem> items) {
    int count = 0;
    for (ShoppingListItem item : items)
      if (statusFilter == null || statusFilter.equals(item.status)) count++;
    return count;
  }

  private void addItemRow(LinearLayout parent, ShoppingListItem item) {
    LinearLayout card = new LinearLayout(requireContext());
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(14), dp(12), dp(12), dp(12));
    card.setBackground(roundRect(WHITE, BORDER, 16));
    LinearLayout details = vertical();
    details.addView(text(item.name, 16, TEXT, true));
    boolean available = ShoppingViewModel.AVAILABLE.equals(item.status);
    TextView amount =
        text(
            item.amount
                + " "
                + com.example.myapplication.domain.catalog.KitchenUnitCatalog.label(item.unit)
                + " · "
                + (available ? "Đã có" : "Cần mua"),
            14,
            MUTED,
            false);
    LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, -2);
    amountParams.topMargin = dp(5);
    details.addView(amount, amountParams);
    card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
    TextView check = text(available ? "✓" : "", 22, available ? WHITE : GREEN, true);
    check.setGravity(Gravity.CENTER);
    check.setBackground(roundRect(available ? GREEN : WHITE, GREEN, 10));
    check.setContentDescription(
        available ? "Đánh dấu cần mua: " + item.name : "Đánh dấu đã có: " + item.name);
    card.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
    cardParams.topMargin = dp(12);
    parent.addView(card, cardParams);
    card.setOnClickListener(v -> host.onShoppingEditItem(item.id));
    check.setOnClickListener(v -> {
      String next = available ? ShoppingViewModel.NEED_TO_BUY : ShoppingViewModel.AVAILABLE;
      viewModel.setStatus(item.id, next);
      Snackbar snackbar =
          Snackbar.make(
                  root, item.name + " · " + (available ? "Cần mua" : "Đã có"), Snackbar.LENGTH_LONG)
              .setAction("Hoàn tác", view -> viewModel.setStatus(item.id, item.status));
      if (bottomNavigation != null) snackbar.setAnchorView(bottomNavigation);
      snackbar.show();
    });
    card.setOnLongClickListener(v -> {
      confirmDelete(item);
      return true;
    });
  }

  private void confirmDelete(ShoppingListItem item) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Xóa nguyên liệu?")
        .setMessage("Xóa “" + item.name + "” khỏi danh sách mua sắm?")
        .setNegativeButton("Hủy", null)
        .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteItem(item.id))
        .show();
  }

  private void confirmClear(int count) {
    if (count <= 0) return;
    new AlertDialog.Builder(requireContext())
        .setTitle("Xóa toàn bộ danh sách?")
        .setMessage(
            "Toàn bộ "
                + count
                + " nguyên liệu sẽ bị xóa khỏi danh sách mua sắm. Không thể hoàn tác thao tác này.")
        .setNegativeButton("Giữ danh sách", null)
        .setPositiveButton(
            "Xóa toàn bộ",
            (dialog, which) -> {
              viewModel.clearItems();
              statusFilter = ShoppingViewModel.NEED_TO_BUY;
            })
        .show();
  }

  private void addBottomNavigation() {
    BottomNavigationBar navigation =
        new BottomNavigationBar(requireContext(), 2, host::onShoppingNavigateTab);
    bottomNavigation = navigation.getNavigationStrip();
    root.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
  }

  private TextView button(String value, boolean primary) {
    TextView button = text(value, 15, primary ? WHITE : GREEN, true);
    button.setGravity(Gravity.CENTER);
    button.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
    return button;
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
    TextView label = text(value, size, color, bold);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    parent.addView(label, params);
  }

  private LinearLayout vertical() {
    LinearLayout layout = new LinearLayout(requireContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  private TextView text(String value, int size, int color, boolean bold) {
    TextView view = new TextView(requireContext());
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
    return view;
  }

  private GradientDrawable roundRect(int fill, int stroke, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(fill);
    drawable.setCornerRadius(dp(radius));
    drawable.setStroke(dp(1), stroke);
    return drawable;
  }

  private int dp(float value) {
    return (int) (value * getResources().getDisplayMetrics().density + .5f);
  }
}
