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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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
import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.model.Recipe;
import com.example.myapplication.domain.model.RecipeCollection;
import com.example.myapplication.presentation.cookingjournal.CookingJournalViewModel;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Owns the local-library landing page and favorites screen. */
public final class PersonalDataFragment extends Fragment {
  private static final String ARG_SECTION = "personal.section";
  private static final String STATE_SECTION = "personal.fragment.section";
  private static final String STATE_QUERY = "personal.fragment.query";
  private static final String LIBRARY = "LIBRARY", FAVORITES = "FAVORITES", COLLECTIONS = "COLLECTIONS";
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

    void onPersonalOpenCollection(long collectionId);

    void onPersonalOpenRecipe(Recipe recipe);

    void onPersonalOpenNote(Recipe recipe);

    void onPersonalOpenJournal();

    void onPersonalNavigateTab(int tab);

    void onPersonalBack();
  }

  private PersonalDataViewModel viewModel;
  private CookingJournalViewModel journalViewModel;
  private Host host;
  private String section = LIBRARY;
  private String searchQuery = "";
  private LinearLayout root;

  public static PersonalDataFragment newInstance(boolean favorites) {
    return newInstance(favorites ? FAVORITES : LIBRARY);
  }

  public static PersonalDataFragment newInstance(String section) {
    PersonalDataFragment f = new PersonalDataFragment();
    Bundle b = new Bundle();
    b.putString(ARG_SECTION, section);
    f.setArguments(b);
    return f;
  }

  public void showSection(String section) {
    this.section = section;
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
    } else if (getArguments() != null) section = getArguments().getString(ARG_SECTION, LIBRARY);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    out.putString(STATE_SECTION, section);
    out.putString(STATE_QUERY, searchQuery);
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
                if (FAVORITES.equals(section) && searchInputFocused()) return;
                render();
              }
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
    else renderLibrary();
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
              new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                  searchQuery = s.toString();
                  populateFavorites(content, favorites);
                }

                @Override
                public void afterTextChanged(Editable s) {}
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
    card.setBackground(roundRect(WHITE, BORDER, 16));
    addText(card, "Chưa có bộ sưu tập", 15, TEXT, true, 0, 0, 0, 3);
    addText(card, "Tạo bộ sưu tập đầu tiên của bạn.", 13, MUTED, false, 0, 0, 0, 0);
    card.setOnClickListener(v -> host.onPersonalCreateCollection());
    parent.addView(card, new LinearLayout.LayoutParams(-1, dp(64)));
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
    item.setOnClickListener(v -> host.onPersonalOpenNote(recipe));
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

  private FrameLayout createSearchBox(String value, String hint, TextWatcher watcher) {
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
    search.addTextChangedListener(watcher);
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
    LinearLayout nav = new LinearLayout(requireContext());
    nav.setGravity(Gravity.CENTER_VERTICAL);
    nav.setBackgroundColor(WHITE);
    nav.setPadding(dp(7), dp(5), dp(7), dp(2));
    View line = new View(requireContext());
    line.setBackgroundColor(BORDER);
    root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    String[] names = {"Khám phá", "Nấu hôm nay", "Mua sắm", "Kho của tôi"};
    int[] ids = {
      R.drawable.ic_home_outline,
      R.drawable.ic_calendar_outline,
      R.drawable.ic_cart_outline,
      R.drawable.ic_star_outline
    };
    for (int i = 0; i < names.length; i++) {
      final int index = i;
      LinearLayout tab = vertical();
      tab.setGravity(Gravity.CENTER);
      ImageView icon = icon(ids[i], i == 3 ? GREEN : MUTED);
      if (i == 3) icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, 18));
      tab.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(30)));
      TextView label = text(names[i], 10, i == 3 ? GREEN : MUTED, i == 3);
      label.setGravity(Gravity.CENTER);
      tab.addView(label);
      nav.addView(tab, new LinearLayout.LayoutParams(0, dp(58), 1));
      tab.setOnClickListener(v -> host.onPersonalNavigateTab(index));
    }
    root.addView(nav, new LinearLayout.LayoutParams(-1, dp(64)));
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
