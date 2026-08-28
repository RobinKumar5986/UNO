package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;

import com.kgjr.uno.screens.fragments.codeHelper.model.RepeatNodeData;

public class RepeatNodeDialog {

    private static final int MAX_TIMES = 1000;

    public static void show(Context context, RepeatNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(
                context, "Repeat", "Run everything on the loop branch this many times.", onChanged);

        SliderRow.add(NodeDialogFrame.contentOf(dialog), "Times", MAX_TIMES, data.times,
                value -> data.times = value);

        dialog.show();
    }

    private RepeatNodeDialog() {
    }
}
