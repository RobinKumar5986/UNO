package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.widget.LinearLayout;

import com.kgjr.uno.screens.fragments.codeHelper.model.WaitNodeData;

public class WaitNodeDialog {

    public static void show(Context context, WaitNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(
                context, "Wait", "Pause before the next step runs.", onChanged);
        LinearLayout content = NodeDialogFrame.contentOf(dialog);

        SliderRow.add(content, "Milliseconds", 999, data.millis, value -> data.millis = value);
        SliderRow.add(content, "Seconds", 59, data.seconds, value -> data.seconds = value);
        SliderRow.add(content, "Minutes", 59, data.minutes, value -> data.minutes = value);
        SliderRow.add(content, "Hours", 23, data.hours, value -> data.hours = value);

        dialog.show();
    }

    private WaitNodeDialog() {
    }
}
