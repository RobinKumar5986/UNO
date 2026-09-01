package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;

/**
 * ACTION node editor: a mode dropdown (Command / API) and, for Command, a free-text box.
 * API is a stub — the section explains itself and edits nothing.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens
 * rather than collected on submit.
 */
public class ActionNodeDialog {

    public static void show(Context context, ActionNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Action",
                "What this step does when the flow reaches it.",
                R.layout.dialog_action_node, onChanged);

        MaterialAutoCompleteTextView modeInput = dialog.findViewById(R.id.action_mode_input);
        TextInputEditText commandInput = dialog.findViewById(R.id.action_command_input);
        View commandGroup = dialog.findViewById(R.id.action_command_group);
        View apiGroup = dialog.findViewById(R.id.action_api_group);

        String[] labels = new String[ActionNodeData.Mode.values().length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = ActionNodeData.Mode.values()[i].label;
        }

        // The dialog's context carries NodeDialogTheme; the host's may not be Material.
        modeInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, labels));
        // false = don't filter the list down to what's already in the field.
        modeInput.setText(data.mode.label, false);

        commandInput.setText(data.command);
        applyMode(data.mode, commandGroup, apiGroup);

        modeInput.setOnItemClickListener((parent, view, position, id) -> {
            data.mode = ActionNodeData.Mode.values()[position];
            applyMode(data.mode, commandGroup, apiGroup);
        });

        commandInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                data.command = s.toString();
            }
        });

        dialog.show();
    }

    private static void applyMode(ActionNodeData.Mode mode, View commandGroup, View apiGroup) {
        boolean isCommand = mode == ActionNodeData.Mode.COMMAND;
        commandGroup.setVisibility(isCommand ? View.VISIBLE : View.GONE);
        apiGroup.setVisibility(isCommand ? View.GONE : View.VISIBLE);
    }

    private ActionNodeDialog() {
    }
}