package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.content.Context;

import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.RepeatNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.WaitNodeData;

public class NodeDialogManager {

    public static void show(Context context, CanvasNode node, Runnable onChanged) {
        switch (node.type) {
            case ACTION:
                EmptyNodeDialog.show(context, "Action");
                break;
            case DECISION:
                EmptyNodeDialog.show(context, "Decision");
                break;
            case WAIT:
                WaitNodeDialog.show(context, (WaitNodeData) node.data, onChanged);
                break;
            case REPEAT:
                RepeatNodeDialog.show(context, (RepeatNodeData) node.data, onChanged);
                break;
            case END:
                EndNodeDialog.show(context, (EndNodeData) node.data, onChanged);
                break;
            case START:
            default:
                break;
        }
    }
}