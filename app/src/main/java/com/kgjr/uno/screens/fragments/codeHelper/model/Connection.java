package com.kgjr.uno.screens.fragments.codeHelper.model;

/** A committed link from an output point of one node to an input point of another. */
public class Connection {

    public final CanvasNode from;
    public final ConnectionPoint fromPoint;
    public final CanvasNode to;
    public final ConnectionPoint toPoint;

    public Connection(CanvasNode from, ConnectionPoint fromPoint, CanvasNode to, ConnectionPoint toPoint) {
        this.from = from;
        this.fromPoint = fromPoint;
        this.to = to;
        this.toPoint = toPoint;
    }

    public boolean touches(CanvasNode node) {
        return from == node || to == node;
    }

    /** True when this link already owns the given point, so nothing else may use it. */
    public boolean occupies(CanvasNode node, ConnectionPoint point) {
        return (from == node && fromPoint == point) || (to == node && toPoint == point);
    }
}
