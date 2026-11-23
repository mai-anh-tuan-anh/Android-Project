package com.example.myapplication.presentation.cookingjournal;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.model.CookingLogEntry;
import com.example.myapplication.domain.model.Recipe;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Owns the cooking journal list, per-recipe history, and cooking-log form. */
public final class CookingJournalFragment extends Fragment {
  private static final String ARG_SCREEN = "journal.screen";
  private static final String ARG_RECIPE_ID = "journal.recipeId";
  private static final String STATE_SCREEN = "journal.fragment.screen";
  private static final String STATE_RECIPE_ID = "journal.fragment.recipeId";
  private static final String STATE_LOG_ID = "journal.fragment.logId";
  private static final String STATE_FORM_RETURN = "journal.fragment.formReturn";
  private static final String STATE_COOKED_AT = "journal.fragment.cookedAt";
  private static final String STATE_SERVINGS = "journal.fragment.servings";
  private static final String STATE_DURATION = "journal.fragment.duration";
  private static final String STATE_RATING = "journal.fragment.rating";
  private static final String STATE_ADJUSTMENT = "journal.fragment.adjustment";
  private static final String STATE_PHOTO = "journal.fragment.photo";
  private static final String STATE_ORIGINAL_PHOTO = "journal.fragment.originalPhoto";
  private static final String STATE_SEARCH = "journal.fragment.search";
  private static final String STATE_FROM_DATE = "journal.fragment.fromDate";
  private static final String STATE_TO_DATE = "journal.fragment.toDate";
  private static final String STATE_FILTER_RATING = "journal.fragment.filterRating";
  private static final String JOURNAL = "COOKING_JOURNAL",
      LOGS = "COOKING_LOGS",
      FORM = "COOKING_LOG_FORM";

  private static final int GREEN = Color.rgb(50, 108, 79);
  private static final int PALE_GREEN = Color.rgb(232, 242, 233);
  private static final int TEXT = Color.rgb(37, 53, 45);
  private static final int MUTED = Color.rgb(88, 102, 92);
  private static final int BORDER = Color.rgb(220, 228, 220);
  private static final int ERROR = Color.rgb(169, 54, 54);
  private static final int WHITE = Color.WHITE;

  public interface Host {
    Recipe onJournalResolveRecipe(String recipeId);

    void onJournalBack(String currentScreen);

    void onJournalOpenRecipeNote(String recipeId);

    void onJournalNavigateTab(int tab);

    void onJournalScreenChanged(String screen, String recipeId);
  }

  private CookingJournalViewModel viewModel;
  private Host host;
  private ActivityResultLauncher<String> photoPicker;
  private LinearLayout root;
  private LinearLayout journalRows;
  private String screen = JOURNAL;
  private String recipeId;
  private String editingLogId;
  private String formReturnScreen = LOGS;
  private long cookedAt = System.currentTimeMillis();
  private int servings = 1;
  private String duration = "";
  private Integer rating;
  private String adjustment = "";
  private String photoPath = "";
  private String originalPhotoPath = "";
  private boolean formInitialized;
  private String search = "";
  private long fromDate;
  private long toDate;
  private int ratingFilter;
  private EditText servingsInput;
  private EditText durationInput;
  private EditText adjustmentInput;
  private ImageView photoPreviewView;
  private List<CookingLogEntry> allLogs = new ArrayList<>();

  public static CookingJournalFragment newInstance(String screen, String recipeId) {
    CookingJournalFragment fragment = new CookingJournalFragment();
    Bundle args = new Bundle();
    args.putString(ARG_SCREEN, screen);
    args.putString(ARG_RECIPE_ID, recipeId);
    fragment.setArguments(args);
    return fragment;
  }

  public void showScreen(String screen, String recipeId) {
    boolean recipeChanged = recipeId != null && !recipeId.equals(this.recipeId);
    if (recipeChanged) {
      this.recipeId = recipeId;
      search = "";
      fromDate = 0;
      toDate = 0;
      ratingFilter = 0;
    }
    boolean changed = !screen.equals(this.screen) || recipeChanged;
    this.screen = screen;
    if (root != null) render();
    if (changed && viewModel != null) viewModel.refresh();
  }

  public boolean isFormShowing() {
    return FORM.equals(screen);
  }

  public void openLogForm(String logId, String recipeId) {
    if (formInitialized
        && FORM.equals(screen)
        && java.util.Objects.equals(editingLogId, logId)
        && java.util.Objects.equals(this.recipeId, recipeId)) return;
    openForm(logId, recipeId);
  }

  public void handleBackPressed() {
    if (FORM.equals(screen)) {
      discardDraftPhoto();
      screen = formReturnScreen;
      if (JOURNAL.equals(screen) || LOGS.equals(screen)) render();
      notifyScreenChanged();
    } else if (LOGS.equals(screen)) {
      host.onJournalOpenRecipeNote(recipeId);
    } else {
      clearDateRangeForExit();
      host.onJournalBack(screen);
    }
  }

  @Override
  public void onAttach(@NonNull android.content.Context context) {
    super.onAttach(context);
    if (!(context instanceof Host))
      throw new IllegalStateException("Activity must implement CookingJournalFragment.Host");
    host = (Host) context;
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication()))
            .get(CookingJournalViewModel.class);
    if (state != null) {
      screen = state.getString(STATE_SCREEN, JOURNAL);
      recipeId = state.getString(STATE_RECIPE_ID);
      editingLogId = state.getString(STATE_LOG_ID);
      formReturnScreen = state.getString(STATE_FORM_RETURN, LOGS);
      cookedAt = state.getLong(STATE_COOKED_AT, System.currentTimeMillis());
      servings = Math.max(1, Math.min(999, state.getInt(STATE_SERVINGS, 1)));
      duration = state.getString(STATE_DURATION, "");
      int savedRating = state.getInt(STATE_RATING, 0);
      rating = savedRating >= 1 && savedRating <= 5 ? savedRating : null;
      adjustment = state.getString(STATE_ADJUSTMENT, "");
      photoPath = state.getString(STATE_PHOTO, "");
      originalPhotoPath = state.getString(STATE_ORIGINAL_PHOTO, "");
      formInitialized = state.getBoolean("journal.fragment.formInitialized", false);
      search = state.getString(STATE_SEARCH, "");
      fromDate = state.getLong(STATE_FROM_DATE, 0);
      toDate = state.getLong(STATE_TO_DATE, 0);
      ratingFilter = state.getInt(STATE_FILTER_RATING, 0);
    } else if (getArguments() != null) {
      screen = getArguments().getString(ARG_SCREEN, JOURNAL);
      recipeId = getArguments().getString(ARG_RECIPE_ID);
    }
    photoPicker =
        registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
              if (uri != null && FORM.equals(screen)) viewModel.savePhotoAsync(uri.toString());
            });
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    captureDraft();
    out.putString(STATE_SCREEN, screen);
    out.putString(STATE_RECIPE_ID, recipeId);
    out.putString(STATE_LOG_ID, editingLogId);
    out.putString(STATE_FORM_RETURN, formReturnScreen);
    out.putLong(STATE_COOKED_AT, cookedAt);
    out.putInt(STATE_SERVINGS, servings);
    out.putString(STATE_DURATION, duration);
    out.putInt(STATE_RATING, rating == null ? 0 : rating);
    out.putString(STATE_ADJUSTMENT, adjustment);
    out.putString(STATE_PHOTO, photoPath);
    out.putString(STATE_ORIGINAL_PHOTO, originalPhotoPath);
    out.putBoolean("journal.fragment.formInitialized", FORM.equals(screen));
    out.putString(STATE_SEARCH, search);
    out.putLong(STATE_FROM_DATE, fromDate);
    out.putLong(STATE_TO_DATE, toDate);
    out.putInt(STATE_FILTER_RATING, ratingFilter);
    super.onSaveInstanceState(out);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
    root = vertical();
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          view.setPadding(
              system.left, system.top, system.right, Math.max(system.bottom, ime.bottom));
          return insets;
        });
    ViewCompat.requestApplyInsets(root);
    viewModel
        .journalState()
        .observe(
            getViewLifecycleOwner(),
            value -> {
              if (value == null) return;
              allLogs = value.logs;
              if (!value.loading) render();
            });
    viewModel
        .mutationResult()
        .observe(
            getViewLifecycleOwner(),
            result -> {
              if (result == null) return;
              viewModel.clearMutationResult();
              onMutationResult(result);
            });
    render();
    viewModel.refresh();
    return root;
  }

  private void onMutationResult(CookingJournalViewModel.MutationResult result) {
    if (CookingJournalViewModel.MutationResult.PHOTO_PREVIEW.equals(result.operation)) {
      if (FORM.equals(screen)
          && photoPath.equals(result.filename)
          && photoPreviewView != null) {
        photoPreviewView.setImageBitmap(viewModel.getPhotoPreviewBitmap(result.filename));
      }
      return;
    }
    if (CookingJournalViewModel.MutationResult.PHOTO.equals(result.operation)) {
      if (result.success && FORM.equals(screen)) {
        if (!originalPhotoPath.equals(photoPath)) viewModel.deletePhoto(photoPath);
        photoPath = result.filename;
        render();
      } else if (result.success) viewModel.deletePhoto(result.filename);
      else toast(result.message == null ? "Không thể lưu ảnh đã chọn." : result.message);
      return;
    }
    if (CookingJournalViewModel.MutationResult.SAVE.equals(result.operation)
        && result.success
        && FORM.equals(screen)) {
      if (!originalPhotoPath.isEmpty() && !originalPhotoPath.equals(photoPath))
        viewModel.deletePhoto(originalPhotoPath);
      originalPhotoPath = photoPath;
      editingLogId = null;
      formInitialized = false;
      screen = formReturnScreen;
      toast("Đã lưu lần nấu.");
      if (JOURNAL.equals(screen) || LOGS.equals(screen)) render();
      notifyScreenChanged();
    } else if (!result.success)
      toast(result.message == null ? "Không thể cập nhật nhật ký nấu." : result.message);
  }

  private void render() {
    if (root == null || !isAdded()) return;
    root.removeAllViews();
    if (JOURNAL.equals(screen)) renderJournal();
    else if (LOGS.equals(screen)) renderRecipeLogs();
    else renderForm();
  }

  private void renderJournal() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(12));
    addBackTitle(page, "Nhật ký nấu của tôi");
    addText(page, "Tìm lại những lần bạn đã nấu.", 14, MUTED, false, 0, 0, 0, 14);
    addStats(page, viewModel.getStats());
    FrameLayout searchBox = searchBox();
    LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
    searchParams.topMargin = dp(16);
    searchParams.bottomMargin = dp(10);
    page.addView(searchBox, searchParams);
    LinearLayout dateFilters = new LinearLayout(requireContext());
    dateFilters.setGravity(Gravity.CENTER_VERTICAL);
    dateFilters.addView(dateFilter(true), new LinearLayout.LayoutParams(0, dp(54), 1));
    dateFilters.addView(new View(requireContext()), new LinearLayout.LayoutParams(dp(8), 1));
    dateFilters.addView(dateFilter(false), new LinearLayout.LayoutParams(0, dp(54), 1));
    page.addView(dateFilters);
    LinearLayout chips = new LinearLayout(requireContext());
    addRatingChip(chips, "Tất cả", 0);
    for (int value = 5; value >= 1; value--) addRatingChip(chips, value + " ★", value);
    HorizontalScrollViewCompat horizontal = new HorizontalScrollViewCompat(requireContext());
    horizontal.setHorizontalScrollBarEnabled(false);
    horizontal.addView(chips);
    LinearLayout.LayoutParams chipsParams = new LinearLayout.LayoutParams(-1, dp(46));
    chipsParams.topMargin = dp(8);
    page.addView(horizontal, chipsParams);
    if (hasFilters()) {
      TextView clear = text("Xóa bộ lọc", 13, GREEN, true);
      clear.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
      page.addView(clear, new LinearLayout.LayoutParams(-1, dp(36)));
      clear.setOnClickListener(v -> {
        search = "";
        fromDate = 0;
        toDate = 0;
        ratingFilter = 0;
        render();
      });
    }
    ScrollView scroll = new ScrollView(requireContext());
    journalRows = vertical();
    journalRows.setPadding(0, dp(8), 0, dp(12));
    scroll.addView(journalRows);
    populateJournalRows(journalRows);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
    addBottomNavigation(root, 3);
  }

  private void populateJournalRows(LinearLayout rows) {
    long toExclusive = 0;
    if (toDate > 0) {
      Calendar next = Calendar.getInstance();
      next.setTimeInMillis(toDate);
      next.add(Calendar.DAY_OF_YEAR, 1);
      toExclusive = next.getTimeInMillis();
    }
    if (fromDate > 0 && toExclusive > 0 && fromDate >= toExclusive) {
      addEmpty(
          rows,
          "Khoảng ngày chưa hợp lệ",
          "Ngày bắt đầu phải trước hoặc cùng ngày kết thúc.",
          () -> {
            fromDate = 0;
            toDate = 0;
            render();
          });
      return;
    }
    List<CookingLogEntry> filtered =
        viewModel.filterLogs(allLogs, search, fromDate, toExclusive, ratingFilter);
    addText(rows, filtered.size() + " lần nấu", 13, MUTED, false, 0, 4, 0, 8);
    if (filtered.isEmpty()) {
      if (allLogs.isEmpty())
        addEmpty(
            rows,
            "Chưa có lần nấu nào",
            "Mở ghi chú của một món để ghi lại lần nấu đầu tiên.",
            () -> host.onJournalNavigateTab(0));
      else
        addEmpty(
            rows,
            "Không tìm thấy lần nấu phù hợp",
            "Thử đổi từ khóa hoặc điều kiện lọc.",
            () -> {
              search = "";
              fromDate = 0;
              toDate = 0;
              ratingFilter = 0;
              render();
            });
      return;
    }
    for (CookingLogEntry log : filtered) addLogCard(rows, log, true);
  }

  private void renderRecipeLogs() {
    Recipe recipe = resolveRecipe(recipeId);
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(12));
    addBackTitle(page, "Nhật ký nấu");
    LinearLayout recipeCard = new LinearLayout(requireContext());
    recipeCard.setGravity(Gravity.CENTER_VERTICAL);
    recipeCard.setPadding(dp(8), dp(7), dp(12), dp(7));
    recipeCard.setBackground(roundRect(WHITE, BORDER, 14));
    recipeCard.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(58), dp(52)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text(recipe.title, 14, TEXT, true));
    labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
    recipeCard.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
    cardParams.topMargin = dp(18);
    page.addView(recipeCard, cardParams);
    List<CookingLogEntry> logs = viewModel.getLogs(recipe.id);
    addText(page, "Đã nấu " + logs.size() + " lần", 14, MUTED, false, 0, 12, 0, 10);
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout content = vertical();
    content.setPadding(0, 0, 0, dp(12));
    scroll.addView(content);
    if (logs.isEmpty())
      addEmpty(
          content,
          "Chưa có lần nấu nào",
          "Lưu lại cách bạn đã nấu để dễ xem lại lần sau.",
          () -> openForm(null, recipe.id));
    else for (CookingLogEntry log : logs) addLogCard(content, log, false);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    addButton(page, "+   Ghi lại lần nấu", true, () -> openForm(null, recipe.id), 8);
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void renderForm() {
    Recipe recipe = resolveRecipe(recipeId);
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(14));
    addBackTitle(page, editingLogId == null ? "Ghi lại lần nấu" : "Sửa lần nấu");
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout fields = vertical();
    fields.setPadding(0, dp(16), 0, dp(18));
    scroll.addView(fields);
    LinearLayout recipeCard = new LinearLayout(requireContext());
    recipeCard.setGravity(Gravity.CENTER_VERTICAL);
    recipeCard.setPadding(dp(8), dp(6), dp(10), dp(6));
    recipeCard.setBackground(roundRect(WHITE, BORDER, 14));
    recipeCard.addView(recipeImage(recipe), new LinearLayout.LayoutParams(dp(56), dp(50)));
    TextView title = text(recipe.title, 14, TEXT, true);
    title.setPadding(dp(10), 0, 0, 0);
    recipeCard.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    fields.addView(recipeCard);
    addText(fields, "Ngày nấu", 14, TEXT, true, 0, 20, 0, 7);
    TextView date = text(formatDate(cookedAt) + "     ▾", 15, TEXT, false);
    date.setPadding(dp(14), 0, dp(14), 0);
    date.setBackground(roundRect(WHITE, BORDER, 14));
    fields.addView(date, new LinearLayout.LayoutParams(-1, dp(50)));
    date.setOnClickListener(v -> showDatePicker());
    addText(fields, "Khẩu phần", 14, TEXT, true, 0, 18, 0, 7);
    servingsInput = editField(String.valueOf(servings), 16, 50);
    servingsInput.setSingleLine(true);
    servingsInput.setInputType(2);
    servingsInput.setHint("Số người");
    fields.addView(servingsInput);
    addText(fields, "Thời gian thực tế (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
    durationInput = editField(duration, 16, 50);
    durationInput.setSingleLine(true);
    durationInput.setInputType(2);
    durationInput.setHint("Số phút");
    fields.addView(durationInput);
    addText(fields, "Bạn đánh giá món này (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
    LinearLayout stars = new LinearLayout(requireContext());
    stars.setGravity(Gravity.CENTER_VERTICAL);
    fields.addView(stars, new LinearLayout.LayoutParams(-1, dp(48)));
    for (int value = 1; value <= 5; value++) {
      final int chosen = value;
      TextView star =
          text(
              value <= (rating == null ? 0 : rating) ? "★" : "☆",
              27,
              value <= (rating == null ? 0 : rating) ? GREEN : MUTED,
              true);
      star.setGravity(Gravity.CENTER);
      stars.addView(star, new LinearLayout.LayoutParams(dp(44), dp(44)));
      star.setOnClickListener(v -> {
        captureDraft();
        rating = rating != null && rating == chosen ? null : chosen;
        render();
      });
    }
    if (rating != null) {
      TextView clear = text("Bỏ", 12, MUTED, false);
      clear.setGravity(Gravity.CENTER);
      stars.addView(clear, new LinearLayout.LayoutParams(dp(42), dp(44)));
      clear.setOnClickListener(v -> {
        captureDraft();
        rating = null;
        render();
      });
    }
    addText(fields, "Điều chỉnh lần này (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
    adjustmentInput = editField(adjustment, 15, 100);
    adjustmentInput.setGravity(Gravity.TOP | Gravity.START);
    adjustmentInput.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    adjustmentInput.setHint("Ví dụ: giảm me, thêm rau thơm...");
    fields.addView(adjustmentInput, new LinearLayout.LayoutParams(-1, dp(104)));
    addText(fields, "Ảnh món đã nấu (không bắt buộc)", 14, TEXT, true, 0, 18, 0, 7);
    TextView photo = text(photoPath.isEmpty() ? "+   Thêm ảnh" : "Đổi ảnh", 14, GREEN, true);
    photo.setGravity(Gravity.CENTER);
    photo.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    fields.addView(photo, new LinearLayout.LayoutParams(-1, dp(48)));
    photo.setOnClickListener(v -> {
      captureDraft();
      photoPicker.launch("image/*");
    });
    if (!photoPath.isEmpty()) {
      ImageView preview = new ImageView(requireContext());
      photoPreviewView = preview;
      Bitmap bitmap = viewModel.getPhotoPreviewBitmap(photoPath);
      if (bitmap != null) preview.setImageBitmap(bitmap);
      preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
      preview.setBackground(roundRect(BORDER, BORDER, 14));
      preview.setClipToOutline(true);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(160));
      p.topMargin = dp(8);
      fields.addView(preview, p);
      TextView remove = text("Xóa ảnh", 13, ERROR, true);
      remove.setGravity(Gravity.CENTER);
      fields.addView(remove, new LinearLayout.LayoutParams(-1, dp(40)));
      remove.setOnClickListener(v -> {
        captureDraft();
        if (!originalPhotoPath.equals(photoPath)) viewModel.deletePhoto(photoPath);
        photoPath = "";
        render();
      });
    }
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    addButton(page, editingLogId == null ? "Lưu lần nấu" : "Lưu thay đổi", true, this::saveForm, 8);
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void openForm(String logId, String id) {
    captureDraft();
    formReturnScreen = screen;
    recipeId = id;
    editingLogId = logId;
    CookingLogEntry log = viewModel.getLog(logId);
    if (log == null) {
      cookedAt = System.currentTimeMillis();
      servings = 1;
      duration = "";
      rating = null;
      adjustment = "";
      photoPath = "";
      originalPhotoPath = "";
    } else {
      recipeId = log.recipeId;
      cookedAt = log.cookedAt;
      servings = log.servings;
      duration = log.actualDurationMinutes == null ? "" : String.valueOf(log.actualDurationMinutes);
      rating = log.rating;
      adjustment = log.adjustmentNote;
      photoPath = log.photoLocalPath;
      originalPhotoPath = log.photoLocalPath;
    }
    screen = FORM;
    formInitialized = true;
    notifyScreenChanged();
    render();
    if (!photoPath.isEmpty()) viewModel.loadPhotoPreviewAsync(photoPath);
  }

  private void saveForm() {
    captureDraft();
    if (servings < 1 || servings > 999) {
      toast("Khẩu phần phải từ 1 đến 999.");
      return;
    }
    Integer minutes = null;
    if (!duration.trim().isEmpty())
      try {
        minutes = Integer.parseInt(duration.trim());
        if (minutes < 1) throw new NumberFormatException();
      } catch (NumberFormatException invalid) {
        toast("Thời gian phải là số phút lớn hơn 0.");
        return;
      }
    if (!viewModel.save(
        editingLogId,
        resolveRecipe(recipeId),
        cookedAt,
        servings,
        minutes,
        rating,
        adjustment,
        photoPath)) toast("Không thể lưu lần nấu. Vui lòng kiểm tra thông tin.");
  }

  private void confirmDelete(CookingLogEntry log) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Xóa lần nấu?")
        .setMessage(
            "Xóa nhật ký nấu ngày "
                + formatDate(log.cookedAt)
                + "? Công thức và ghi chú chung vẫn được giữ.")
        .setNegativeButton("Hủy", null)
        .setPositiveButton(
            "Xóa",
            (d, w) -> {
              viewModel.delete(log.id);
              viewModel.deletePhoto(log.photoLocalPath);
            })
        .show();
  }

  private void addLogCard(LinearLayout parent, CookingLogEntry log, boolean showRecipe) {
    LinearLayout card = new LinearLayout(requireContext());
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(10), dp(10), dp(10), dp(10));
    card.setBackground(roundRect(WHITE, BORDER, 16));
    ImageView photo = new ImageView(requireContext());
    Bitmap bitmap = viewModel.getPhotoBitmap(log.photoLocalPath);
    if (bitmap != null) photo.setImageBitmap(bitmap);
    else photo.setImageResource(recipeDrawable(log.recipeId));
    photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
    photo.setBackground(roundRect(BORDER, BORDER, 12));
    photo.setClipToOutline(true);
    card.addView(photo, new LinearLayout.LayoutParams(dp(64), dp(64)));
    LinearLayout details = vertical();
    details.setPadding(dp(10), 0, dp(4), 0);
    if (showRecipe) details.addView(text(log.title, 14, TEXT, true));
    details.addView(
        text(
            formatDate(log.cookedAt),
            showRecipe ? 12 : 14,
            showRecipe ? MUTED : TEXT,
            !showRecipe));
    String info =
        log.servings
            + " người"
            + (log.actualDurationMinutes == null
                ? ""
                : " · " + log.actualDurationMinutes + " phút");
    addText(details, info, 12, MUTED, false, 0, 3, 0, 0);
    if (log.rating != null) addText(details, stars(log.rating), 12, GREEN, true, 0, 2, 0, 0);
    if (!log.adjustmentNote.isEmpty()) {
      TextView note = text(log.adjustmentNote, 12, MUTED, false);
      note.setMaxLines(2);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
      p.topMargin = dp(3);
      details.addView(note, p);
    }
    card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
    TextView delete = text("×", 24, ERROR, false);
    delete.setGravity(Gravity.CENTER);
    card.addView(delete, new LinearLayout.LayoutParams(dp(40), dp(48)));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(10);
    parent.addView(card, p);
    card.setOnClickListener(v -> openForm(log.id, log.recipeId));
    delete.setContentDescription("Xóa lần nấu ngày " + formatDate(log.cookedAt));
    delete.setOnClickListener(v -> confirmDelete(log));
  }

  private FrameLayout searchBox() {
    FrameLayout box = new FrameLayout(requireContext());
    box.setPadding(dp(12), 0, dp(12), 0);
    box.setBackground(roundRect(WHITE, BORDER, 15));
    EditText input = new EditText(requireContext());
    input.setSingleLine(true);
    input.setTextSize(14);
    input.setTextColor(TEXT);
    input.setHintTextColor(MUTED);
    input.setHint("Tìm món hoặc ghi chú");
    input.setBackgroundColor(Color.TRANSPARENT);
    input.setText(search);
    box.addView(input, new FrameLayout.LayoutParams(-1, -1));
    input.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          @Override
          public void onTextChanged(CharSequence s, int st, int before, int count) {
            search = s.toString();
            if (journalRows != null) populateJournalRows(journalRows);
          }

          @Override
          public void afterTextChanged(Editable e) {}
        });
    return box;
  }

  private View dateFilter(boolean start) {
    long selected = start ? fromDate : toDate;
    TextView field =
        text(
            (start ? "Từ ngày" : "Đến ngày")
                + "\n"
                + (selected == 0 ? "Chọn ngày" : formatDate(selected)),
            12,
            selected == 0 ? MUTED : TEXT,
            true);
    field.setGravity(Gravity.CENTER_VERTICAL);
    field.setPadding(dp(12), dp(5), dp(6), dp(5));
    field.setOnClickListener(v -> showFilterDatePicker(start));
    LinearLayout container = new LinearLayout(requireContext());
    container.setGravity(Gravity.CENTER_VERTICAL);
    container.setBackground(roundRect(WHITE, BORDER, 14));
    container.addView(field, new LinearLayout.LayoutParams(0, -1, 1));
    if (selected > 0) {
      TextView clear = text("×", 20, MUTED, true);
      clear.setGravity(Gravity.CENTER);
      clear.setContentDescription(start ? "Xóa ngày bắt đầu" : "Xóa ngày kết thúc");
      container.addView(clear, new LinearLayout.LayoutParams(dp(38), dp(44)));
      clear.setOnClickListener(v -> clearDateFilter(start));
    }
    return container;
  }

  private void showFilterDatePicker(boolean start) {
    long current = start ? fromDate : toDate;
    Calendar selected = Calendar.getInstance();
    if (current > 0) selected.setTimeInMillis(current);
    new DatePickerDialog(
            requireContext(),
            (picker, year, month, day) -> {
              Calendar date = Calendar.getInstance();
              date.clear();
              date.set(year, month, day);
              if (start) fromDate = date.getTimeInMillis();
              else toDate = date.getTimeInMillis();
              render();
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH))
        .show();
  }

  private void showDatePicker() {
    captureDraft();
    Calendar selected = Calendar.getInstance();
    selected.setTimeInMillis(cookedAt);
    new DatePickerDialog(
            requireContext(),
            (view, year, month, day) -> {
              Calendar date = Calendar.getInstance();
              date.clear();
              date.set(year, month, day);
              cookedAt = date.getTimeInMillis();
              render();
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH))
        .show();
  }

  private void addRatingChip(LinearLayout parent, String label, int value) {
    boolean selected = ratingFilter == value;
    TextView chip = text(label, 12, selected ? GREEN : MUTED, true);
    chip.setGravity(Gravity.CENTER);
    chip.setPadding(dp(11), 0, dp(11), 0);
    chip.setBackground(
        roundRect(selected ? PALE_GREEN : WHITE, selected ? PALE_GREEN : BORDER, 20));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(34));
    p.rightMargin = dp(7);
    parent.addView(chip, p);
    chip.setOnClickListener(v -> {
      ratingFilter = value;
      render();
    });
  }

  private void addStats(LinearLayout parent, CookingJournalViewModel.JournalStats stats) {
    LinearLayout card = vertical();
    card.setPadding(dp(14), dp(12), dp(14), dp(12));
    card.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 16));
    addStatRow(
        card,
        "Món nấu nhiều nhất",
        stats.mostCookedCount == 0
            ? "Chưa có"
            : stats.mostCookedRecipe + " · " + stats.mostCookedCount + " lần");
    addStatRow(
        card,
        "Lần nấu gần nhất",
        stats.lastCookedAt == null ? "Chưa có" : formatDate(stats.lastCookedAt));
    addStatRow(
        card,
        "Đánh giá gần nhất",
        stats.latestRating == null ? "Chưa đánh giá" : stars(stats.latestRating));
    parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
  }

  private void addStatRow(LinearLayout card, String label, String value) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.addView(text(label, 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
    TextView val = text(value, 13, TEXT, true);
    val.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    val.setMaxLines(2);
    val.setMaxWidth(dp(200));
    row.addView(val, new LinearLayout.LayoutParams(0, -2, 1));
    card.addView(row);
    if (!"Đánh giá gần nhất".equals(label)) {
      View divider = new View(requireContext());
      divider.setBackgroundColor(Color.rgb(211, 226, 212));
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
      p.topMargin = dp(7);
      p.bottomMargin = dp(7);
      card.addView(divider, p);
    }
  }

  private void addBottomNavigation(LinearLayout parent, int selected) {
    BottomNavigationBar navigation =
        new BottomNavigationBar(requireContext(), selected, host::onJournalNavigateTab);
    parent.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
  }

  private void addBackTitle(LinearLayout parent, String title) {
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
    float widthDp =
        getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
    float available = Math.max(160, widthDp - 108);
    float titleDp =
        heading.getPaint().measureText(title) / getResources().getDisplayMetrics().density;
    if (titleDp > available) heading.setTextSize(Math.max(17, 23 * available / titleDp));
    heading.setPadding(dp(12), 0, 0, 0);
    row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private void addButton(
      LinearLayout parent, String label, boolean primary, Runnable action, int top) {
    TextView button = text(label, 15, primary ? WHITE : GREEN, true);
    button.setGravity(Gravity.CENTER);
    button.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(49));
    p.topMargin = dp(top);
    parent.addView(button, p);
    button.setOnClickListener(v -> action.run());
  }

  private void addEmpty(LinearLayout parent, String title, String subtitle, Runnable action) {
    LinearLayout card = vertical();
    card.setGravity(Gravity.CENTER);
    card.setPadding(dp(20), dp(24), dp(20), dp(24));
    card.setBackground(roundRect(WHITE, BORDER, 15));
    card.setFocusable(true);
    card.setContentDescription(title + ". " + subtitle);
    card.addView(text(title, 15, TEXT, true));
    card.addView(text(subtitle, 13, MUTED, false));
    parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
    card.setOnClickListener(v -> action.run());
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
    TextView view = text(value, size, color, bold);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    parent.addView(view, p);
  }

  private EditText editField(String value, int size, int minHeight) {
    EditText field = new EditText(requireContext());
    field.setText(value);
    field.setTextSize(size);
    field.setTextColor(TEXT);
    field.setHintTextColor(MUTED);
    field.setSingleLine(false);
    field.setBackground(roundRect(WHITE, BORDER, 14));
    field.setPadding(dp(15), dp(10), dp(15), dp(10));
    if (minHeight > 0) field.setMinHeight(dp(minHeight));
    return field;
  }

  private ImageView recipeImage(Recipe recipe) {
    ImageView image = new ImageView(requireContext());
    image.setImageResource(recipeDrawable(recipe.id));
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setBackground(roundRect(BORDER, BORDER, 12));
    image.setClipToOutline(true);
    return image;
  }

  private int recipeDrawable(String id) {
    if ("sample-ga-chien-nuoc-mam".equals(id)) return R.drawable.recipe_ga_chien;
    if ("sample-mi-y-sot-bo-bam".equals(id)) return R.drawable.recipe_mi_y;
    if ("sample-com-rang-dua-bo".equals(id)) return R.drawable.recipe_com_rang;
    if ("sample-salad-rau-cu".equals(id)) return R.drawable.recipe_salad;
    return R.drawable.recipe_canh_chua;
  }

  private TextView text(String value, int size, int color, boolean bold) {
    TextView view = new TextView(requireContext());
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
    view.setGravity(Gravity.CENTER_VERTICAL);
    return view;
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

  private Recipe resolveRecipe(String id) {
    Recipe recipe = host.onJournalResolveRecipe(id);
    return recipe == null ? PreviewRecipeCatalog.designRecipes().get(0) : recipe;
  }

  private String formatDate(long millis) {
    return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(millis));
  }

  private String stars(int value) {
    StringBuilder result = new StringBuilder();
    for (int i = 1; i <= 5; i++) result.append(i <= value ? '★' : '☆');
    return result.toString();
  }

  void clearDateFilter(boolean start) {
    if (start) fromDate = 0;
    else toDate = 0;
    if (root != null) render();
  }

  void clearDateRangeForExit() {
    fromDate = 0;
    toDate = 0;
  }

  private boolean hasFilters() {
    return fromDate > 0 || toDate > 0 || ratingFilter > 0 || !search.trim().isEmpty();
  }

  private void captureDraft() {
    if (servingsInput != null)
      try {
        servings = Integer.parseInt(servingsInput.getText().toString().trim());
      } catch (NumberFormatException ignored) {
      }
    if (durationInput != null) duration = durationInput.getText().toString();
    if (adjustmentInput != null) adjustment = adjustmentInput.getText().toString();
  }

  private void discardDraftPhoto() {
    if (!photoPath.isEmpty() && !photoPath.equals(originalPhotoPath))
      viewModel.deletePhoto(photoPath);
  }

  private void notifyScreenChanged() {
    host.onJournalScreenChanged(screen, recipeId);
  }

  private void toast(String message) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
  }

  /** Keeps horizontal rating chips scrollable without adding another project dependency. */
  private static final class HorizontalScrollViewCompat
      extends android.widget.HorizontalScrollView {
    HorizontalScrollViewCompat(android.content.Context context) {
      super(context);
    }
  }
}
