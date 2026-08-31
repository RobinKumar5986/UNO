package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

/**
 * Touch state machine for the canvas.
 *
 * <ul>
 *   <li>touch a palette chip -> a node is created under the finger immediately, no long press</li>
 *   <li>drag a node -> nearest compatible port pair is previewed, committed only on release</li>
 *   <li>release over the palette strip -> node and its links are removed</li>
 *   <li>hold a node without moving -> its links are dropped, the node stays</li>
 *   <li>drag empty space -> pan; two fingers -> pinch zoom and pan</li>
 * </ul>
 */
class CanvasGestureHandler {

    private static final float TAP_SLOP_DP = 6f;
    private static final float SNAP_RADIUS_DP = 140f;
    private static final long HOLD_MS = 420L;

    private final CodeCanvasView view;
    private final CanvasGraph graph;
    private final CanvasTransform transform;
    private final PaletteStrip palette;
    private final ScaleGestureDetector scaleDetector;
    private final float density;

    private CanvasNode dragging;
    private Connection candidate;
    private Runnable holdRunnable;

    private float grabOffsetX, grabOffsetY;
    private float originX, originY;
    private float downX, downY, lastX, lastY;
    private boolean moved;
    private boolean spawned;
    private boolean panning;
    private boolean overDeleteZone;

    CanvasGestureHandler(CodeCanvasView view, CanvasGraph graph,
                         CanvasTransform transform, PaletteStrip palette) {
        this.view = view;
        this.graph = graph;
        this.transform = transform;
        this.palette = palette;
        this.density = view.getResources().getDisplayMetrics().density;
        this.scaleDetector = new ScaleGestureDetector(view.getContext(), new ZoomListener());
        // Quick scale is a single-pointer gesture; it would race the node drag below.
        this.scaleDetector.setQuickScaleEnabled(false);
    }

    CanvasNode dragging() {
        return dragging;
    }

    Connection candidate() {
        return candidate;
    }

    boolean overDeleteZone() {
        return overDeleteZone;
    }

    boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return onDown(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_DOWN:
                abandonDrag();
                return true;
            case MotionEvent.ACTION_MOVE:
                return onMove(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_UP:
                return onPointerUp(event);
            case MotionEvent.ACTION_UP:
                return onUp(event.getX(), event.getY(), false);
            case MotionEvent.ACTION_CANCEL:
                return onUp(event.getX(), event.getY(), true);
            default:
                return true;
        }
    }

    private boolean onDown(float x, float y) {
        downX = x;
        downY = y;
        lastX = x;
        lastY = y;
        moved = false;
        spawned = false;
        panning = false;
        overDeleteZone = false;
        candidate = null;

        NodeType type = palette.itemAt(x, y);
        if (type != null) {
            CanvasNode node = view.createNode(type, transform.toWorldX(x), transform.toWorldY(y));
            graph.add(node);
            dragging = node;
            spawned = true;
            grabOffsetX = node.width / 2f;
            grabOffsetY = node.height / 2f;
            originX = node.x;
            originY = node.y;
            moved = true; // a freshly spawned node must never open its dialog on release
            view.invalidate();
            return true;
        }

        // Touches inside the strip that miss a chip are swallowed, so nodes panned underneath
        // the strip can never be grabbed through it.
        if (palette.contains(x, y)) return true;

        CanvasNode hit = graph.findNodeAt(transform.toWorldX(x), transform.toWorldY(y));
        if (hit != null) {
            dragging = hit;
            grabOffsetX = transform.toWorldX(x) - hit.x;
            grabOffsetY = transform.toWorldY(y) - hit.y;
            originX = hit.x;
            originY = hit.y;
            graph.bringToFront(hit);
            scheduleHold(hit);
            view.invalidate();
            return true;
        }

        panning = true;
        return true;
    }

    private boolean onMove(float x, float y) {
        if (scaleDetector.isInProgress()) {
            lastX = x;
            lastY = y;
            return true;
        }

        if (!moved && Math.hypot(x - downX, y - downY) > TAP_SLOP_DP * density) {
            moved = true;
            cancelHold();
        }

        if (dragging != null) {
            if (!moved) return true;
            dragging.x = transform.toWorldX(x) - grabOffsetX;
            dragging.y = transform.toWorldY(y) - grabOffsetY;

            overDeleteZone = isDeletable(dragging) && isInDeleteZone(x, y);
            candidate = overDeleteZone ? null : graph.findSnapCandidate(
                    dragging, transform.toWorldSize(SNAP_RADIUS_DP * density));
            view.invalidate();
            return true;
        }

        if (panning) {
            transform.pan(x - lastX, y - lastY);
            lastX = x;
            lastY = y;
            view.invalidate();
        }
        return true;
    }

    /** When a pinch drops back to one finger, hand that finger the pan so the canvas keeps up. */
    private boolean onPointerUp(MotionEvent event) {
        if (event.getPointerCount() != 2) return true;
        int remaining = event.getActionIndex() == 0 ? 1 : 0;
        lastX = event.getX(remaining);
        lastY = event.getY(remaining);
        moved = true;
        panning = true;
        return true;
    }

    private boolean onUp(float x, float y, boolean cancelled) {
        cancelHold();

        CanvasNode released = dragging;
        Connection pending = candidate;

        dragging = null;
        candidate = null;
        panning = false;
        overDeleteZone = false;

        if (released == null) return true;

        if (cancelled) {
            discard(released);
        } else if (isInDeleteZone(x, y)) {
            // Dropped on or past the palette: delete it, or return START to where it was.
            if (isDeletable(released)) graph.remove(released);
            else restore(released);
        } else if (!moved) {
            view.notifyNodeTapped(released);
        } else {
            graph.connect(pending);
        }

        view.invalidate();
        return true;
    }

    /** A second finger turns the gesture into pan/zoom, so the node drag is undone. */
    private void abandonDrag() {
        cancelHold();
        if (dragging != null) discard(dragging);
        dragging = null;
        candidate = null;
        panning = false;
        overDeleteZone = false;
        view.invalidate();
    }

    /** A node that was just spawned disappears again; an existing one goes back where it was. */
    private void discard(CanvasNode node) {
        if (spawned) graph.remove(node);
        else restore(node);
    }

    private void restore(CanvasNode node) {
        node.x = originX;
        node.y = originY;
    }

    private boolean isDeletable(CanvasNode node) {
        return spawned || node.type != NodeType.START;
    }

    private void scheduleHold(CanvasNode node) {
        cancelHold();
        holdRunnable = () -> {
            holdRunnable = null;
            if (moved || dragging != node) return;
            if (!isDeletable(node)) return;

            graph.remove(node);
            dragging = null;
            candidate = null;
            overDeleteZone = false;
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            view.invalidate();
        };
        view.postDelayed(holdRunnable, HOLD_MS);
    }
    private void cancelHold() {
        if (holdRunnable == null) return;
        view.removeCallbacks(holdRunnable);
        holdRunnable = null;
    }

    private class ZoomListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float focusX, focusY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            abandonDrag();
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            transform.pan(detector.getFocusX() - focusX, detector.getFocusY() - focusY);
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            transform.zoomBy(detector.getScaleFactor(), focusX, focusY);
            view.invalidate();
            return true;
        }
    }
    /**
     * Delete zone = anything at or left of the palette's right edge, including coordinates that
     * have run off the screen. RectF.contains() would reject a negative x, so a node dragged
     * clean past the strip would survive.
     */
    private boolean isInDeleteZone(float screenX, float screenY) {
        return screenX <= palette.bounds().right;
    }
}
