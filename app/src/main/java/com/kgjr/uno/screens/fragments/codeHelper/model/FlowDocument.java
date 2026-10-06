package com.kgjr.uno.screens.fragments.codeHelper.model;

import java.util.ArrayList;
import java.util.List;

/** One canvas worth of flow: its graph plus the viewport it was last left at. */
public class FlowDocument {

    public List<CanvasNode> nodes = new ArrayList<>();
    public List<Connection> connections = new ArrayList<>();

    public boolean viewportSaved;
    public float scale = 1f;
    public float translateX;
    public float translateY;

    /** Data for the Start node seeded onto an empty canvas. */
    public NodeData newStartData() {
        return new StartNodeData();
    }

    public CanvasNode startNode() {
        for (CanvasNode node : nodes) {
            if (node.type == NodeType.START) return node;
        }
        return null;
    }

    /** Only the seeded Start node, or not even that yet. */
    public boolean isEmpty() {
        return nodes.size() <= 1;
    }

    public void setViewport(float scale, float translateX, float translateY) {
        this.viewportSaved = true;
        this.scale = scale <= 0f ? 1f : scale;
        this.translateX = translateX;
        this.translateY = translateY;
    }
}
