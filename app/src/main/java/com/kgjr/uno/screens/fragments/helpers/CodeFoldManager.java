package com.kgjr.uno.screens.fragments.helpers;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;

import java.util.Arrays;
import java.util.Map;

/** Collapses/expands the block after a { using FoldSpan as a live position marker. */
public final class CodeFoldManager {

    private static final String PLACEHOLDER = " \u22EF ";

    public static void toggleFold(EditText editor, int openBraceIndex, Map<Integer, Integer> matches) {
        Editable editable = editor.getText();
        int contentStart = openBraceIndex + 1;

        FoldSpan existing = findSpanAt(editable, contentStart);
        if (existing != null) {
            int start = editable.getSpanStart(existing);
            int end = editable.getSpanEnd(existing);
            editable.removeSpan(existing);
            editable.replace(start, end, existing.originalText);
            return;
        }

        Integer closeIndex = matches.get(openBraceIndex);
        if (closeIndex == null || closeIndex <= contentStart) return;

        String original = editable.subSequence(contentStart, closeIndex).toString();
        FoldSpan span = new FoldSpan(original);
        editable.replace(contentStart, closeIndex, PLACEHOLDER);
        int end = contentStart + PLACEHOLDER.length();
        editable.setSpan(span, contentStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        editable.setSpan(new BackgroundColorSpan(0x33FFFFFF), contentStart, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static boolean isFolded(Editable editable, int openBraceIndex) {
        return findSpanAt(editable, openBraceIndex + 1) != null;
    }

    /** Real source with every collapsed block restored — always compile/send this, never the placeholder text. */
    public static String expandForCompile(Editable editable) {
        FoldSpan[] spans = editable.getSpans(0, editable.length(), FoldSpan.class);
        Arrays.sort(spans, (a, b) -> editable.getSpanStart(b) - editable.getSpanStart(a));
        StringBuilder result = new StringBuilder(editable.toString());
        for (FoldSpan span : spans) {
            int start = editable.getSpanStart(span);
            int end = editable.getSpanEnd(span);
            result.replace(start, end, span.originalText);
        }
        return result.toString();
    }

    private static FoldSpan findSpanAt(Editable editable, int contentStart) {
        for (FoldSpan span : editable.getSpans(0, editable.length(), FoldSpan.class)) {
            if (editable.getSpanStart(span) == contentStart) return span;
        }
        return null;
    }

    private CodeFoldManager() {}
}