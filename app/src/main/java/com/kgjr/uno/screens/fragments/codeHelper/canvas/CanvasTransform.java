package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.graphics.Canvas;

/** Viewport state for the canvas: converts between screen and world coordinates. */
public class CanvasTransform {

    private static final float MIN_SCALE = 0.4f;
    private static final float MAX_SCALE = 3.0f;

    private float scale = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    public float scale() {
        return scale;
    }

    public void pan(float dx, float dy) {
        translateX += dx;
        translateY += dy;
    }

    /** Scales around a screen-space focal point so content under the fingers stays put. */
    public void zoomBy(float factor, float focusX, float focusY) {
        float target = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
        if (target == scale) return;
        float applied = target / scale;
        translateX = focusX - (focusX - translateX) * applied;
        translateY = focusY - (focusY - translateY) * applied;
        scale = target;
    }

    public float toWorldX(float screenX) {
        return (screenX - translateX) / scale;
    }

    public float toWorldY(float screenY) {
        return (screenY - translateY) / scale;
    }

    public float toWorldSize(float screenSize) {
        return screenSize / scale;
    }

    public void apply(Canvas canvas) {
        canvas.translate(translateX, translateY);
        canvas.scale(scale, scale);
    }
}
