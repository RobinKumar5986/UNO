package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
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
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;

import java.util.List;

/**
 * DECISION node editor: a picker over the available value sources (selected sensors, plus the
 * received data inside a trigger) and one button per value. Tapping a value points the node's
 * single condition at it, replacing whatever it pointed at before.
 *
 * <p>The frame has no Done button, so every change is written into {@code data} as it happens.
 */
public class DecisionNodeDialog {

    /** Diameter of an operator button. Big enough to hit without aiming. */
    private static final int OPERATOR_SIZE_DP = 52;

    private static final String[] TEXT_OPERATORS = {"==", "!="};

    private static final int NUMBER_INPUT =
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | InputType.TYPE_NUMBER_FLAG_SIGNED;
    private static final int TEXT_INPUT =
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    public static void show(Context context, DecisionNodeData data, FlowScope scope,
                            Runnable onChanged) {
        Dialog dialog = NodeDialogFrame.create(context, "Decision",
                "Which branch the flow takes when it reaches this step.",
                R.layout.dialog_decision_node, onChanged);

        List<ValueSource> sources = ValueSource.available(scope);

        bindValue(dialog, data, false);
        refreshCondition(dialog, data, sources);
        setUpSourcePicker(dialog, data, sources);

        dialog.show();
    }

    private static void setUpSourcePicker(Dialog dialog, DecisionNodeData data,
                                          List<ValueSource> sources) {
        MaterialAutoCompleteTextView sourceInput = dialog.findViewById(R.id.decision_sensor_input);
        LinearLayout channels = dialog.findViewById(R.id.decision_sensor_channels);

        if (sources.isEmpty()) {
            dialog.findViewById(R.id.decision_sensor_empty).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.decision_sensor_layout).setEnabled(false);
            sourceInput.setEnabled(false);
            dialog.findViewById(R.id.decision_sensor_values_label).setVisibility(View.GONE);
            dialog.findViewById(R.id.decision_sensor_channels_scroll).setVisibility(View.GONE);
            return;
        }

        String[] names = new String[sources.size()];
        for (int i = 0; i < names.length; i++) names[i] = sources.get(i).displayName;

        // The dialog's context carries NodeDialogTheme; the host's may not be Material.
        sourceInput.setAdapter(new ArrayAdapter<>(dialog.getContext(),
                android.R.layout.simple_list_item_1, names));

        ValueSource current = ValueSource.find(sources, data.sensorName);
        if (current == null) current = sources.get(0);

        data.sensorName = current.name;
        sourceInput.setText(current.displayName, false);
        bindItems(dialog, channels, current, data, sources);

        sourceInput.setOnItemClickListener((parent, view, position, id) -> {
            ValueSource picked = sources.get(position);
            data.sensorName = picked.name;
            bindItems(dialog, channels, picked, data, sources);
        });
    }

    private static void bindItems(Dialog dialog, LinearLayout container, ValueSource source,
                                  DecisionNodeData data, List<ValueSource> sources) {
        container.removeAllViews();

        int gap = dp(container, 8);

        for (int i = 0; i < source.items.size(); i++) {
            ValueSource.Item item = source.items.get(i);
            TextView button = chip(container.getContext(), item.label);
            paintChip(button, isCurrent(data, source, item));
            button.setOnClickListener(v -> {
                data.condition.pointAt(source.name, item.key);
                bindValue(dialog, data, item.text);
                refreshCondition(dialog, data, sources);
                repaintItems(container, data, source);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            // Leading gaps only, so the row stays optically centred.
            if (i > 0) params.setMarginStart(gap);
            container.addView(button, params);
        }
    }

    private static void repaintItems(LinearLayout container, DecisionNodeData data,
                                     ValueSource source) {
        for (int i = 0; i < container.getChildCount() && i < source.items.size(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                paintChip((TextView) child, isCurrent(data, source, source.items.get(i)));
            }
        }
    }

    private static boolean isCurrent(DecisionNodeData data, ValueSource source,
                                     ValueSource.Item item) {
        return data.condition.isSet()
                && source.name.equals(data.condition.sensorName)
                && item.key.equals(data.condition.channelKey);
    }

    /** Label, unit, operators and value type: everything that depends on the picked value. */
    private static void refreshCondition(Dialog dialog, DecisionNodeData data,
                                         List<ValueSource> sources) {
        TextView label = dialog.findViewById(R.id.decision_condition_label);
        TextView unit = dialog.findViewById(R.id.decision_condition_unit);
        View emptyNote = dialog.findViewById(R.id.decision_condition_empty);

        ValueSource.Item item = currentItem(data, sources);
        boolean text = item != null && item.text;

        label.setText(item != null ? item.label : "");
        unit.setText(item != null ? item.unit : "");
        emptyNote.setVisibility(data.condition.isSet() ? View.GONE : View.VISIBLE);

        if (text && !isTextOperator(data.condition.operator)) data.condition.operator = "==";
        bindOperators(dialog, data, text);
        setValueInputType(dialog, text);
    }

    /** All four operators stay on screen; a text value greys out the ones it can't use. */
    private static void bindOperators(Dialog dialog, DecisionNodeData data, boolean text) {
        LinearLayout container = dialog.findViewById(R.id.decision_condition_operators);
        container.removeAllViews();

        int size = dp(container, OPERATOR_SIZE_DP);
        int gap = dp(container, 10);
        TextView[] buttons = new TextView[DecisionNodeData.OPERATORS.length];

        for (int i = 0; i < DecisionNodeData.OPERATORS.length; i++) {
            String operator = DecisionNodeData.OPERATORS[i];
            boolean usable = !text || isTextOperator(operator);

            TextView button = new TextView(container.getContext());
            button.setText(operator);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setClickable(usable);
            button.setEnabled(usable);
            button.setAlpha(usable ? 1f : 0.35f);
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

    private static void bindValue(Dialog dialog, DecisionNodeData data, boolean text) {
        EditText value = dialog.findViewById(R.id.decision_condition_value);

        // Set before the watcher, so restoring the stored text isn't read back as an edit.
        setValueInputType(dialog, text);
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

    private static void setValueInputType(Dialog dialog, boolean text) {
        EditText value = dialog.findViewById(R.id.decision_condition_value);
        int type = text ? TEXT_INPUT : NUMBER_INPUT;
        if (value.getInputType() != type) value.setInputType(type);
        value.setHint(text ? "text" : "0");
    }

    private static boolean isTextOperator(String operator) {
        for (String allowed : TEXT_OPERATORS) {
            if (allowed.equals(operator)) return true;
        }
        return false;
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

    private static ValueSource.Item currentItem(DecisionNodeData data, List<ValueSource> sources) {
        if (!data.condition.isSet()) return null;

        ValueSource source = ValueSource.find(sources, data.condition.sensorName);
        return source == null ? null : source.item(data.condition.channelKey);
    }

    private DecisionNodeDialog() {
    }
}
