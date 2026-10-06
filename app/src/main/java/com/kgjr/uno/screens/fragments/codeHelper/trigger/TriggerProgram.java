package com.kgjr.uno.screens.fragments.codeHelper.trigger;

import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerStartData;

import java.util.List;

/** A validated trigger, ready for the execution screen. */
public final class TriggerProgram {

    public final String id;
    public final String label;
    public final PayloadFormat format;
    public final byte[] startMarker;
    public final byte[] endMarker;
    public final List<FlowBlock> tree;

    /** Expects a format that already passed {@link TriggerFormats#problemOf}. */
    public TriggerProgram(String id, String label, TriggerStartData receive, List<FlowBlock> tree) {
        this.id = id;
        this.label = label;
        this.format = PayloadFormat.parse(receive.payload);
        this.startMarker = Escapes.decode(receive.startMarker);
        this.endMarker = Escapes.decode(receive.endMarker);
        this.tree = tree;
    }

    /** Ranks competing matches for one message: typed values, literals and markers beat loose text. */
    public int specificity() {
        return format.specificity() + (startMarker.length + endMarker.length) * 4;
    }
}
