package com.kgjr.uno.screens.fragments.helpers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CodeModeHelper {

    private static final String PREF = "editor";
    private static final String KEY_SOURCE = "source";

    public static final String DEFAULT_SKETCH =
            "// Blink the built-in LED on pin 13.\n"
                    + "\n"
                    + "#define LED_PIN LED_BUILTIN\n"
                    + "\n"
                    + "void setup() {\n"
                    + "  pinMode(LED_PIN, OUTPUT);\n"
                    + "}\n"
                    + "\n"
                    + "void loop() {\n"
                    + "  digitalWrite(LED_PIN, HIGH);\n"
                    + "  delay(500);\n"
                    + "  digitalWrite(LED_PIN, LOW);\n"
                    + "  delay(500);\n"
                    + "}\n";

    private static final List<String> SUGGESTIONS = Arrays.asList(
            "void", "int", "float", "double", "bool", "char", "const", "static",
            "if", "else", "for", "while", "return", "struct", "true", "false",
            "setup", "loop", "pinMode", "digitalWrite", "digitalRead",
            "analogWrite", "analogRead", "delay", "delayMicroseconds",
            "millis", "micros", "attachInterrupt", "Serial.begin",
            "Serial.print", "Serial.println", "HIGH", "LOW", "INPUT", "OUTPUT",
            "LED_BUILTIN");

    public static final class AiSite {
        public final String label;
        public final String url;
        public final String host;

        public AiSite(String label, String url, String host) {
            this.label = label;
            this.url = url;
            this.host = host;
        }
    }

    public static List<AiSite> aiSites() {
        return Arrays.asList(
                new AiSite("Claude", "https://claude.ai", "claude.ai"),
                new AiSite("ChatGPT", "https://chat.openai.com", "openai.com"),
                new AiSite("Gemini", "https://gemini.google.com", "gemini.google.com"));
    }

    public static List<String> suggestionsFor(String prefix) {
        List<String> result = new ArrayList<>();
        for (String candidate : SUGGESTIONS) {
            if (candidate.regionMatches(true, 0, prefix, 0, prefix.length())
                    && !candidate.equalsIgnoreCase(prefix)) {
                result.add(candidate);
            }
            if (result.size() >= 8) break;
        }
        return result;
    }

    public static String loadSource(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SOURCE, DEFAULT_SKETCH);
    }

    public static void saveSource(Context context, String source) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SOURCE, source)
                .apply();
    }

    public static boolean isAiHost(String url) {
        if (url == null) return false;
        for (AiSite site : aiSites()) {
            if (url.contains(site.host)) return true;
        }
        return false;
    }

    public static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private CodeModeHelper() {}
}