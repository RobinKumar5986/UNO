package com.kgjr.uno;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerFlow;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerStartData;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerFormats;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerProgram;
import com.kgjr.uno.screens.fragments.exeHelper.FrameDecoder;
import com.kgjr.uno.screens.fragments.exeHelper.ReceivedValues;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriggerDecodingTest {

    @Test
    public void escapesDecodeToBytes() {
        assertArrayEquals(new byte[]{'\r', '\n'}, Escapes.decode("\\r\\n"));
        assertArrayEquals(new byte[]{(byte) 0xFF, 2}, Escapes.decode("\\xFF\\x02"));
        assertNotNull(Escapes.firstProblem("\\x4"));
        assertNull(Escapes.firstProblem("<\\n"));
    }

    @Test
    public void formatParsesTypedValues() {
        PayloadFormat format = PayloadFormat.parse("%d , %d , %f, %s");

        assertEquals(Arrays.asList("12", "-3", "4.5", "hello world"),
                format.match("12,-3 , 4.5,hello world"));
        assertNull(format.match("12,3,4.5"));
        assertNull(format.match("1.5,3,4.5,x"));
    }

    @Test
    public void whitespaceSeparatorNeedsAtLeastOneSpace() {
        PayloadFormat format = PayloadFormat.parse("%d %d");

        assertEquals(Arrays.asList("1", "2"), format.match("1   2"));
        assertNull(format.match("12"));
    }

    @Test
    public void invalidFormatsAreRejected() {
        assertNotNull(PayloadFormat.problemOf("%d%d"));
        assertNotNull(PayloadFormat.problemOf("%x"));
        assertNotNull(PayloadFormat.problemOf("no values"));
        assertNotNull(PayloadFormat.problemOf("%d,%"));
        assertNull(PayloadFormat.problemOf("T=%d%%"));
    }

    @Test
    public void signatureIgnoresWhitespace() {
        assertEquals(PayloadFormat.parse("%d,%d").signature(),
                PayloadFormat.parse("%d , %d").signature());
        assertNotEquals(PayloadFormat.parse("%d,%d").signature(),
                PayloadFormat.parse("%d;%d").signature());
    }

    @Test
    public void decoderSplitsFramesAcrossChunks() {
        TriggerProgram single = program("", "\\n", "%d");
        FrameDecoder decoder = new FrameDecoder(list(single));

        assertEquals(0, decoder.feed(bytes("4")).size());
        List<FrameDecoder.Match> matches = decoder.feed(bytes("2\n7\n"));

        assertEquals(2, matches.size());
        assertEquals(Arrays.asList("42"), matches.get(0).values);
        assertEquals(Arrays.asList("7"), matches.get(1).values);
    }

    @Test
    public void decoderRoutesByStartMarkerAndSkipsNoise() {
        TriggerProgram temp = program("T:", "\\n", "%f");
        TriggerProgram button = program("B:", "\\n", "%d,%s");
        FrameDecoder decoder = new FrameDecoder(list(temp, button));

        List<FrameDecoder.Match> matches = decoder.feed(bytes("junk T:21.5\nB:2,down\nxx\n"));

        assertEquals(2, matches.size());
        assertEquals(temp, matches.get(0).program);
        assertEquals(Arrays.asList("21.5"), matches.get(0).values);
        assertEquals(button, matches.get(1).program);
        assertEquals(Arrays.asList("2", "down"), matches.get(1).values);
    }

    @Test
    public void stricterFormatWinsWhenBothMatch() {
        TriggerProgram anyText = program("", "\\n", "%s");
        TriggerProgram decimal = program("", "\\n", "%f");
        TriggerProgram whole = program("", "\\n", "%d");
        FrameDecoder decoder = new FrameDecoder(list(anyText, decimal, whole));

        List<FrameDecoder.Match> matches = decoder.feed(bytes("5\n2.5\nhi\n"));

        assertEquals(whole, matches.get(0).program);
        assertEquals(decimal, matches.get(1).program);
        assertEquals(anyText, matches.get(2).program);
    }

    @Test
    public void differentEndMarkersCutAtTheEarliest() {
        TriggerProgram crlf = program("", "\\r\\n", "%d");
        TriggerProgram semi = program("", ";", "%d");
        FrameDecoder decoder = new FrameDecoder(list(crlf, semi));

        List<FrameDecoder.Match> matches = decoder.feed(bytes("3;10\r\n"));

        assertEquals(semi, matches.get(0).program);
        assertEquals(crlf, matches.get(1).program);
        assertEquals(Arrays.asList("10"), matches.get(1).values);
    }

    @Test
    public void longerEndMarkerWinsATie() {
        TriggerProgram lf = program("", "\\n", "%d");
        TriggerProgram crlf = program("", "\\r\\n", "%d");
        FrameDecoder decoder = new FrameDecoder(list(lf, crlf));

        assertEquals(crlf, decoder.feed(bytes("5\r\n")).get(0).program);
        assertEquals(lf, decoder.feed(bytes("6\n")).get(0).program);
    }

    @Test
    public void formatsMustBeDistinguishable() {
        TriggerFlow a = trigger("", "\\n", "%d");
        TriggerFlow b = trigger("", "\\n", "%d ");
        List<TriggerFlow> all = Arrays.asList(a, b);
        assertNotNull(TriggerFormats.conflictMessage(a, all));

        b.receive().startMarker = "B:";
        assertNull(TriggerFormats.conflictMessage(a, all));
        assertNull(TriggerFormats.conflictMessage(b, all));

        TriggerFlow comma = trigger("", ",", "%d");
        TriggerFlow pair = trigger("", "\\n", "%d,%d");
        assertNotNull(TriggerFormats.conflictMessage(pair, Arrays.asList(comma, pair)));

        TriggerFlow self = trigger("", ",", "%d,%d");
        assertNotNull(TriggerFormats.conflictMessage(self, Arrays.asList(self)));
    }

    @Test
    public void receivedTokensResolve() {
        PayloadFormat format = PayloadFormat.parse("%d,%s");
        ReceivedValues values = new ReceivedValues(format, Arrays.asList("7", "on"));

        assertEquals("led 7 on", values.resolveTokens("led [received: var1] [received:var2]"));
        assertEquals(7f, values.number(0), 0f);
        assertNull(values.number(1));
    }

    private static TriggerProgram program(String start, String end, String payload) {
        TriggerStartData data = new TriggerStartData();
        data.startMarker = start;
        data.endMarker = end;
        data.payload = payload;
        return new TriggerProgram(payload, payload, data, new ArrayList<FlowBlock>());
    }

    private static TriggerFlow trigger(String start, String end, String payload) {
        TriggerFlow flow = new TriggerFlow();
        flow.receive().startMarker = start;
        flow.receive().endMarker = end;
        flow.receive().payload = payload;
        return flow;
    }

    private static List<TriggerProgram> list(TriggerProgram... programs) {
        return Arrays.asList(programs);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
