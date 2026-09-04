package com.kgjr.uno.models.sensors;

/**
 * Every channel key the app knows. Sensors reference these instead of raw strings so a key can
 * never be misspelled, and {@link #wireName} is the only spelling that reaches generated code.
 */
public enum ChannelKey {

    AZIMUTH("azimuth"),
    X("x"),
    Y("y"),
    Z("z");

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
