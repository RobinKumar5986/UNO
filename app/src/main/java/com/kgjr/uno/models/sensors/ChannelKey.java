package com.kgjr.uno.models.sensors;

/**
 * Every channel key the app knows. Sensors reference these instead of raw strings so a key can
 * never be misspelled, and {@link #wireName} is the only spelling that reaches generated code.
 *
 * <p>Keys are shared across sensors — the accelerometer, gyroscope and magnetometer all report
 * {@link #X}, {@link #Y} and {@link #Z}, and a token is only ever read alongside the sensor it
 * was written against, so there is no ambiguity.
 */
public enum ChannelKey {

    // Orientation, from the rotation matrix.
    AZIMUTH("azimuth"),
    PITCH("pitch"),
    ROLL("roll"),

    // Triaxial sensors: acceleration, rotation rate, magnetic field, gravity.
    X("x"),
    Y("y"),
    Z("z"),

    // Environment.
    LUX("lux"),
    DISTANCE("distance"),
    PRESSURE("pressure"),
    ALTITUDE("altitude"),
    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),

    // Device.
    ANGLE("angle"),

    // Body.
    STEPS("steps"),
    BPM("bpm");

    public final String wireName;

    ChannelKey(String wireName) {
        this.wireName = wireName;
    }

    /** Null when the name isn't a known key. */
    public static ChannelKey fromWireName(String name) {
        if (name == null) return null;

        String trimmed = name.trim();
        for (ChannelKey key : values()) {
            if (key.wireName.equalsIgnoreCase(trimmed)) return key;
        }
        return null;
    }
}
