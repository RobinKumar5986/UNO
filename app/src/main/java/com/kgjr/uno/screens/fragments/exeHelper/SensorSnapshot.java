package com.kgjr.uno.screens.fragments.exeHelper;

import androidx.annotation.Nullable;

import com.kgjr.uno.models.sensors.ChannelKey;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.models.sensors.SensorToken;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * One reading per channel per pass.
 *
 * <p>{@link #prepare} walks the flow once to find the channels the program actually references,
 * {@link #refresh} takes a single reading of each, and everything the pass does afterwards reads
 * from here. So a command and a decision in the same iteration see the same number, the hardware
 * is not polled once per token, and a value cannot drift halfway through a pass.
 */
public final class SensorSnapshot {

    /** Substituted when a channel has not reported anything yet. */
    private static final String NO_READING = "0";

    private final SensorLiveReadingHelper sensors;
    private final SensorLiveReadingHelper.Listener listener;

    /** The channels the program references. Fixed for a run, since the flow cannot change. */
    private final List<Target> targets = new ArrayList<>();

    /** Readings taken by the last {@link #refresh}, already clamped to each channel's range. */
    private final Map<String, Float> values = new HashMap<>();

    public SensorSnapshot(SensorLiveReadingHelper sensors,
                          SensorLiveReadingHelper.Listener listener) {
        this.sensors = sensors;
        this.listener = listener;
    }

    // ----------------------------------------------------------------- setup

    /** Collects the channels used by Action commands and Decision conditions across the tree. */
    public void prepare(List<FlowBlock> tree) {
        targets.clear();
        values.clear();
        collect(tree, new HashSet<>());
    }

    public int size() {
        return targets.size();
    }

    private void collect(List<FlowBlock> blocks, Set<String> seen) {
        if (blocks == null) return;

        for (FlowBlock b : blocks) {
            if (b.data instanceof ActionNodeData) {
                collectTokens(((ActionNodeData) b.data).command, seen);
            } else if (b.data instanceof DecisionNodeData) {
                collectCondition(((DecisionNodeData) b.data).condition, seen);
            }
            collect(b.body, seen);
            collect(b.elseBody, seen);
        }
    }

    private void collectTokens(String command, Set<String> seen) {
        if (command == null || command.isEmpty()) return;

        Matcher matcher = SensorToken.PATTERN.matcher(command);
        while (matcher.find()) {
            SensorToken.Resolved resolved = SensorToken.resolve(matcher);
            if (resolved != null) add(resolved.sensor, resolved.channel, seen);
        }
    }

    private void collectCondition(DecisionNodeData.Condition condition, Set<String> seen) {
        if (condition == null || !condition.isSet()) return;

        PhoneSensor sensor = SensorCatalog.byName(condition.sensorName);
        if (sensor == null) return;

        SensorChannel channel = sensor.channel(ChannelKey.fromWireName(condition.channelKey));
        if (channel != null) add(sensor, channel, seen);
    }

    private void add(PhoneSensor sensor, SensorChannel channel, Set<String> seen) {
        String key = key(sensor, channel);
        if (seen.add(key)) targets.add(new Target(key, sensor, channel));
    }

    // -------------------------------------------------------------- readings

    /** Takes one reading of every referenced channel. Called at the top of each pass. */
    public void refresh() {
        if (sensors == null) return;

        for (Target target : targets) {
            Float value = sensors.read(target.sensor, target.channel);
            if (value == null) values.remove(target.key);
            else values.put(target.key, target.channel.clamp(value));
        }
    }

    /** The value taken by the last refresh, or null when that channel had not reported. */
    @Nullable
    public Float read(PhoneSensor sensor, SensorChannel channel) {
        if (sensor == null || channel == null) return null;
        return values.get(key(sensor, channel));
    }

    /** Replaces every {@code [sensor: channel]} token with this pass's reading. */
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
            matcher.appendReplacement(out, SensorLiveReadingHelper.format(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String key(PhoneSensor sensor, SensorChannel channel) {
        return sensor.name + ":" + channel.key.wireName;
    }

    private void log(String message) {
        if (listener != null) listener.onLog(message);
    }

    private static final class Target {

        final String key;
        final PhoneSensor sensor;
        final SensorChannel channel;

        Target(String key, PhoneSensor sensor, SensorChannel channel) {
            this.key = key;
            this.sensor = sensor;
            this.channel = channel;
        }
    }
}
