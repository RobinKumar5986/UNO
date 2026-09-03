package com.kgjr.uno.models.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A sensor the app can offer the user, independent of the phone it runs on. Definitions live in
 * {@link SensorCatalog}; nothing else should construct one.
 *
 * <p>Instances are compared by {@link #name}, so a definition stays the same sensor across
 * screens even though {@link #id} is regenerated when the process restarts.
 */
public class PhoneSensor {

    /** Unique per definition instance. Useful as a stable key inside a single run. */
    public final String id;

    /** Stable, code-facing name, e.g. "orientation". This is the identity of the sensor. */
    public final String name;

    /** What the UI shows, e.g. "Orientation". */
    public final String displayName;

    /** One or two sentences explaining what the sensor reports. */
    public final String description;

    /** Whether the sensor feeds the program or is driven by it. */
    public final SensorType type;

    /** The values this sensor reports — one entry per output. Never empty. */
    public final List<SensorChannel> channels;

    /** Drawable name, without extension, resolved lazily by {@link #imageRes(Context)}. */
    public final String imageName;

    /** Hardware that has to be present. Any one satisfied requirement makes the sensor usable. */
    public final List<HardwareRequirement> requirements;

    private PhoneSensor(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.type = builder.type;
        this.channels = Collections.unmodifiableList(new ArrayList<>(builder.channels));
        this.imageName = builder.imageName;
        this.requirements = Collections.unmodifiableList(new ArrayList<>(builder.requirements));
    }

    public int channelCount() {
        return channels.size();
    }

    public SensorChannel channel(String key) {
        for (SensorChannel channel : channels) {
            if (channel.key.equals(key)) return channel;
        }
        return null;
    }

    /** e.g. "1 value · 0–360°" for a single channel, or "3 values · X, Y, Z" for several. */
    public String channelSummary() {
        String count = channelCount() == 1 ? "1 value" : channelCount() + " values";

        if (channelCount() == 1) {
            String range = channels.get(0).rangeLabel();
            return range.isEmpty() ? count : count + " · " + range;
        }

        StringBuilder names = new StringBuilder();
        for (SensorChannel channel : channels) {
            if (names.length() > 0) names.append(", ");
            names.append(channel.displayName);
        }
        return count + " · " + names;
    }

    /** True when this phone has the hardware to back the sensor. */
    public boolean isAvailable(SensorManager manager) {
        if (requirements.isEmpty()) return true;

        for (HardwareRequirement requirement : requirements) {
            if (requirement.isSatisfiedBy(manager)) return true;
        }
        return false;
    }

    /** The hardware sensors actually backing this one, empty when the phone can't support it. */
    public List<Sensor> resolve(SensorManager manager) {
        if (manager == null) return Collections.emptyList();

        for (HardwareRequirement requirement : requirements) {
            if (!requirement.isSatisfiedBy(manager)) continue;

            List<Sensor> resolved = new ArrayList<>();
            for (int sensorType : requirement.sensorTypes) {
                resolved.add(manager.getDefaultSensor(sensorType));
            }
            return resolved;
        }
        return Collections.emptyList();
    }

    /** Resolves {@link #imageName} against the drawable folder, or 0 when it isn't there. */
    public int imageRes(Context context) {
        if (context == null || imageName == null || imageName.isEmpty()) return 0;

        return context.getResources()
                .getIdentifier(imageName, "drawable", context.getPackageName());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PhoneSensor)) return false;
        return name.equals(((PhoneSensor) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private final List<SensorChannel> channels = new ArrayList<>();
        private final List<HardwareRequirement> requirements = new ArrayList<>();

        private String displayName;
        private String description = "";
        private SensorType type = SensorType.INPUT;
        private String imageName = "";

        private Builder(String name) {
            this.name = name;
            this.displayName = name;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(SensorType type) {
            this.type = type;
            return this;
        }

        public Builder image(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder channel(SensorChannel channel) {
            this.channels.add(channel);
            return this;
        }

        /** Adds one alternative set of hardware that can back this sensor. */
        public Builder requires(int... sensorTypes) {
            this.requirements.add(HardwareRequirement.of(sensorTypes));
            return this;
        }

        public PhoneSensor build() {
            if (channels.isEmpty()) {
                throw new IllegalStateException("Sensor " + name + " has no output channels");
            }
            return new PhoneSensor(this);
        }
    }
}
