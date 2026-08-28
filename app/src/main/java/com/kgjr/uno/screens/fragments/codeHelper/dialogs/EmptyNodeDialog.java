package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.content.Context;

import com.kgjr.uno.R;

public class EmptyNodeDialog {

    public static void show(Context context, String title) {
        NodeDialogFrame.create(context, title, null, R.layout.dialog_node_empty, null).show();
    }

    private EmptyNodeDialog() {
    }
}
