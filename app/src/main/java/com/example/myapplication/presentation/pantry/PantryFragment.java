package com.example.myapplication.presentation.pantry;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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

import com.example.myapplication.core.ui.DebouncedSearchTextWatcher;
import com.example.myapplication.R;
import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.catalog.KitchenUnitCatalog;
import com.example.myapplication.domain.catalog.ShoppingCategoryCatalog;
import com.example.myapplication.domain.model.PantryIngredient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;

/** Owns pantry list and add/edit presentation. Persistence and business state stay in PantryViewModel. */
public final class PantryFragment extends Fragment {
    private static final String ARG_SCREEN = "pantry.initialScreen";
    private static final String ARG_ID = "pantry.initialId";
    private static final String ARG_NAME = "pantry.initialName";
    private static final String ARG_AMOUNT = "pantry.initialAmount";
    private static final String ARG_UNIT = "pantry.initialUnit";
    private static final String ARG_CATEGORY = "pantry.initialCategory";
    private static final String ARG_EXPIRY = "pantry.initialExpiry";
    private static final String ARG_LOCATION = "pantry.initialLocation";
    private static final String ARG_PURCHASE = "pantry.initialPurchase";
    private static final String ARG_FILTER = "pantry.initialFilter";
    private static final String ARG_QUERY = "pantry.initialQuery";
    private static final String STATE_SCREEN = "pantry.fragment.screen";
    private static final String STATE_ID = "pantry.fragment.id";
    private static final String STATE_NAME = "pantry.fragment.name";
    private static final String STATE_AMOUNT = "pantry.fragment.amount";
    private static final String STATE_UNIT = "pantry.fragment.unit";
    private static final String STATE_CATEGORY = "pantry.fragment.category";
    private static final String STATE_EXPIRY = "pantry.fragment.expiry";
    private static final String STATE_LOCATION = "pantry.fragment.location";
    private static final String STATE_PURCHASE = "pantry.fragment.purchase";
    private static final String STATE_FILTER = "pantry.fragment.filter";
    private static final String STATE_QUERY = "pantry.fragment.query";

    private static final int CREAM = Color.rgb(250, 249, 246);
    private static final int GREEN = Color.rgb(50, 108, 79);
    private static final int PALE_GREEN = Color.rgb(232, 242, 233);
    private static final int TEXT = Color.rgb(37, 53, 45);
    private static final int MUTED = Color.rgb(88, 102, 92);
    private static final int BORDER = Color.rgb(220, 228, 220);
    private static final int ERROR = Color.rgb(169, 54, 54);
    private static final int WHITE = Color.WHITE;

    public interface Host {
        void onPantryClose();
        void onPantryNavigateTab(int selectedTab);
        void onPantryScreenChanged(boolean showingForm);
    }

    private PantryViewModel viewModel;
    private Host host;
    private boolean showingForm;
    private String editingId;
    private String draftName = "";
    private String draftAmount = "";
    private String draftUnit = "";
    private String draftUnitId = "";
    private String draftCategory = ShoppingCategoryCatalog.OTHER;
    private String draftExpiry = "";
    private String draftLocation = "";
    private String draftPurchaseDate = "";
    private String filter = "ALL";
    private String query = "";
    private LinearLayout root;
    private EditText nameInput;
    private EditText amountInput;
    private EditText unitInput;
    private EditText locationInput;
    private TextView categoryInput;
    private TextView expiryInput;
    private TextView purchaseDateInput;
    private final Handler expiryHandler = new Handler(Looper.getMainLooper());
    private Runnable expiryRefresh;

    public static PantryFragment newInstance(Bundle initialState) {
        PantryFragment fragment = new PantryFragment();
        Bundle args = new Bundle();
        if (initialState != null) args.putAll(initialState);
        fragment.setArguments(args);
        return fragment;
    }

    public void handleBackPressed() {
        if (showingForm) {
            showingForm = false;
            host.onPantryScreenChanged(false);
            render();
        } else {
            host.onPantryClose();
        }
    }

    @Override public void onResume() {
        super.onResume();
        scheduleExpiryRefresh();
    }

    @Override public void onPause() {
        if (expiryRefresh != null) expiryHandler.removeCallbacks(expiryRefresh);
        expiryRefresh = null;
        super.onPause();
    }

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (!(context instanceof Host)) throw new IllegalStateException("Host Activity must implement PantryFragment.Host");
        host = (Host) context;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle state = savedInstanceState == null ? getArguments() : savedInstanceState;
        showingForm = state != null && state.getBoolean(savedInstanceState == null ? ARG_SCREEN : STATE_SCREEN, false);
        editingId = state == null ? null : state.getString(savedInstanceState == null ? ARG_ID : STATE_ID);
        draftName = value(state, savedInstanceState == null ? ARG_NAME : STATE_NAME, "");
        draftAmount = value(state, savedInstanceState == null ? ARG_AMOUNT : STATE_AMOUNT, "");
        draftUnit = value(state, savedInstanceState == null ? ARG_UNIT : STATE_UNIT, "");
        draftCategory = value(state, savedInstanceState == null ? ARG_CATEGORY : STATE_CATEGORY, ShoppingCategoryCatalog.OTHER);
        draftExpiry = value(state, savedInstanceState == null ? ARG_EXPIRY : STATE_EXPIRY, "");
        draftLocation = value(state, savedInstanceState == null ? ARG_LOCATION : STATE_LOCATION, "");
        draftPurchaseDate = value(state, savedInstanceState == null ? ARG_PURCHASE : STATE_PURCHASE, "");
        filter = value(state, savedInstanceState == null ? ARG_FILTER : STATE_FILTER, "ALL");
        query = value(state, savedInstanceState == null ? ARG_QUERY : STATE_QUERY, "");
    }

    private static String value(Bundle state, String key, String fallback) {
        return state == null || state.getString(key) == null ? fallback : state.getString(key);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        captureDraft();
        outState.putBoolean(STATE_SCREEN, showingForm);
        outState.putString(STATE_ID, editingId);
        outState.putString(STATE_NAME, draftName);
        outState.putString(STATE_AMOUNT, draftAmount);
        outState.putString(STATE_UNIT, draftUnit);
        outState.putString(STATE_CATEGORY, draftCategory);
        outState.putString(STATE_EXPIRY, draftExpiry);
        outState.putString(STATE_LOCATION, draftLocation);
        outState.putString(STATE_PURCHASE, draftPurchaseDate);
        outState.putString(STATE_FILTER, filter);
        outState.putString(STATE_QUERY, query);
        super.onSaveInstanceState(outState);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                  @Nullable Bundle savedInstanceState) {
        AppViewModelFactory factory = new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(PantryViewModel.class);
        root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(system.left, system.top, system.right, Math.max(system.bottom, ime.bottom));
            return insets;
        });
        viewModel.pantryState().observe(getViewLifecycleOwner(), state -> {
            if (!showingForm && state != null && !state.loading) render();
        });
        viewModel.mutationResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            viewModel.clearMutationResult();
            if (!result.success) {
                Toast.makeText(requireContext(), result.error == null ? "Không thể cập nhật tủ nguyên liệu." : result.error,
                        Toast.LENGTH_SHORT).show();
            } else if (PantryViewModel.MutationResult.SAVE.equals(result.operation)) {
                showingForm = false;
                editingId = null;
                host.onPantryScreenChanged(false);
                Toast.makeText(requireContext(), "Đã lưu nguyên liệu.", Toast.LENGTH_SHORT).show();
                render();
            }
        });
        render();
        return root;
    }

    private void render() {
        if (root == null || !isAdded()) return;
        root.removeAllViews();
        nameInput = null; amountInput = null; unitInput = null; locationInput = null;
        if (showingForm) renderForm(); else renderList();
    }

    private void scheduleExpiryRefresh() {
        if (expiryRefresh != null) expiryHandler.removeCallbacks(expiryRefresh);
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_YEAR, 1);
        nextDay.set(Calendar.HOUR_OF_DAY, 0); nextDay.set(Calendar.MINUTE, 0);
        nextDay.set(Calendar.SECOND, 0); nextDay.set(Calendar.MILLISECOND, 0);
        long delay = Math.max(1000L, nextDay.getTimeInMillis() - System.currentTimeMillis());
        expiryRefresh = () -> { expiryRefresh = null; if (isAdded() && !showingForm) render(); };
        expiryHandler.postDelayed(expiryRefresh, delay);
    }

    private void renderList() {
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(8), dp(24), dp(12));
        addBackTitle(page, "Tủ nguyên liệu");
        List<PantryIngredient> items = viewModel.getItems();
        int usable = 0, expiring = 0, expired = 0;
        for (PantryIngredient item : items) {
            String state = viewModel.expiryState(item.expiryDate);
            if ("EXPIRED".equals(state)) expired++; else if ("EXPIRING".equals(state)) expiring++; else usable++;
        }
        addText(page, usable + " sử dụng được · " + expiring + " sắp hết hạn · " + expired + " hết hạn",
                14, MUTED, false, 0, 8, 0, 12);
        EditText search = editField(query, 15, 52);
        search.setSingleLine(true);
        search.setHint("Tìm nguyên liệu trong tủ");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(48), dp(8), dp(12), dp(8));
        FrameLayout searchBox = new FrameLayout(requireContext());
        searchBox.setBackground(roundRect(WHITE, BORDER, 16));
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_search_outline);
        icon.setColorFilter(GREEN);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchBox.addView(icon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
        searchBox.addView(search, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.bottomMargin = dp(12);
        page.addView(searchBox, searchParams);
        LinearLayout chips = new LinearLayout(requireContext());
        chips.setGravity(Gravity.CENTER_VERTICAL);
        addFilterChip(chips, "Tất cả", "ALL");
        addFilterChip(chips, "Sử dụng được", "USABLE");
        addFilterChip(chips, "Sắp hết hạn", "EXPIRING");
        addFilterChip(chips, "Hết hạn", "EXPIRED");
        HorizontalScrollView chipScroll = new HorizontalScrollView(requireContext());
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(chips, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(chipScroll, new LinearLayout.LayoutParams(-1, dp(46)));
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        LinearLayout rows = vertical();
        rows.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(rows);
        populateRows(rows, items);
        search.addTextChangedListener(new DebouncedSearchTextWatcher(search, value -> {
            query = value;
            populateRows(rows, viewModel.getItems());
        }));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView add = text("+   Thêm nguyên liệu", 14, WHITE, true);
        add.setGravity(Gravity.CENTER); add.setBackground(roundRect(GREEN, GREEN, 14));
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(48), 1));
        add.setOnClickListener(v -> openForm(null));
        TextView clear = text("Xóa tất cả", 14, ERROR, true);
        clear.setGravity(Gravity.CENTER); clear.setPadding(dp(10), 0, dp(10), 0);
        clear.setBackground(roundRect(WHITE, BORDER, 14));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-2, dp(48));
        clearParams.leftMargin = dp(10); actions.addView(clear, clearParams);
        clear.setEnabled(!items.isEmpty()); clear.setAlpha(items.isEmpty() ? .5f : 1f);
        clear.setOnClickListener(v -> confirmClear(items.size()));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionParams.topMargin = dp(8); page.addView(actions, actionParams);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottomNavigation();
    }

    private void populateRows(LinearLayout rows, List<PantryIngredient> items) {
        rows.removeAllViews();
        List<PantryIngredient> matches = new ArrayList<>();
        String normalizedQuery = normalize(query);
        for (PantryIngredient item : items) {
            String state = viewModel.expiryState(item.expiryDate);
            if ("USABLE".equals(filter) && !state.isEmpty()) continue;
            if ("EXPIRING".equals(filter) && !"EXPIRING".equals(state)) continue;
            if ("EXPIRED".equals(filter) && !"EXPIRED".equals(state)) continue;
            if (!normalizedQuery.isEmpty() && !normalize(item.name + " " + item.category + " " + item.storageLocation).contains(normalizedQuery)) continue;
            matches.add(item);
        }
        if (matches.isEmpty()) {
            PantryViewModel.PantryState state = viewModel.pantryState().getValue();
            if (items.isEmpty() && state != null && state.loading) {
                android.widget.ProgressBar progress = new android.widget.ProgressBar(requireContext());
                LinearLayout loading = vertical(); loading.setGravity(Gravity.CENTER);
                loading.addView(progress, new LinearLayout.LayoutParams(dp(32), dp(32)));
                TextView label = text("Đang tải tủ nguyên liệu…", 14, MUTED, false);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
                labelParams.topMargin = dp(10); loading.addView(label, labelParams);
                rows.addView(loading, new LinearLayout.LayoutParams(-1, dp(160))); return;
            }
            boolean empty = items.isEmpty();
            emptyCard(rows, empty ? "Tủ nguyên liệu đang trống" : "Không tìm thấy nguyên liệu",
                    empty ? "Thêm những nguyên liệu bạn đang có để quản lý hạn sử dụng." : "Thử từ khóa khác hoặc chọn bộ lọc Tất cả.",
                    empty ? () -> openForm(null) : () -> { filter = "ALL"; query = ""; render(); });
            return;
        }
        Map<String, List<PantryIngredient>> groups = new LinkedHashMap<>();
        for (String category : ShoppingCategoryCatalog.ORDER) groups.put(category, new ArrayList<>());
        for (PantryIngredient item : matches) {
            String category = ShoppingCategoryCatalog.contains(item.category) ? item.category : ShoppingCategoryCatalog.OTHER;
            groups.get(category).add(item);
        }
        int index = 0;
        for (Map.Entry<String, List<PantryIngredient>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            addText(rows, group.getKey(), 18, TEXT, true, 0, index++ == 0 ? 4 : 20, 0, 0);
            for (PantryIngredient item : group.getValue()) addItemCard(rows, item);
        }
    }

    private void addFilterChip(LinearLayout parent, String label, String key) {
        boolean selected = key.equals(filter);
        TextView chip = text(label, 13, selected ? GREEN : MUTED, true);
        chip.setGravity(Gravity.CENTER); chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(roundRect(selected ? PALE_GREEN : WHITE, selected ? PALE_GREEN : BORDER, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38));
        params.rightMargin = dp(8); parent.addView(chip, params);
        chip.setOnClickListener(v -> { filter = key; render(); });
    }

    private void addItemCard(LinearLayout parent, PantryIngredient item) {
        LinearLayout card = vertical(); card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(WHITE, BORDER, 16));
        LinearLayout top = new LinearLayout(requireContext()); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(item.name, 16, TEXT, true); top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView edit = text("Sửa", 13, GREEN, true); edit.setGravity(Gravity.CENTER); edit.setPadding(dp(10), 0, dp(10), 0);
        top.addView(edit, new LinearLayout.LayoutParams(-2, dp(40))); card.addView(top);
        String quantity = item.amountDecimal == null ? "Chưa biết lượng" : item.amountDecimal +
                (item.unitId == null || item.unitId.isEmpty() ? "" : " " + KitchenUnitCatalog.label(item.unitId));
        String availability = viewModel.availabilityLabel(item.expiryDate);
        TextView detail = text(quantity + " · " + availability + " · " + item.category, 13,
                "Hết hạn".equals(availability) ? ERROR : "Sắp hết hạn".equals(availability) ? Color.rgb(180, 112, 30) : MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2); detailParams.topMargin = dp(4); card.addView(detail, detailParams);
        String expiryState = viewModel.expiryState(item.expiryDate);
        if (item.expiryDate != null || !item.storageLocation.isEmpty()) {
            LinearLayout meta = new LinearLayout(requireContext()); meta.setGravity(Gravity.CENTER_VERTICAL);
            if (item.expiryDate != null) {
                int color = "EXPIRED".equals(expiryState) ? ERROR : "EXPIRING".equals(expiryState) ? Color.rgb(180,112,30) : MUTED;
                String label = "EXPIRED".equals(expiryState) ? "Hạn đã qua · " : "EXPIRING".equals(expiryState) ? "Hạn sắp đến · " : "Dùng trước · ";
                meta.addView(text(label + displayDate(item.expiryDate), 13, color, true), new LinearLayout.LayoutParams(0, -2, 1));
            }
            if (!item.storageLocation.isEmpty()) meta.addView(text(item.storageLocation, 12, MUTED, false));
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2); metaParams.topMargin = dp(5); card.addView(meta, metaParams);
        }
        if (item.purchaseDate != null) {
            TextView purchased = text("Mua ngày " + displayDate(item.purchaseDate), 12, MUTED, false);
            LinearLayout.LayoutParams purchasedParams = new LinearLayout.LayoutParams(-1, -2); purchasedParams.topMargin = dp(4); card.addView(purchased, purchasedParams);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(9); parent.addView(card, params);
        card.setOnClickListener(v -> openForm(item.id)); edit.setOnClickListener(v -> openForm(item.id));
        card.setOnLongClickListener(v -> { confirmDelete(item); return true; });
    }

    private void openForm(String id) {
        query = "";
        editingId = id;
        PantryIngredient item = viewModel.getItem(id);
        draftName = item == null ? "" : item.name;
        draftAmount = item == null || item.amountDecimal == null ? "" : item.amountDecimal;
        draftUnitId = item == null || item.unitId == null ? "" : item.unitId;
        draftUnit = draftUnitId.isEmpty() ? "" : KitchenUnitCatalog.label(draftUnitId);
        draftCategory = item == null ? ShoppingCategoryCatalog.OTHER : item.category;
        draftExpiry = item == null || item.expiryDate == null ? "" : item.expiryDate;
        draftLocation = item == null ? "" : item.storageLocation;
        draftPurchaseDate = item == null || item.purchaseDate == null ? "" : item.purchaseDate;
        showingForm = true; host.onPantryScreenChanged(true); render();
    }

    private void renderForm() {
        LinearLayout page = vertical(); page.setPadding(dp(24), dp(8), dp(24), dp(16));
        addBackTitle(page, editingId == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu");
        ScrollView scroll = new ScrollView(requireContext()); LinearLayout fields = vertical();
        fields.setPadding(0, dp(18), 0, dp(20)); scroll.addView(fields);
        addText(fields, "Tên nguyên liệu", 14, TEXT, true, 0, 0, 0, 0);
        nameInput = editField(draftName, 16, 52); nameInput.setSingleLine(true); nameInput.setHint("Ví dụ: Cà chua"); nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT); addField(fields, nameInput, 8, 20);
        addText(fields, "Số lượng (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        amountInput = editField(draftAmount, 16, 52); amountInput.setSingleLine(true); amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); amountInput.setHint("Để trống nếu chưa biết lượng"); addField(fields, amountInput, 8, 20);
        addText(fields, "Đơn vị", 14, TEXT, true, 0, 0, 0, 0);
        unitInput = editField(draftUnit, 16, 52); unitInput.setSingleLine(true); unitInput.setHint("Ví dụ: quả, g, ml, muỗng cà phê"); addField(fields, unitInput, 8, 20);
        addText(fields, "Danh mục", 14, TEXT, true, 0, 0, 0, 0);
        categoryInput = selectionField(draftCategory + "     ▾", 15, 52); addField(fields, categoryInput, 8, 20); categoryInput.setOnClickListener(v -> showCategoryPicker());
        addText(fields, "Hạn sử dụng (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        LinearLayout expiryRow = new LinearLayout(requireContext()); expiryRow.setGravity(Gravity.CENTER_VERTICAL);
        expiryInput = dateField(draftExpiry); expiryRow.addView(expiryInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView clearExpiry = text("Xóa", 13, GREEN, true); clearExpiry.setGravity(Gravity.CENTER); expiryRow.addView(clearExpiry, new LinearLayout.LayoutParams(dp(48), dp(48))); addField(fields, expiryRow, 8, 20);
        expiryInput.setOnClickListener(v -> showDatePicker(true)); clearExpiry.setOnClickListener(v -> { draftExpiry = ""; expiryInput.setText("Không đặt"); expiryInput.setTextColor(MUTED); });
        addText(fields, "Ngày mua (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        LinearLayout purchaseRow = new LinearLayout(requireContext()); purchaseRow.setGravity(Gravity.CENTER_VERTICAL);
        purchaseDateInput = dateField(draftPurchaseDate); purchaseRow.addView(purchaseDateInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView clearPurchase = text("Xóa", 13, GREEN, true); clearPurchase.setGravity(Gravity.CENTER); purchaseRow.addView(clearPurchase, new LinearLayout.LayoutParams(dp(48), dp(48))); addField(fields, purchaseRow, 8, 20);
        purchaseDateInput.setOnClickListener(v -> showDatePicker(false)); clearPurchase.setOnClickListener(v -> { draftPurchaseDate = ""; purchaseDateInput.setText("Không đặt"); purchaseDateInput.setTextColor(MUTED); });
        addText(fields, "Nơi cất (không bắt buộc)", 14, TEXT, true, 0, 0, 0, 0);
        locationInput = editField(draftLocation, 16, 52); locationInput.setSingleLine(true); locationInput.setHint("Ví dụ: Ngăn mát"); addField(fields, locationInput, 8, 14);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addButton(page, "Lưu nguyên liệu", this::saveForm, 8);
        if (editingId != null) { TextView remove = text("Xóa nguyên liệu", 14, ERROR, true); remove.setGravity(Gravity.CENTER); page.addView(remove, new LinearLayout.LayoutParams(-1, dp(44))); remove.setOnClickListener(v -> confirmDelete(viewModel.getItem(editingId))); }
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private TextView dateField(String date) {
        TextView value = text(date.isEmpty() ? "Không đặt" : displayDate(date), 15, date.isEmpty() ? MUTED : TEXT, false);
        value.setPadding(dp(15), 0, dp(15), 0); value.setBackground(roundRect(WHITE, BORDER, 14)); return value;
    }

    private void showCategoryPicker() {
        String[] categories = ShoppingCategoryCatalog.ORDER.toArray(new String[0]); int selected = ShoppingCategoryCatalog.ORDER.indexOf(draftCategory);
        new AlertDialog.Builder(requireContext()).setTitle("Chọn danh mục").setSingleChoiceItems(categories, Math.max(0, selected), (dialog, which) -> {
            draftCategory = categories[which]; categoryInput.setText(draftCategory + "     ▾"); dialog.dismiss();
        }).setNegativeButton("Hủy", null).show();
    }

    private void showDatePicker(boolean expiry) {
        String current = expiry ? draftExpiry : draftPurchaseDate;
        Calendar selected = Calendar.getInstance();
        if (!current.isEmpty()) try { Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(current); if (date != null) selected.setTime(date); }
        catch (java.text.ParseException ignored) { }
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar date = Calendar.getInstance(); date.clear(); date.set(year, month, day);
            String value = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(date.getTime());
            if (expiry) { draftExpiry = value; expiryInput.setText(displayDate(value)); expiryInput.setTextColor(TEXT); }
            else { draftPurchaseDate = value; purchaseDateInput.setText(displayDate(value)); purchaseDateInput.setTextColor(TEXT); }
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void captureDraft() {
        if (nameInput != null) draftName = nameInput.getText().toString();
        if (amountInput != null) draftAmount = amountInput.getText().toString();
        if (unitInput != null) draftUnit = unitInput.getText().toString();
        if (locationInput != null) draftLocation = locationInput.getText().toString();
        draftUnitId = draftAmount.trim().isEmpty() ? "" : KitchenUnitCatalog.canonicalId(draftUnit);
    }

    private void saveForm() {
        captureDraft();
        if (!draftAmount.trim().isEmpty() && draftUnitId.isEmpty()) { Toast.makeText(requireContext(), "Nhập đơn vị cho số lượng.", Toast.LENGTH_SHORT).show(); return; }
        if (!viewModel.saveItem(editingId, draftName, draftAmount, draftUnitId, draftCategory, draftExpiry, draftLocation, draftPurchaseDate))
            Toast.makeText(requireContext(), "Vui lòng kiểm tra tên, số lượng và đơn vị.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(PantryIngredient item) {
        if (item == null) return;
        new AlertDialog.Builder(requireContext()).setTitle("Xóa nguyên liệu khỏi tủ?")
                .setMessage("Xóa “" + item.name + "” khỏi tủ nguyên liệu?")
                .setNegativeButton("Hủy", null).setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteItem(item.id);
                    if (showingForm) { showingForm = false; host.onPantryScreenChanged(false); render(); }
                }).show();
    }

    private void confirmClear(int count) {
        if (count <= 0) return;
        new AlertDialog.Builder(requireContext()).setTitle("Xóa tất cả nguyên liệu trong tủ?")
                .setMessage("Xóa toàn bộ " + count + " nguyên liệu đã lưu trong tủ? Thao tác này không thể hoàn tác.")
                .setNegativeButton("Hủy", null).setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    viewModel.deleteAllItems(); filter = "ALL"; query = "";
                }).show();
    }

    private void addBackTitle(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(requireContext()); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(requireContext()); back.setImageResource(R.drawable.ic_arrow_back_outline); back.setColorFilter(GREEN); back.setPadding(dp(12), dp(12), dp(12), dp(12)); back.setBackground(roundRect(WHITE, WHITE, 15)); back.setContentDescription("Quay lại");
        row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48))); back.setOnClickListener(v -> handleBackPressed());
        TextView heading = text(title, 23, TEXT, true); heading.setPadding(dp(12), 0, 0, 0); row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
    }

    private void addBottomNavigation() {
        LinearLayout nav = new LinearLayout(requireContext()); nav.setGravity(Gravity.CENTER_VERTICAL); nav.setBackgroundColor(WHITE); nav.setPadding(dp(7), dp(5), dp(7), dp(2));
        View line = new View(requireContext()); line.setBackgroundColor(BORDER); root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        String[] labels = {"Khám phá", "Nấu hôm nay", "Mua sắm", "Kho của tôi"};
        int[] icons = {R.drawable.ic_home_outline, R.drawable.ic_calendar_outline, R.drawable.ic_cart_outline, R.drawable.ic_star_outline};
        for (int i = 0; i < labels.length; i++) {
            final int tabIndex = i; LinearLayout tab = vertical(); tab.setGravity(Gravity.CENTER);
            ImageView icon = new ImageView(requireContext()); icon.setImageResource(icons[i]); icon.setColorFilter(i == 2 ? GREEN : MUTED); if (i == 2) icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
            tab.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(30)));
            TextView label = text(labels[i], 10, i == 2 ? GREEN : MUTED, i == 2); label.setGravity(Gravity.CENTER); tab.addView(label);
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(58), 1));
            tab.setOnClickListener(v -> {
                query = "";
                host.onPantryNavigateTab(tabIndex);
            });
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(64)));
    }

    private void emptyCard(LinearLayout parent, String title, String subtitle, Runnable action) {
        LinearLayout card = vertical(); card.setPadding(dp(16), dp(18), dp(16), dp(18)); card.setBackground(roundRect(WHITE, BORDER, 16));
        addText(card, title, 16, TEXT, true, 0, 0, 0, 4); addText(card, subtitle, 14, MUTED, false, 0, 0, 0, 0);
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2)); card.setOnClickListener(v -> action.run());
    }

    private void addButton(LinearLayout parent, String label, Runnable action, int top) {
        TextView button = text(label, 15, WHITE, true); button.setGravity(Gravity.CENTER); button.setBackground(roundRect(GREEN, GREEN, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52)); params.topMargin = dp(top); parent.addView(button, params); button.setOnClickListener(v -> action.run());
    }

    private void addField(LinearLayout parent, View view, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52)); params.topMargin = dp(top); params.bottomMargin = dp(bottom); parent.addView(view, params);
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold, int left, int top, int right, int bottom) {
        TextView label = text(value, size, color, bold); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(dp(left), dp(top), dp(right), dp(bottom)); parent.addView(label, params);
    }

    private EditText editField(String value, int size, int height) {
        EditText edit = new EditText(requireContext()); edit.setText(value); edit.setTextSize(size); edit.setTextColor(TEXT); edit.setHintTextColor(MUTED); edit.setSingleLine(true);
        edit.setPadding(dp(14), 0, dp(14), 0); edit.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); edit.setBackground(roundRect(WHITE, BORDER, 14)); edit.setHeight(dp(height)); edit.setSelectAllOnFocus(false); return edit;
    }

    private TextView selectionField(String value, int size, int height) {
        TextView text = text(value, size, TEXT, false); text.setGravity(Gravity.CENTER_VERTICAL); text.setPadding(dp(14), 0, dp(14), 0); text.setBackground(roundRect(WHITE, BORDER, 14)); text.setHeight(dp(height)); return text;
    }

    private LinearLayout vertical() { LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private TextView text(String value, int size, int color, boolean bold) { TextView view = new TextView(requireContext()); view.setText(value); view.setTextSize(size); view.setTextColor(color); if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD); return view; }
    private GradientDrawable roundRect(int fill, int stroke, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(fill); drawable.setCornerRadius(dp(radius)); drawable.setStroke(dp(1), stroke); return drawable; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
    private String displayDate(String value) { try { Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(value); return date == null ? value : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date); } catch (java.text.ParseException ignored) { return value; } }
    private String normalize(String value) { if (value == null) return ""; return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim(); }
}
