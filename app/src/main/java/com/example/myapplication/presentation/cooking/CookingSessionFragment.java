package com.example.myapplication.presentation.cooking;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BepNhaApplication;
import com.example.myapplication.R;
import com.example.myapplication.di.AppViewModelFactory;
import com.example.myapplication.domain.model.CookingSession;
import com.example.myapplication.domain.model.CookingSession.IngredientLine;
import com.example.myapplication.domain.model.CookingTimer;

import java.math.RoundingMode;
import java.util.Locale;

/** Minimal cooking UI for validating the session and navigation. */
public final class CookingSessionFragment extends Fragment {
    public interface Host {
        void onCookingSessionExit(boolean discover);
    }

    private static final int CREAM = 0xfffaf9f6;
    private static final int GREEN = 0xff326c4f;
    private static final int PALE = 0xffe8f2e9;
    private static final int TEXT = 0xff25352d;
    private static final int MUTED = 0xff58665c;
    private static final int WHITE = 0xffffffff;
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final String TIMER_OPEN = "timerOpen";
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = this::updateTimerUi;
    private CookingSessionViewModel model;
    private LinearLayout root;
    private AlertDialog cancelDialog;
    private boolean timerOpen;
    private CookingTimerView timerView;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        timerOpen = state != null && state.getBoolean(TIMER_OPEN);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        out.putBoolean(TIMER_OPEN, timerOpen);
        super.onSaveInstanceState(out);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(requireActivity(),
                new AppViewModelFactory((BepNhaApplication) requireActivity().getApplication()))
                .get(CookingSessionViewModel.class);
        root = column();
        root.setBackgroundColor(CREAM);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left + dp(24), bars.top + dp(8), bars.right + dp(24), bars.bottom + dp(12));
            return insets;
        });
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model.state().observe(getViewLifecycleOwner(), state -> {
            render(state);
            updateTimerUi();
        });
    }

    public void handleBack() {
        if (timerOpen) {
            showTimer(false);
            return;
        }
        CookingSessionViewModel.State state = model.state().getValue();
        CookingSession session = state == null ? null : state.session;
        if (session == null) exit(false);
        else if (session.status == CookingSession.Status.ACTIVE) model.pause();
        else if (session.status == CookingSession.Status.PAUSED) model.resume();
        else exit(false);
    }

    private void render(CookingSessionViewModel.State state) {
        timerView = null;
        if (state == null) return;
        root.removeAllViews();
        CookingSession session = state.session;
        if (timerOpen && model.currentTimer() != null) {
            renderTimer(session);
            return;
        }
        timerOpen = false;
        String title = "Nấu từng bước";
        if (session != null) {
            if (session.status == CookingSession.Status.PAUSED) title = "Tiếp tục nấu";
            else if (session.status == CookingSession.Status.COMPLETED) title = "Hoàn thành";
            else title = session.snapshot.recipeTitle;
        }
        header(title, session != null && session.status == CookingSession.Status.ACTIVE);
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout body = column();
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (session == null) {
            add(body, label(state.failure == null ? "Đang chuẩn bị phiên nấu…" : failureMessage(state.failure),
                    16, TEXT, false), 20);
            if (state.failure != null) add(root, button("Xem lại công thức", true, () -> exit(false)), 8);
            return;
        }

        switch (session.status) {
            case ACTIVE:
                showStep(body, session);
                break;
            case PAUSED:
                add(body, label(session.snapshot.recipeTitle, 25, TEXT, true), 22);
                progress(body, session);
                String resumeLabel = session.stepIndex < session.completedSteps ? "Bước đang xem" : "Bước tiếp theo";
                add(body, label(resumeLabel, 17, TEXT, true), 20);
                add(body, label(session.currentStep().source.title, 16, MUTED, false), 4);
                add(root, button("Tiếp tục nấu", true, model::resume), 8);
                add(root, button("Kết thúc phiên nấu", false, this::confirmCancel), 8);
                break;
            case COMPLETED:
                add(body, label("Món đã sẵn sàng!", 26, TEXT, true), 22);
                add(body, label(session.snapshot.recipeTitle + " · " + session.snapshot.servings + " người",
                        16, MUTED, false), 8);
                progress(body, session);
                add(root, button("Về Khám phá", true, () -> exit(true)), 8);
                add(root, button("Xem lại công thức", false, () -> exit(false)), 8);
                break;
            case CANCELED:
                break;
        }
    }

    private void showStep(LinearLayout body, CookingSession session) {
        CookingSession.Step step = session.currentStep();
        add(body, label(session.snapshot.servings + " người", 15, MUTED, false), 10);
        add(body, label("Bước " + (session.stepIndex + 1) + " / " + session.snapshot.steps.size(),
                17, TEXT, true), 6);
        progress(body, session);
        add(body, label(step.source.title, 25, TEXT, true), 20);
        add(body, label(step.source.instruction, 17, TEXT, false), 10);
        add(body, label("Nguyên liệu cho bước này", 18, TEXT, true), 20);
        if (!step.ingredients.isEmpty()) {
            add(body, label("Định lượng theo cả công thức", 14, MUTED, false), 6);
            for (IngredientLine line : step.ingredients) {
                String name = line.name == null || line.name.trim().isEmpty() ? line.source.ingredientId : line.name;
                String amount = line.amount == null ? line.source.qualitativeNote
                        : line.amount.formatForDisplay(RoundingMode.HALF_UP, VIETNAMESE);
                if (amount == null || amount.trim().isEmpty()) amount = "Chưa có định lượng";
                add(body, label(name + " · " + amount, 16, TEXT, false), 8);
            }
        }
        if (step.source.recipeIngredientIds.isEmpty()) {
            add(body, label("Chưa có nguyên liệu được liên kết cho bước này.", 14, MUTED, false), 6);
        } else if (step.ingredients.size() < step.source.recipeIngredientIds.size()) {
            add(body, label("Một số nguyên liệu của bước chưa có dữ liệu.", 14, MUTED, false), 6);
        }
        CookingSession.TimerInput timer = session.timerInput();
        if (timer != null) {
            add(body, button("Hẹn giờ " + CookingTimerView.duration(timer.durationSeconds),
                    false, () -> showTimer(true)), 18);
        }

        LinearLayout controls = new LinearLayout(requireContext());
        TextView previous = button("Bước trước", false, model::previous);
        previous.setEnabled(session.stepIndex > 0);
        previous.setAlpha(previous.isEnabled() ? 1f : 0.55f);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        left.setMarginEnd(dp(8));
        controls.addView(previous, left);
        TextView next = session.isLastStep() ? button("Hoàn thành món", true, model::complete)
                : button("Hoàn thành bước", true, model::next);
        controls.addView(next, new LinearLayout.LayoutParams(0, -2, 1.35f));
        add(root, controls, 8);
    }

    private void renderTimer(CookingSession session) {
        header("Hẹn giờ nấu", false);
        ScrollView scroll = new ScrollView(requireContext());
        timerView = new CookingTimerView(requireContext(), session.snapshot.recipeTitle, session.stepIndex + 1,
                session.currentStep().source.title, status -> {
                    switch (status) {
                        case READY:
                        case PAUSED: model.startOrResumeTimer(); break;
                        case RUNNING: model.pauseTimer(); break;
                        case FINISHED: showTimer(false); return;
                    }
                    updateTimerUi();
                }, () -> {
                    model.resetTimer();
                    updateTimerUi();
                }, () -> {
                    model.resetTimer();
                    showTimer(false);
                });
        scroll.addView(timerView);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showTimer(boolean open) {
        timerOpen = open;
        render(model.state().getValue());
        updateTimerUi();
    }

    private void updateTimerUi() {
        timerHandler.removeCallbacks(timerTick);
        if (root == null || !isResumed()) return;
        boolean running = model.refreshTimers();
        CookingTimer timer = model.currentTimer();
        if (timerView != null && timer != null) timerView.show(timer, model.remainingMillis(timer));
        CookingTimer finished;
        while ((finished = model.takeTimerCompletion()) != null) announceTimerCompletion(finished);
        if (running) timerHandler.postDelayed(timerTick, 250);
    }

    private void announceTimerCompletion(CookingTimer finished) {
        CookingSession session = model.state().getValue().session;
        for (int step = 0; step < session.snapshot.steps.size(); step++) {
            if (finished.stepId.equals(session.snapshot.steps.get(step).source.stepId)) {
                Toast.makeText(requireContext(), "Đã hết giờ · Bước " + (step + 1), Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
        updateTimerUi();
    }

    @Override public void onPause() {
        timerHandler.removeCallbacks(timerTick);
        super.onPause();
    }

    private void progress(LinearLayout body, CookingSession session) {
        int total = session.snapshot.steps.size();
        add(body, label("Đã hoàn thành " + session.completedSteps + " trong " + total + " bước",
                14, MUTED, false), 8);
        ProgressBar bar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(total);
        bar.setProgress(session.completedSteps);
        bar.setProgressTintList(ColorStateList.valueOf(GREEN));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(PALE));
        add(body, bar, 8);
    }

    private void header(String title, boolean canPause) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = new ImageButton(requireContext());
        back.setImageResource(R.drawable.ic_arrow_back_outline);
        back.setImageTintList(ColorStateList.valueOf(GREEN));
        back.setBackground(surface(WHITE));
        back.setContentDescription("Quay lại");
        back.setOnClickListener(v -> handleBack());
        row.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView titleView = label(title, 21, TEXT, true);
        titleView.setPadding(dp(12), 0, dp(8), 0);
        row.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        if (canPause) row.addView(button("Tạm dừng", false, model::pause));
        add(root, row, 0);
    }

    private void confirmCancel() {
        if (cancelDialog != null) return;
        cancelDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Kết thúc phiên nấu?")
                .setMessage("Tiến độ của phiên này sẽ kết thúc.")
                .setNegativeButton("Tiếp tục nấu", null)
                .setPositiveButton("Kết thúc", (dialog, which) -> {
                    model.cancel();
                    exit(false);
                })
                .create();
        cancelDialog.setOnDismissListener(dialog -> cancelDialog = null);
        cancelDialog.show();
    }

    private void exit(boolean discover) {
        ((Host) requireActivity()).onCookingSessionExit(discover);
    }

    @Override public void onDestroyView() {
        timerHandler.removeCallbacks(timerTick);
        timerView = null;
        if (cancelDialog != null) cancelDialog.dismiss();
        root = null;
        super.onDestroyView();
    }

    private static String failureMessage(CookingSessionViewModel.Failure failure) {
        switch (failure) {
            case REMOVED: return "Công thức đã được gỡ";
            case MISSING: return "Không tìm thấy công thức";
            case NO_STEPS: return "Công thức chưa có bước nấu.";
            case INVALID_SERVINGS: return "Không thể bắt đầu với khẩu phần đã chọn.";
            default: return "Không thể đọc kho công thức.";
        }
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(requireContext());
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private TextView button(String text, boolean primary, Runnable action) {
        TextView view = label(text, 14, primary ? WHITE : GREEN, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(48));
        view.setMinWidth(dp(48));
        view.setPadding(dp(8), dp(6), dp(8), dp(6));
        view.setBackground(surface(primary ? GREEN : PALE));
        view.setOnClickListener(v -> action.run());
        view.setFocusable(true);
        return view;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void add(LinearLayout parent, View child, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        parent.addView(child, params);
    }

    private GradientDrawable surface(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(12));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
