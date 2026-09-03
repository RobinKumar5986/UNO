package com.kgjr.uno.screens.fragments.sensorHelper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorType;

import java.util.List;
import java.util.Locale;

/** Everything the phone will tell us about a sensor, shown in a bottom sheet. */
public class SensorInfoSheet {

    private SensorInfoSheet() {
    }

    public static void show(Context context, PhoneSensor sensor) {
        View content = LayoutInflater.from(context).inflate(R.layout.sheet_sensor_info, null);

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(content);
        expandFully(dialog, context);

        bindHeader(context, content, sensor);
        bindOutputs(context, content.findViewById(R.id.infoOutputsContainer), sensor);
        bindHardware(context, content.findViewById(R.id.infoHardwareContainer), sensor);

        content.findViewById(R.id.infoSensorClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * The app runs landscape, where a collapsed sheet is barely a strip. Capping the height also
     * keeps the content inside the scroll view rather than running off the bottom.
     */
    private static void expandFully(BottomSheetDialog dialog, Context context) {
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setMaxHeight((int) (screenHeight * 0.92f));
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private static void bindHeader(Context context, View content, PhoneSensor sensor) {
        ImageView image = content.findViewById(R.id.infoSensorImage);
        int drawable = sensor.imageRes(context);
        image.setImageResource(drawable != 0 ? drawable : R.drawable.ic_sensors);

        ((TextView) content.findViewById(R.id.infoSensorName)).setText(sensor.displayName);
        ((TextView) content.findViewById(R.id.infoSensorDescription)).setText(sensor.description);

        TextView badge = content.findViewById(R.id.infoSensorTypeBadge);
        badge.setText(sensor.type.label);
        badge.setBackgroundResource(sensor.type == SensorType.OUTPUT
                ? R.drawable.bg_sensor_badge_output
                : R.drawable.bg_sensor_badge_input);
    }

    private static void bindOutputs(Context context, LinearLayout container, PhoneSensor sensor) {
        for (SensorChannel channel : sensor.channels) {
            String range = channel.isBounded()
                    ? channel.rangeLabel()
                    : context.getString(R.string.sensors_info_no_range);
            addRow(context, container, channel.displayName, range);
        }
    }

    private static void bindHardware(Context context, LinearLayout container, PhoneSensor sensor) {
        SensorManager manager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> backing = sensor.resolve(manager);

        if (backing.isEmpty()) {
            addRow(context, container, context.getString(R.string.sensors_info_status),
                    context.getString(R.string.sensors_unavailable));
            return;
        }

        for (Sensor hardware : backing) {
            if (hardware != null) addHardwareGroup(context, container, hardware);
        }
    }

    private static void addHardwareGroup(Context context, LinearLayout container, Sensor hardware) {
        View group = LayoutInflater.from(context)
                .inflate(R.layout.item_sensor_info_group, container, false);

        ((TextView) group.findViewById(R.id.infoGroupTitle)).setText(hardware.getName());

        LinearLayout start = group.findViewById(R.id.infoGroupColumnStart);
        LinearLayout end = group.findViewById(R.id.infoGroupColumnEnd);

        addRow(context, start, R.string.sensors_info_vendor, hardware.getVendor());
        addRow(context, start, R.string.sensors_info_type, typeName(hardware));
        addRow(context, start, R.string.sensors_info_version, String.valueOf(hardware.getVersion()));
        addRow(context, start, R.string.sensors_info_max_range, decimal(hardware.getMaximumRange()));
        addRow(context, start, R.string.sensors_info_resolution, decimal(hardware.getResolution()));
        addRow(context, start, R.string.sensors_info_power,
                decimal(hardware.getPower()) + " mA");

        addRow(context, end, R.string.sensors_info_min_delay, delay(context, hardware.getMinDelay()));
        addRow(context, end, R.string.sensors_info_max_delay, delay(context, hardware.getMaxDelay()));
        addRow(context, end, R.string.sensors_info_reporting, reportingMode(context, hardware));
        addRow(context, end, R.string.sensors_info_wake_up, yesNo(context, hardware.isWakeUpSensor()));
        addRow(context, end, R.string.sensors_info_fifo,
                hardware.getFifoReservedEventCount() + " / " + hardware.getFifoMaxEventCount());
        addRow(context, end, R.string.sensors_info_dynamic, yesNo(context, hardware.isDynamicSensor()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addRow(context, end, R.string.sensors_info_direct_report,
                    String.valueOf(hardware.getHighestDirectReportRateLevel()));
        }

        container.addView(group);
    }

    private static void addRow(Context context, LinearLayout container, int labelRes, String value) {
        addRow(context, container, context.getString(labelRes), value);
    }

    private static void addRow(Context context, LinearLayout container, String label, String value) {
        View row = LayoutInflater.from(context)
                .inflate(R.layout.item_sensor_info_row, container, false);

        ((TextView) row.findViewById(R.id.infoRowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.infoRowValue)).setText(blankToDash(context, value));

        container.addView(row);
    }

    /** Strips Android's "android.sensor." prefix, falling back to the numeric type. */
    private static String typeName(Sensor hardware) {
        String type = hardware.getStringType();
        if (type == null) return String.valueOf(hardware.getType());

        int dot = type.lastIndexOf('.');
        return dot >= 0 ? type.substring(dot + 1).replace('_', ' ') : type;
    }

    /** Microseconds, with the rate it works out to, since that's the useful part. */
    private static String delay(Context context, int microseconds) {
        if (microseconds <= 0) return context.getString(R.string.sensors_info_not_reported);

        int hz = Math.round(1_000_000f / microseconds);
        return microseconds + " µs (" + hz + " Hz)";
    }

    private static String reportingMode(Context context, Sensor hardware) {
        switch (hardware.getReportingMode()) {
            case Sensor.REPORTING_MODE_CONTINUOUS:
                return context.getString(R.string.sensors_info_mode_continuous);
            case Sensor.REPORTING_MODE_ON_CHANGE:
                return context.getString(R.string.sensors_info_mode_on_change);
            case Sensor.REPORTING_MODE_ONE_SHOT:
                return context.getString(R.string.sensors_info_mode_one_shot);
            case Sensor.REPORTING_MODE_SPECIAL_TRIGGER:
                return context.getString(R.string.sensors_info_mode_special);
            default:
                return context.getString(R.string.sensors_info_unknown);
        }
    }

    /** Resolutions are often around 1e-8, so anything very small goes to scientific notation. */
    private static String decimal(float value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        if (Math.abs(value) < 0.001f) return String.format(Locale.US, "%.3e", value);

        String text = String.format(Locale.US, "%.4f", value).replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static String yesNo(Context context, boolean value) {
        return context.getString(value ? R.string.sensors_info_yes : R.string.sensors_info_no);
    }

    private static String blankToDash(Context context, String value) {
        return value == null || value.trim().isEmpty()
                ? context.getString(R.string.sensors_info_unknown)
                : value;
    }
}
