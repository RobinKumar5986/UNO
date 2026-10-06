package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;

import java.util.List;

/**
 * ACTION node editor: a mode dropdown (Command / Sensor / API) and a free-text command box.
 *
 * <p>Sensor mode adds a picker over the {@link ValueSource}s (selected sensors, plus received
 * data inside a trigger) and one button per value; tapping one drops its token into the command
 * at the caret. API is a stub — the section explains itself and edits nothing.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens
 * rather than collected on submit.
 */
public class ActionNodeDialog {

    private static final String COMMAND_HINT =
            "Sent to the device exactly as written when the flow reaches this step.";
    private static final String SENSOR_HINT =
            "Each [source: value] token is replaced with its live or received value before "
                    + "the line is sent.";

    public static void show(Context context, ActionNodeData data, FlowScope scope,
                            Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Action",
                "What this step does when the flow reaches it.",
                R.layout.dialog_action_node, onChanged);

        MaterialAutoCompleteTextView modeInput = dialog.findViewById(R.id.action_mode_input);
        TextInputEditText commandInput = dialog.findViewById(R.id.action_command_input);
        TextView commandHint = dialog.findViewById(R.id.action_command_hint);
        View commandGroup = dialog.findViewById(R.id.action_command_group);
        View sensorGroup = dialog.findViewById(R.id.action_sensor_group);
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
        // Without this the caret sits at 0 and a channel chip would prepend its token.
        commandInput.setSelection(commandInput.length());
        List<ValueSource> sources = ValueSource.available(scope);
        setUpSensorPicker(dialog, data, sources, commandInput);
        applyMode(data.mode, commandGroup, sensorGroup, apiGroup, commandHint);

        modeInput.setOnItemClickListener((parent, view, position, id) -> {
            data.mode = ActionNodeData.Mode.values()[position];
            // Re-run so switching into Sensor mode claims the sensor now on show.
            if (data.mode == ActionNodeData.Mode.SENSOR) {
                setUpSensorPicker(dialog, data, sources, commandInput);
            }
            applyMode(data.mode, commandGroup, sensorGroup, apiGroup, commandHint);
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

    private static void setUpSensorPicker(Dialog dialog, ActionNodeData data,
                                          List<ValueSource> sources,
                                          TextInputEditText commandInput) {
        MaterialAutoCompleteTextView sourceInput = dialog.findViewById(R.id.action_sensor_input);
        LinearLayout channels = dialog.findViewById(R.id.action_sensor_channels);

        if (sources.isEmpty()) {
            dialog.findViewById(R.id.action_sensor_empty).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.action_sensor_layout).setEnabled(false);
            sourceInput.setEnabled(false);
            dialog.findViewById(R.id.action_sensor_values_label).setVisibility(View.GONE);
            dialog.findViewById(R.id.action_sensor_channels_scroll).setVisibility(View.GONE);
            return;
        }

        String[] names = new String[sources.size()];
        for (int i = 0; i < names.length; i++) names[i] = sources.get(i).displayName;

        sourceInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, names));

        ValueSource current = ValueSource.find(sources, data.sensorName);
        if (current == null) current = sources.get(0);

        // Only claim a source for a node that is actually in Sensor mode, so merely opening a
        // Command node doesn't write one in.
        if (data.mode == ActionNodeData.Mode.SENSOR) data.sensorName = current.name;

        sourceInput.setText(current.displayName, false);
        bindItems(channels, current, commandInput);

        sourceInput.setOnItemClickListener((parent, view, position, id) -> {
            ValueSource picked = sources.get(position);
            data.sensorName = picked.name;
            bindItems(channels, picked, commandInput);
        });
    }

    private static void bindItems(LinearLayout container, ValueSource source,
                                  TextInputEditText commandInput) {
        container.removeAllViews();

        float density = container.getResources().getDisplayMetrics().density;
        int paddingH = (int) (14 * density);
        int paddingV = (int) (9 * density);
        int gap = (int) (8 * density);

        for (ValueSource.Item item : source.items) {
            TextView button = new TextView(container.getContext());
            button.setText(item.label);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setTextColor(ContextCompat.getColor(container.getContext(), R.color.accent_blue));
            button.setBackgroundResource(R.drawable.bg_channel_chip);
            button.setPadding(paddingH, paddingV, paddingH, paddingV);
            button.setClickable(true);
            button.setOnClickListener(v -> insertAtCaret(commandInput, item.token));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(gap);
            container.addView(button, params);
        }
    }

    /** Drops the token in at the caret, replacing any selection, and leaves the caret after it. */
    private static void insertAtCaret(TextInputEditText input, String token) {
        Editable text = input.getText();
        if (text == null) {
            input.setText(token);
            return;
        }

        int start = Math.max(input.getSelectionStart(), 0);
        int end = Math.max(input.getSelectionEnd(), 0);

        text.replace(Math.min(start, end), Math.max(start, end), token);
        input.setSelection(Math.min(start, end) + token.length());
        input.requestFocus();
    }

    private static void applyMode(ActionNodeData.Mode mode, View commandGroup, View sensorGroup,
                                  View apiGroup, TextView commandHint) {
        boolean isApi = mode == ActionNodeData.Mode.API;
        boolean isSensor = mode == ActionNodeData.Mode.SENSOR;

        commandGroup.setVisibility(isApi ? View.GONE : View.VISIBLE);
        sensorGroup.setVisibility(isSensor ? View.VISIBLE : View.GONE);
        apiGroup.setVisibility(isApi ? View.VISIBLE : View.GONE);
        commandHint.setText(isSensor ? SENSOR_HINT : COMMAND_HINT);
    }

    private ActionNodeDialog() {
    }
}
