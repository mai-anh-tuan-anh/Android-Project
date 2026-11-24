package com.example.myapplication.core.ui;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.BaseInputConnection;
import android.widget.EditText;

import java.util.function.Consumer;

/** Applies live search only after the user pauses typing, without interrupting IME composition. */
public final class DebouncedSearchTextWatcher implements TextWatcher {
    public static final long DEFAULT_DELAY_MILLIS = 1000L;

    private final EditText input;
    private final Consumer<String> onSearch;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;

    public DebouncedSearchTextWatcher(EditText input, Consumer<String> onSearch) {
        this.input = input;
        this.onSearch = onSearch;
        input.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(android.view.View view) {}
            @Override public void onViewDetachedFromWindow(android.view.View view) { cancel(); }
        });
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable editable) {
        if (BaseInputConnection.getComposingSpanStart(editable) >= 0) return;
        cancel();
        String query = editable.toString();
        pending = () -> {
            pending = null;
            if (input.isAttachedToWindow()
                    && BaseInputConnection.getComposingSpanStart(input.getText()) < 0) {
                onSearch.accept(query);
            }
        };
        handler.postDelayed(pending, DEFAULT_DELAY_MILLIS);
    }

    private void cancel() {
        if (pending != null) handler.removeCallbacks(pending);
        pending = null;
    }
}
