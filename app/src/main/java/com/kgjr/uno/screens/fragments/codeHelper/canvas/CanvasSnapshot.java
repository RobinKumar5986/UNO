package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;

import java.util.List;

/**
 * Renders the whole flow to a bitmap for a project's thumbnail. Independent of the on-screen
 * viewport: the graph's own bounds are fitted to the target size, so the thumbnail shows the
 * complete flow however the user happened to be panned or zoomed.
 */
public final class CanvasSnapshot {

    private static final float PADDING_DP = 20f;

    /** Never enlarge past this, or a two-node flow becomes a wall of pixels. */
    private static final float MAX_SCALE = 1.4f;

    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 400;

    private CanvasSnapshot() {
    }

    @Nullable
    public static Bitmap of(Context context, List<CanvasNode> nodes, List<Connection> connections) {
        return of(context, nodes, connections, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Nullable
    public static Bitmap of(Context context, List<CanvasNode> nodes, List<Connection> connections,
                            int maxWidth, int maxHeight) {
        if (context == null || nodes == null || nodes.isEmpty()) return null;
        if (maxWidth <= 0 || maxHeight <= 0) return null;

        float padding = PADDING_DP * context.getResources().getDisplayMetrics().density;

        RectF bounds = boundsOf(nodes);
        if (bounds == null) return null;
        bounds.inset(-padding, -padding);

        float scale = Math.min(maxWidth / bounds.width(), maxHeight / bounds.height());
        scale = Math.min(MAX_SCALE, scale);
        if (scale <= 0f || Float.isNaN(scale)) return null;

        int width = Math.max(1, Math.min(maxWidth, Math.round(bounds.width() * scale)));
        int height = Math.max(1, Math.min(maxHeight, Math.round(bounds.height() * scale)));

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return null;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(ContextCompat.getColor(context, R.color.background_light));

        float translateX = -bounds.left * scale + (width - bounds.width() * scale) / 2f;
        float translateY = -bounds.top * scale + (height - bounds.height() * scale) / 2f;

        NodeRenderer renderer = new NodeRenderer(context);
        CanvasTransform transform = new CanvasTransform();
        transform.setExact(scale, translateX, translateY);

        int checkpoint = canvas.save();
        transform.apply(canvas);

        renderer.drawGrid(canvas, transform, width, height);
        if (connections != null) renderer.drawConnections(canvas, connections);
        for (CanvasNode node : nodes) {
            renderer.drawNode(canvas, node, false, false);
        }
        canvas.restoreToCount(checkpoint);

        return bitmap;
    }

    @Nullable
    private static RectF boundsOf(List<CanvasNode> nodes) {
        RectF union = null;
        for (CanvasNode node : nodes) {
            if (node == null) continue;

            if (union == null) union = node.bounds();
            else union.union(node.bounds());
        }
        if (union == null || union.width() <= 0f || union.height() <= 0f) return null;
        return union;
    }
}
