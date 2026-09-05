package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.models.sensors.ChannelKey;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.models.sensors.SensorChannel;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.ActionNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.DecisionNodeData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Walks a parsed flow on a background thread, sending each Action over serial. */
public final class FlowRunner {

    public interface Listener {
        void onLog(String message);

        void onStopped();
    }

    /** Breathing room for the board between two commands. */
    private static final long COMMAND_GAP_MS = 50L;

    /** Sensor readings are floats, so == and != need a tolerance rather than an exact match. */
    private static final float EQUALITY_EPSILON = 0.001f;

    private final SerialLink serial;
    private final SensorLiveReadingHelper sensors;
    private final Listener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ExecutorService executor;

    public FlowRunner(SerialLink serial, SensorLiveReadingHelper sensors, Listener listener) {
        this.serial = serial;
        this.sensors = sensors;
        this.listener = listener;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** False when there was nothing to run, in which case no listener callback follows. */
    public synchronized boolean start(List<FlowBlock> tree) {
        if (running.get()) return false;

        if (tree == null || tree.isEmpty()) {
            log("Nothing to run. Build the flow first.");
            return false;
        }

        running.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            log("--- Execution started ---");
            try {
                execute(tree);
            } catch (Exception e) {
                log("Execution error: " + e.getMessage());
            }
            running.set(false);
            releaseExecutor(false);
            log("--- Execution finished ---");
            if (listener != null) listener.onStopped();
        });
        return true;
    }

    public synchronized void stop() {
        boolean wasRunning = running.getAndSet(false);
        releaseExecutor(true);

        if (wasRunning) {
            log("--- Execution stopped ---");
            if (listener != null) listener.onStopped();
        }
    }

    private synchronized void releaseExecutor(boolean interrupt) {
        if (executor == null) return;
        if (interrupt) executor.shutdownNow();
        else executor.shutdown();
        executor = null;
    }

    private void execute(List<FlowBlock> blocks) {
        for (FlowBlock b : blocks) {
            if (!running.get()) return;

            switch (b.type) {
                case ACTION:
                    sendAction(b);
                    break;

                case WAIT:
                    sleep(FlowCode.waitMillis(b));
                    break;

                case REPEAT:
                    if (b.forever) {
                        while (running.get()) execute(b.body);
                    } else {
                        int times = FlowCode.repeatTimes(b);
                        for (int i = 0; i < times && running.get(); i++) execute(b.body);
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

        // Resolved per send, so a command inside a loop picks up a fresh reading each pass.
        if (sensors != null) command = sensors.resolveTokens(command);

        if (!serial.write(command)) {
            // The board is gone. Clearing the flag unwinds execute() and lets start()'s wrapper
            // do the teardown, so onStopped still fires exactly once.
            log("Stopping: the board is not connected");
            running.set(false);
            return;
        }
        sleep(COMMAND_GAP_MS);
    }

    private boolean evaluate(FlowBlock b) {
        DecisionNodeData data = b.data instanceof DecisionNodeData ? (DecisionNodeData) b.data : null;
        if (data == null || !data.condition.isSet()) return false;

        return matches(data.condition);
    }

    private boolean matches(DecisionNodeData.Condition c) {
        PhoneSensor sensor = SensorCatalog.byName(c.sensorName);
        SensorChannel channel = sensor == null
                ? null : sensor.channel(ChannelKey.fromWireName(c.channelKey));

        if (channel == null) {
            log("Unknown sensor in condition " + c.expression());
            return false;
        }

        // Read per pass, so a decision inside a loop sees a fresh value each time.
        Float reading = sensors == null ? null : sensors.read(sensor, channel);
        if (reading == null) {
            log("No reading yet for " + c.token() + ", treated as false");
            return false;
        }

        float left = channel.clamp(reading);
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

        while (running.get()) {
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
        if (listener != null) listener.onLog(message);
    }
}