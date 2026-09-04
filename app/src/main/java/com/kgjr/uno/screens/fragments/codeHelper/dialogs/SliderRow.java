package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kgjr.uno.R;

/**
 * One labelled slider row, appended to a dialog's content column.
 *
 * <p>The value chip is an {@link EditText}: dragging the thumb writes into it, and typing into
 * it moves the thumb. Both directions are guarded by a re-entrancy flag so the two views never
 * bounce updates off each other.
 */
class SliderRow {

    interface OnValueChanged {
        void onValue(int value);
    }

    static void add(LinearLayout parent, String label, int max, int value, OnValueChanged callback) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_slider_row, parent, false);

        ((TextView) row.findViewById(R.id.slider_label)).setText(label);
        EditText valueView = row.findViewById(R.id.slider_value);
        SeekBar seekBar = row.findViewById(R.id.slider_seek);

        seekBar.setMax(max);
        seekBar.setProgress(value);
        valueView.setText(String.valueOf(value));
        // Typing more digits than max can hold is always a correction we'd have to undo.
        valueView.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(String.valueOf(max).length())});

        // Set while one view is pushing its value into the other.
        final boolean[] syncing = {false};

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                callback.onValue(progress);
                if (syncing[0]) return;
                syncing[0] = true;
                valueView.setText(String.valueOf(progress));
                // Keep the caret valid; the field may be focused while the user drags.
                valueView.setSelection(valueView.getText().length());
                syncing[0] = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        valueView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (syncing[0]) return;
                // An empty field is a half-finished edit, not a zero — leave it alone until the
                // user commits, so backspacing doesn't yank the thumb to the far left.
                if (s.length() == 0) return;

                int typed;
                try {
                    typed = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    return;
                }
                int clamped = Math.max(0, Math.min(max, typed));

                syncing[0] = true;
                seekBar.setProgress(clamped);
                if (clamped != typed) {
                    // Only rewrite the field when we actually had to clamp it.
                    valueView.setText(String.valueOf(clamped));
                    valueView.setSelection(valueView.getText().length());
                }
                syncing[0] = false;

                callback.onValue(clamped);
            }
        });

        valueView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit(valueView, seekBar, max, callback, syncing);
                valueView.clearFocus();
                hideKeyboard(valueView);
                return true;
            }
            return false;
        });

        valueView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) commit(valueView, seekBar, max, callback, syncing);
        });

        parent.addView(row);
    }

    /** Normalises whatever is in the field — including an empty one — back to the slider value. */
    private static void commit(EditText valueView, SeekBar seekBar, int max,
                               OnValueChanged callback, boolean[] syncing) {
        int resolved;
        try {
            resolved = Integer.parseInt(valueView.getText().toString());
        } catch (NumberFormatException e) {
            resolved = seekBar.getProgress();
        }
        resolved = Math.max(0, Math.min(max, resolved));

        syncing[0] = true;
        valueView.setText(String.valueOf(resolved));
        seekBar.setProgress(resolved);
        syncing[0] = false;

        callback.onValue(resolved);
    }

    private static void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager)
                view.getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private SliderRow() {
    }
}
