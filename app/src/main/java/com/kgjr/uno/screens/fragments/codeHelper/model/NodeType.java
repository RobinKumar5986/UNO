package com.kgjr.uno.screens.fragments.codeHelper.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum NodeType {

    START("Start", Collections.singletonList(
            new ConnectionPoint("out", Side.BOTTOM, 0.5f)
    )),

    ACTION("Action", Arrays.asList(
            new ConnectionPoint("in", Side.TOP, 0.5f),
            new ConnectionPoint("out", Side.BOTTOM, 0.5f)
    )),

    DECISION("Decision", Arrays.asList(
            new ConnectionPoint("in", Side.TOP, 0.5f),
            new ConnectionPoint("out", Side.BOTTOM, 0.5f),
            new ConnectionPoint("yes", Side.RIGHT, 0.33f),
            new ConnectionPoint("no", Side.RIGHT, 0.66f)
    )),

    WAIT("Wait", Arrays.asList(
            new ConnectionPoint("in", Side.TOP, 0.5f),
            new ConnectionPoint("out", Side.BOTTOM, 0.5f)
    )),

    REPEAT("Repeat", Arrays.asList(
            new ConnectionPoint("in", Side.TOP, 0.5f),
            new ConnectionPoint("out", Side.BOTTOM, 0.5f),
            new ConnectionPoint("loop", Side.RIGHT, 0.5f)
    )),

    END("End", Collections.singletonList(
            new ConnectionPoint("in", Side.TOP, 0.5f)
    ));

    public final String label;
    public final List<ConnectionPoint> connectionPoints;

    NodeType(String label, List<ConnectionPoint> connectionPoints) {
        this.label = label;
        this.connectionPoints = connectionPoints;
    }

    /**
     * The shared point instance with this id, or null. The canvas and the parser compare points
     * with {@code ==}, so a link rebuilt from a saved project has to carry the same object the
     * enum holds, not an equal copy.
     */
    public ConnectionPoint pointById(String id) {
        if (id == null) return null;
        for (ConnectionPoint point : connectionPoints) {
            if (point.id.equals(id)) return point;
        }
        return null;
    }

    /** Parses a stored {@code name()}, falling back to ACTION rather than throwing. */
    public static NodeType fromName(String name) {
        if (name != null) {
            for (NodeType type : values()) {
                if (type.name().equals(name)) return type;
            }
        }
        return ACTION;
    }
}