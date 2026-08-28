package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import android.content.ClipData;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

public class NodePalette {

    public static void populate(Context context, LinearLayout container) {
        container.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int margin = (int) (8 * density);
        int itemHeight = (int) (56 * density);

        for (NodeType type : NodeType.values()) {
            if (type == NodeType.START) continue;

            TextView item = new TextView(context);
            item.setText(type.label);
            item.setGravity(Gravity.CENTER);
            item.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark));
            item.setBackground(ContextCompat.getDrawable(context, backgroundFor(type)));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, itemHeight);
            params.setMargins(margin, margin, margin, 0);
            item.setLayoutParams(params);

            item.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("node_type", type.name());
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadow, type, 0);
                return true;
            });

            container.addView(item);
        }
    }

    private static int backgroundFor(NodeType type) {
        switch (type) {
            case ACTION: return R.drawable.bg_node_action;
            case DECISION: return R.drawable.bg_node_decision;
            case WAIT: return R.drawable.bg_node_wait;
            case REPEAT: return R.drawable.bg_node_repeat;
            case END: return R.drawable.bg_node_end;
            default: return R.drawable.bg_node_action;
        }
    }
}