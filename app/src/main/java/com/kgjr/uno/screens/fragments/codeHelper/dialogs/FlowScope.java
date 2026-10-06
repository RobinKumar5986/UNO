package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerFlow;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerFormats;

import java.util.Collections;
import java.util.List;

/** Which flow a node dialog is editing: the Stage 1 flow, or one trigger among all of them. */
public final class FlowScope {

    public static final FlowScope MAIN = new FlowScope(null, Collections.emptyList());

    public final TriggerFlow trigger;
    public final List<TriggerFlow> triggers;

    private FlowScope(TriggerFlow trigger, List<TriggerFlow> triggers) {
        this.trigger = trigger;
        this.triggers = triggers;
    }

    public static FlowScope of(TriggerFlow trigger, List<TriggerFlow> triggers) {
        return new FlowScope(trigger, triggers);
    }

    public boolean isTrigger() {
        return trigger != null;
    }

    /** The trigger's parsed payload, or null outside a trigger or while the format is invalid. */
    public PayloadFormat receivedFormat() {
        if (trigger == null || TriggerFormats.problemOf(trigger.receive()) != null) return null;
        return PayloadFormat.parse(trigger.receive().payload);
    }
}
