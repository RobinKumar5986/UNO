package com.kgjr.uno.screens.fragments.exeHelper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;

import com.kgjr.uno.models.sensors.ChannelKey;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * Live readings for every sensor a program can reference, and the substitution that turns
 * {@code [orientation: azimuth]} in a command into the current value.
 *
 * <p>Hardware events land on the main thread while the runner reads from its own thread, so the
 * raw values are held in a concurrent map and copied on every event.
 *
 * <p>Adding a sensor means adding a {@code read...} method and one case to {@link #read}.
 */
public final class SensorLiveReadingHelper implements SensorEventListener {

    public interface Listener {
        void onLog(String message);
    }

    /** Substituted when a token names a sensor that has not reported anything yet. */
    private static final String NO_READING = "0";

    private final SensorManager manager;
    private final Listener listener;

    /** Latest raw values per hardware sensor type, e.g. TYPE_ROTATION_VECTOR. */
    private final Map<Integer, float[]> readings = new ConcurrentHashMap<>();

    private final List<Sensor> registered = new ArrayList<>();

    public SensorLiveReadingHelper(Context context, Listener listener) {
        this.manager = SensorCatalog.sensorManager(context);
        this.listener = listener;
    }

    // ------------------------------------------------------------- lifecycle

    /** Starts listening to the hardware behind each sensor. Safe to call more than once. */
    public void start(List<PhoneSensor> sensors) {
        if (manager == null || sensors == null) return;
        stop();

        for (PhoneSensor sensor : sensors) {
            for (Sensor hardware : sensor.resolve(manager)) {
                if (hardware == null || registered.contains(hardware)) continue;

                manager.registerListener(this, hardware, SensorManager.SENSOR_DELAY_GAME);
                registered.add(hardware);
            }
        }
    }

    public void stop() {
        if (manager != null && !registered.isEmpty()) manager.unregisterListener(this);
        registered.clear();
        readings.clear();
    }

    // ----------------------------------------------------------- substitution

    /** Replaces every {@code [sensor: channel]} token in the command with its current reading. */
    public String resolveTokens(String command) {
        if (command == null || command.isEmpty()) return command;

        Matcher matcher = SensorToken.PATTERN.matcher(command);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            SensorToken.Resolved target = SensorToken.resolve(matcher);

            if (target == null) {
                log("Unknown sensor token " + matcher.group() + ", left as-is");
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            Float value = read(target.sensor, target.channel);
            if (value == null) {
                log("No reading yet for " + matcher.group() + ", sent " + NO_READING);
                matcher.appendReplacement(out, NO_READING);
                continue;
            }
            matcher.appendReplacement(out, format(target.channel.clamp(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    // -------------------------------------------------------------- readings

    /** The channel's current value, or null when the sensor hasn't reported yet. */
    @Nullable
    public Float read(PhoneSensor sensor, SensorChannel channel) {
        if (sensor == null || channel == null) return null;

        if (SensorCatalog.ORIENTATION.equals(sensor)) return readOrientation(channel.key);
        return null;
    }

    /** Compass heading, 0–360° clockwise from magnetic north. */
    @Nullable
    private Float readOrientation(ChannelKey key) {
        if (key != ChannelKey.AZIMUTH) return null;

        float[] rotation = new float[9];
        if (!orientationMatrix(rotation)) return null;

        float[] angles = new float[3];
        SensorManager.getOrientation(rotation, angles);

        float degrees = (float) Math.toDegrees(angles[0]);
        return (degrees + 360f) % 360f;
    }

    /** Rotation vector where the phone has one, otherwise accelerometer plus magnetometer. */
    private boolean orientationMatrix(float[] out) {
        float[] vector = readings.get(Sensor.TYPE_ROTATION_VECTOR);
        if (vector != null) {
            SensorManager.getRotationMatrixFromVector(out, vector);
            return true;
        }

        float[] gravity = readings.get(Sensor.TYPE_ACCELEROMETER);
        float[] magnetic = readings.get(Sensor.TYPE_MAGNETIC_FIELD);
        if (gravity == null || magnetic == null) return false;

        return SensorManager.getRotationMatrix(out, null, gravity, magnetic);
    }

    // --------------------------------------------------------------- events

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;

        float[] copy = new float[event.values.length];
        System.arraycopy(event.values, 0, copy, 0, copy.length);
        readings.put(event.sensor.getType(), copy);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // --------------------------------------------------------------- helpers

    /** Two decimals, without a trailing ".00" the board would have to parse. */
    public static String format(float value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value);
    }

    private void log(String message) {
        if (listener != null) listener.onLog(message);
    }
}
