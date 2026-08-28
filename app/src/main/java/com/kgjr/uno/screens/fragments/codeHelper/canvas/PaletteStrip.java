package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.content.Context;
import android.graphics.RectF;

import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry for the node palette that lives inside the canvas, pinned to the left edge in
 * screen space so pan and zoom never move it. Also acts as the drop target for deletion.
 */
public class PaletteStrip {

    private static final float WIDTH_DP = 84f;
    private static final float ITEM_HEIGHT_DP = 34f;
    private static final float ITEM_GAP_DP = 10f;
    private static final float TOP_PADDING_DP = 16f;
    private static final float SIDE_PADDING_DP = 10f;

    private final List<NodeType> items = new ArrayList<>();
    private final RectF bounds = new RectF();
    private final float density;

    public PaletteStrip(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        for (NodeType type : NodeType.values()) {
            if (type != NodeType.START) items.add(type);
        }
    }

    public void layout(int viewHeight) {
        bounds.set(0f, 0f, WIDTH_DP * density, viewHeight);
    }

    public RectF bounds() {
        return bounds;
    }

    public List<NodeType> items() {
        return items;
    }

    public RectF itemBounds(int index) {
        float inset = SIDE_PADDING_DP * density;
        float top = TOP_PADDING_DP * density + index * (ITEM_HEIGHT_DP + ITEM_GAP_DP) * density;
        return new RectF(inset, top, bounds.right - inset, top + ITEM_HEIGHT_DP * density);
    }

    public NodeType itemAt(float screenX, float screenY) {
        if (!bounds.contains(screenX, screenY)) return null;
        for (int i = 0; i < items.size(); i++) {
            if (itemBounds(i).contains(screenX, screenY)) return items.get(i);
        }
        return null;
    }

    public boolean contains(float screenX, float screenY) {
        return bounds.contains(screenX, screenY);
    }
}
