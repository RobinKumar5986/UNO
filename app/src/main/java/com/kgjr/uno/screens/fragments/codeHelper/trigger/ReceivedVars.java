package com.kgjr.uno.screens.fragments.codeHelper.trigger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The values a trigger received, exposed to its flow like a sensor: source {@code received},
 * channels {@code var1, var2, ...}, token {@code [received: var1]}.
 */
public final class ReceivedVars {

    public static final String SOURCE = "received";
    public static final String DISPLAY_NAME = "Received data";

    /** Group 1 is the 1-based variable number. */
    public static final Pattern TOKEN =
            Pattern.compile("\\[\\s*" + SOURCE + "\\s*:\\s*var(\\d+)\\s*]");

    private ReceivedVars() {
    }

    public static String key(int index) {
        return "var" + (index + 1);
    }

    public static String label(int index) {
        return "Received var " + (index + 1);
    }

    public static String token(int index) {
        return "[" + SOURCE + ": " + key(index) + "]";
    }

    public static boolean isSource(String name) {
        return SOURCE.equals(name);
    }

    /** 0-based index for a key like {@code var2}, or -1. */
    public static int indexOf(String key) {
        if (key == null || !key.startsWith("var")) return -1;
        try {
            return Integer.parseInt(key.substring(3)) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int indexOf(Matcher tokenMatch) {
        try {
            return Integer.parseInt(tokenMatch.group(1)) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
