package com.example.myapplication.presentation.cooking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import com.example.myapplication.domain.model.CookingTimer;
import com.example.myapplication.domain.model.CookingTimer.Status;

import java.util.Locale;
import java.util.function.Consumer;

/** Renders a timer snapshot. The Fragment owns refresh scheduling. */
final class CookingTimerView extends LinearLayout {
    private static final int GREEN = 0xff326c4f;
    private static final int PALE = 0xffe8f2e9;
    private static final int BORDER = 0xffdce4dc;
    private static final int TEXT = 0xff25352d;
    private static final int MUTED = 0xff58665c;
    private final TextView time;
    private final TextView subtitle;
    private final TextView primary;
    private final TextView reset;
    private final TextView cancel;
    private final Ring ring;
    private Status lastStatus;

    CookingTimerView(Context context, String recipeTitle, int stepNumber, String stepTitle,
                     Consumer<Status> onPrimary, Runnable onReset, Runnable onCancel) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        add(label(recipeTitle + " · Bước " + stepNumber, 16, MUTED, true), 24);
        add(label(stepTitle, 23, TEXT, true), 8);

        FrameLayout dial = new FrameLayout(context);
        ring = new Ring(context);
        dial.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout dialContents = new LinearLayout(context);
        dialContents.setOrientation(VERTICAL);
        dialContents.setGravity(Gravity.CENTER);
        dialContents.setPadding(dp(12), 0, dp(12), 0);
        time = new AppCompatTextView(context);
        time.setGravity(Gravity.CENTER);
        time.setTextColor(0xff193b2d);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(time, 16, 36, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        time.setMaxLines(1);
        dialContents.addView(time, new LayoutParams(-1, dp(60)));
        subtitle = label("", 14, MUTED, false);
        dialContents.addView(subtitle);
        dial.addView(dialContents, new FrameLayout.LayoutParams(-1, -1));
        android.content.res.Configuration config = getResources().getConfiguration();
        int size = dp(Math.max(120, Math.min(232,
                Math.min(config.screenWidthDp - 48, config.screenHeightDp / 2))));
        LayoutParams dialParams = new LayoutParams(size, size);
        dialParams.topMargin = dp(24);
        addView(dial, dialParams);

        primary = button("", true);
        primary.setOnClickListener(view -> onPrimary.accept(lastStatus));
        reset = button("Đặt lại", false);
        reset.setOnClickListener(view -> onReset.run());
        cancel = button("Hủy hẹn giờ", false);
        cancel.setOnClickListener(view -> onCancel.run());
        add(primary, 24);
        add(reset, 8);
        add(cancel, 8);
    }

    void show(CookingTimer timer, long millis) {
        long seconds = (millis + 999) / 1000;
        String clock = seconds >= 3600
                ? String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        if (!clock.contentEquals(time.getText())) time.setText(clock);
        ring.fraction = timer.status == Status.FINISHED ? 1f : millis / (timer.durationSeconds * 1000f);
        ring.invalidate();
        if (lastStatus == timer.status) return;
        lastStatus = timer.status;
        reset.setVisibility(lastStatus == Status.READY ? GONE : VISIBLE);
        cancel.setVisibility(lastStatus == Status.FINISHED ? GONE : VISIBLE);
        String duration = "Thời lượng " + duration(timer.durationSeconds);
        switch (lastStatus) {
            case READY:
                primary.setText("Bắt đầu");
                subtitle.setText(duration);
                break;
            case RUNNING:
                primary.setText("Tạm dừng");
                subtitle.setText(duration);
                break;
            case PAUSED:
                primary.setText("Tiếp tục");
                subtitle.setText("Đang tạm dừng");
                break;
            case FINISHED:
                primary.setText("Về bước nấu");
                subtitle.setText("Đã hết giờ");
                break;
        }
    }

    static String duration(int seconds) {
        return seconds % 60 == 0 ? seconds / 60 + " phút" : seconds + " giây";
    }

    private TextView label(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String label, boolean main) {
        TextView button = label(label, 15, main ? 0xffffffff : GREEN, true);
        button.setFocusable(true);
        button.setMinHeight(dp(48));
        button.setPadding(dp(8), dp(6), dp(8), dp(6));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(main ? GREEN : 0xffffffff);
        if (!main) shape.setStroke(dp(1), BORDER);
        shape.setCornerRadius(dp(12));
        button.setBackground(shape);
        return button;
    }

    private void add(View view, int margin) {
        LayoutParams params = new LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        addView(view, params);
    }

    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    private final class Ring extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();
        private float fraction = 1f;

        Ring(Context context) { super(context); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float center = getWidth() / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PALE);
            canvas.drawCircle(center, center, center, paint);
            float inset = dp(12);
            arc.set(inset, inset, getWidth() - inset, getHeight() - inset);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(5));
            paint.setColor(GREEN);
            canvas.drawArc(arc, -90, 360 * fraction, false, paint);
        }
    }
}
