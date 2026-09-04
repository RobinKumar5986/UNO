package com.kgjr.uno.models.sensors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code [sensor: channel]} placeholder a command uses to stand in for a live reading, e.g.
 * {@code [orientation: azimuth]} or {@code [accelerometer: x]}. Both the Action dialog that
 * writes tokens and the runner that resolves them go through here.
 */
public final class SensorToken {

    /** Group 1 is the sensor name, group 2 the channel key. */
    public static final Pattern PATTERN =
            Pattern.compile("\\[\\s*([A-Za-z0-9_]+)\\s*:\\s*([A-Za-z0-9_]+)\\s*]");

    public static String of(PhoneSensor sensor, SensorChannel channel) {
        return "[" + sensor.name + ": " + channel.key.wireName + "]";
    }

    /** The sensor and channel a matched token points at, or null when either is unknown. */
    public static Resolved resolve(Matcher matcher) {
        PhoneSensor sensor = SensorCatalog.byName(matcher.group(1));
        if (sensor == null) return null;

        SensorChannel channel = sensor.channel(ChannelKey.fromWireName(matcher.group(2)));
        if (channel == null) return null;

        return new Resolved(sensor, channel);
    }

    public static final class Resolved {

        public final PhoneSensor sensor;
        public final SensorChannel channel;

        Resolved(PhoneSensor sensor, SensorChannel channel) {
            this.sensor = sensor;
            this.channel = channel;
        }
    }

    private SensorToken() {
    }
}
