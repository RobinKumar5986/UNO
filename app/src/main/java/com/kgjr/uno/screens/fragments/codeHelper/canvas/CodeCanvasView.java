package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

/**
 * The whole builder surface: palette, nodes, links, pan and zoom. Wiring only — geometry lives
 * in {@link CanvasGraph} / {@link CanvasTransform}, painting in {@link NodeRenderer} and input
 * in {@link CanvasGestureHandler}.
 */
public class CodeCanvasView extends View {

    private static final float NODE_WIDTH_DP = 116f;
    private static final float NODE_HEIGHT_DP = 34f;
    private static final float START_TOP_DP = 72f;

    private final CanvasGraph graph = new CanvasGraph();
    private final CanvasTransform transform = new CanvasTransform();
    private final PaletteStrip palette;
    private final NodeRenderer renderer;
    private final CanvasGestureHandler gestures;

    private OnCanvasNodeListener listener;
    private boolean seeded;

    public CodeCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        palette = new PaletteStrip(context);
        renderer = new NodeRenderer(context);
        gestures = new CanvasGestureHandler(this, graph, transform, palette);
    }

    public void setOnCanvasNodeListener(OnCanvasNodeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        palette.layout(height);

        if (!seeded && width > 0) {
            seeded = true;
            float centerX = (palette.bounds().right + width) / 2f;
            graph.add(createNode(NodeType.START, centerX, dp(START_TOP_DP)));
        }
    }

    /** Builds a node of the given type centred on a world-space point. */
    CanvasNode createNode(NodeType type, float worldCenterX, float worldCenterY) {
        float width = dp(NODE_WIDTH_DP);
        float height = dp(NODE_HEIGHT_DP);
        return new CanvasNode(type, worldCenterX - width / 2f, worldCenterY - height / 2f,
                width, height, CanvasNode.createDefaultData(type));
    }

    void notifyNodeTapped(CanvasNode node) {
        if (listener != null) listener.onNodeTapped(node);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int checkpoint = canvas.save();
        transform.apply(canvas);

        renderer.drawGrid(canvas, transform, getWidth(), getHeight());
        renderer.drawConnections(canvas, graph.connections());

        Connection candidate = gestures.candidate();
        if (candidate != null) renderer.drawCandidate(canvas, candidate);

        CanvasNode dragging = gestures.dragging();
        for (CanvasNode node : graph.nodes()) {
            boolean active = node == dragging;
            renderer.drawNode(canvas, node, active, active && gestures.overDeleteZone());
        }
        canvas.restoreToCount(checkpoint);

        renderer.drawPalette(canvas, palette, gestures.overDeleteZone());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
