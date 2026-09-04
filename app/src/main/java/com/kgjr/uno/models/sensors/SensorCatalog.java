package com.kgjr.uno.models.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every sensor the app knows how to work with. This is the single place new sensors get added —
 * the sensors screen, the selection state and the code generator all read from here.
 */
public final class SensorCatalog {

    /**
     * Compass heading. Reported as a single 0–360° azimuth, so a program can steer by it
     * directly. Backed by the rotation vector where the phone has one, otherwise derived from
     * the accelerometer and magnetometer.
     */
    public static final PhoneSensor ORIENTATION = PhoneSensor.named("orientation")
            .displayName("Orientation")
            .description("Compass heading of the phone. 0° points to magnetic north and rises clockwise.")
            .type(SensorType.INPUT)
            .image("orientation_sensor")
            .channel(SensorChannel.bounded(ChannelKey.AZIMUTH, "Azimuth", "°", 0f, 360f))
            .requires(Sensor.TYPE_ROTATION_VECTOR)
            .requires(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD)
            .build();

    private static final List<PhoneSensor> ALL =
            Collections.unmodifiableList(Arrays.asList(ORIENTATION));

    private SensorCatalog() {
    }

    /** Everything in the catalog, in the order it should appear on screen. */
    public static List<PhoneSensor> all() {
        return ALL;
    }

    /** Only the sensors this phone actually has the hardware for. */
    public static List<PhoneSensor> availableOn(Context context) {
        SensorManager manager = sensorManager(context);

        List<PhoneSensor> available = new ArrayList<>();
        for (PhoneSensor sensor : ALL) {
            if (sensor.isAvailable(manager)) available.add(sensor);
        }
        return available;
    }

    /** Looks a definition up by its stable name, or null when the name is unknown. */
    public static PhoneSensor byName(String name) {
        if (name == null) return null;

        String trimmed = name.trim();
        for (PhoneSensor sensor : ALL) {
            if (sensor.name.equalsIgnoreCase(trimmed)) return sensor;
        }
        return null;
    }

    public static SensorManager sensorManager(Context context) {
        if (context == null) return null;
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
}
