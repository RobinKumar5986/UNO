package com.kgjr.uno.screens.fragments.codeHelper.model;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Backslash escapes for the framing markers: {@code \n \r \t \0 \\} and {@code \xHH} for any
 * raw byte. Decoding goes straight to bytes so {@code \xFF} is one byte, not a UTF-8 pair.
 */
public final class Escapes {

    private Escapes() {
    }

    public static byte[] decode(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (text == null) return out.toByteArray();

        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int escapedByte = c == '\\' ? escapeAt(text, i) : -1;

            if (escapedByte < 0) {
                literal.append(c);
                i++;
                continue;
            }

            flush(literal, out);
            out.write(escapedByte);
            i += text.charAt(i + 1) == 'x' ? 4 : 2;
        }
        flush(literal, out);
        return out.toByteArray();
    }

    /** Null when the text is fine, otherwise a message naming the first bad escape. */
    public static String firstProblem(String text) {
        if (text == null) return null;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\\') continue;
            if (escapeAt(text, i) < 0) {
                boolean hex = i + 1 < text.length() && text.charAt(i + 1) == 'x';
                String bad = text.substring(i, Math.min(text.length(), i + (hex ? 4 : 2)));
                return "Unknown escape \"" + bad + "\". Use \\n, \\r, \\t, \\0, \\\\ or \\xHH.";
            }
            i += text.charAt(i + 1) == 'x' ? 3 : 1;
        }
        return null;
    }

    /** Makes control and non-ASCII bytes readable, for logs and previews. */
    public static String visible(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int v = b & 0xFF;
            switch (v) {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case 0: sb.append("\\0"); break;
                case '\\': sb.append("\\\\"); break;
                default:
                    if (v < 0x20 || v >= 0x7F) sb.append(String.format("\\x%02X", v));
                    else sb.append((char) v);
            }
        }
        return sb.toString();
    }

    /** The byte for the escape starting at {@code i}, or -1 when it isn't a valid one. */
    private static int escapeAt(String text, int i) {
        if (i + 1 >= text.length()) return -1;

        switch (text.charAt(i + 1)) {
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case '0': return 0;
            case '\\': return '\\';
            case 'x':
                if (i + 3 >= text.length()) return -1;
                int hi = Character.digit(text.charAt(i + 2), 16);
                int lo = Character.digit(text.charAt(i + 3), 16);
                return hi < 0 || lo < 0 ? -1 : (hi << 4) | lo;
            default:
                return -1;
        }
    }

    private static void flush(StringBuilder literal, ByteArrayOutputStream out) {
        if (literal.length() == 0) return;
        byte[] bytes = literal.toString().getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        literal.setLength(0);
    }
}
