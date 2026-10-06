package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.model.Escapes;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerStartData;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerFormats;

/**
 * Start node of a trigger: what message from the board fires it. Edits are stored as typed,
 * even while invalid, so the user never loses text; Next refuses to continue until it's fixed.
 */
public class TriggerStartDialog {

    public static void show(Context context, FlowScope scope, Runnable onChanged) {
        TriggerStartData data = scope.trigger.receive();
        int index = scope.triggers.indexOf(scope.trigger);

        Dialog dialog = NodeDialogFrame.create(context, TriggerFormats.label(Math.max(index, 0)),
                "Runs once each time the board sends a message in this format.",
                R.layout.dialog_node_trigger_start, onChanged);

        TextInputLayout payloadLayout = dialog.findViewById(R.id.trigger_payload_layout);
        TextInputLayout startLayout = dialog.findViewById(R.id.trigger_start_layout);
        TextInputLayout endLayout = dialog.findViewById(R.id.trigger_end_layout);
        TextInputEditText payloadInput = dialog.findViewById(R.id.trigger_payload_input);
        TextInputEditText startInput = dialog.findViewById(R.id.trigger_start_input);
        TextInputEditText endInput = dialog.findViewById(R.id.trigger_end_input);

        Runnable refresh = () -> {
            data.payload = text(payloadInput);
            data.startMarker = text(startInput);
            data.endMarker = text(endInput);

            payloadLayout.setError(PayloadFormat.problemOf(data.payload));
            startLayout.setError(Escapes.firstProblem(data.startMarker));
            endLayout.setError(endProblem(data.endMarker));

            showPreview(dialog, data);
            showConflict(dialog, scope);
        };

        payloadInput.setText(data.payload);
        startInput.setText(data.startMarker);
        endInput.setText(data.endMarker);
        payloadInput.addTextChangedListener(watcher(refresh));
        startInput.addTextChangedListener(watcher(refresh));
        endInput.addTextChangedListener(watcher(refresh));
        refresh.run();

        // Each setText re-reads all three fields, so the defaults come from a fresh copy.
        dialog.findViewById(R.id.trigger_reset).setOnClickListener(v -> {
            TriggerStartData defaults = new TriggerStartData();
            payloadInput.setText(defaults.payload);
            startInput.setText(defaults.startMarker);
            endInput.setText(defaults.endMarker);
        });

        dialog.show();
    }

    private static String endProblem(String endMarker) {
        String problem = Escapes.firstProblem(endMarker);
        if (problem != null) return problem;
        return Escapes.decode(endMarker).length == 0 ? "Needed to know where a message stops." : null;
    }

    private static void showPreview(Dialog dialog, TriggerStartData data) {
        TextView example = dialog.findViewById(R.id.trigger_example);
        LinearLayout vars = dialog.findViewById(R.id.trigger_vars);
        View varsLabel = dialog.findViewById(R.id.trigger_vars_label);
        View varsScroll = dialog.findViewById(R.id.trigger_vars_scroll);

        vars.removeAllViews();

        PayloadFormat format;
        try {
            format = PayloadFormat.parse(data.payload);
        } catch (IllegalArgumentException e) {
            example.setText("—");
            varsLabel.setVisibility(View.GONE);
            varsScroll.setVisibility(View.GONE);
            return;
        }

        example.setText(Escapes.visible(Escapes.decode(data.startMarker))
                + format.example()
                + Escapes.visible(Escapes.decode(data.endMarker)));

        varsLabel.setVisibility(View.VISIBLE);
        varsScroll.setVisibility(View.VISIBLE);

        int gap = dp(vars, 8);
        for (int i = 0; i < format.size(); i++) {
            TextView chip = varChip(vars.getContext(),
                    ReceivedVars.label(i) + " · " + format.types().get(i).label);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(gap);
            vars.addView(chip, params);
        }
    }

    private static void showConflict(Dialog dialog, FlowScope scope) {
        TextView status = dialog.findViewById(R.id.trigger_status);
        String conflict = TriggerFormats.conflictMessage(scope.trigger, scope.triggers);

        status.setText(conflict);
        status.setVisibility(conflict == null ? View.GONE : View.VISIBLE);
    }

    private static TextView varChip(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setTextColor(ContextCompat.getColor(context, R.color.accent_blue));
        view.setBackgroundResource(R.drawable.bg_channel_chip);
        view.setPadding(dp(view, 14), dp(view, 9), dp(view, 14), dp(view, 9));
        return view;
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density);
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

    private TriggerStartDialog() {
    }
}
