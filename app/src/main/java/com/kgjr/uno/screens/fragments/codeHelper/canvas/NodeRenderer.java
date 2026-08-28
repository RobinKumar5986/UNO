package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.ConnectionPoint;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;
import com.kgjr.uno.screens.fragments.codeHelper.model.Side;

import java.util.List;

/** All painting for the canvas. Owns every Paint so nothing is allocated per frame. */
class NodeRenderer {

    private static final float CORNER_DP = 9f;
    private static final float PORT_RADIUS_DP = 3.5f;
    private static final float GRID_SPACING_DP = 28f;
    private static final float GRID_DOT_DP = 1.1f;

    private final float density;
    private final float scaledDensity;
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final int[] nodeColors = new int[NodeType.values().length];

    private final int dividerColor;
    private final int accentColor;

    private final Paint bodyPaint;
    private final Paint borderPaint;
    private final Paint labelPaint;
    private final Paint portPaint;
    private final Paint linkPaint;
    private final Paint candidatePaint;
    private final Paint haloPaint;
    private final Paint gridPaint;
    private final Paint stripPaint;
    private final Paint stripEdgePaint;
    private final Paint stripLabelPaint;
    private final Paint deleteHintPaint;

    NodeRenderer(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;

        dividerColor = ContextCompat.getColor(context, R.color.divider_light);
        accentColor = ContextCompat.getColor(context, R.color.accent_blue);
        int surfaceColor = ContextCompat.getColor(context, R.color.surface_light);
        int onNodeColor = ContextCompat.getColor(context, R.color.text_primary_dark);
        int dangerColor = ContextCompat.getColor(context, R.color.accent_red);

        nodeColors[NodeType.START.ordinal()] = ContextCompat.getColor(context, R.color.surface_dark);
        nodeColors[NodeType.ACTION.ordinal()] = accentColor;
        nodeColors[NodeType.DECISION.ordinal()] = ContextCompat.getColor(context, R.color.accent_yellow);
        nodeColors[NodeType.WAIT.ordinal()] = ContextCompat.getColor(context, R.color.accent_green);
        nodeColors[NodeType.REPEAT.ordinal()] = dangerColor;
        nodeColors[NodeType.END.ordinal()] = ContextCompat.getColor(context, R.color.text_secondary_light);

        bodyPaint = fill(surfaceColor);
        portPaint = fill(surfaceColor);
        gridPaint = fill(dividerColor);
        stripPaint = fill(surfaceColor);

        haloPaint = fill(accentColor);
        haloPaint.setAlpha(80);
        deleteHintPaint = fill(dangerColor);
        deleteHintPaint.setAlpha(38);

        borderPaint = stroke(dividerColor, 1.2f);
        stripEdgePaint = stroke(dividerColor, 1f);
        linkPaint = stroke(ContextCompat.getColor(context, R.color.text_secondary_light), 2f);
        candidatePaint = stroke(accentColor, 2.4f);
        candidatePaint.setPathEffect(new DashPathEffect(new float[]{dp(7f), dp(5f)}, 0f));

        labelPaint = fill(onNodeColor);
        labelPaint.setTextSize(sp(12f));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        stripLabelPaint = fill(onNodeColor);
        stripLabelPaint.setTextSize(sp(11f));
        stripLabelPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ---------------------------------------------------------------- world space

    void drawGrid(Canvas canvas, CanvasTransform transform, int viewWidth, int viewHeight) {
        float spacing = dp(GRID_SPACING_DP);
        float right = transform.toWorldX(viewWidth);
        float bottom = transform.toWorldY(viewHeight);
        float radius = transform.toWorldSize(dp(GRID_DOT_DP));
        float startX = (float) Math.floor(transform.toWorldX(0f) / spacing) * spacing;
        float startY = (float) Math.floor(transform.toWorldY(0f) / spacing) * spacing;

        for (float x = startX; x <= right; x += spacing) {
            for (float y = startY; y <= bottom; y += spacing) {
                canvas.drawCircle(x, y, radius, gridPaint);
            }
        }
    }

    void drawConnections(Canvas canvas, List<Connection> connections) {
        for (Connection connection : connections) {
            buildLinkPath(connection);
            canvas.drawPath(path, linkPaint);
        }
    }

    void drawCandidate(Canvas canvas, Connection candidate) {
        buildLinkPath(candidate);
        canvas.drawPath(path, candidatePaint);

        PointF target = candidate.to.connectionPointPosition(candidate.toPoint);
        canvas.drawCircle(target.x, target.y, dp(PORT_RADIUS_DP) * 2.4f, haloPaint);
    }

    void drawNode(Canvas canvas, CanvasNode node, boolean active, boolean markedForDelete) {
        float corner = dp(CORNER_DP);
        rect.set(node.bounds());

        bodyPaint.setColor(nodeColors[node.type.ordinal()]);
        canvas.drawRoundRect(rect, corner, corner, bodyPaint);
        if (markedForDelete) canvas.drawRoundRect(rect, corner, corner, deleteHintPaint);

        borderPaint.setColor(active ? accentColor : dividerColor);
        borderPaint.setStrokeWidth(dp(active ? 2f : 1.2f));
        canvas.drawRoundRect(rect, corner, corner, borderPaint);

        float baseline = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f;
        canvas.drawText(node.type.label, rect.centerX(), baseline, labelPaint);

        for (ConnectionPoint point : node.type.connectionPoints) {
            PointF position = node.connectionPointPosition(point);
            canvas.drawCircle(position.x, position.y, dp(PORT_RADIUS_DP), portPaint);
            canvas.drawCircle(position.x, position.y, dp(PORT_RADIUS_DP), borderPaint);
        }
    }

    // --------------------------------------------------------------- screen space

    void drawPalette(Canvas canvas, PaletteStrip palette, boolean armedForDelete) {
        RectF bounds = palette.bounds();
        canvas.drawRect(bounds, stripPaint);
        if (armedForDelete) canvas.drawRect(bounds, deleteHintPaint);
        canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, stripEdgePaint);

        float corner = dp(CORNER_DP);
        borderPaint.setColor(dividerColor);
        borderPaint.setStrokeWidth(dp(1.2f));

        List<NodeType> items = palette.items();
        for (int i = 0; i < items.size(); i++) {
            NodeType type = items.get(i);
            RectF item = palette.itemBounds(i);

            bodyPaint.setColor(nodeColors[type.ordinal()]);
            canvas.drawRoundRect(item, corner, corner, bodyPaint);
            canvas.drawRoundRect(item, corner, corner, borderPaint);

            float baseline = item.centerY() - (stripLabelPaint.descent() + stripLabelPaint.ascent()) / 2f;
            canvas.drawText(type.label, item.centerX(), baseline, stripLabelPaint);
        }
    }

    // -------------------------------------------------------------------- helpers

    private void buildLinkPath(Connection connection) {
        PointF start = connection.from.connectionPointPosition(connection.fromPoint);
        PointF end = connection.to.connectionPointPosition(connection.toPoint);
        float slack = Math.max(dp(26f), Math.abs(end.y - start.y) * 0.45f);
        Side side = connection.fromPoint.side;

        path.reset();
        path.moveTo(start.x, start.y);
        if (side == Side.LEFT || side == Side.RIGHT) {
            float direction = side == Side.RIGHT ? 1f : -1f;
            path.cubicTo(start.x + direction * slack, start.y, end.x, end.y - slack, end.x, end.y);
        } else {
            path.cubicTo(start.x, start.y + slack, end.x, end.y - slack, end.x, end.y);
        }
    }

    private Paint fill(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        return paint;
    }

    private Paint stroke(int color, float widthDp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        paint.setStrokeWidth(dp(widthDp));
        return paint;
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * scaledDensity;
    }
}
