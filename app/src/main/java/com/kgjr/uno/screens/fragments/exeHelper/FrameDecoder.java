package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerProgram;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuts the serial byte stream into messages and works out which trigger each one belongs to.
 *
 * <p>Bytes are held as ISO-8859-1 text so every byte is exactly one char and markers can be
 * searched for directly; the payload is re-read as UTF-8 before it is parsed. A message ends
 * at the earliest end marker any trigger listens for. Bytes before a trigger's start marker
 * are line noise and ignored. When one message fits several triggers, the most specific
 * format wins, then the earliest trigger.
 */
public final class FrameDecoder {

    public static final class Match {

        public final TriggerProgram program;
        public final List<String> values;

        Match(TriggerProgram program, List<String> values) {
            this.program = program;
            this.values = values;
        }
    }

    /** A board printing with no end marker in sight shouldn't grow the buffer forever. */
    private static final int MAX_BUFFER = 4096;
    private static final int KEEP_ON_OVERFLOW = 1024;

    private final List<TriggerProgram> programs;
    private final String[] starts;
    private final String[] ends;
    private final StringBuilder buffer = new StringBuilder();

    public FrameDecoder(List<TriggerProgram> programs) {
        this.programs = programs;
        this.starts = new String[programs.size()];
        this.ends = new String[programs.size()];

        for (int i = 0; i < programs.size(); i++) {
            starts[i] = latin1(programs.get(i).startMarker);
            ends[i] = latin1(programs.get(i).endMarker);
        }
    }

    /** Appends the chunk and returns every complete message it finished, in arrival order. */
    public List<Match> feed(byte[] data) {
        List<Match> matches = new ArrayList<>();
        if (data == null || data.length == 0 || programs.isEmpty()) return matches;

        buffer.append(latin1(data));

        while (true) {
            int finish = nextFinish();
            if (finish < 0) break;

            Match match = bestMatch(finish);
            if (match != null) matches.add(match);
            buffer.delete(0, finish);
        }

        if (buffer.length() > MAX_BUFFER) {
            buffer.delete(0, buffer.length() - KEEP_ON_OVERFLOW);
        }
        return matches;
    }

    public void reset() {
        buffer.setLength(0);
    }

    /** Index just past the earliest complete end marker, or -1 when no message is complete. */
    private int nextFinish() {
        int earliest = -1;
        for (String end : ends) {
            int at = buffer.indexOf(end);
            if (at < 0) continue;

            int finish = at + end.length();
            if (earliest < 0 || finish < earliest) earliest = finish;
        }
        return earliest;
    }

    private Match bestMatch(int finish) {
        Match best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < programs.size(); i++) {
            int endAt = buffer.indexOf(ends[i]);
            if (endAt < 0 || endAt + ends[i].length() != finish) continue;

            String frame = buffer.substring(0, endAt);
            int startAt = starts[i].isEmpty() ? 0 : frame.lastIndexOf(starts[i]);
            if (startAt < 0) continue;

            String payload = utf8(frame.substring(startAt + starts[i].length()));
            List<String> values = programs.get(i).format.match(payload);
            if (values == null) continue;

            int score = programs.get(i).specificity();
            if (score > bestScore) {
                bestScore = score;
                best = new Match(programs.get(i), values);
            }
        }
        return best;
    }

    private static String latin1(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static String utf8(String latin1) {
        return new String(latin1.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}
