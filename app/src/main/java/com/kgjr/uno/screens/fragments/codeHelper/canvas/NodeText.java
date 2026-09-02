package com.kgjr.uno.screens.fragments.codeHelper.canvas;

import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

/** The caption a node draws, which for some types depends on its data rather than its type. */
public final class NodeText {

    private NodeText() {
    }

    public static String title(CanvasNode node) {
        if (node == null || node.type == null) return "";

        if (node.type == NodeType.END
                && node.data instanceof EndNodeData
                && ((EndNodeData) node.data).loop) {
            return "Loop";
        }
        return node.type.label;
    }
}