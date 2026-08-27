package com.kgjr.uno.screens.fragments.helpers;

import java.util.HashMap;
import java.util.Map;

/** Matches { } pairs, skipping braces inside strings/comments. */
public final class BraceUtils {

    public static Map<Integer, Integer> buildBraceMatches(CharSequence text) {
        boolean[] skip = new boolean[text.length()];
        for (CppLexer.Token token : CppLexer.tokenize(text)) {
            if (token.type == CppLexer.TokenType.STRING
                    || token.type == CppLexer.TokenType.COMMENT
                    || token.type == CppLexer.TokenType.PREPROCESSOR) {
                for (int i = token.start; i < token.end && i < skip.length; i++) skip[i] = true;
            }
        }

        Map<Integer, Integer> matches = new HashMap<>();
        java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            if (skip[i]) continue;
            char c = text.charAt(i);
            if (c == '{') stack.push(i);
            else if (c == '}' && !stack.isEmpty()) matches.put(stack.pop(), i);
        }
        return matches;
    }

    /** Innermost brace pair that encloses cursor, or null. */
    public static Integer findEnclosingOpen(Map<Integer, Integer> matches, int cursor) {
        Integer best = null;
        for (Map.Entry<Integer, Integer> entry : matches.entrySet()) {
            int open = entry.getKey();
            int close = entry.getValue();
            if (open < cursor && cursor <= close) {
                if (best == null || open > best) best = open;
            }
        }
        return best;
    }

    private BraceUtils() {}
}