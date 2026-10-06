package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.models.sensors.ChannelKey;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.ReceivedVars;

import java.util.List;

/**
 * Walks parsed flow blocks on the caller's thread, sending each Action over serial. Shared by
 * the Stage 1 runner and the trigger runner; each supplies the gate that cuts a walk short.
 */
final class FlowInterpreter {

    interface Gate {
        /** False once the walk on the calling thread should stop. */
        boolean open();
    }

    interface Host {
        void onLog(String message);

        /** A write failed, so the board is gone. The host should stop its run. */
        void onBoardLost();
    }

    /** Breathing room for the board between two commands. */
    private static final long COMMAND_GAP_MS = 50L;

    /** Readings are floats, so == and != need a tolerance rather than an exact match. */
    private static final float EQUALITY_EPSILON = 0.001f;

    private final SerialLink serial;
    private final SensorSnapshot snapshot;
    private final Gate running;
    private final Host host;

    private SendFraming framing;
    private ReceivedValues received;

    FlowInterpreter(SerialLink serial, SensorSnapshot snapshot, Gate running, Host host) {
        this.serial = serial;
        this.snapshot = snapshot;
        this.running = running;
        this.host = host;
    }

    void setFraming(SendFraming framing) {
        this.framing = framing;
    }

    /** One walk of {@code blocks}; {@code received} is null outside a trigger. */
    void run(List<FlowBlock> blocks, ReceivedValues received) {
        this.received = received;
        try {
            execute(blocks);
        } finally {
            this.received = null;
        }
    }

    private void execute(List<FlowBlock> blocks) {
        for (FlowBlock b : blocks) {
            if (!running.open()) return;

            switch (b.type) {
                case ACTION:
                    sendAction(b);
                    break;

                case WAIT:
                    sleep(FlowCode.waitMillis(b));
                    break;

                case REPEAT:
                    // Re-read at the top of every iteration, so one pass works off one set of
                    // values but a loop still tracks the sensors as it goes round.
                    if (b.forever) {
                        while (running.open()) {
                            snapshot.refresh();
                            execute(b.body);
                        }
                    } else {
                        int times = FlowCode.repeatTimes(b);
                        for (int i = 0; i < times && running.open(); i++) {
                            snapshot.refresh();
                            execute(b.body);
                        }
                    }
                    break;

                case DECISION:
                    if (evaluate(b)) execute(b.body);
                    else execute(b.elseBody);
                    break;

                default: // START, END
                    break;
            }
        }
    }

    private void sendAction(FlowBlock b) {
        ActionNodeData data = b.data instanceof ActionNodeData ? (ActionNodeData) b.data : null;
        if (data == null) return;

        if (data.mode != null && !data.mode.sendsCommand()) {
            log("Skipped: API actions are not supported yet");
            return;
        }

        String command = data.command == null ? "" : data.command.trim();
        if (command.isEmpty()) return;

        // Received tokens first: they share the [name: key] shape the sensor resolver would
        // otherwise report as unknown.
        if (received != null) command = received.resolveTokens(command);
        command = snapshot.resolveTokens(command);

        if (!serial.write(framing.wrap(command))) {
            log("Stopping: the board is not connected");
            host.onBoardLost();
            return;
        }
        sleep(COMMAND_GAP_MS);
    }

    private boolean evaluate(FlowBlock b) {
        DecisionNodeData data = b.data instanceof DecisionNodeData ? (DecisionNodeData) b.data : null;
        if (data == null || !data.condition.isSet()) return false;

        DecisionNodeData.Condition c = data.condition;
        return ReceivedVars.isSource(c.sensorName) ? matchesReceived(c) : matchesSensor(c);
    }

    private boolean matchesReceived(DecisionNodeData.Condition c) {
        int index = ReceivedVars.indexOf(c.channelKey);
        if (received == null || !received.has(index)) {
            log("No received value for " + c.token() + ", treated as false");
            return false;
        }

        if (received.isText(index)) {
            boolean equal = received.text(index).equals(c.value == null ? "" : c.value.trim());
            return "!=".equals(c.operator) != equal;
        }

        Float value = received.number(index);
        if (value == null) {
            log("Received " + c.token() + " is not a number, treated as false");
            return false;
        }
        return compare(value, c);
    }

    private boolean matchesSensor(DecisionNodeData.Condition c) {
        PhoneSensor sensor = SensorCatalog.byName(c.sensorName);
        SensorChannel channel = sensor == null
                ? null : sensor.channel(ChannelKey.fromWireName(c.channelKey));

        if (channel == null) {
            log("Unknown sensor in condition " + c.expression());
            return false;
        }

        // This pass's cached value, so the decision agrees with any command that used the same
        // channel earlier in the same iteration.
        Float reading = snapshot.read(sensor, channel);
        if (reading == null) {
            log("No reading yet for " + c.token() + ", treated as false");
            return false;
        }
        return compare(reading, c);
    }

    private boolean compare(float left, DecisionNodeData.Condition c) {
        float right;
        try {
            right = Float.parseFloat(c.value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            log("Bad value in condition " + c.expression());
            return false;
        }

        switch (c.operator) {
            case "<":
                return left < right;
            case ">":
                return left > right;
            case "==":
                return Math.abs(left - right) < EQUALITY_EPSILON;
            case "!=":
                return Math.abs(left - right) >= EQUALITY_EPSILON;
            default:
                return false;
        }
    }

    /** Sleeps in slices so Stop takes effect without waiting out a long Wait block. */
    private void sleep(long millis) {
        long end = System.currentTimeMillis() + millis;

        while (running.open()) {
            long remaining = end - System.currentTimeMillis();
            if (remaining <= 0) return;
            try {
                Thread.sleep(Math.min(50L, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void log(String message) {
        host.onLog(message);
    }
}
