package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.graphics.PointF;

import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.ConnectionPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Holds the nodes and their links, and works out which points want to snap together. */
public class CanvasGraph {

    private final List<CanvasNode> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    public List<CanvasNode> nodes() {
        return nodes;
    }

    public List<Connection> connections() {
        return connections;
    }

    public void add(CanvasNode node) {
        nodes.add(node);
    }

    public void remove(CanvasNode node) {
        nodes.remove(node);
        disconnect(node);
    }

    /** Drops every link attached to the node but keeps the node itself. */
    public void disconnect(CanvasNode node) {
        for (Iterator<Connection> it = connections.iterator(); it.hasNext(); ) {
            if (it.next().touches(node)) it.remove();
        }
    }

    public boolean hasConnections(CanvasNode node) {
        for (Connection connection : connections) {
            if (connection.touches(node)) return true;
        }
        return false;
    }

    public void bringToFront(CanvasNode node) {
        if (nodes.remove(node)) nodes.add(node);
    }

    public CanvasNode findNodeAt(float worldX, float worldY) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).bounds().contains(worldX, worldY)) return nodes.get(i);
        }
        return null;
    }

    /**
     * Nearest acceptable input/output pair between the moving node and the rest of the graph,
     * or null when nothing is within range. Not committed until {@link #connect}.
     */
    public Connection findSnapCandidate(CanvasNode moving, float radius) {
        Connection best = null;
        float bestDistance = radius;

        for (ConnectionPoint movingPoint : moving.type.connectionPoints) {
            if (isOccupied(moving, movingPoint)) continue;
            PointF origin = moving.connectionPointPosition(movingPoint);

            for (CanvasNode other : nodes) {
                if (other == moving) continue;
                for (ConnectionPoint otherPoint : other.type.connectionPoints) {
                    if (movingPoint.isInput() == otherPoint.isInput()) continue;
                    if (isOccupied(other, otherPoint)) continue;

                    Connection link = movingPoint.isInput()
                            ? new Connection(other, otherPoint, moving, movingPoint)
                            : new Connection(moving, movingPoint, other, otherPoint);

                    PointF target = other.connectionPointPosition(otherPoint);
                    float distance = (float) Math.hypot(origin.x - target.x, origin.y - target.y);
                    if (distance >= bestDistance) continue;

                    bestDistance = distance;
                    best = link;
                }
            }
        }
        return best;
    }

    public void connect(Connection connection) {
        if (connection == null) return;
        if (isOccupied(connection.from, connection.fromPoint)) return;
        if (isOccupied(connection.to, connection.toPoint)) return;
        connections.add(connection);
    }

    /**
     * Every point holds at most one link, so a point already in use is never offered as a snap
     * target. Two nodes can still be linked more than once, as long as each link uses a pair of
     * points that are both free.
     */
    private boolean isOccupied(CanvasNode node, ConnectionPoint point) {
        for (Connection connection : connections) {
            if (connection.occupies(node, point)) return true;
        }
        return false;
    }
}
