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
 * <p>Readings are pulled rather than pushed: {@link #onSensorChanged} only files the latest raw
 * event away, and the work of turning it into a channel value happens in {@link #read}, when
 * something actually asks. So a sensor firing at 200 Hz costs nothing until it is read.
 *
 * <p>Adding a sensor means adding a {@code read...} method — or reusing {@link #readAxis} for a
 * triaxial one — and one case to {@link #read}.
 */
public final class SensorLiveReadingHelper implements SensorEventListener {

    public interface Listener {
        void onLog(String message);
    }

    /** Substituted when a token names a sensor that has not reported anything yet. */
    private static final String NO_READING = "0";

    private final Context context;
    private final SensorManager manager;
    private final Listener listener;

    /** Latest raw values per hardware sensor type, e.g. TYPE_ROTATION_VECTOR. */
    private final Map<Integer, float[]> readings = new ConcurrentHashMap<>();

    private final List<Sensor> registered = new ArrayList<>();

    public SensorLiveReadingHelper(Context context, Listener listener) {
        this.context = context == null ? null : context.getApplicationContext();
        this.manager = SensorCatalog.sensorManager(context);
        this.listener = listener;
    }

    // ------------------------------------------------------------- lifecycle

    /** Starts listening to the hardware behind each sensor. Safe to call more than once. */
    public void start(List<PhoneSensor> sensors) {
        if (manager == null || sensors == null) return;
        stop();

        for (PhoneSensor sensor : sensors) {
            // Registering for a guarded sensor without its permission throws on some phones and
            // silently reports nothing on the rest, so skip it until the user has granted it.
            if (!sensor.hasPermission(context)) {
                log(sensor.displayName + " needs permission before it can be read");
                continue;
            }

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

    /**
     * The channel's current value, or null when the sensor hasn't reported yet.
     *
     * <p>One case per catalog entry. The triaxial sensors all share {@link #readAxis} — their
     * only difference is which hardware type the axes come from.
     */
    @Nullable
    public Float read(PhoneSensor sensor, SensorChannel channel) {
        if (sensor == null || channel == null) return null;

        ChannelKey key = channel.key;

        if (SensorCatalog.ORIENTATION.equals(sensor)) return readOrientation(key);
        if (SensorCatalog.ACCELEROMETER.equals(sensor)) {
            return readAxis(Sensor.TYPE_ACCELEROMETER, key);
        }
        if (SensorCatalog.GYROSCOPE.equals(sensor)) {
            return readAxis(Sensor.TYPE_GYROSCOPE, key);
        }
        if (SensorCatalog.GRAVITY.equals(sensor)) {
            return readAxis(Sensor.TYPE_GRAVITY, key);
        }
        if (SensorCatalog.LINEAR_ACCELERATION.equals(sensor)) {
            return readAxis(Sensor.TYPE_LINEAR_ACCELERATION, key);
        }
        if (SensorCatalog.MAGNETOMETER.equals(sensor)) {
            return readAxis(Sensor.TYPE_MAGNETIC_FIELD, key);
        }
        if (SensorCatalog.STEP_COUNTER.equals(sensor)) {
            return readFirst(Sensor.TYPE_STEP_COUNTER, key, ChannelKey.STEPS);
        }
        if (SensorCatalog.LIGHT.equals(sensor)) {
            return readFirst(Sensor.TYPE_LIGHT, key, ChannelKey.LUX);
        }
        if (SensorCatalog.PROXIMITY.equals(sensor)) {
            return readFirst(Sensor.TYPE_PROXIMITY, key, ChannelKey.DISTANCE);
        }
        if (SensorCatalog.PRESSURE.equals(sensor)) return readPressure(key);
        if (SensorCatalog.TEMPERATURE.equals(sensor)) {
            return readFirst(Sensor.TYPE_AMBIENT_TEMPERATURE, key, ChannelKey.TEMPERATURE);
        }
        if (SensorCatalog.HUMIDITY.equals(sensor)) {
            return readFirst(Sensor.TYPE_RELATIVE_HUMIDITY, key, ChannelKey.HUMIDITY);
        }
        if (SensorCatalog.HINGE_ANGLE.equals(sensor)) {
            return readFirst(Sensor.TYPE_HINGE_ANGLE, key, ChannelKey.ANGLE);
        }
        if (SensorCatalog.HEART_RATE.equals(sensor)) return readHeartRate(key);

        return null;
    }

    /** Compass heading and tilt, from the rotation matrix. Degrees, not radians. */
    @Nullable
    private Float readOrientation(ChannelKey key) {
        int index = orientationIndex(key);
        if (index < 0) return null;

        float[] rotation = new float[9];
        if (!orientationMatrix(rotation)) return null;

        float[] angles = new float[3];
        SensorManager.getOrientation(rotation, angles);

        float degrees = (float) Math.toDegrees(angles[index]);
        // Azimuth is reported -180..180 but is far easier to steer by as a 0..360 heading.
        return key == ChannelKey.AZIMUTH ? (degrees + 360f) % 360f : degrees;
    }

    /** The slot each orientation channel takes in {@code getOrientation}'s output. */
    private static int orientationIndex(ChannelKey key) {
        if (key == ChannelKey.AZIMUTH) return 0;
        if (key == ChannelKey.PITCH) return 1;
        if (key == ChannelKey.ROLL) return 2;
        return -1;
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

    /**
     * Air pressure straight from the barometer, and the altitude it implies. Altitude is measured
     * against standard sea level pressure, so it follows a change in height closely but carries
     * whatever error the day's weather adds.
     */
    @Nullable
    private Float readPressure(ChannelKey key) {
        Float hectopascals = value(Sensor.TYPE_PRESSURE, 0);
        if (hectopascals == null) return null;

        if (key == ChannelKey.PRESSURE) return hectopascals;
        if (key == ChannelKey.ALTITUDE) {
            return SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hectopascals);
        }
        return null;
    }

    /**
     * Pulse in beats per minute. The monitor keeps reporting while nothing is touching it, with
     * a value of zero, so zero is treated as no reading rather than a heart that has stopped.
     */
    @Nullable
    private Float readHeartRate(ChannelKey key) {
        if (key != ChannelKey.BPM) return null;

        Float bpm = value(Sensor.TYPE_HEART_RATE, 0);
        if (bpm == null || bpm <= 0f) return null;
        return bpm;
    }

    /** One axis of a triaxial sensor — accelerometer, gyroscope, gravity, magnetic field. */
    @Nullable
    private Float readAxis(int hardwareType, ChannelKey key) {
        int index = axisIndex(key);
        if (index < 0) return null;
        return value(hardwareType, index);
    }

    private static int axisIndex(ChannelKey key) {
        if (key == ChannelKey.X) return 0;
        if (key == ChannelKey.Y) return 1;
        if (key == ChannelKey.Z) return 2;
        return -1;
    }

    /** The single value a scalar sensor reports, guarded by the channel it belongs to. */
    @Nullable
    private Float readFirst(int hardwareType, ChannelKey key, ChannelKey expected) {
        if (key != expected) return null;
        return value(hardwareType, 0);
    }

    /** Raw value at one index of the last event from a hardware type, or null if none yet. */
    @Nullable
    private Float value(int hardwareType, int index) {
        float[] values = readings.get(hardwareType);
        if (values == null || index >= values.length) return null;
        return values[index];
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
