package com.kgjr.uno.screens.fragments.codeHelper.model;

import java.util.UUID;

/**
 * A flow that runs once each time the board sends a message matching its receive format.
 * The format object is created with the trigger so it can be checked for conflicts before the
 * canvas has ever been laid out; the seeded Start node then shares that same instance.
 */
public class TriggerFlow extends FlowDocument {

    public final String id;
    private TriggerStartData receive = new TriggerStartData();

    public TriggerFlow() {
        this(null);
    }

    public TriggerFlow(String id) {
        this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
    }

    @Override
    public NodeData newStartData() {
        return receive;
    }

    public TriggerStartData receive() {
        CanvasNode start = startNode();
        if (start != null && start.data instanceof TriggerStartData) {
            receive = (TriggerStartData) start.data;
        }
        return receive;
    }
}
