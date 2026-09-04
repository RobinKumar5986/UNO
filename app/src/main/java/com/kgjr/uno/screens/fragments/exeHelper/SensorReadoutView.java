package com.kgjr.uno.screens.fragments.exeHelper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Live card for one sensor: its name, and per channel the current value, a meter showing where
 * that value sits in the channel's range, and the token to paste into an Action command.
 *
 * <p>Bind once with {@link #bind}, then call {@link #refresh} as often as the screen ticks.
 */
public class SensorReadoutView extends LinearLayout {

    /** Meter resolution — a value's position in range is mapped onto 0..this. */
    private static final int METER_STEPS = 1000;

    private final TextView nameView;
    private final TextView statusView;
    private final ImageView imageView;
    private final LinearLayout channelsView;

    private final List<ChannelRow> rows = new ArrayList<>();

    private PhoneSensor sensor;

    public SensorReadoutView(@NonNull Context context) {
        this(context, null);
    }

    public SensorReadoutView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_readout_card);

        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_grid);
        setPadding(padding, padding, padding, padding);

        LayoutInflater.from(context).inflate(R.layout.view_sensor_readout, this, true);

        nameView = findViewById(R.id.readoutName);
        statusView = findViewById(R.id.readoutStatus);
        imageView = findViewById(R.id.readoutImage);
        channelsView = findViewById(R.id.readoutChannels);
    }

    public void bind(PhoneSensor sensor) {
        this.sensor = sensor;

        nameView.setText(sensor.displayName);

        int drawable = sensor.imageRes(getContext());
        imageView.setImageResource(drawable != 0 ? drawable : R.drawable.ic_sensors);

        rows.clear();
        channelsView.removeAllViews();

        for (SensorChannel channel : sensor.channels) {
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.view_sensor_readout_channel, channelsView, false);

            ChannelRow bound = new ChannelRow(sensor, channel, row);
            rows.add(bound);
            channelsView.addView(row);
        }
    }

    /** Pulls the current value for every channel and repaints. */
    public void refresh(SensorLiveReadingHelper sensors) {
        if (sensor == null) return;

        boolean anyLive = false;
        for (ChannelRow row : rows) {
            if (row.update(sensors == null ? null : sensors.read(sensor, row.channel))) {
                anyLive = true;
            }
        }

        statusView.setText(anyLive ? R.string.exe_readout_live : R.string.exe_readout_waiting);
        statusView.setTextColor(ContextCompat.getColor(getContext(),
                anyLive ? R.color.color_success : R.color.text_secondary_dark));
    }

    /** One channel's views, kept so a refresh doesn't re-find them. */
    private static final class ChannelRow {

        private final SensorChannel channel;
        private final TextView value;
        private final ProgressBar meter;

        ChannelRow(PhoneSensor sensor, SensorChannel channel, View row) {
            this.channel = channel;
            this.value = row.findViewById(R.id.readoutChannelValue);
            this.meter = row.findViewById(R.id.readoutChannelMeter);

            ((TextView) row.findViewById(R.id.readoutChannelName)).setText(channel.displayName);
            ((TextView) row.findViewById(R.id.readoutChannelUnit)).setText(channel.unit);

            // The exact text an Action command needs to read this channel, tap to copy.
            String token = SensorToken.of(sensor, channel);
            TextView tokenView = row.findViewById(R.id.readoutChannelToken);
            tokenView.setText(token);
            tokenView.setOnClickListener(v -> copy(v, token));

            meter.setMax(METER_STEPS);
            // An unbounded channel has nothing to draw a meter against.
            meter.setVisibility(channel.isBounded() ? View.VISIBLE : View.GONE);
        }

        /** Returns true when there was a reading to show. */
        boolean update(Float reading) {
            if (reading == null) {
                value.setText(R.string.exe_readout_no_value);
                meter.setProgress(0);
                return false;
            }

            float clamped = channel.clamp(reading);
            value.setText(SensorLiveReadingHelper.format(clamped));

            if (channel.isBounded()) {
                float span = channel.max - channel.min;
                float fraction = span == 0f ? 0f : (clamped - channel.min) / span;
                meter.setProgress(Math.round(fraction * METER_STEPS));
            }
            return true;
        }

        private static void copy(View anchor, String token) {
            ClipboardManager clipboard = (ClipboardManager)
                    anchor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;

            clipboard.setPrimaryClip(ClipData.newPlainText("sensor token", token));
            Toast.makeText(anchor.getContext(),
                    anchor.getContext().getString(R.string.exe_readout_copied, token),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
