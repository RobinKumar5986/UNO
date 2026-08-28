package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;

public class EndNodeDialog {

    public static void show(Context context, EndNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "End",
                "Choose how the flow finishes.", R.layout.dialog_node_end, onChanged);

        MaterialButtonToggleGroup group = dialog.findViewById(R.id.end_mode_group);
        group.check(data.loop ? R.id.end_mode_loop : R.id.end_mode_end);
        group.addOnButtonCheckedListener((source, checkedId, isChecked) -> {
            if (isChecked) data.loop = checkedId == R.id.end_mode_loop;
        });

        dialog.show();
    }

    private EndNodeDialog() {
    }
}
