package com.kgjr.uno.screens.fragments.dataHelper.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;

/**
 * The data-design surface reached from the right-edge tab. For now it is an empty pan-and-zoom
 * canvas with the same dot grid as the flow builder — the widgets that render sensor data get
 * added on top of this in the next step.
 */
public class DataCanvasView extends View {

    private static final float GRID_STEP_DP = 22f;
    private static final float DOT_RADIUS_DP = 1.1f;
    private static final float MIN_SCALE = 0.4f;
    private static final float MAX_SCALE = 3.0f;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyHintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ScaleGestureDetector scaleDetector;

    private float scale = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    public DataCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundColor(ContextCompat.getColor(context, R.color.background_light));

        dotPaint.setColor(ContextCompat.getColor(context, R.color.divider_light));
        dotPaint.setStyle(Paint.Style.FILL);

        emptyTitlePaint.setColor(ContextCompat.getColor(context, R.color.text_secondary_light));
        emptyTitlePaint.setTextAlign(Paint.Align.CENTER);
        emptyTitlePaint.setTextSize(sp(15f));

        emptyHintPaint.setColor(withAlpha(
                ContextCompat.getColor(context, R.color.text_secondary_light), 0.7f));
        emptyHintPaint.setTextAlign(Paint.Align.CENTER);
        emptyHintPaint.setTextSize(sp(12f));

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /** True once something has been dropped on the canvas; drives the empty state. */
    public boolean isEmpty() {
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawGrid(canvas);

        if (isEmpty()) drawEmptyState(canvas);
    }

    /** The dots move and scale with the viewport, so panning reads as moving the surface. */
    private void drawGrid(Canvas canvas) {
        float step = dp(GRID_STEP_DP) * scale;
        if (step < 6f) return;

        float radius = dp(DOT_RADIUS_DP) * Math.min(scale, 1.5f);
        float startX = translateX % step;
        float startY = translateY % step;

        for (float x = startX; x < getWidth() + step; x += step) {
            for (float y = startY; y < getHeight() + step; y += step) {
                canvas.drawCircle(x, y, radius, dotPaint);
            }
        }
    }

    /** Drawn in screen space so the hint stays put while the surface pans underneath. */
    private void drawEmptyState(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        canvas.drawText(getContext().getString(R.string.data_canvas_empty),
                centerX, centerY, emptyTitlePaint);
        canvas.drawText(getContext().getString(R.string.data_canvas_empty_hint),
                centerX, centerY + sp(20f), emptyHintPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                activePointerId = event.getPointerId(0);
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (scaleDetector.isInProgress()) return true;

                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) return true;

                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                translateX += x - lastTouchX;
                translateY += y - lastTouchY;
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                // Keep panning from whichever finger is still down.
                int liftedIndex = event.getActionIndex();
                if (event.getPointerId(liftedIndex) == activePointerId) {
                    int replacement = liftedIndex == 0 ? 1 : 0;
                    activePointerId = event.getPointerId(replacement);
                    lastTouchX = event.getX(replacement);
                    lastTouchY = event.getY(replacement);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            }
            default:
                return super.onTouchEvent(event);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float previous = scale;
            scale = clamp(scale * detector.getScaleFactor(), MIN_SCALE, MAX_SCALE);

            // Zoom about the pinch midpoint rather than the top-left corner.
            float factor = scale / previous;
            translateX = detector.getFocusX() - (detector.getFocusX() - translateX) * factor;
            translateY = detector.getFocusY() - (detector.getFocusY() - translateY) * factor;

            invalidate();
            return true;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
