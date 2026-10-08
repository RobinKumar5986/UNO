package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;

import java.util.ArrayList;
import java.util.List;

/**
 * ACTION node editor: a mode dropdown (Command / Sensor / Received data / API) and a free-text
 * command box.
 *
 * <p>Sensor mode adds a picker over the selected sensors, Received data mode (offered inside a
 * trigger) lists the values the board sent. Each shows one button per value; tapping one drops
 * its token into the command at the caret, where {@link TokenChipEditText} shows it as a chip.
 * API is a stub — the section explains itself and edits nothing.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens
 * rather than collected on submit.
 */
public class ActionNodeDialog {

    private static final String COMMAND_HINT =
            "Sent to the device exactly as written when the flow reaches this step.";
    private static final String SENSOR_HINT =
            "Each sensor token is replaced with its live value before the line is sent.";
    private static final String RECEIVED_HINT =
            "Each received token is replaced with the value the board sent before the line is sent.";

    public static void show(Context context, ActionNodeData data, FlowScope scope,
                            Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Action",
                "What this step does when the flow reaches it.",
                R.layout.dialog_action_node, onChanged);

        MaterialAutoCompleteTextView modeInput = dialog.findViewById(R.id.action_mode_input);
        TokenChipEditText commandInput = dialog.findViewById(R.id.action_command_input);

        // Older projects stored received data as a Sensor-mode source; same command, new home.
        if (data.mode == ActionNodeData.Mode.SENSOR && ReceivedVars.isSource(data.sensorName)) {
            data.mode = ActionNodeData.Mode.RECEIVED;
        }

        List<ActionNodeData.Mode> modes = offeredModes(data.mode, scope);
        String[] labels = new String[modes.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = modes.get(i).label;

        // The dialog's context carries NodeDialogTheme; the host's may not be Material.
        modeInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, labels));
        // false = don't filter the list down to what's already in the field.
        modeInput.setText(data.mode.label, false);

        commandInput.setText(data.command);
        // Without this the caret sits at 0 and a channel chip would prepend its token.
        commandInput.setSelection(commandInput.length());
        List<ValueSource> sensors = ValueSource.sensors();
        ValueSource received = ValueSource.received(scope);
        setUpSensorPicker(dialog, data, sensors, commandInput);
        setUpReceived(dialog, data, received, commandInput);
        applyMode(dialog, data.mode);

        modeInput.setOnItemClickListener((parent, view, position, id) -> {
            data.mode = modes.get(position);
            // Re-run so the newly shown picker claims its source.
            if (data.mode == ActionNodeData.Mode.SENSOR) {
                setUpSensorPicker(dialog, data, sensors, commandInput);
            } else if (data.mode == ActionNodeData.Mode.RECEIVED) {
                setUpReceived(dialog, data, received, commandInput);
            }
            applyMode(dialog, data.mode);
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

    /** Received data only makes sense in a trigger, unless the node already uses it. */
    private static List<ActionNodeData.Mode> offeredModes(ActionNodeData.Mode current,
                                                          FlowScope scope) {
        List<ActionNodeData.Mode> modes = new ArrayList<>();
        for (ActionNodeData.Mode mode : ActionNodeData.Mode.values()) {
            if (mode == ActionNodeData.Mode.RECEIVED && !scope.isTrigger() && current != mode) {
                continue;
            }
            modes.add(mode);
        }
        return modes;
    }

    private static void setUpSensorPicker(Dialog dialog, ActionNodeData data,
                                          List<ValueSource> sources,
                                          TokenChipEditText commandInput) {
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

    private static void setUpReceived(Dialog dialog, ActionNodeData data, ValueSource received,
                                      TokenChipEditText commandInput) {
        boolean available = received != null && !received.items.isEmpty();

        dialog.findViewById(R.id.action_received_empty)
                .setVisibility(available ? View.GONE : View.VISIBLE);
        dialog.findViewById(R.id.action_received_values_label)
                .setVisibility(available ? View.VISIBLE : View.GONE);
        dialog.findViewById(R.id.action_received_channels_scroll)
                .setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) return;

        if (data.mode == ActionNodeData.Mode.RECEIVED) data.sensorName = received.name;
        bindItems(dialog.findViewById(R.id.action_received_channels), received, commandInput);
    }

    private static void bindItems(LinearLayout container, ValueSource source,
                                  TokenChipEditText commandInput) {
        container.removeAllViews();

        float density = container.getResources().getDisplayMetrics().density;
        int paddingH = (int) (14 * density);
        int paddingV = (int) (8 * density);
        int gap = (int) (8 * density);
        int accent = TokenChipSpan.accentFor(container.getContext(), source.name);

        for (ValueSource.Item item : source.items) {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(100 * density);
            background.setColor(ColorUtils.setAlphaComponent(accent, 0x26));
            background.setStroke((int) density, ColorUtils.setAlphaComponent(accent, 0x8C));

            TextView button = new TextView(container.getContext());
            button.setText("+  " + item.label);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setTextColor(accent);
            button.setBackground(background);
            button.setPadding(paddingH, paddingV, paddingH, paddingV);
            button.setClickable(true);
            button.setOnClickListener(v -> commandInput.insertToken(item.token));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(gap);
            container.addView(button, params);
        }
    }

    private static void applyMode(Dialog dialog, ActionNodeData.Mode mode) {
        boolean isApi = mode == ActionNodeData.Mode.API;
        boolean isSensor = mode == ActionNodeData.Mode.SENSOR;
        boolean isReceived = mode == ActionNodeData.Mode.RECEIVED;

        dialog.findViewById(R.id.action_command_group)
                .setVisibility(isApi ? View.GONE : View.VISIBLE);
        dialog.findViewById(R.id.action_sensor_group)
                .setVisibility(isSensor ? View.VISIBLE : View.GONE);
        dialog.findViewById(R.id.action_received_group)
                .setVisibility(isReceived ? View.VISIBLE : View.GONE);
        dialog.findViewById(R.id.action_api_group)
                .setVisibility(isApi ? View.VISIBLE : View.GONE);

        TextView hint = dialog.findViewById(R.id.action_command_hint);
        hint.setText(isSensor ? SENSOR_HINT : isReceived ? RECEIVED_HINT : COMMAND_HINT);
    }

    private ActionNodeDialog() {
    }
}
