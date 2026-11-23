package com.example.myapplication.presentation.personaldata;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
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
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.example.myapplication.domain.catalog.PreviewRecipeCatalog;
import com.example.myapplication.domain.model.CookingLogEntry;
import com.example.myapplication.presentation.cookingjournal.CookingJournalViewModel;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Owns Module 5 library, favorites, collection list/detail, and collection form screens. */
public final class PersonalDataFragment extends Fragment {
  private static final String ARG_SECTION = "personal.section";
  private static final String ARG_COLLECTION_ID = "personal.collectionId";
  private static final String ARG_NOTE_RECIPE_ID = "personal.noteRecipeId";
  private static final String STATE_SECTION = "personal.fragment.section";
  private static final String STATE_QUERY = "personal.fragment.query";
  private static final String STATE_DETAIL_QUERY = "personal.fragment.collectionQuery";
  private static final String STATE_FORM_QUERY = "personal.fragment.collectionFormQuery";
  private static final String STATE_COLLECTION_ID = "personal.fragment.collectionId";
  private static final String STATE_NOTE_RECIPE_ID = "personal.fragment.noteRecipeId";
  private static final String LIBRARY = "LIBRARY", FAVORITES = "FAVORITES", COLLECTIONS = "COLLECTIONS",
      COLLECTION = "COLLECTION", COLLECTION_FORM = "COLLECTION_FORM";
  private static final String NOTE = "NOTE";
  private static final int CREAM = Color.rgb(250, 249, 246),
      GREEN = Color.rgb(50, 108, 79),
      PALE_GREEN = Color.rgb(232, 242, 233),
      TEXT = Color.rgb(37, 53, 45),
      MUTED = Color.rgb(88, 102, 92),
      BORDER = Color.rgb(220, 228, 220),
      WHITE = Color.WHITE;

  public interface Host {
    void onPersonalOpenFavorites();

    void onPersonalOpenCollections();

    void onPersonalCreateCollection();

    void onPersonalEditCollection(long collectionId);

    void onPersonalDeleteCollection(long collectionId);

    void onPersonalCollectionSaved(long collectionId);

    void onPersonalOpenCollection(long collectionId);

    void onPersonalOpenRecipe(Recipe recipe);

    Recipe onPersonalResolveRecipe(String recipeId);

    void onPersonalOpenRecipeLogs(String recipeId);

    void onPersonalCreateCookingLog(String recipeId);

    void onPersonalOpenJournal();

    void onPersonalNavigateTab(int tab);

    void onPersonalBack();
  }

  private PersonalDataViewModel viewModel;
  private CookingJournalViewModel journalViewModel;
  private Host host;
  private String section = LIBRARY;
  private String searchQuery = "";
  private String collectionDetailQuery = "";
  private String collectionFormQuery = "";
  private long collectionId = -1L;
  private String noteRecipeId;
  private LinearLayout root;
  private EditText collectionNameInput;
  private TextView collectionError;
  private TextView collectionSaveButton;
  private LinearLayout collectionRecipeResults;
  private LinearLayout collectionFormRecipeRows;
  private EditText noteInput;
  private LinearLayout noteSavedBanner;

  public static PersonalDataFragment newInstance(boolean favorites) {
    return newInstance(favorites ? FAVORITES : LIBRARY);
  }

  public static PersonalDataFragment newInstance(String section) {
    return newInstance(section, -1L, null);
  }

  public static PersonalDataFragment newInstance(String section, long collectionId, String noteRecipeId) {
    PersonalDataFragment f = new PersonalDataFragment();
    Bundle b = new Bundle();
    b.putString(ARG_SECTION, section);
    b.putLong(ARG_COLLECTION_ID, collectionId);
    b.putString(ARG_NOTE_RECIPE_ID, noteRecipeId);
    f.setArguments(b);
    return f;
  }

  public void showScreen(String section, long collectionId, String noteRecipeId) {
    if (!java.util.Objects.equals(this.section, section)) {
      searchQuery = "";
      collectionDetailQuery = "";
      collectionFormQuery = "";
    }
    if (this.collectionId != collectionId) collectionDetailQuery = "";
    this.section = section;
    this.collectionId = collectionId;
    this.noteRecipeId = noteRecipeId;
    render();
  }

  @Override
  public void onAttach(@NonNull android.content.Context context) {
    super.onAttach(context);
    if (!(context instanceof Host))
      throw new IllegalStateException("Host Activity must implement PersonalDataFragment.Host");
    host = (Host) context;
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    if (state != null) {
      section = state.getString(STATE_SECTION, LIBRARY);
      searchQuery = state.getString(STATE_QUERY, "");
      collectionDetailQuery = state.getString(STATE_DETAIL_QUERY, "");
      collectionFormQuery = state.getString(STATE_FORM_QUERY, "");
      collectionId = state.getLong(STATE_COLLECTION_ID, -1L);
      noteRecipeId = state.getString(STATE_NOTE_RECIPE_ID);
    } else if (getArguments() != null) {
      section = getArguments().getString(ARG_SECTION, LIBRARY);
      collectionId = getArguments().getLong(ARG_COLLECTION_ID, -1L);
      noteRecipeId = getArguments().getString(ARG_NOTE_RECIPE_ID);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    out.putString(STATE_SECTION, section);
    out.putString(STATE_QUERY, searchQuery);
    out.putString(STATE_DETAIL_QUERY, collectionDetailQuery);
    out.putString(STATE_FORM_QUERY, collectionFormQuery);
    out.putLong(STATE_COLLECTION_ID, collectionId);
    out.putString(STATE_NOTE_RECIPE_ID, noteRecipeId);
    super.onSaveInstanceState(out);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    AppViewModelFactory factory =
        new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication());
    viewModel = new ViewModelProvider(requireActivity(), factory).get(PersonalDataViewModel.class);
    journalViewModel =
        new ViewModelProvider(requireActivity(), factory).get(CookingJournalViewModel.class);
    root = vertical();
    root.setBackgroundColor(CREAM);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          view.setPadding(sys.left, sys.top, sys.right, Math.max(sys.bottom, ime.bottom));
          return insets;
        });
    viewModel
        .personalDataState()
        .observe(
            getViewLifecycleOwner(),
            stateValue -> {
              if (stateValue != null && !stateValue.loading) {
                if (NOTE.equals(section)) return;
                if (FAVORITES.equals(section) && searchInputFocused()) return;
                render();
              }
            });
    viewModel.collectionSaveResult().observe(getViewLifecycleOwner(), result -> {
      if (result == null) return;
      viewModel.clearCollectionSaveResult();
      if (!COLLECTION_FORM.equals(section)) return;
      if (result.collectionId != null) host.onPersonalCollectionSaved(result.collectionId);
      else {
        collectionError.setText(result.error == null ? "Không thể lưu bộ sưu tập." : result.error);
        collectionNameInput.setBackground(roundRect(WHITE, Color.rgb(169, 54, 54), 14));
        updateCollectionSaveButton(false);
      }
    });
    viewModel.noteSaveResult().observe(getViewLifecycleOwner(), result -> {
      if (result == null) return;
      viewModel.clearNoteSaveResult();
      if (result.success && NOTE.equals(section) && noteSavedBanner != null) {
        noteSavedBanner.setVisibility(View.VISIBLE);
      } else if (!result.success) {
        android.widget.Toast.makeText(requireContext(),
            result.error == null ? "Không thể lưu ghi chú." : result.error,
            android.widget.Toast.LENGTH_SHORT).show();
      }
    });
    journalViewModel.journalState().observe(getViewLifecycleOwner(), stateValue -> {
      if (stateValue != null && !stateValue.loading && NOTE.equals(section) && !noteInputFocused()) render();
    });
    render();
    return root;
  }

  private boolean searchInputFocused() {
    EditText input = root == null ? null : root.findViewWithTag("personal-favorite-search");
    return input != null && input.hasFocus();
  }

  private void render() {
    if (root == null || !isAdded()) return;
    root.removeAllViews();
    if (FAVORITES.equals(section)) renderFavorites();
    else if (COLLECTIONS.equals(section)) renderCollections();
    else if (COLLECTION.equals(section)) renderCollection();
    else if (COLLECTION_FORM.equals(section)) renderCollectionForm();
    else if (NOTE.equals(section)) renderNote();
    else renderLibrary();
  }

  private boolean noteInputFocused() {
    return noteInput != null && noteInput.hasFocus();
  }

  private void renderLibrary() {
    ScrollView scroll = new ScrollView(requireContext());
    scroll.setFillViewport(false);
    LinearLayout content = vertical();
    content.setPadding(dp(24), dp(28), dp(24), dp(24));
    scroll.addView(content);
    addText(content, "Kho của tôi", 23, TEXT, true, 0, 0, 0, 0);
    addText(content, "Các món và ghi chú lưu trên thiết bị.", 14, MUTED, false, 0, 22, 0, 0);
    addSectionHeader(
        content,
        "Yêu thích",
        "Xem tất cả ›",
        () -> {
          section = FAVORITES;
          host.onPersonalOpenFavorites();
          render();
        },
        30);
    List<Recipe> favorites = viewModel.getFavorites();
    if (favorites.isEmpty()) addEmptyFavoriteCard(content);
    else addFavoriteGrid(content, favorites.subList(0, Math.min(2, favorites.size())));
    addSectionHeader(
        content,
        "Bộ sưu tập",
        "Xem tất cả ›",
        () -> {
          section = COLLECTIONS;
          host.onPersonalOpenCollections();
          render();
        },
        14);
    List<RecipeCollection> collections = viewModel.getCollections();
    if (collections.isEmpty()) addEmptyCollectionCard(content);
    else
      for (int i = 0; i < Math.min(5, collections.size()); i++)
        addCollectionRow(content, collections.get(i));
    addText(content, "Đã xem gần đây", 19, TEXT, true, 0, 24, 0, 12);
    List<Recipe> history = viewModel.getHistory();
    for (int i = 0; i < Math.min(10, history.size()); i++) addHistoryRow(content, history.get(i));
    addSectionHeader(
        content, "Nhật ký nấu", "Xem tất cả ›", () -> host.onPersonalOpenJournal(), 24);
    addText(
        content,
        journalViewModel.getAllLogs().size() + " lần nấu đã ghi",
        13,
        MUTED,
        false,
        0,
        0,
        0,
        8);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    addBottomNavigation();
  }

  private void renderCollections() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(24));
    addBackTitle(page, "Bộ sưu tập");
    addText(page, "Các bộ sưu tập bạn đã tạo.", 14, MUTED, false, 0, 0, 0, 20);

    ScrollView scroll = new ScrollView(requireContext());
    scroll.setFillViewport(false);
    LinearLayout content = vertical();
    scroll.addView(content);
    List<RecipeCollection> collections = viewModel.getCollections();
    if (collections.isEmpty()) addEmptyCollectionCard(content);
    else for (RecipeCollection collection : collections) addCollectionRow(content, collection);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    TextView create = button("+     Tạo bộ sưu tập", true);
    LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(-1, dp(52));
    createParams.topMargin = dp(18);
    page.addView(create, createParams);
    create.setOnClickListener(v -> host.onPersonalCreateCollection());
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void renderFavorites() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(24));
    addBackTitle(page, "Yêu thích");
    addText(page, "Những công thức bạn đã lưu.", 14, MUTED, false, 0, 0, 0, 16);
    List<Recipe> favorites = viewModel.getFavorites();
    LinearLayout content = vertical();
    content.setGravity(Gravity.CENTER_HORIZONTAL);
    if (!favorites.isEmpty()) {
      FrameLayout box =
          createSearchBox(
              searchQuery,
              "Tìm món yêu thích",
              query -> {
                searchQuery = query;
                populateFavorites(content, favorites);
              });
      EditText input = box.findViewWithTag("personal-favorite-search");
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(49));
      params.bottomMargin = dp(18);
      page.addView(box, params);
    }
    ScrollView scroll = new ScrollView(requireContext());
    scroll.addView(content);
    if (favorites.isEmpty()) renderEmptyFavorites(content);
    else populateFavorites(content, favorites);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void renderCollection() {
    RecipeCollection selected = findCollection(collectionId);
    if (selected == null) {
      host.onPersonalOpenCollections();
      return;
    }
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(24));
    addBackTitle(page, "Bộ sưu tập");
    LinearLayout titleLine = new LinearLayout(requireContext());
    titleLine.setGravity(Gravity.CENTER_VERTICAL);
    TextView name = text(selected.name, 19, TEXT, true);
    titleLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout delete = collectionActionButton("Xóa", R.drawable.ic_delete_outline,
        Color.rgb(169, 54, 54), WHITE, () -> confirmDeleteCollection(selected));
    delete.setContentDescription("Xóa bộ sưu tập");
    titleLine.addView(delete, new LinearLayout.LayoutParams(-2, dp(48)));
    LinearLayout edit = collectionActionButton("Chỉnh sửa", R.drawable.ic_edit_outline,
        GREEN, PALE_GREEN, () -> host.onPersonalEditCollection(selected.id));
    edit.setContentDescription("Chỉnh sửa bộ sưu tập");
    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(-2, dp(48));
    editParams.setMarginStart(dp(8));
    titleLine.addView(edit, editParams);
    page.addView(titleLine);
    addText(page, selected.recipeCount + " món", 14, MUTED, false, 0, 8, 0, 24);

    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout content = vertical();
    scroll.addView(content);
    List<Recipe> recipes = viewModel.getCollectionRecipes(collectionId);
    if (recipes.isEmpty()) {
      LinearLayout empty = vertical();
      empty.setGravity(Gravity.CENTER);
      ImageView folder = icon(R.drawable.ic_folder_outline, GREEN);
      folder.setPadding(dp(24), dp(24), dp(24), dp(24));
      folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
      empty.addView(folder, new LinearLayout.LayoutParams(dp(84), dp(84)));
      TextView emptyTitle = text("Bộ sưu tập đang trống", 22, TEXT, true);
      emptyTitle.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
      titleParams.topMargin = dp(18);
      titleParams.bottomMargin = dp(8);
      empty.addView(emptyTitle, titleParams);
      TextView hint = text("Thêm món đã lưu vào bộ sưu tập này.", 14, MUTED, false);
      hint.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
      hintParams.bottomMargin = dp(16);
      empty.addView(hint, hintParams);
      TextView add = button("Thêm món", true);
      empty.addView(add, new LinearLayout.LayoutParams(-1, dp(49)));
      add.setOnClickListener(v -> host.onPersonalEditCollection(collectionId));
      content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
    } else {
      FrameLayout search = createSearchBox(
          collectionDetailQuery, "Tìm món trong bộ sưu tập", query -> {
            collectionDetailQuery = query;
            populateCollectionResults(recipes);
          });
      LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
      searchParams.bottomMargin = dp(14);
      content.addView(search, searchParams);
      collectionRecipeResults = vertical();
      content.addView(collectionRecipeResults);
      populateCollectionResults(recipes);
    }
    List<RecipeCollection> collections = viewModel.getCollections();
    boolean hasOther = false;
    for (RecipeCollection other : collections) {
      if (other.id == collectionId) continue;
      if (!hasOther) {
        addText(content, "Bộ sưu tập khác", 19, TEXT, true, 0, 22, 0, 12);
        hasOther = true;
      }
      addRelatedCollectionRow(content, other);
    }
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    TextView create = button("+     Tạo bộ sưu tập", true);
    LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(-1, dp(49));
    createParams.topMargin = dp(18);
    page.addView(create, createParams);
    create.setOnClickListener(v -> host.onPersonalCreateCollection());
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void populateCollectionResults(List<Recipe> recipes) {
    if (collectionRecipeResults == null) return;
    collectionRecipeResults.removeAllViews();
    String query = normalize(collectionDetailQuery);
    List<Recipe> matches = new ArrayList<>();
    for (Recipe recipe : recipes)
      if (query.isEmpty() || normalize(recipe.title).contains(query)) matches.add(recipe);
    if (matches.isEmpty()) {
      TextView none = text("Không tìm thấy món trong bộ sưu tập.", 14, MUTED, false);
      none.setGravity(Gravity.CENTER);
      collectionRecipeResults.addView(none, new LinearLayout.LayoutParams(-1, dp(80)));
      return;
    }
    addFavoriteGrid(collectionRecipeResults, matches);
  }

  private void addRelatedCollectionRow(LinearLayout parent, RecipeCollection collection) {
    LinearLayout item = horizontalCard();
    List<Recipe> recipes = viewModel.getCollectionRecipes(collection.id);
    if (recipes.isEmpty()) {
      ImageView folder = icon(R.drawable.ic_folder_outline, GREEN);
      folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 12));
      item.addView(folder, new LinearLayout.LayoutParams(dp(52), dp(52)));
    } else item.addView(recipeImage(recipes.get(0)), new LinearLayout.LayoutParams(dp(52), dp(52)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text(collection.name, 14, TEXT, true));
    labels.addView(text(collection.recipeCount + " món đã lưu", 12, MUTED, false));
    item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    item.addView(text("›", 23, GREEN, false));
    item.setOnClickListener(v -> host.onPersonalOpenCollection(collection.id));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
    params.bottomMargin = dp(9);
    parent.addView(item, params);
  }

  private void renderCollectionForm() {
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(12));
    RecipeCollection editing = collectionId > 0 ? findCollection(collectionId) : null;
    addBackTitle(page, "Tạo bộ sưu tập");
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout content = vertical();
    scroll.addView(content);
    addText(content, "Tên bộ sưu tập", 13, TEXT, true, 0, 20, 0, 10);
    collectionNameInput = new EditText(requireContext());
    collectionNameInput.setSingleLine(true);
    collectionNameInput.setText(viewModel.getCollectionNameDraft());
    collectionNameInput.setTextSize(16);
    collectionNameInput.setTextColor(TEXT);
    collectionNameInput.setPadding(dp(15), dp(8), dp(15), dp(8));
    collectionNameInput.setBackground(roundRect(WHITE, BORDER, 14));
    collectionNameInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
    content.addView(collectionNameInput, new LinearLayout.LayoutParams(-1, dp(49)));
    collectionError = text("", 12, Color.rgb(169, 54, 54), false);
    content.addView(collectionError);
    addText(content, "Chọn món để thêm", 19, TEXT, true, 0, 34, 0, 18);
    FrameLayout search = createSearchBox(collectionFormQuery, "Tìm món để thêm", query -> {
      collectionFormQuery = query;
      populateCollectionFormRows();
    });
    LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(49));
    searchParams.bottomMargin = dp(14);
    content.addView(search, searchParams);
    collectionFormRecipeRows = vertical();
    content.addView(collectionFormRecipeRows);
    populateCollectionFormRows();
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    collectionSaveButton = button("Lưu bộ sưu tập", true);
    LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(49));
    saveParams.topMargin = dp(18);
    page.addView(collectionSaveButton, saveParams);
    collectionSaveButton.setOnClickListener(v -> saveCollectionForm());
    collectionNameInput.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        viewModel.setCollectionNameDraft(s.toString());
        updateCollectionSaveButton(!s.toString().trim().isEmpty());
      }
      @Override public void afterTextChanged(Editable s) {}
    });
    updateCollectionSaveButton(!viewModel.getCollectionNameDraft().trim().isEmpty());
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void renderNote() {
    Recipe recipe = host.onPersonalResolveRecipe(noteRecipeId);
    if (recipe == null) recipe = PreviewRecipeCatalog.designRecipes().get(0);
    final Recipe selectedRecipe = recipe;
    LinearLayout page = vertical();
    page.setPadding(dp(24), dp(8), dp(24), dp(12));
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout content = vertical();
    content.setPadding(0, 0, 0, dp(8));
    scroll.addView(content);
    addBackTitle(content, "Ghi chú cá nhân");

    LinearLayout recipeRow = new LinearLayout(requireContext());
    recipeRow.setGravity(Gravity.CENTER_VERTICAL);
    recipeRow.setPadding(dp(8), dp(6), dp(12), dp(6));
    recipeRow.setBackground(roundRect(WHITE, BORDER, 14));
    recipeRow.addView(recipeImage(selectedRecipe), new LinearLayout.LayoutParams(dp(60), dp(52)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text(selectedRecipe.title, 14, TEXT, true));
    labels.addView(text(selectedRecipe.duration + " · " + selectedRecipe.difficulty, 12, MUTED, false));
    recipeRow.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    recipeRow.addView(favoriteToggleButton(selectedRecipe), new LinearLayout.LayoutParams(dp(48), dp(48)));
    LinearLayout.LayoutParams recipeParams = new LinearLayout.LayoutParams(-1, dp(64));
    recipeParams.topMargin = dp(30);
    content.addView(recipeRow, recipeParams);

    addText(content, "Ghi chú sau khi nấu", 13, TEXT, true, 0, 48, 0, 16);
    noteInput = new EditText(requireContext());
    noteInput.setTag("personal-note-input");
    noteInput.setText(viewModel.getNote(selectedRecipe.id));
    noteInput.setTextSize(16);
    noteInput.setTextColor(TEXT);
    noteInput.setGravity(Gravity.TOP | Gravity.START);
    noteInput.setPadding(dp(16), dp(14), dp(16), dp(14));
    noteInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
        | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    noteInput.setBackground(roundRect(WHITE, BORDER, 14));
    int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels
        / getResources().getDisplayMetrics().density);
    int noteHeightDp = Math.max(160, Math.min(220, Math.round(screenHeightDp * 0.215f)));
    content.addView(noteInput, new LinearLayout.LayoutParams(-1, dp(noteHeightDp)));
    noteSavedBanner = createInfoBanner("Ghi chú đã lưu");
    LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, dp(48));
    bannerParams.topMargin = dp(24);
    bannerParams.bottomMargin = dp(12);
    content.addView(noteSavedBanner, bannerParams);
    noteSavedBanner.setVisibility(View.GONE);

    addCookingJournalPreview(content, selectedRecipe);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    TextView save = button("Lưu ghi chú", true);
    LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(49));
    saveParams.topMargin = dp(8);
    page.addView(save, saveParams);
    save.setOnClickListener(v -> {
      viewModel.saveNote(selectedRecipe, noteInput.getText().toString());
      noteInput.clearFocus();
      InputMethodManager imm = (InputMethodManager) requireContext()
          .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
      if (imm != null) imm.hideSoftInputFromWindow(noteInput.getWindowToken(), 0);
    });
    root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private FrameLayout favoriteToggleButton(Recipe recipe) {
    boolean favorite = viewModel.isFavorite(recipe.id);
    FrameLayout button = new FrameLayout(requireContext());
    button.setFocusable(true);
    button.setClickable(true);
    button.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    ImageView star = icon(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline, GREEN);
    button.addView(star, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
    button.setContentDescription((favorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    button.setOnClickListener(v -> {
      boolean nowFavorite = !viewModel.isFavorite(recipe.id);
      if (nowFavorite) viewModel.addFavorite(recipe);
      else viewModel.removeFavorite(recipe.id);
      star.setImageResource(nowFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
      button.setContentDescription((nowFavorite ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    });
    return button;
  }

  private void addCookingJournalPreview(LinearLayout parent, Recipe recipe) {
    List<CookingLogEntry> logs = journalViewModel.getLogs(recipe.id);
    LinearLayout heading = new LinearLayout(requireContext());
    heading.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
    headingParams.topMargin = dp(24);
    headingParams.bottomMargin = dp(6);
    parent.addView(heading, headingParams);
    heading.addView(text("Nhật ký nấu", 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
    TextView all = text("Xem tất cả ›", 12, GREEN, true);
    heading.addView(all);
    all.setOnClickListener(v -> host.onPersonalOpenRecipeLogs(recipe.id));
    addText(parent, "Đã nấu " + logs.size() + " lần", 13, MUTED, false, 0, 0, 0, 0);
    if (logs.isEmpty()) {
      addText(parent, "Ghi lại trải nghiệm mỗi lần bạn nấu món này.", 13, MUTED, false, 0, 5, 0, 0);
    } else {
      for (int index = 0; index < Math.min(2, logs.size()); index++) {
        CookingLogEntry log = logs.get(index);
        LinearLayout card = vertical();
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(roundRect(WHITE, BORDER, 14));
        String summary = formatDate(log.cookedAt) + " · " + log.servings + " người"
            + (log.rating == null ? "" : " · " + ratingStars(log.rating));
        card.addView(text(summary, 12, MUTED, true));
        if (!log.adjustmentNote.isEmpty()) {
          TextView note = text(log.adjustmentNote, 13, TEXT, false);
          note.setMaxLines(2);
          LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
          noteParams.topMargin = dp(4);
          card.addView(note, noteParams);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(8);
        parent.addView(card, cardParams);
        card.setOnClickListener(v -> host.onPersonalOpenRecipeLogs(log.recipeId));
      }
    }
    TextView addLog = button("+   Ghi lại lần nấu", false);
    LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(-1, dp(45));
    logParams.topMargin = dp(10);
    parent.addView(addLog, logParams);
    addLog.setOnClickListener(v -> host.onPersonalCreateCookingLog(recipe.id));
  }

  private LinearLayout createInfoBanner(String value) {
    LinearLayout banner = new LinearLayout(requireContext());
    banner.setGravity(Gravity.CENTER_VERTICAL);
    banner.setPadding(dp(14), dp(10), dp(14), dp(10));
    banner.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 15));
    banner.addView(text("ⓘ", 18, GREEN, false));
    TextView content = text(value, 13, TEXT, true);
    content.setPadding(dp(12), 0, 0, 0);
    banner.addView(content);
    return banner;
  }

  private String formatDate(long millis) {
    return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(millis));
  }

  private String ratingStars(int rating) {
    StringBuilder stars = new StringBuilder();
    for (int value = 1; value <= 5; value++) stars.append(value <= rating ? '★' : '☆');
    return stars.toString();
  }

  private void populateCollectionFormRows() {
    if (collectionFormRecipeRows == null) return;
    collectionFormRecipeRows.removeAllViews();
    String query = normalize(collectionFormQuery);
    int matches = 0;
    for (Recipe recipe : PreviewRecipeCatalog.designRecipes()) {
      if (!query.isEmpty() && !normalize(recipe.title).contains(query)) continue;
      addSelectableRecipeRow(collectionFormRecipeRows, recipe);
      matches++;
    }
    if (matches == 0) {
      TextView none = text("Không tìm thấy món phù hợp.", 14, MUTED, false);
      none.setGravity(Gravity.CENTER);
      collectionFormRecipeRows.addView(none, new LinearLayout.LayoutParams(-1, dp(72)));
    }
  }

  private void addSelectableRecipeRow(LinearLayout parent, Recipe recipe) {
    boolean selected = viewModel.isCollectionRecipeSelected(recipe.id);
    LinearLayout item = horizontalCard();
    ImageView image = recipeImage(recipe);
    item.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text(recipe.title, 14, TEXT, true));
    labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
    item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    TextView check = text(selected ? "✓" : "+", 21, selected ? WHITE : GREEN, false);
    check.setGravity(Gravity.CENTER);
    if (selected) check.setBackground(roundRect(GREEN, GREEN, 7));
    item.addView(check, new LinearLayout.LayoutParams(dp(32), dp(32)));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
    params.bottomMargin = dp(9);
    parent.addView(item, params);
    item.setOnClickListener(v -> {
      viewModel.toggleCollectionRecipe(recipe.id);
      populateCollectionFormRows();
    });
  }

  private void saveCollectionForm() {
    String name = collectionNameInput.getText().toString();
    viewModel.setCollectionNameDraft(name);
    viewModel.markCollectionSubmitAttempted();
    Long editingId = collectionId > 0 ? collectionId : null;
    if (name.trim().isEmpty()) {
      collectionError.setText("Nhập tên bộ sưu tập.");
      updateCollectionSaveButton(false);
      return;
    }
    if (!viewModel.isLoaded()) {
      collectionError.setText("Đang tải dữ liệu bộ sưu tập. Vui lòng thử lại.");
      updateCollectionSaveButton(false);
      return;
    }
    if (!viewModel.isCollectionNameAvailable(name, editingId)) {
      collectionError.setText("Tên này đã được sử dụng.");
      updateCollectionSaveButton(false);
      return;
    }
    collectionError.setText("");
    viewModel.saveCollectionForm(editingId, PreviewRecipeCatalog.designRecipes());
  }

  private void updateCollectionSaveButton(boolean enabled) {
    if (collectionSaveButton == null) return;
    collectionSaveButton.setEnabled(enabled);
    collectionSaveButton.setTextColor(enabled ? WHITE : MUTED);
    collectionSaveButton.setBackground(roundRect(enabled ? GREEN : PALE_GREEN, enabled ? GREEN : PALE_GREEN, 14));
  }

  private RecipeCollection findCollection(long id) {
    for (RecipeCollection collection : viewModel.getCollections())
      if (collection.id == id) return collection;
    return null;
  }

  private LinearLayout collectionActionButton(String label, int iconId, int tint, int background,
      Runnable action) {
    LinearLayout button = new LinearLayout(requireContext());
    button.setGravity(Gravity.CENTER);
    button.setPadding(dp(10), 0, dp(10), 0);
    button.setBackground(roundRect(background, background == WHITE ? BORDER : background, 14));
    button.addView(icon(iconId, tint), new LinearLayout.LayoutParams(dp(18), dp(18)));
    TextView caption = text(label, 12, tint, true);
    LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-2, -2);
    captionParams.setMarginStart(dp(5));
    button.addView(caption, captionParams);
    button.setContentDescription(label + " bộ sưu tập");
    button.setFocusable(true);
    button.setOnClickListener(v -> action.run());
    return button;
  }

  private void confirmDeleteCollection(RecipeCollection collection) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Xóa bộ sưu tập?")
        .setMessage("Bộ sưu tập “" + collection.name + "” và danh sách món trong đó sẽ bị xóa. Yêu thích và ghi chú của các món vẫn được giữ.")
        .setNegativeButton("Hủy", null)
        .setPositiveButton("Xóa", (dialog, which) -> host.onPersonalDeleteCollection(collection.id))
        .show();
  }

  private void populateFavorites(LinearLayout content, List<Recipe> favorites) {
    content.removeAllViews();
    String query = normalize(searchQuery);
    List<Recipe> matches = new ArrayList<>();
    for (Recipe recipe : favorites)
      if (query.isEmpty() || normalize(recipe.title).contains(query)) matches.add(recipe);
    if (matches.isEmpty()) {
      TextView none = text("Không tìm thấy món yêu thích phù hợp.", 14, MUTED, false);
      none.setGravity(Gravity.CENTER);
      content.addView(none, new LinearLayout.LayoutParams(-1, dp(96)));
      return;
    }
    addFavoriteGrid(content, matches);
    addInfoBanner(content, "Chạm biểu tượng lưu để bỏ yêu thích.", 22);
  }

  private void addFavoriteGrid(LinearLayout parent, List<Recipe> recipes) {
    int columns = 2;
    GridLayout grid = new GridLayout(requireContext());
    grid.setColumnCount(columns);
    for (Recipe recipe : recipes) {
      LinearLayout card = vertical();
      card.setBackground(roundRect(WHITE, BORDER, 15));
      card.setClipToOutline(true);
      FrameLayout imageFrame = new FrameLayout(requireContext());
      ImageView image = recipeImage(recipe);
      imageFrame.addView(image, new FrameLayout.LayoutParams(-1, dp(96)));
      ImageView star =
          icon(
              viewModel.isFavorite(recipe.id)
                  ? R.drawable.ic_star_filled
                  : R.drawable.ic_star_outline,
              GREEN);
      star.setBackground(roundRect(WHITE, WHITE, 18));
      FrameLayout.LayoutParams starParams =
          new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.TOP | Gravity.END);
      starParams.setMargins(0, dp(2), dp(2), 0);
      imageFrame.addView(star, starParams);
      star.setContentDescription(
          viewModel.isFavorite(recipe.id) ? "Bỏ yêu thích" : "Lưu yêu thích");
      star.setOnClickListener(v -> {
        if (viewModel.isFavorite(recipe.id)) viewModel.removeFavorite(recipe.id);
        else viewModel.addFavorite(recipe);
      });
      imageFrame.setOnClickListener(v -> host.onPersonalOpenRecipe(recipe));
      card.addView(imageFrame);
      TextView title = text(recipe.title, 13, TEXT, true);
      title.setMaxLines(1);
      title.setEllipsize(android.text.TextUtils.TruncateAt.END);
      title.setPadding(dp(9), dp(7), dp(8), 0);
      card.addView(title);
      TextView meta = text(recipe.duration + " · " + recipe.difficulty, 11, MUTED, false);
      meta.setPadding(dp(9), dp(6), dp(8), dp(11));
      card.addView(meta);
      card.setOnClickListener(v -> host.onPersonalOpenRecipe(recipe));
      GridLayout.LayoutParams params = new GridLayout.LayoutParams();
      params.width = 0;
      params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      params.setMargins(dp(4), dp(4), dp(4), dp(4));
      grid.addView(card, params);
    }
    parent.addView(grid, new LinearLayout.LayoutParams(-1, -2));
  }

  private void renderEmptyFavorites(LinearLayout content) {
    LinearLayout empty = vertical();
    empty.setGravity(Gravity.CENTER);
    content.addView(empty, new LinearLayout.LayoutParams(-1, -1));
    ImageView star = icon(R.drawable.ic_star_outline, GREEN);
    star.setPadding(dp(24), dp(24), dp(24), dp(24));
    star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 26));
    empty.addView(star, new LinearLayout.LayoutParams(dp(84), dp(84)));
    TextView title = text("Chưa lưu món nào", 22, TEXT, true);
    title.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
    tp.topMargin = dp(18);
    tp.bottomMargin = dp(8);
    empty.addView(title, tp);
    TextView hint = text("Lưu một công thức để tìm lại nhanh hơn.", 14, MUTED, false);
    hint.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
    hp.bottomMargin = dp(16);
    empty.addView(hint, hp);
    TextView explore = button("Khám phá món", true);
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(49));
    bp.setMargins(dp(24), dp(16), dp(24), 0);
    empty.addView(explore, bp);
    explore.setOnClickListener(v -> host.onPersonalNavigateTab(0));
  }

  private void addEmptyFavoriteCard(LinearLayout parent) {
    LinearLayout item = horizontalCard();
    ImageView star = icon(R.drawable.ic_star_outline, GREEN);
    star.setPadding(dp(10), dp(10), dp(10), dp(10));
    star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 13));
    item.addView(star, new LinearLayout.LayoutParams(dp(40), dp(40)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text("Chưa có món yêu thích nào", 13, TEXT, true));
    labels.addView(text("Lưu món để tìm lại nhanh hơn.", 11, MUTED, false));
    item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    item.addView(text("›", 23, GREEN, false));
    item.setOnClickListener(v -> host.onPersonalOpenFavorites());
    parent.addView(item, new LinearLayout.LayoutParams(-1, dp(62)));
  }

  private void addEmptyCollectionCard(LinearLayout parent) {
    LinearLayout card = vertical();
    card.setPadding(dp(16), dp(18), dp(16), dp(18));
    card.setMinimumHeight(dp(64));
    card.setBackground(roundRect(WHITE, BORDER, 16));
    addText(card, "Chưa có bộ sưu tập", 15, TEXT, true, 0, 0, 0, 3);
    addText(card, "Tạo bộ sưu tập đầu tiên của bạn.", 13, MUTED, false, 0, 0, 0, 0);
    card.setOnClickListener(v -> host.onPersonalCreateCollection());
    parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
  }

  private LinearLayout horizontalCard() {
    LinearLayout item = new LinearLayout(requireContext());
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setPadding(dp(9), dp(10), dp(14), dp(10));
    item.setBackground(roundRect(WHITE, BORDER, 16));
    return item;
  }

  private void addCollectionRow(LinearLayout parent, RecipeCollection collection) {
    LinearLayout item = horizontalCard();
    ImageView folder = icon(R.drawable.ic_folder_outline, GREEN);
    folder.setPadding(dp(10), dp(10), dp(10), dp(10));
    folder.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 13));
    item.addView(folder, new LinearLayout.LayoutParams(dp(40), dp(40)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(18), 0, 0, 0);
    labels.addView(text(collection.name, 14, TEXT, true));
    labels.addView(text(collection.recipeCount + " món đã lưu", 12, MUTED, false));
    item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    item.addView(text("›", 23, GREEN, false));
    item.setOnClickListener(v -> host.onPersonalOpenCollection(collection.id));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(62));
    p.bottomMargin = dp(9);
    parent.addView(item, p);
  }

  private void addHistoryRow(LinearLayout parent, Recipe recipe) {
    LinearLayout item = horizontalCard();
    ImageView image = recipeImage(recipe);
    item.addView(image, new LinearLayout.LayoutParams(dp(52), dp(52)));
    LinearLayout labels = vertical();
    labels.setPadding(dp(12), 0, 0, 0);
    labels.addView(text(recipe.title, 14, TEXT, true));
    labels.addView(text(recipe.duration + " · " + recipe.difficulty, 12, MUTED, false));
    item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    TextView star = text(viewModel.isFavorite(recipe.id) ? "★" : "☆", 20, GREEN, true);
    star.setGravity(Gravity.CENTER);
    star.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 14));
    star.setContentDescription(
        (viewModel.isFavorite(recipe.id) ? "Bỏ yêu thích " : "Thêm vào yêu thích ") + recipe.title);
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(48), dp(48));
    sp.setMarginEnd(dp(8));
    item.addView(star, sp);
    star.setOnClickListener(v -> {
      if (viewModel.isFavorite(recipe.id)) viewModel.removeFavorite(recipe.id);
      else viewModel.addFavorite(recipe);
    });
    item.addView(text("›", 23, GREEN, false));
    item.setOnClickListener(v -> host.onPersonalOpenRecipe(recipe));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(64));
    p.bottomMargin = dp(9);
    parent.addView(item, p);
  }

  private void addSectionHeader(
      LinearLayout parent, String title, String action, Runnable onClick, int top) {
    View spacer = new View(requireContext());
    parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(top == 30 ? 22 : top)));
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.addView(text(title, 19, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
    TextView link = text(action, 12, GREEN, true);
    row.addView(link);
    link.setOnClickListener(v -> onClick.run());
    parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    View gap = new View(requireContext());
    parent.addView(gap, new LinearLayout.LayoutParams(1, dp(16)));
  }

  private FrameLayout createSearchBox(String value, String hint, Consumer<String> onSearch) {
    EditText search = new EditText(requireContext());
    search.setTag("personal-favorite-search");
    search.setText(value);
    search.setTextSize(16);
    search.setTextColor(TEXT);
    search.setHintTextColor(MUTED);
    search.setSingleLine(true);
    search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    search.setHint(hint);
    search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    search.setBackgroundColor(Color.TRANSPARENT);
    search.setPadding(dp(48), dp(10), dp(12), dp(10));
    FrameLayout box = new FrameLayout(requireContext());
    box.setBackground(roundRect(WHITE, BORDER, 16));
    ImageView icon = icon(R.drawable.ic_search_outline, GREEN);
    icon.setPadding(dp(12), dp(12), dp(12), dp(12));
    box.addView(icon, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
    box.addView(search, new FrameLayout.LayoutParams(-1, -1));
    search.addTextChangedListener(new DebouncedSearchTextWatcher(search, onSearch));
    return box;
  }

  private void addInfoBanner(LinearLayout parent, String value, int top) {
    LinearLayout banner = new LinearLayout(requireContext());
    banner.setGravity(Gravity.CENTER_VERTICAL);
    banner.setPadding(dp(14), dp(10), dp(14), dp(10));
    banner.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 15));
    banner.addView(text("ⓘ", 18, GREEN, false));
    TextView content = text(value, 13, TEXT, true);
    content.setPadding(dp(12), 0, 0, 0);
    banner.addView(content);
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(48));
    bp.topMargin = dp(top);
    bp.bottomMargin = dp(12);
    parent.addView(banner, bp);
  }

  private void addBackTitle(LinearLayout parent, String title) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    ImageView back = icon(R.drawable.ic_arrow_back_outline, GREEN);
    back.setPadding(dp(12), dp(12), dp(12), dp(12));
    back.setBackground(roundRect(WHITE, WHITE, 15));
    back.setContentDescription("Quay lại");
    row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
    back.setOnClickListener(v -> host.onPersonalBack());
    TextView heading = text(title, 23, TEXT, true);
    heading.setPadding(dp(12), 0, 0, 0);
    row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    parent.addView(row, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private void addBottomNavigation() {
    BottomNavigationBar navigation =
        new BottomNavigationBar(requireContext(), 3, host::onPersonalNavigateTab);
    root.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
  }

  private TextView button(String value, boolean primary) {
    TextView b = text(value, 15, primary ? WHITE : GREEN, true);
    b.setGravity(Gravity.CENTER);
    b.setBackground(roundRect(primary ? GREEN : WHITE, primary ? GREEN : BORDER, 14));
    return b;
  }

  private ImageView recipeImage(Recipe recipe) {
    int id = R.drawable.recipe_canh_chua;
    switch (recipe.id) {
      case "sample-ga-chien-nuoc-mam":
        id = R.drawable.recipe_ga_chien;
        break;
      case "sample-mi-y-sot-bo-bam":
        id = R.drawable.recipe_mi_y;
        break;
      case "sample-com-rang-dua-bo":
        id = R.drawable.recipe_com_rang;
        break;
      case "sample-salad-rau-cu":
        id = R.drawable.recipe_salad;
        break;
    }
    ImageView image = icon(id, TEXT);
    image.setColorFilter(null);
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setBackground(roundRect(BORDER, BORDER, 12));
    image.setClipToOutline(true);
    return image;
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
    TextView t = text(value, size, color, bold);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    parent.addView(t, p);
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

  private String normalize(String value) {
    String decomposed =
        Normalizer.normalize(
            value == null ? "" : value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd');
  }
}
