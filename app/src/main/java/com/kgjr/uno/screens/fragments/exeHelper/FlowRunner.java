package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the Stage 1 flow once, start to finish, on a background thread.
 *
 * <p>Every start gets its own flag, interpreter and snapshot, so a run that is still winding
 * down after Stop (stuck in a serial write, say) can't carry on inside the next one.
 */
public final class FlowRunner {

    public interface Listener {
        void onLog(String message);

        void onStopped();
    }

    private final SerialLink serial;
    private final SensorLiveReadingHelper sensors;
    private final Listener listener;

    private AtomicBoolean current;
    private ExecutorService executor;

    public FlowRunner(SerialLink serial, SensorLiveReadingHelper sensors, Listener listener) {
        this.serial = serial;
        this.sensors = sensors;
        this.listener = listener;
    }

    public synchronized boolean isRunning() {
        return current != null && current.get();
    }

    /** False when there was nothing to run, in which case no listener callback follows. */
    public synchronized boolean start(List<FlowBlock> tree, SendFraming framing) {
        if (isRunning()) return false;

        if (tree == null || tree.isEmpty()) {
            log("Nothing to run. Build the flow first.");
            return false;
        }

        AtomicBoolean live = new AtomicBoolean(true);
        SensorSnapshot snapshot = new SensorSnapshot(sensors, this::log);
        FlowInterpreter interpreter = new FlowInterpreter(serial, snapshot, live::get,
                new FlowInterpreter.Host() {
                    @Override
                    public void onLog(String message) {
                        log(message);
                    }

                    // Clearing the flag unwinds the walk; the wrapper below reports the stop.
                    @Override
                    public void onBoardLost() {
                        live.set(false);
                    }
                });

        // Worked out once: the flow cannot change while it runs.
        snapshot.prepare(tree);
        interpreter.setFraming(framing);

        current = live;
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            log("--- Execution started ---");
            log("Caching " + snapshot.size() + " sensor value(s) per pass");
            try {
                snapshot.refresh();
                interpreter.run(tree, null);
            } catch (Exception e) {
                log("Execution error: " + e.getMessage());
            }
            finish(live, "--- Execution finished ---", false);
        });
        return true;
    }

    public void stop() {
        AtomicBoolean live;
        synchronized (this) {
            live = current;
        }
        if (live != null) finish(live, "--- Execution stopped ---", true);
    }

    /** Whichever of the run's end or Stop gets here first reports it; the other is a no-op. */
    private void finish(AtomicBoolean live, String message, boolean interrupt) {
        live.set(false);

        synchronized (this) {
            if (current != live) return;
            current = null;
            if (executor != null) {
                if (interrupt) executor.shutdownNow();
                else executor.shutdown();
                executor = null;
            }
        }

        log(message);
        if (listener != null) listener.onStopped();
    }

    private void log(String message) {
        if (listener != null) listener.onLog(message);
    }
}
