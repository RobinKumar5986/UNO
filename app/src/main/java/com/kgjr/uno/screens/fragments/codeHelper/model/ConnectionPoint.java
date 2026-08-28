package com.kgjr.uno.screens.fragments.codeHelper.model;

public class ConnectionPoint {

    public final String id;
    public final Side side;
    public final float offset;

    public ConnectionPoint(String id, Side side, float offset) {
        this.id = id;
        this.side = side;
        this.offset = offset;
    }

    /** Points on the top edge accept incoming flow; every other edge emits it. */
    public boolean isInput() {
        return side == Side.TOP;
    }
}
