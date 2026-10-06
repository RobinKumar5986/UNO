package com.kgjr.uno.screens.fragments.codeHelper.trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A printf-style payload pattern, e.g. {@code %d, %d, %f, %s}. Placeholders become typed values;
 * everything between them is a literal separator. Whitespace is forgiving: spaces around a
 * separator are optional, and a separator made only of spaces matches one or more of them.
 */
public final class PayloadFormat {

    public enum ValueType {
        INT('d', "int", "[+-]?\\d+", 3),
        FLOAT('f', "float", "[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?", 2),
        TEXT('s', "text", null, 1);

        public final char code;
        public final String label;
        final String regex;

        /** When one message parses under two formats, the stricter types win. */
        final int weight;

        ValueType(char code, String label, String regex, int weight) {
            this.code = code;
            this.label = label;
            this.regex = regex;
            this.weight = weight;
        }

        static ValueType of(char code) {
            for (ValueType type : values()) {
                if (type.code == code) return type;
            }
            return null;
        }
    }

    private final List<ValueType> types;
    private final Pattern pattern;
    private final String signature;
    private final String example;
    private final List<String> literals;
    private final int specificity;

    private PayloadFormat(List<ValueType> types, Pattern pattern, String signature,
                          String example, List<String> literals, int specificity) {
        this.types = Collections.unmodifiableList(types);
        this.pattern = pattern;
        this.signature = signature;
        this.example = example;
        this.literals = Collections.unmodifiableList(literals);
        this.specificity = specificity;
    }

    /** Throws {@link IllegalArgumentException} with a user-facing message when invalid. */
    public static PayloadFormat parse(String spec) {
        List<Object> segments = segment(spec == null ? "" : spec);

        List<ValueType> types = new ArrayList<>();
        StringBuilder regex = new StringBuilder("\\s*");
        StringBuilder signature = new StringBuilder();
        StringBuilder example = new StringBuilder();
        List<String> literals = new ArrayList<>();
        int specificity = 0;

        for (int i = 0; i < segments.size(); i++) {
            Object segment = segments.get(i);

            if (segment instanceof ValueType) {
                ValueType type = (ValueType) segment;
                boolean last = i == segments.size() - 1;
                String group = type.regex != null ? type.regex : (last ? ".*" : ".*?");

                regex.append('(').append(group).append(')');
                signature.append('%').append(type.code);
                example.append(sample(type, types.size()));
                specificity += type.weight;
                types.add(type);
                continue;
            }

            String literal = (String) segment;
            String trimmed = literal.trim().replaceAll("\\s+", " ");
            boolean edge = i == 0 || i == segments.size() - 1;
            if (trimmed.isEmpty() && edge) {
                // Already covered by the optional whitespace around the whole payload.
                continue;
            } else if (trimmed.isEmpty()) {
                regex.append("\\s+");
                signature.append(' ');
            } else {
                regex.append("\\s*").append(flexibleQuote(trimmed)).append("\\s*");
                signature.append(trimmed);
                literals.add(trimmed);
                specificity += trimmed.replace(" ", "").length();
            }
            example.append(literal);
        }
        regex.append("\\s*");

        if (types.isEmpty()) {
            throw new IllegalArgumentException("Add at least one value: %d, %f or %s.");
        }
        return new PayloadFormat(types, Pattern.compile(regex.toString(), Pattern.DOTALL),
                signature.toString(), example.toString(), literals, specificity);
    }

    /** Null when the spec parses, otherwise why it doesn't. */
    public static String problemOf(String spec) {
        try {
            parse(spec);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** The raw text of each value, or null when the payload doesn't fit this format. */
    public List<String> match(String payload) {
        if (payload == null) return null;

        Matcher matcher = pattern.matcher(payload);
        if (!matcher.matches()) return null;

        List<String> values = new ArrayList<>(types.size());
        for (int i = 1; i <= matcher.groupCount(); i++) {
            values.add(matcher.group(i).trim());
        }
        return values;
    }

    public List<ValueType> types() {
        return types;
    }

    public int size() {
        return types.size();
    }

    /** Whitespace-insensitive form, so {@code %d,%d} and {@code %d , %d} count as the same. */
    public String signature() {
        return signature;
    }

    /** A payload that would match, for previews. */
    public String example() {
        return example;
    }

    /** The non-blank separators, e.g. {@code [","]} for {@code %d, %d}. */
    public List<String> literals() {
        return literals;
    }

    public int specificity() {
        return specificity;
    }

    /** Splits the spec into ValueType and String literal segments. */
    private static List<Object> segment(String spec) {
        List<Object> segments = new ArrayList<>();
        StringBuilder literal = new StringBuilder();

        for (int i = 0; i < spec.length(); i++) {
            char c = spec.charAt(i);
            if (c != '%') {
                literal.append(c);
                continue;
            }
            if (i + 1 >= spec.length()) {
                throw new IllegalArgumentException("A % at the end needs a type: %d, %f or %s.");
            }

            char code = spec.charAt(++i);
            if (code == '%') {
                literal.append('%');
                continue;
            }

            ValueType type = ValueType.of(code);
            if (type == null) {
                throw new IllegalArgumentException(
                        "Unknown placeholder %" + code + ". Use %d, %f or %s.");
            }

            if (literal.length() > 0) {
                segments.add(literal.toString());
                literal.setLength(0);
            } else if (!segments.isEmpty() && segments.get(segments.size() - 1) instanceof ValueType) {
                throw new IllegalArgumentException(
                        "Put a separator between values, e.g. %d,%d instead of %d%d.");
            }
            segments.add(type);
        }
        if (literal.length() > 0) segments.add(literal.toString());
        return segments;
    }

    private static String flexibleQuote(String literal) {
        String[] words = literal.split(" ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) out.append("\\s+");
            out.append(Pattern.quote(words[i]));
        }
        return out.toString();
    }

    private static String sample(ValueType type, int index) {
        switch (type) {
            case INT: return String.valueOf(12 + index * 11);
            case FLOAT: return (index + 1) + ".5";
            default: return "on";
        }
    }
}
