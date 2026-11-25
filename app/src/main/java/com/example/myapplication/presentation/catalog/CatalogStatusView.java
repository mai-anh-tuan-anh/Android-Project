package com.example.myapplication.presentation.catalog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.presentation.catalog.CatalogHomeViewModel.State;
import com.example.myapplication.presentation.catalog.CatalogHomeViewModel.Status;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class CatalogStatusView extends LinearLayout {
    private static final int GREEN = Color.rgb(50, 108, 79);
    private static final int TEXT = Color.rgb(37, 53, 45);
    private static final int MUTED = Color.rgb(88, 102, 92);
    private static final int PALE = Color.rgb(232, 242, 233);
    private static final int AMBER = Color.rgb(253, 239, 218);

    public static boolean replacesHome(State state) {
        if (state == null) return true;
        switch (state.status) {
            case FIRST_LOADING:
            case FIRST_OFFLINE:
            case FIRST_FAILURE:
                return true;
            case DATABASE_ERROR:
                return state.catalog == null || state.catalog.revision == null;
            default:
                return false;
        }
    }

    public CatalogStatusView(Context context, State state, Runnable retry) {
        super(context);
        setOrientation(VERTICAL);
        Status status = state == null ? Status.FIRST_LOADING : state.status;
        if (replacesHome(state)) {
            setPadding(dp(24), dp(24), dp(24), 0);
            addView(label("Khám phá", 23, TEXT, true));
            ScrollView scroll = new ScrollView(context);
            scroll.setFillViewport(true);
            LinearLayout body = new LinearLayout(context);
            body.setOrientation(VERTICAL);
            body.setGravity(Gravity.CENTER);
            body.setPadding(dp(24), dp(16), dp(24), dp(32));
            scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
            addView(scroll, new LayoutParams(-1, 0, 1));
            ImageView icon = icon(status);
            icon.setPadding(dp(24), dp(24), dp(24), dp(24));
            icon.setBackground(surface(PALE, 24));
            body.addView(icon, new LayoutParams(dp(84), dp(84)));
            TextView title = label(title(status), 22, TEXT, true);
            title.setGravity(Gravity.CENTER);
            body.addView(title, spaced(18));
            TextView message = label(description(status), 15, MUTED, false);
            message.setGravity(Gravity.CENTER);
            body.addView(message, spaced(12));
            if (status == Status.FIRST_LOADING) body.addView(progress(), spaced(20));
            else body.addView(action("Thử lại", retry, true), spaced(40));
            return;
        }

        boolean compact = status == Status.AVAILABLE;
        setPadding(dp(14), dp(compact ? 0 : 8), dp(12), dp(compact ? 0 : 8));
        if (!compact) setBackground(surface(status == Status.OFFLINE || status == Status.REFRESH_FAILED
                ? AMBER : PALE, 15));
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        if (!compact) {
            LayoutParams iconParams = new LayoutParams(dp(20), dp(20));
            iconParams.setMarginEnd(dp(10));
            row.addView(icon(status), iconParams);
            labels.addView(label(title(status), 14, TEXT, true));
        }
        String description = description(status);
        if (state.catalog != null && state.catalog.revision != null && state.catalog.recipes.isEmpty()) {
            if (status == Status.OFFLINE) description = "Kho công thức đang trống.";
            if (status == Status.REFRESH_FAILED) description = "Kho công thức đang trống. Bạn có thể thử lại.";
        }
        if (!description.isEmpty()) labels.addView(label(description, 12, MUTED, false));
        if (state.confirmedAt > 0) {
            String confirmed = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                    Locale.forLanguageTag("vi-VN")).format(new Date(state.confirmedAt));
            labels.addView(label("Đồng bộ lần cuối · " + confirmed, 12, MUTED, false));
        }
        row.addView(labels, new LayoutParams(0, -2, 1));
        if (status != Status.REFRESHING) {
            String actionText = status == Status.AVAILABLE || status == Status.EMPTY || status == Status.NOT_LOADED
                    ? "Cập nhật" : "Thử lại";
            row.addView(action(actionText, retry, false), new LayoutParams(-2, -2));
        }
        addView(row, new LayoutParams(-1, -2));
        if (status == Status.REFRESHING) addView(progress(), spaced(6));
    }

    private static String title(Status status) {
        switch (status) {
            case FIRST_LOADING: return "Đang chuẩn bị kho món";
            case FIRST_OFFLINE:
            case NOT_LOADED: return "Chưa có công thức đã tải";
            case FIRST_FAILURE:
            case REFRESH_FAILED: return "Cập nhật chưa thành công";
            case REFRESHING: return "Đang cập nhật công thức...";
            case OFFLINE: return "Đang ngoại tuyến";
            case EMPTY: return "Kho công thức đang trống";
            case DATABASE_ERROR: return "Không thể đọc kho công thức";
            default: return "";
        }
    }

    private static String description(Status status) {
        switch (status) {
            case FIRST_LOADING: return "Món sẽ xuất hiện sau khi tải xong lần đầu.";
            case FIRST_OFFLINE: return "Kết nối Internet để tải kho món lần đầu.";
            case FIRST_FAILURE: return "Thử lại để tải kho món lần đầu.";
            case OFFLINE: return "Bạn vẫn có thể xem món đã tải.";
            case REFRESH_FAILED: return "Bạn có thể thử lại hoặc tiếp tục xem công thức đã tải.";
            case EMPTY: return "Hiện chưa có món nào được công bố.";
            case DATABASE_ERROR: return "Thử lại để mở dữ liệu trên thiết bị.";
            default: return "";
        }
    }

    private ImageView icon(Status status) {
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(status == Status.OFFLINE || status == Status.FIRST_OFFLINE
                ? R.drawable.ic_catalog_wifi : status == Status.FIRST_LOADING || status == Status.FIRST_FAILURE
                || status == Status.REFRESH_FAILED ? R.drawable.ic_catalog_refresh : R.drawable.ic_catalog_info);
        return icon;
    }

    private ProgressBar progress() {
        ProgressBar view = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        view.setIndeterminate(true);
        view.setIndeterminateTintList(ColorStateList.valueOf(GREEN));
        return view;
    }

    private TextView action(String title, Runnable action, boolean primary) {
        TextView view = label(title, 14, primary ? Color.WHITE : GREEN, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(48));
        view.setPadding(dp(12), 0, dp(12), 0);
        if (primary) view.setBackground(surface(GREEN, 14));
        view.setOnClickListener(button -> action.run());
        view.setFocusable(true);
        return view;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LayoutParams spaced(int margin) {
        LayoutParams params = new LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        return params;
    }

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
