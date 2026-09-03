package com.kgjr.uno.models.sensors;

/**
 * One value a sensor reports. A sensor owns as many channels as it has outputs: the orientation
 * sensor has a single channel (azimuth), an accelerometer has three (x, y, z).
 *
 * <p>Bounds are optional — {@code min} and {@code max} are null for channels with no fixed
 * range, such as raw acceleration.
 */
public class SensorChannel {

    /** Stable, code-facing name. Used when the channel is referenced from generated code. */
    public final String key;

    /** What the UI shows, e.g. "Azimuth". */
    public final String displayName;

    /** Unit suffix, e.g. "°" or "m/s²". Empty string when the value is unitless. */
    public final String unit;

    /** Inclusive lower bound, or null when the channel is unbounded. */
    public final Float min;

    /** Inclusive upper bound, or null when the channel is unbounded. */
    public final Float max;

    private SensorChannel(String key, String displayName, String unit, Float min, Float max) {
        this.key = key;
        this.displayName = displayName;
        this.unit = unit == null ? "" : unit;
        this.min = min;
        this.max = max;
    }

    /** A channel with a known range, e.g. a compass heading of 0–360°. */
    public static SensorChannel bounded(String key, String displayName, String unit,
                                        float min, float max) {
        return new SensorChannel(key, displayName, unit, min, max);
    }

    /** A channel with no fixed range, e.g. raw acceleration on one axis. */
    public static SensorChannel unbounded(String key, String displayName, String unit) {
        return new SensorChannel(key, displayName, unit, null, null);
    }

    public boolean isBounded() {
        return min != null && max != null;
    }

    /** Clamps a reading into the channel's range; unbounded channels pass through. */
    public float clamp(float value) {
        if (!isBounded()) return value;
        return Math.max(min, Math.min(max, value));
    }

    /** e.g. "0–360°" for a bounded channel, or just the unit when unbounded. */
    public String rangeLabel() {
        if (!isBounded()) return unit;
        return format(min) + "–" + format(max) + unit;
    }

    /** e.g. "Azimuth (0–360°)". */
    public String detailLabel() {
        String range = rangeLabel();
        return range.isEmpty() ? displayName : displayName + " (" + range + ")";
    }

    /** Drops the trailing ".0" so 360f reads as "360" rather than "360.0". */
    private static String format(float value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }
}
