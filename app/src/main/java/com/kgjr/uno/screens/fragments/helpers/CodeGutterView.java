package com.kgjr.uno.screens.fragments.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Map;

/** Line numbers plus fold arrows for the { } block on each line. */
public class CodeGutterView extends View {

    private CodeEditorView editor;
    private final Paint numberPaint = new Paint();
    private final Paint arrowPaint = new Paint();

    public CodeGutterView(Context context) { super(context); init(); }
    public CodeGutterView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CodeGutterView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        numberPaint.setColor(0xFF6B7580);
        numberPaint.setTextSize(28f);
        numberPaint.setTextAlign(Paint.Align.RIGHT);
        arrowPaint.setColor(0xFFCED6DF);
        arrowPaint.setTextSize(34f);
        arrowPaint.setTextAlign(Paint.Align.LEFT);
        setBackgroundColor(0xFF14181D);
    }

    public void attach(CodeEditorView editorView) {
        editor = editorView;
        editor.setScrollListener(vert -> {
            scrollTo(0, vert);
            invalidate();
        });
        editor.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { invalidate(); }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (editor == null) return;
        Layout layout = editor.getLayout();
        if (layout == null) return;

        int firstLine = layout.getLineForVertical(getScrollY());
        int lastLine = layout.getLineForVertical(getScrollY() + getHeight());
        Map<Integer, Integer> matches = editor.getBraceMatches();
        Editable text = editor.getText();

        for (int line = firstLine; line <= lastLine && line < layout.getLineCount(); line++) {
            int baseline = layout.getLineBaseline(line);
            canvas.drawText(String.valueOf(line + 1), getWidth() - 32f, baseline, numberPaint);

            Integer openIndex = foldableOpenOnLine(layout, matches, line, text);
            if (openIndex != null) {
                boolean folded = CodeFoldManager.isFolded(text, openIndex);
                canvas.drawText(folded ? "\u25B8" : "\u25BE", 4f, baseline, arrowPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (editor == null || event.getAction() != MotionEvent.ACTION_UP) return true;
        Layout layout = editor.getLayout();
        if (layout == null) return true;

        int y = (int) event.getY() + getScrollY();
        int line = layout.getLineForVertical(y);
        Map<Integer, Integer> matches = editor.getBraceMatches();
        Integer openIndex = foldableOpenOnLine(layout, matches, line, editor.getText());
        if (openIndex != null) {
            CodeFoldManager.toggleFold(editor, openIndex, matches);
        }
        return true;
    }

    /** Foldable on this line if it's already folded (now single-line), or still spans multiple lines unfolded. */
    private Integer foldableOpenOnLine(Layout layout, Map<Integer, Integer> matches, int line, Editable text) {
        int lineStart = layout.getLineStart(line);
        int lineEnd = layout.getLineEnd(line);
        for (Map.Entry<Integer, Integer> entry : matches.entrySet()) {
            int open = entry.getKey();
            if (open < lineStart || open >= lineEnd) continue;
            if (CodeFoldManager.isFolded(text, open)) return open;
            int closeLine = layout.getLineForOffset(entry.getValue());
            if (closeLine > line) return open;
        }
        return null;
    }
}