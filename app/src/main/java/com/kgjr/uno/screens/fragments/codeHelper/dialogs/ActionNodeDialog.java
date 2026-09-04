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
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorToken;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;

import java.util.List;

/**
 * ACTION node editor: a mode dropdown (Command / Sensor / API) and a free-text command box.
 *
 * <p>Sensor mode adds a picker over {@link AppConstant#selectedSensors} and one button per
 * channel; tapping a button drops that channel's {@link SensorToken} into the command at the
 * caret. API is a stub — the section explains itself and edits nothing.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens
 * rather than collected on submit.
 */
public class ActionNodeDialog {

    private static final String COMMAND_HINT =
            "Sent to the device exactly as written when the flow reaches this step.";
    private static final String SENSOR_HINT =
            "Each [sensor: value] token is replaced with that sensor's live reading before "
                    + "the line is sent.";

    public static void show(Context context, ActionNodeData data, Runnable onChanged) {
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
        setUpSensorPicker(dialog, data, commandInput);
        applyMode(data.mode, commandGroup, sensorGroup, apiGroup, commandHint);

        modeInput.setOnItemClickListener((parent, view, position, id) -> {
            data.mode = ActionNodeData.Mode.values()[position];
            // Re-run so switching into Sensor mode claims the sensor now on show.
            if (data.mode == ActionNodeData.Mode.SENSOR) {
                setUpSensorPicker(dialog, data, commandInput);
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
                                          TextInputEditText commandInput) {
        MaterialAutoCompleteTextView sensorInput = dialog.findViewById(R.id.action_sensor_input);
        LinearLayout channels = dialog.findViewById(R.id.action_sensor_channels);
        View emptyNote = dialog.findViewById(R.id.action_sensor_empty);
        View valuesLabel = dialog.findViewById(R.id.action_sensor_values_label);
        View channelsScroll = dialog.findViewById(R.id.action_sensor_channels_scroll);

        List<PhoneSensor> selected = AppConstant.selectedSensors;

        if (selected.isEmpty()) {
            emptyNote.setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.action_sensor_layout).setEnabled(false);
            sensorInput.setEnabled(false);
            valuesLabel.setVisibility(View.GONE);
            channelsScroll.setVisibility(View.GONE);
            return;
        }

        String[] names = new String[selected.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = selected.get(i).displayName;
        }
        sensorInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, names));

        PhoneSensor current = findByName(selected, data.sensorName);
        if (current == null) current = selected.get(0);

        // Only claim a sensor for a node that is actually in Sensor mode, so merely opening a
        // Command node doesn't write one in.
        if (data.mode == ActionNodeData.Mode.SENSOR) data.sensorName = current.name;

        sensorInput.setText(current.displayName, false);
        bindChannels(channels, current, commandInput);

        sensorInput.setOnItemClickListener((parent, view, position, id) -> {
            PhoneSensor picked = selected.get(position);
            data.sensorName = picked.name;
            bindChannels(channels, picked, commandInput);
        });
    }

    private static void bindChannels(LinearLayout container, PhoneSensor sensor,
                                     TextInputEditText commandInput) {
        container.removeAllViews();

        float density = container.getResources().getDisplayMetrics().density;
        int paddingH = (int) (14 * density);
        int paddingV = (int) (9 * density);
        int gap = (int) (8 * density);

        for (SensorChannel channel : sensor.channels) {
            TextView button = new TextView(container.getContext());
            button.setText(channel.displayName);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setTextColor(ContextCompat.getColor(container.getContext(), R.color.accent_blue));
            button.setBackgroundResource(R.drawable.bg_channel_chip);
            button.setPadding(paddingH, paddingV, paddingH, paddingV);
            button.setClickable(true);
            button.setOnClickListener(v ->
                    insertAtCaret(commandInput, SensorToken.of(sensor, channel)));

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

    private static PhoneSensor findByName(List<PhoneSensor> sensors, String name) {
        if (name == null || name.isEmpty()) return null;

        for (PhoneSensor sensor : sensors) {
            if (sensor.name.equals(name)) return sensor;
        }
        return null;
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
