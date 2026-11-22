package com.example.myapplication.core.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;

/** Shared bottom navigation presentation used by the app's top-level destinations. */
public final class BottomNavigationBar extends LinearLayout {
  private static final int GREEN = Color.rgb(50, 108, 79);
  private static final int PALE_GREEN = Color.rgb(232, 242, 233);
  private static final int MUTED = Color.rgb(88, 102, 92);
  private static final int BORDER = Color.rgb(220, 228, 220);
  private static final int WHITE = Color.WHITE;

  private static final String[] LABELS = {"Khám phá", "Nấu hôm nay", "Mua sắm", "Kho của tôi"};
  private static final int[] ICONS = {
    R.drawable.ic_home_outline,
    R.drawable.ic_calendar_outline,
    R.drawable.ic_cart_outline,
    R.drawable.ic_star_outline
  };

  public interface OnTabSelectedListener {
    void onTabSelected(int index);
  }

  private final LinearLayout navigationStrip;

  public BottomNavigationBar(
      Context context, int selectedTab, OnTabSelectedListener onTabSelectedListener) {
    super(context);
    if (selectedTab < 0 || selectedTab >= LABELS.length) {
      throw new IllegalArgumentException("Selected tab is outside the navigation range.");
    }
    if (onTabSelectedListener == null) {
      throw new IllegalArgumentException("A tab listener is required.");
    }

    setOrientation(LinearLayout.VERTICAL);
    View divider = new View(context);
    divider.setBackgroundColor(BORDER);
    addView(divider, new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));

    navigationStrip = new LinearLayout(context);
    navigationStrip.setGravity(Gravity.CENTER_VERTICAL);
    navigationStrip.setBackgroundColor(WHITE);
    navigationStrip.setPadding(dp(7), dp(5), dp(7), dp(2));
    for (int index = 0; index < LABELS.length; index++) {
      final int tabIndex = index;
      boolean selected = index == selectedTab;
      LinearLayout tab = new LinearLayout(context);
      tab.setOrientation(LinearLayout.VERTICAL);
      tab.setGravity(Gravity.CENTER);

      ImageView icon = new ImageView(context);
      icon.setImageResource(ICONS[index]);
      icon.setColorFilter(selected ? GREEN : MUTED);
      if (selected) icon.setBackground(roundRect(PALE_GREEN, PALE_GREEN, dp(18)));
      tab.addView(icon, new LayoutParams(dp(52), dp(30)));

      TextView label = new TextView(context);
      label.setText(LABELS[index]);
      label.setTextSize(10);
      label.setTextColor(selected ? GREEN : MUTED);
      label.setTypeface(
          android.graphics.Typeface.create(
              "sans-serif", selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
      label.setGravity(Gravity.CENTER);
      tab.addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

      navigationStrip.addView(tab, new LayoutParams(0, dp(58), 1));
      tab.setOnClickListener(view -> onTabSelectedListener.onTabSelected(tabIndex));
    }
    addView(navigationStrip, new LayoutParams(LayoutParams.MATCH_PARENT, dp(64)));
  }

  /** The interactive strip, exposed for snackbar anchoring. */
  public LinearLayout getNavigationStrip() {
    return navigationStrip;
  }

  private GradientDrawable roundRect(int fill, int stroke, int radiusPx) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(fill);
    drawable.setCornerRadius(radiusPx);
    drawable.setStroke(dp(1), stroke);
    return drawable;
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
