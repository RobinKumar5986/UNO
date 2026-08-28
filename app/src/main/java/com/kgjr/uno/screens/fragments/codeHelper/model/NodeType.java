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
}