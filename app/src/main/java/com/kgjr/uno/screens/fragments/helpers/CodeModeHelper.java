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
            "// SG90 servo on digital pin 9, driven over serial. No libraries.\n"
                    + "// Commands (newline terminated):\n"
                    + "//   set-<v>   move to angle v\n"
                    + "//   inc-<v>   raise the angle by v\n"
                    + "//   dec-<v>   lower the angle by v\n"
                    + "//   get       report the current angle\n"
                    + "// Values wrap into 0..180, so a loop of inc-30 keeps sweeping.\n"
                    + "\n"
                    + "#define SERVO_PIN 9\n"
                    + "#define MAX_ANGLE 180\n"
                    + "#define MIN_PULSE 500\n"
                    + "#define MAX_PULSE 2400\n"
                    + "#define FRAME_MS 20\n"
                    + "\n"
                    + "int angle = 0;\n"
                    + "char buffer[24];\n"
                    + "byte length = 0;\n"
                    + "\n"
                    + "int wrapAngle(long value) {\n"
                    + "  long span = MAX_ANGLE + 1;\n"
                    + "  value %= span;\n"
                    + "  if (value < 0) value += span;\n"
                    + "  return (int) value;\n"
                    + "}\n"
                    + "\n"
                    + "void applyAngle(int next) {\n"
                    + "  angle = next;\n"
                    + "  Serial.print(\"OK \");\n"
                    + "  Serial.println(angle);\n"
                    + "}\n"
                    + "\n"
                    + "// One 50Hz frame: a HIGH pulse whose width picks the angle, then idle.\n"
                    + "void pulseServo() {\n"
                    + "  int pulse = MIN_PULSE + (long)(MAX_PULSE - MIN_PULSE) * angle / MAX_ANGLE;\n"
                    + "  digitalWrite(SERVO_PIN, HIGH);\n"
                    + "  delayMicroseconds(pulse);\n"
                    + "  digitalWrite(SERVO_PIN, LOW);\n"
                    + "  delay(FRAME_MS);\n"
                    + "}\n"
                    + "\n"
                    + "void handleCommand(char *command) {\n"
                    + "  while (*command == ' ') command++;\n"
                    + "\n"
                    + "  int end = 0;\n"
                    + "  while (command[end] != 0) end++;\n"
                    + "  while (end > 0 && (command[end - 1] == ' ' || command[end - 1] == '\\r')) command[--end] = 0;\n"
                    + "\n"
                    + "  for (int i = 0; i < end; i++) {\n"
                    + "    if (command[i] >= 'A' && command[i] <= 'Z') command[i] += 32;\n"
                    + "  }\n"
                    + "  if (end == 0) return;\n"
                    + "\n"
                    + "  if (strcmp(command, \"get\") == 0) {\n"
                    + "    Serial.print(\"ANGLE \");\n"
                    + "    Serial.println(angle);\n"
                    + "    return;\n"
                    + "  }\n"
                    + "\n"
                    + "  char *dash = strchr(command, '-');\n"
                    + "  if (dash == NULL) {\n"
                    + "    Serial.print(\"ERR \");\n"
                    + "    Serial.println(command);\n"
                    + "    return;\n"
                    + "  }\n"
                    + "\n"
                    + "  *dash = 0;\n"
                    + "  long value = atol(dash + 1);\n"
                    + "\n"
                    + "  if (strcmp(command, \"set\") == 0) {\n"
                    + "    applyAngle(wrapAngle(value));\n"
                    + "  } else if (strcmp(command, \"inc\") == 0) {\n"
                    + "    applyAngle(wrapAngle((long) angle + value));\n"
                    + "  } else if (strcmp(command, \"dec\") == 0) {\n"
                    + "    applyAngle(wrapAngle((long) angle - value));\n"
                    + "  } else {\n"
                    + "    Serial.print(\"ERR \");\n"
                    + "    Serial.println(command);\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "void readSerial() {\n"
                    + "  while (Serial.available()) {\n"
                    + "    char c = Serial.read();\n"
                    + "    if (c == '\\n') {\n"
                    + "      buffer[length] = 0;\n"
                    + "      handleCommand(buffer);\n"
                    + "      length = 0;\n"
                    + "    } else if (length < sizeof(buffer) - 1) {\n"
                    + "      buffer[length++] = c;\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "void setup() {\n"
                    + "  Serial.begin(9600);\n"
                    + "  pinMode(SERVO_PIN, OUTPUT);\n"
                    + "  digitalWrite(SERVO_PIN, LOW);\n"
                    + "}\n"
                    + "\n"
                    + "void loop() {\n"
                    + "  readSerial();\n"
                    + "  pulseServo();\n"
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