package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kgjr.uno.R;

/** One labelled slider row, appended to a dialog's content column. */
class SliderRow {

    interface OnValueChanged {
        void onValue(int value);
    }

    static void add(LinearLayout parent, String label, int max, int value, OnValueChanged callback) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_slider_row, parent, false);

        ((TextView) row.findViewById(R.id.slider_label)).setText(label);
        TextView valueView = row.findViewById(R.id.slider_value);
        SeekBar seekBar = row.findViewById(R.id.slider_seek);

        seekBar.setMax(max);
        seekBar.setProgress(value);
        valueView.setText(String.valueOf(value));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                valueView.setText(String.valueOf(progress));
                callback.onValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        parent.addView(row);
    }

    private SliderRow() {
    }
}
