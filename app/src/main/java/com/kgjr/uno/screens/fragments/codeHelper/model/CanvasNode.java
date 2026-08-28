package com.kgjr.uno.screens.fragments.codeHelper.model;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.UUID;

public class CanvasNode {

    public final String id;
    public final NodeType type;
    public float x, y, width, height;
    public NodeData data;

    public CanvasNode(NodeType type, float x, float y, float width, float height, NodeData data) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public RectF bounds() {
        return new RectF(x, y, x + width, y + height);
    }

    public PointF connectionPointPosition(ConnectionPoint point) {
        switch (point.side) {
            case TOP:
                return new PointF(x + width * point.offset, y);
            case BOTTOM:
                return new PointF(x + width * point.offset, y + height);
            case LEFT:
                return new PointF(x, y + height * point.offset);
            case RIGHT:
            default:
                return new PointF(x + width, y + height * point.offset);
        }
    }

    public static NodeData createDefaultData(NodeType type) {
        switch (type) {
            case START: return new StartNodeData();
            case ACTION: return new ActionNodeData();
            case DECISION: return new DecisionNodeData();
            case WAIT: return new WaitNodeData();
            case REPEAT: return new RepeatNodeData();
            case END: return new EndNodeData();
            default: return null;
        }
    }
}