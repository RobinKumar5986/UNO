package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import com.kgjr.uno.AppConstant;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorToken;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;

import java.util.ArrayList;
import java.util.List;

/**
 * Something the Action and Decision pickers can read values from: a selected phone sensor, or
 * inside a trigger, the data the board sent. Both are addressed as {@code [name: key]}.
 */
final class ValueSource {

    static final class Item {

        final String key;
        final String label;
        final String unit;
        final String token;

        /** A %s value: compared as text, so only == and != apply. */
        final boolean text;

        Item(String key, String label, String unit, String token, boolean text) {
            this.key = key;
            this.label = label;
            this.unit = unit;
            this.token = token;
            this.text = text;
        }
    }

    final String name;
    final String displayName;
    final List<Item> items = new ArrayList<>();

    private ValueSource(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    /** Received data first inside a trigger, since that is what the flow is reacting to. */
    static List<ValueSource> available(FlowScope scope) {
        List<ValueSource> sources = new ArrayList<>();

        PayloadFormat format = scope.receivedFormat();
        if (format != null) sources.add(received(format));

        for (PhoneSensor sensor : AppConstant.selectedSensors) sources.add(sensor(sensor));
        return sources;
    }

    static ValueSource find(List<ValueSource> sources, String name) {
        if (name == null || name.isEmpty()) return null;

        for (ValueSource source : sources) {
            if (source.name.equals(name)) return source;
        }
        return null;
    }

    Item item(String key) {
        for (Item item : items) {
            if (item.key.equals(key)) return item;
        }
        return null;
    }

    private static ValueSource sensor(PhoneSensor sensor) {
        ValueSource source = new ValueSource(sensor.name, sensor.displayName);
        for (SensorChannel channel : sensor.channels) {
            source.items.add(new Item(channel.key.wireName, channel.displayName, channel.unit,
                    SensorToken.of(sensor, channel), false));
        }
        return source;
    }

    private static ValueSource received(PayloadFormat format) {
        ValueSource source = new ValueSource(ReceivedVars.SOURCE, ReceivedVars.DISPLAY_NAME);
        for (int i = 0; i < format.size(); i++) {
            PayloadFormat.ValueType type = format.types().get(i);
            source.items.add(new Item(ReceivedVars.key(i), ReceivedVars.label(i), type.label,
                    ReceivedVars.token(i), type == PayloadFormat.ValueType.TEXT));
        }
        return source;
    }
}
