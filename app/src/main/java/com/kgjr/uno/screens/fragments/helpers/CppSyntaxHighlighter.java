package com.kgjr.uno.screens.fragments.helpers;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.List;

/** Applies color spans to an Editable based on CppLexer tokens. */
public final class CppSyntaxHighlighter {

    private static final class SyntaxSpan extends ForegroundColorSpan {
        SyntaxSpan(int color) { super(color); }
    }

    public static void highlight(Editable editable) {
        for (SyntaxSpan span : editable.getSpans(0, editable.length(), SyntaxSpan.class)) {
            editable.removeSpan(span);
        }
        List<CppLexer.Token> tokens = CppLexer.tokenize(editable);
        for (CppLexer.Token token : tokens) {
            editable.setSpan(new SyntaxSpan(colorFor(token.type)), token.start, token.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static int colorFor(CppLexer.TokenType type) {
        switch (type) {
            case KEYWORD: return 0xFF6BB8FF;
            case STRING: return 0xFFCE9178;
            case COMMENT: return 0xFF6A9955;
            case NUMBER: return 0xFFB5CEA8;
            case PREPROCESSOR: return 0xFFC586C0;
            default: return 0xFFD6E2EA;
        }
    }

    private CppSyntaxHighlighter() {}
}