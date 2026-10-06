package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;

import java.util.List;
import java.util.regex.Matcher;

/** The values one incoming message carried, as seen by the trigger flow it fired. */
public final class ReceivedValues {

    private final PayloadFormat format;
    private final List<String> raw;

    public ReceivedValues(PayloadFormat format, List<String> raw) {
        this.format = format;
        this.raw = raw;
    }

    public boolean has(int index) {
        return index >= 0 && index < raw.size();
    }

    public boolean isText(int index) {
        return has(index) && format.types().get(index) == PayloadFormat.ValueType.TEXT;
    }

    public String text(int index) {
        return has(index) ? raw.get(index) : "";
    }

    /** Null for a text value or an index the payload doesn't have. */
    public Float number(int index) {
        if (!has(index) || isText(index)) return null;
        try {
            return Float.parseFloat(raw.get(index));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Replaces every {@code [received: varN]} with the value exactly as the board sent it. */
    public String resolveTokens(String command) {
        Matcher matcher = ReceivedVars.TOKEN.matcher(command);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            int index = ReceivedVars.indexOf(matcher);
            String value = has(index) ? raw.get(index) : matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(raw.get(i));
        }
        return sb.toString();
    }
}
