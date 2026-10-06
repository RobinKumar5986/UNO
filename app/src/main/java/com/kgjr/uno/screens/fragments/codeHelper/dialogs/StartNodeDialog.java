package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.StartNodeData;

public class StartNodeDialog {

    private static final String PREVIEW_COMMAND = "command";

    public static void show(Context context, StartNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Start",
                "How each command is framed when sent to the device.",
                R.layout.dialog_node_start, onChanged);

        TextInputLayout startLayout = dialog.findViewById(R.id.start_marker_layout);
        TextInputLayout endLayout = dialog.findViewById(R.id.end_marker_layout);
        TextInputEditText startInput = dialog.findViewById(R.id.start_marker_input);
        TextInputEditText endInput = dialog.findViewById(R.id.end_marker_input);
        TextView preview = dialog.findViewById(R.id.start_marker_preview);

        Runnable refresh = () -> {
            String start = text(startInput);
            String end = text(endInput);
            String startProblem = Escapes.firstProblem(start);
            String endProblem = Escapes.firstProblem(end);

            startLayout.setError(startProblem);
            endLayout.setError(endProblem);

            // A half-typed escape like "\x4" is left out rather than saved and sent wrong.
            if (startProblem == null) data.startMarker = start;
            if (endProblem == null) data.endMarker = end;

            preview.setText(previewOf(data));
        };

        startInput.setText(data.startMarker);
        endInput.setText(data.endMarker);
        startInput.addTextChangedListener(watcher(refresh));
        endInput.addTextChangedListener(watcher(refresh));
        refresh.run();

        // Each setText re-reads both fields, so the defaults come from a fresh copy.
        dialog.findViewById(R.id.start_marker_reset).setOnClickListener(v -> {
            StartNodeData defaults = new StartNodeData();
            startInput.setText(defaults.startMarker);
            endInput.setText(defaults.endMarker);
        });

        dialog.show();
    }

    private static String previewOf(StartNodeData data) {
        byte[] start = Escapes.decode(data.startMarker);
        byte[] end = Escapes.decode(data.endMarker);
        return Escapes.visible(start) + PREVIEW_COMMAND + Escapes.visible(end);
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static TextWatcher watcher(Runnable onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChange.run();
            }
        };
    }

    private StartNodeDialog() {
    }
}
