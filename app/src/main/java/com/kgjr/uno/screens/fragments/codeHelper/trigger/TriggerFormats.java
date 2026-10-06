package com.kgjr.uno.screens.fragments.codeHelper.trigger;

import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerFlow;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerStartData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Validation and identity of trigger receive formats. */
public final class TriggerFormats {

    private TriggerFormats() {
    }

    public static String label(int index) {
        return "Trigger " + (index + 1);
    }

    /** Null when the format is usable, otherwise why it isn't. */
    public static String problemOf(TriggerStartData data) {
        String start = Escapes.firstProblem(data.startMarker);
        if (start != null) return "Start marker: " + start;

        String end = Escapes.firstProblem(data.endMarker);
        if (end != null) return "End marker: " + end;
        if (Escapes.decode(data.endMarker).length == 0) {
            return "Set an end marker so the app knows where a message stops.";
        }

        String payload = PayloadFormat.problemOf(data.payload);
        if (payload != null) return "Payload: " + payload;

        String sendStart = Escapes.firstProblem(data.sendStartMarker);
        if (sendStart != null) return "Send start marker: " + sendStart;

        String sendEnd = Escapes.firstProblem(data.sendEndMarker);
        return sendEnd == null ? null : "Send end marker: " + sendEnd;
    }

    /**
     * Start marker, payload and end marker as one comparable string, or null when invalid.
     * Markers are compared by their bytes, so {@code \n} and {@code \x0A} are the same.
     */
    public static String signatureOf(TriggerStartData data) {
        if (problemOf(data) != null) return null;

        return Escapes.visible(Escapes.decode(data.startMarker))
                + '\u0001' + PayloadFormat.parse(data.payload).signature()
                + '\u0001' + Escapes.visible(Escapes.decode(data.endMarker));
    }

    /** Index of another trigger listening for exactly the same format, or -1. */
    public static int conflictIndex(TriggerFlow self, List<TriggerFlow> all) {
        String signature = signatureOf(self.receive());
        if (signature == null) return -1;

        for (int i = 0; i < all.size(); i++) {
            TriggerFlow other = all.get(i);
            if (other != self && signature.equals(signatureOf(other.receive()))) return i;
        }
        return -1;
    }

    /**
     * Null when this trigger can be told apart from every other one. Besides an identical
     * format, a stream is ambiguous when another trigger's end marker shows up inside this
     * one's start marker or separators: the decoder would end the message there.
     */
    public static String conflictMessage(TriggerFlow self, List<TriggerFlow> all) {
        int same = conflictIndex(self, all);
        if (same >= 0) {
            return label(same) + " already listens for this exact format. Change a marker or the payload.";
        }

        TriggerStartData mine = self.receive();
        if (problemOf(mine) != null) return null;

        List<String> inside = new ArrayList<>();
        inside.add(latin1(Escapes.decode(mine.startMarker)));
        for (String literal : PayloadFormat.parse(mine.payload).literals()) {
            inside.add(latin1(literal.getBytes(StandardCharsets.UTF_8)));
        }

        // Includes itself: "%d,%d" ending in "," is just as ambiguous.
        for (int i = 0; i < all.size(); i++) {
            TriggerFlow other = all.get(i);
            if (problemOf(other.receive()) != null) continue;

            byte[] otherEnd = Escapes.decode(other.receive().endMarker);
            String end = latin1(otherEnd);
            for (String text : inside) {
                if (text.contains(end)) {
                    String owner = other == self ? "The end marker" : label(i) + "'s end marker";
                    return owner + " \"" + Escapes.visible(otherEnd)
                            + "\" also appears in this format, so messages would be cut short.";
                }
            }
        }
        return null;
    }

    private static String latin1(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
