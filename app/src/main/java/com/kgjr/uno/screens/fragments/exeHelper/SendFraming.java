package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.StartNodeData;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** The bytes wrapped around every command sent to the board, set on the Stage 1 Start node. */
public final class SendFraming {

    private final byte[] start;
    private final byte[] end;

    private SendFraming(StartNodeData data) {
        start = Escapes.decode(data.startMarker);
        end = Escapes.decode(data.endMarker);
    }

    /** The parser always puts Start first; anything else falls back to the defaults. */
    public static SendFraming of(List<FlowBlock> mainTree) {
        if (mainTree != null && !mainTree.isEmpty()
                && mainTree.get(0).data instanceof StartNodeData) {
            return new SendFraming((StartNodeData) mainTree.get(0).data);
        }
        return new SendFraming(new StartNodeData());
    }

    public byte[] wrap(String command) {
        byte[] body = command.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[start.length + body.length + end.length];

        System.arraycopy(start, 0, payload, 0, start.length);
        System.arraycopy(body, 0, payload, start.length, body.length);
        System.arraycopy(end, 0, payload, start.length + body.length, end.length);
        return payload;
    }
}
