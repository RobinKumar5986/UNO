package com.kgjr.uno.screens.fragments.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** EditText with C++ syntax highlighting, an active-block indent guide, and basic autocomplete. */
public class CodeEditorView extends AppCompatEditText {

    /** Notified so the gutter can keep its scroll in sync. */
    public interface ScrollListener {
        void onEditorScrolled(int scrollY);
    }

    private Map<Integer, Integer> matches = new HashMap<>();
    private final Paint guidePaint = new Paint();
    private ScrollListener scrollListener;
    private ListPopupWindow autocompletePopup;

    public CodeEditorView(Context context) { super(context); init(); }
    public CodeEditorView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CodeEditorView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        guidePaint.setColor(0x40FFFFFF);
        guidePaint.setStrokeWidth(2f);
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                CppSyntaxHighlighter.highlight(s);
                matches = BraceUtils.buildBraceMatches(s);
                updateAutocomplete(s);
                invalidate();
            }
        });
    }

    public void setScrollListener(ScrollListener listener) { scrollListener = listener; }

    public Map<Integer, Integer> getBraceMatches() { return matches; }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        if (scrollListener != null) scrollListener.onEditorScrolled(vert);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawIndentGuide(canvas);
        super.onDraw(canvas);
    }

    private void drawIndentGuide(Canvas canvas) {
        Layout layout = getLayout();
        if (layout == null) return;
        int cursor = getSelectionStart();
        if (cursor < 0) return;
        Integer open = BraceUtils.findEnclosingOpen(matches, cursor);
        if (open == null) return;
        int close = matches.get(open);
        int openLine = layout.getLineForOffset(open);
        int closeLine = layout.getLineForOffset(close);
        if (closeLine <= openLine) return;

        float x = layout.getPrimaryHorizontal(open) + getTotalPaddingLeft();
        float top = layout.getLineBottom(openLine) + getTotalPaddingTop();
        float bottom = layout.getLineTop(closeLine) + getTotalPaddingTop();
        canvas.drawLine(x, top, x, bottom, guidePaint);
    }

    // -- basic autocomplete --------------------------------------------

    private void updateAutocomplete(Editable s) {
        int cursor = getSelectionStart();
        if (cursor <= 0) { dismissAutocomplete(); return; }
        int start = cursor;
        while (start > 0 && isWordChar(s.charAt(start - 1))) start--;
        String word = s.subSequence(start, cursor).toString();
        if (word.length() < 2) { dismissAutocomplete(); return; }

        List<String> suggestions = CodeModeHelper.suggestionsFor(word);
        if (suggestions.isEmpty()) { dismissAutocomplete(); return; }
        showAutocomplete(suggestions, start, cursor);
    }

    private void showAutocomplete(List<String> suggestions, int wordStart, int wordEnd) {
        if (autocompletePopup == null) {
            autocompletePopup = new ListPopupWindow(getContext());
            autocompletePopup.setAnchorView(this);
            autocompletePopup.setModal(false);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, suggestions);
        autocompletePopup.setAdapter(adapter);
        autocompletePopup.setWidth(400);
        autocompletePopup.setHeight(ListPopupWindow.WRAP_CONTENT);
        autocompletePopup.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = suggestions.get(position);
            getText().replace(wordStart, wordEnd, chosen);
            dismissAutocomplete();
        });
        autocompletePopup.show();
    }

    private void dismissAutocomplete() {
        if (autocompletePopup != null && autocompletePopup.isShowing()) autocompletePopup.dismiss();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused) dismissAutocomplete();
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}