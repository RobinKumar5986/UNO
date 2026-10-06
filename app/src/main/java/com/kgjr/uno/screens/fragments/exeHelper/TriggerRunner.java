package com.kgjr.uno.screens.fragments.exeHelper;

import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerProgram;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens to data from the board while a run is active and fires the matching trigger flow.
 *
 * <p>Triggers run one at a time on their own thread, alongside the Stage 1 flow. A message for
 * the trigger that is executing right now is dropped; any other is queued. A trigger already
 * waiting in the queue keeps its place and takes the newer values, so a fast stream can't
 * pile up stale runs.
 */
public final class TriggerRunner {

    private static final class Pending {

        final TriggerProgram program;
        ReceivedValues values;

        Pending(TriggerProgram program, ReceivedValues values) {
            this.program = program;
            this.values = values;
        }
    }

    private final SerialLink serial;
    private final SensorLiveReadingHelper sensors;
    private final FlowRunner.Listener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopReported = new AtomicBoolean(true);

    private final Object lock = new Object();
    private final ArrayDeque<Pending> queue = new ArrayDeque<>();
    private TriggerProgram executing;

    private FrameDecoder decoder;
    /** A worker from an earlier run that hasn't wound down yet must not pick up this run's queue. */
    private volatile Thread worker;

    public TriggerRunner(SerialLink serial, SensorLiveReadingHelper sensors,
                         FlowRunner.Listener listener) {
        this.serial = serial;
        this.sensors = sensors;
        this.listener = listener;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** False when there are no triggers, in which case no listener callback follows. */
    public synchronized boolean start(List<TriggerProgram> programs, SendFraming framing) {
        if (running.get() || programs == null || programs.isEmpty()) return false;

        // Fresh per run, so a worker still unwinding from the last Stop shares nothing with this one.
        SensorSnapshot snapshot = new SensorSnapshot(sensors, this::log);
        FlowInterpreter interpreter = new FlowInterpreter(serial, snapshot, this::active,
                new FlowInterpreter.Host() {
                    @Override
                    public void onLog(String message) {
                        log(message);
                    }

                    @Override
                    public void onBoardLost() {
                        running.set(false);
                        wake();
                    }
                });

        List<FlowBlock> all = new ArrayList<>();
        for (TriggerProgram program : programs) all.addAll(program.tree);
        snapshot.prepare(all);
        interpreter.setFraming(framing);

        synchronized (lock) {
            queue.clear();
            executing = null;
            decoder = new FrameDecoder(programs);
        }

        running.set(true);
        stopReported.set(false);
        worker = new Thread(() -> work(snapshot, interpreter), "TriggerRunner");
        worker.start();

        log("--- Listening for " + programs.size() + " trigger(s) ---");
        return true;
    }

    public synchronized void stop() {
        if (!running.getAndSet(false)) return;

        Thread current = worker;
        worker = null;
        if (current != null) current.interrupt();
        wake();
        reportStopped();
    }

    /** Called on the serial IO thread with each chunk the board sends. */
    public void onData(byte[] data) {
        if (!running.get()) return;

        synchronized (lock) {
            if (decoder == null) return;
            for (FrameDecoder.Match match : decoder.feed(data)) {
                offer(match.program, new ReceivedValues(match.program.format, match.values));
            }
        }
    }

    /** Must hold {@link #lock}. */
    private void offer(TriggerProgram program, ReceivedValues values) {
        if (program == executing) {
            log("Ignored " + program.label + " (" + values + "): it is still running");
            return;
        }

        for (Iterator<Pending> it = queue.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.program == program) {
                pending.values = values;
                return;
            }
        }

        queue.add(new Pending(program, values));
        lock.notifyAll();
    }

    private boolean active() {
        return running.get() && worker == Thread.currentThread();
    }

    private void work(SensorSnapshot snapshot, FlowInterpreter interpreter) {
        try {
            while (active()) {
                Pending next;
                synchronized (lock) {
                    while (active() && queue.isEmpty()) lock.wait();
                    if (!active()) break;

                    next = queue.poll();
                    executing = next.program;
                }

                log(next.program.label + " fired: " + next.values);
                try {
                    snapshot.refresh();
                    interpreter.run(next.program.tree, next.values);
                } catch (RuntimeException e) {
                    log(next.program.label + " error: " + e.getMessage());
                } finally {
                    synchronized (lock) {
                        if (worker == Thread.currentThread()) executing = null;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Covers the board dropping mid-run, where nobody called stop().
        if (worker == Thread.currentThread()) {
            running.set(false);
            reportStopped();
        }
    }

    private void wake() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void reportStopped() {
        if (!stopReported.compareAndSet(false, true)) return;

        log("--- Stopped listening ---");
        if (listener != null) listener.onStopped();
    }

    private void log(String message) {
        if (listener != null) listener.onLog(message);
    }
}
