package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.content.Context;

import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.RepeatNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.StartNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.WaitNodeData;

public class NodeDialogManager {

    public static void show(Context context, CanvasNode node, FlowScope scope, Runnable onChanged) {
        switch (node.type) {
            case ACTION:
                ActionNodeDialog.show(context, (ActionNodeData) node.data, scope, onChanged);
                break;
            case DECISION:
                DecisionNodeDialog.show(context, (DecisionNodeData) node.data, scope, onChanged);
                break;
            case WAIT:
                WaitNodeDialog.show(context, (WaitNodeData) node.data, onChanged);
                break;
            case REPEAT:
                RepeatNodeDialog.show(context, (RepeatNodeData) node.data, onChanged);
                break;
            case END:
                // A trigger runs once per message, so its End can't be switched to Loop.
                if (!scope.isTrigger()) {
                    EndNodeDialog.show(context, (EndNodeData) node.data, onChanged);
                }
                break;
            case START:
                if (scope.isTrigger()) {
                    TriggerStartDialog.show(context, scope, onChanged);
                } else {
                    StartNodeDialog.show(context, (StartNodeData) node.data, onChanged);
                }
                break;
            default:
                break;
        }
    }
}
