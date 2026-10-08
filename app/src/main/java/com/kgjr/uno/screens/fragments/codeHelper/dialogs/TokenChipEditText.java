package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputEditText;
import com.kgjr.uno.models.sensors.SensorToken;

import java.util.regex.Matcher;

/**
 * Command box that shows {@code [source: key]} tokens as chips. The text itself is untouched;
 * chips are atomic: the caret can't land inside one, and deleting any part removes the whole token.
 */
public class TokenChipEditText extends TextInputEditText {

    private boolean updating;
    private boolean snapping;

    public TokenChipEditText(Context context) {
        super(context);
        init();
    }

    public TokenChipEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TokenChipEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updating) return;
                updating = true;
                try {
                    dropBrokenChips(s);
                    applyChips(s);
                } finally {
                    updating = false;
                }
            }
        });
    }

    /** Inserts at the caret (replacing any selection) and leaves the caret after it. */
    public void insertToken(String token) {
        Editable text = getText();
        if (text == null) {
            setText(token);
            return;
        }

        int start = Math.max(Math.min(getSelectionStart(), getSelectionEnd()), 0);
        int end = Math.max(Math.max(getSelectionStart(), getSelectionEnd()), 0);

        text.replace(start, end, token);
        setSelection(Math.min(start + token.length(), text.length()));
        requestFocus();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Chips are capped to the line width, so they need rebuilding once it is known.
        if (w != oldw) post(() -> {
            Editable text = getText();
            if (text != null) applyChips(text);
        });
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        Editable text = getText();
        if (snapping || text == null || selStart < 0 || selEnd < 0) return;

        int start = selStart;
        int end = selEnd;
        for (TokenChipSpan span : text.getSpans(0, text.length(), TokenChipSpan.class)) {
            int a = text.getSpanStart(span);
            int b = text.getSpanEnd(span);
            if (selStart == selEnd) {
                if (selStart > a && selStart < b) {
                    start = end = (selStart - a < b - selStart) ? a : b;
                }
            } else {
                if (start > a && start < b) start = a;
                if (end > a && end < b) end = b;
            }
        }

        if (start != selStart || end != selEnd) {
            snapping = true;
            setSelection(start, end);
            snapping = false;
        }
    }

    /** A chip whose text no longer matches its token was partly edited; remove what's left. */
    private void dropBrokenChips(Editable s) {
        for (TokenChipSpan span : s.getSpans(0, s.length(), TokenChipSpan.class)) {
            int a = s.getSpanStart(span);
            int b = s.getSpanEnd(span);
            s.removeSpan(span);
            if (a < 0 || b <= a) continue;
            if (!s.subSequence(a, b).toString().equals(span.token)) s.delete(a, b);
        }
    }

    private void applyChips(Editable s) {
        for (TokenChipSpan span : s.getSpans(0, s.length(), TokenChipSpan.class)) {
            s.removeSpan(span);
        }

        float maxWidth = getWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
        Matcher match = SensorToken.PATTERN.matcher(s);
        while (match.find()) {
            s.setSpan(TokenChipSpan.of(getContext(), match, maxWidth),
                    match.start(), match.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
