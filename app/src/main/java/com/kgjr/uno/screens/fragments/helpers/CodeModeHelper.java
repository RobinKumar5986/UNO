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
            "// USB serial -> 16x2 I2C LCD (PCF8574 backpack, HD44780) using raw AVR TWI registers. No Wire.h.\n"
                    + "// Address 0x27 (try 0x3F if yours differs). Whatever text arrives over serial is shown.\n"
                    + "\n"
                    + "#include <avr/io.h>\n"
                    + "\n"
                    + "#define ADDR 0x27\n"
                    + "#define BACKLIGHT 0x08\n"
                    + "#define ENABLE 0x04\n"
                    + "#define RS_CMD 0x00\n"
                    + "#define RS_DATA 0x01\n"
                    + "\n"
                    + "char buffer[33];\n"
                    + "byte length = 0;\n"
                    + "\n"
                    + "void twiInit() {\n"
                    + "  TWSR = 0x00;\n"
                    + "  TWBR = 72;\n"
                    + "  TWCR = (1 << TWEN);\n"
                    + "}\n"
                    + "\n"
                    + "void twiStart() {\n"
                    + "  TWCR = (1 << TWINT) | (1 << TWSTA) | (1 << TWEN);\n"
                    + "  while (!(TWCR & (1 << TWINT)));\n"
                    + "}\n"
                    + "\n"
                    + "void twiStop() {\n"
                    + "  TWCR = (1 << TWINT) | (1 << TWSTO) | (1 << TWEN);\n"
                    + "  while (TWCR & (1 << TWSTO));\n"
                    + "}\n"
                    + "\n"
                    + "void twiWrite(byte data) {\n"
                    + "  TWDR = data;\n"
                    + "  TWCR = (1 << TWINT) | (1 << TWEN);\n"
                    + "  while (!(TWCR & (1 << TWINT)));\n"
                    + "}\n"
                    + "\n"
                    + "void pcfWrite(byte data) {\n"
                    + "  twiStart();\n"
                    + "  twiWrite(ADDR << 1);\n"
                    + "  twiWrite(data | BACKLIGHT);\n"
                    + "  twiStop();\n"
                    + "}\n"
                    + "\n"
                    + "void pulseEnable(byte data) {\n"
                    + "  pcfWrite(data | ENABLE);\n"
                    + "  delayMicroseconds(1);\n"
                    + "  pcfWrite(data & ~ENABLE);\n"
                    + "  delayMicroseconds(50);\n"
                    + "}\n"
                    + "\n"
                    + "void writeNibble(byte nibble, byte mode) {\n"
                    + "  byte data = (nibble & 0xF0) | mode;\n"
                    + "  pcfWrite(data);\n"
                    + "  pulseEnable(data);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdSend(byte value, byte mode) {\n"
                    + "  writeNibble(value & 0xF0, mode);\n"
                    + "  writeNibble((value << 4) & 0xF0, mode);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdCommand(byte value) {\n"
                    + "  lcdSend(value, RS_CMD);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdChar(byte value) {\n"
                    + "  lcdSend(value, RS_DATA);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdSetCursor(byte col, byte row) {\n"
                    + "  lcdCommand((row == 0 ? 0x80 : 0xC0) + col);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdClear() {\n"
                    + "  lcdCommand(0x01);\n"
                    + "  delay(2);\n"
                    + "}\n"
                    + "\n"
                    + "void lcdInit() {\n"
                    + "  delay(50);\n"
                    + "  writeNibble(0x30, RS_CMD);\n"
                    + "  delay(5);\n"
                    + "  writeNibble(0x30, RS_CMD);\n"
                    + "  delayMicroseconds(150);\n"
                    + "  writeNibble(0x30, RS_CMD);\n"
                    + "  writeNibble(0x20, RS_CMD);\n"
                    + "  lcdCommand(0x28);\n"
                    + "  lcdCommand(0x0C);\n"
                    + "  lcdCommand(0x06);\n"
                    + "  lcdClear();\n"
                    + "}\n"
                    + "\n"
                    + "void showText(char *text) {\n"
                    + "  lcdClear();\n"
                    + "  byte len = strlen(text);\n"
                    + "  lcdSetCursor(0, 0);\n"
                    + "  for (byte i = 0; i < len && i < 16; i++) lcdChar(text[i]);\n"
                    + "  if (len > 16) {\n"
                    + "    lcdSetCursor(0, 1);\n"
                    + "    for (byte i = 16; i < len && i < 32; i++) lcdChar(text[i]);\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "void readSerial() {\n"
                    + "  while (Serial.available()) {\n"
                    + "    char c = Serial.read();\n"
                    + "    if (c == '\\n') {\n"
                    + "      buffer[length] = 0;\n"
                    + "      showText(buffer);\n"
                    + "      length = 0;\n"
                    + "    } else if (c != '\\r' && length < sizeof(buffer) - 1) {\n"
                    + "      buffer[length++] = c;\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "void setup() {\n"
                    + "  Serial.begin(9600);\n"
                    + "  twiInit();\n"
                    + "  lcdInit();\n"
                    + "  showText(\"Robin\");\n"
                    + "}\n"
                    + "\n"
                    + "void loop() {\n"
                    + "  readSerial();\n"
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