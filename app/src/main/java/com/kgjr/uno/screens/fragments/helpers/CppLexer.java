package com.kgjr.uno.screens.fragments.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Splits C++/Arduino source into colorable tokens. Not a full parser. */
public final class CppLexer {

    public enum TokenType { KEYWORD, STRING, COMMENT, NUMBER, PREPROCESSOR }

    public static final class Token {
        public final TokenType type;
        public final int start;
        public final int end;
        Token(TokenType type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }
    }

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "void", "int", "float", "double", "char", "bool", "long", "unsigned",
            "signed", "short", "const", "static", "if", "else", "for", "while",
            "do", "switch", "case", "break", "continue", "return", "struct",
            "class", "public", "private", "protected", "namespace", "using",
            "new", "delete", "true", "false", "nullptr", "typedef", "enum",
            "sizeof", "this", "virtual", "template", "try", "catch", "throw"));

    public static List<Token> tokenize(CharSequence text) {
        List<Token> tokens = new ArrayList<>();
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);

            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                int start = i;
                while (i < n && text.charAt(i) != '\n') i++;
                tokens.add(new Token(TokenType.COMMENT, start, i));
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                int start = i;
                i += 2;
                while (i + 1 < n && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) i++;
                i = Math.min(n, i + 2);
                tokens.add(new Token(TokenType.COMMENT, start, i));
                continue;
            }
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = i;
                i++;
                while (i < n && text.charAt(i) != quote) {
                    if (text.charAt(i) == '\\') i++;
                    i++;
                }
                i = Math.min(n, i + 1);
                tokens.add(new Token(TokenType.STRING, start, i));
                continue;
            }
            if (c == '#') {
                int start = i;
                while (i < n && text.charAt(i) != '\n') i++;
                tokens.add(new Token(TokenType.PREPROCESSOR, start, i));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '.')) i++;
                tokens.add(new Token(TokenType.NUMBER, start, i));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) i++;
                if (KEYWORDS.contains(text.subSequence(start, i).toString())) {
                    tokens.add(new Token(TokenType.KEYWORD, start, i));
                }
                continue;
            }
            i++;
        }
        return tokens;
    }

    private CppLexer() {}
}