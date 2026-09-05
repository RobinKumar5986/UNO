package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.ChannelKey;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;

import java.util.List;

/**
 * DECISION node editor: a picker over {@link AppConstant#selectedSensors} and one button per
 * channel, as in the Action dialog. Tapping a channel points the node's single condition at it,
 * replacing whatever it pointed at before.
 *
 * <p>The operator buttons and the value box are always on screen — only the label above them and
 * the value itself change as channels are picked.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens.
 */
public class DecisionNodeDialog {

    /** Diameter of an operator button. Big enough to hit without aiming. */
    private static final int OPERATOR_SIZE_DP = 52;

    public static void show(Context context, DecisionNodeData data, Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Decision",
                "Which branch the flow takes when it reaches this step.",
                R.layout.dialog_decision_node, onChanged);

        bindOperators(dialog, data);
        bindValue(dialog, data);
        showCondition(dialog, data);
        setUpSensorPicker(dialog, data);

        dialog.show();
    }

    private static void setUpSensorPicker(Dialog dialog, DecisionNodeData data) {
        MaterialAutoCompleteTextView sensorInput = dialog.findViewById(R.id.decision_sensor_input);
        LinearLayout channels = dialog.findViewById(R.id.decision_sensor_channels);
        View sensorEmpty = dialog.findViewById(R.id.decision_sensor_empty);
        View valuesLabel = dialog.findViewById(R.id.decision_sensor_values_label);
        View channelsScroll = dialog.findViewById(R.id.decision_sensor_channels_scroll);

        List<PhoneSensor> selected = AppConstant.selectedSensors;

        if (selected.isEmpty()) {
            sensorEmpty.setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.decision_sensor_layout).setEnabled(false);
            sensorInput.setEnabled(false);
            valuesLabel.setVisibility(View.GONE);
            channelsScroll.setVisibility(View.GONE);
            return;
        }

        String[] names = new String[selected.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = selected.get(i).displayName;
        }
        // The dialog's context carries NodeDialogTheme; the host's may not be Material.
        sensorInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, names));

        PhoneSensor current = findByName(selected, data.sensorName);
        if (current == null) current = selected.get(0);

        data.sensorName = current.name;
        sensorInput.setText(current.displayName, false);
        bindChannels(dialog, channels, current, data);

        sensorInput.setOnItemClickListener((parent, view, position, id) -> {
            PhoneSensor picked = selected.get(position);
            data.sensorName = picked.name;
            bindChannels(dialog, channels, picked, data);
        });
    }

    private static void bindChannels(Dialog dialog, LinearLayout container, PhoneSensor sensor,
                                     DecisionNodeData data) {
        container.removeAllViews();

        int gap = dp(container, 8);

        for (int i = 0; i < sensor.channels.size(); i++) {
            SensorChannel channel = sensor.channels.get(i);
            TextView button = chip(container.getContext(), channel.displayName);
            paintChip(button, isCurrent(data, sensor, channel));
            button.setOnClickListener(v -> {
                data.condition.pointAt(sensor.name, channel.key.wireName);
                bindValue(dialog, data);
                showCondition(dialog, data);
                repaintChannels(container, data, sensor);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            // Leading gaps only, so the row stays optically centred.
            if (i > 0) params.setMarginStart(gap);
            container.addView(button, params);
        }
    }

    /** Marks whichever channel the condition points at, so the picker reads as a choice. */
    private static void repaintChannels(LinearLayout container, DecisionNodeData data,
                                        PhoneSensor sensor) {
        for (int i = 0; i < container.getChildCount() && i < sensor.channels.size(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                paintChip((TextView) child, isCurrent(data, sensor, sensor.channels.get(i)));
            }
        }
    }

    private static boolean isCurrent(DecisionNodeData data, PhoneSensor sensor,
                                     SensorChannel channel) {
        return data.condition.isSet()
                && sensor.name.equals(data.condition.sensorName)
                && channel.key.wireName.equals(data.condition.channelKey);
    }

    /** Label, unit and the hint below them — everything that depends on the picked channel. */
    private static void showCondition(Dialog dialog, DecisionNodeData data) {
        TextView label = dialog.findViewById(R.id.decision_condition_label);
        TextView unit = dialog.findViewById(R.id.decision_condition_unit);
        View emptyNote = dialog.findViewById(R.id.decision_condition_empty);

        SensorChannel channel = channelOf(data);
        label.setText(channel != null ? channel.displayName : "");
        unit.setText(channel != null ? channel.unit : "");
        emptyNote.setVisibility(data.condition.isSet() ? View.GONE : View.VISIBLE);
    }

    /** All four operators stay on screen; tapping one selects it and clears the rest. */
    private static void bindOperators(Dialog dialog, DecisionNodeData data) {
        LinearLayout container = dialog.findViewById(R.id.decision_condition_operators);
        container.removeAllViews();

        int size = dp(container, OPERATOR_SIZE_DP);
        int gap = dp(container, 10);
        TextView[] buttons = new TextView[DecisionNodeData.OPERATORS.length];

        for (int i = 0; i < DecisionNodeData.OPERATORS.length; i++) {
            String operator = DecisionNodeData.OPERATORS[i];
            TextView button = new TextView(container.getContext());
            button.setText(operator);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setClickable(true);
            paintOperator(button, operator.equals(data.condition.operator));
            buttons[i] = button;

            button.setOnClickListener(v -> {
                data.condition.operator = operator;
                for (int j = 0; j < buttons.length; j++) {
                    paintOperator(buttons[j],
                            DecisionNodeData.OPERATORS[j].equals(data.condition.operator));
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) params.setMarginStart(gap);
            container.addView(button, params);
        }
    }

    private static void bindValue(Dialog dialog, DecisionNodeData data) {
        EditText value = dialog.findViewById(R.id.decision_condition_value);

        // Set before the watcher, so restoring the stored text isn't read back as an edit.
        value.setText(data.condition.value);
        value.setSelection(value.length());

        if (value.getTag() != null) return;
        value.setTag(Boolean.TRUE);
        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                data.condition.value = s.toString();
            }
        });
    }

    private static TextView chip(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        return view;
    }

    /** setBackgroundResource wipes padding, so it is reapplied on every repaint. */
    private static void paintChip(TextView view, boolean selected) {
        view.setBackgroundResource(
                selected ? R.drawable.bg_channel_chip : R.drawable.bg_condition_input);
        view.setTextColor(ContextCompat.getColor(view.getContext(),
                selected ? R.color.accent_blue : R.color.text_secondary_light));
        view.setPadding(dp(view, 14), dp(view, 9), dp(view, 14), dp(view, 9));
    }

    /** Circular, and sized by its LayoutParams rather than padding so it stays round. */
    private static void paintOperator(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_operator_circle_selected : R.drawable.bg_operator_circle);
        view.setTextColor(ContextCompat.getColor(view.getContext(),
                selected ? R.color.accent_blue : R.color.text_secondary_light));
        view.setPadding(0, 0, 0, 0);
    }

    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density);
    }

    private static SensorChannel channelOf(DecisionNodeData data) {
        if (!data.condition.isSet()) return null;

        PhoneSensor sensor = SensorCatalog.byName(data.condition.sensorName);
        if (sensor == null) return null;
        return sensor.channel(ChannelKey.fromWireName(data.condition.channelKey));
    }

    private static PhoneSensor findByName(List<PhoneSensor> sensors, String name) {
        if (name == null || name.isEmpty()) return null;

        for (PhoneSensor sensor : sensors) {
            if (sensor.name.equals(name)) return sensor;
        }
        return null;
    }

    private DecisionNodeDialog() {
    }
}
