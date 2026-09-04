package com.kgjr.uno.screens.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.screens.fragments.exeHelper.FlowRunner;
import com.kgjr.uno.screens.fragments.exeHelper.SensorLiveReadingHelper;
import com.kgjr.uno.screens.fragments.exeHelper.SensorReadoutView;
import com.kgjr.uno.screens.fragments.exeHelper.SerialLink;

import java.util.ArrayList;
import java.util.List;


public class CodeExeFragment extends Fragment implements FlowRunner.Listener {

    private static final String TAG = "CodeExe";

    /** Cards per row. The app is landscape, so three fit comfortably. */
    private static final int READOUT_COLUMNS = 3;

    /** Fast enough to look live, slow enough not to matter. */
    private static final long REFRESH_INTERVAL_MS = 100L;

    private FloatingActionButton runCode;
    private SerialLink serial;
    private SensorLiveReadingHelper sensors;
    private FlowRunner runner;

    private final List<SensorReadoutView> readouts = new ArrayList<>();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());

    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            for (SensorReadoutView readout : readouts) {
                readout.refresh(sensors);
            }
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_code_exe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        runCode = view.findViewById(R.id.run_code);

        serial = new SerialLink(requireContext(), this::onLog);
        serial.register();
        serial.connect();

        sensors = new SensorLiveReadingHelper(requireContext(), this::onLog);
        runner = new FlowRunner(serial, sensors, this);

        buildReadouts(view);

        runCode.setOnClickListener(v -> {
            if (runner.isRunning()) {
                runner.stop();
                return;
            }
            if (!serial.isConnected()) {
                serial.connect();

                onLog("Connect the board over USB first");
                return;
            }
            // Dim only once the run is really under way — start() bails on an empty flow, and
            // no onStopped() follows to undo it.
            if (runner.start(AppConstant.flowTree)) runCode.setAlpha(0.5f);
        });
    }

    /**
     * One card per selected sensor, packed into weighted rows. Filler views keep the last row's
     * cards the same width as a full row's rather than stretching them across the screen.
     */
    private void buildReadouts(View root) {
        LinearLayout container = root.findViewById(R.id.exeReadouts);
        View empty = root.findViewById(R.id.exeReadoutsEmpty);

        container.removeAllViews();
        readouts.clear();

        List<PhoneSensor> selected = AppConstant.selectedSensors;
        empty.setVisibility(selected.isEmpty() ? View.VISIBLE : View.GONE);
        if (selected.isEmpty()) return;

        int gap = getResources().getDimensionPixelSize(R.dimen.spacing_card);
        LinearLayout row = null;

        for (int i = 0; i < selected.size(); i++) {
            if (i % READOUT_COLUMNS == 0) {
                row = newRow(container, gap, i > 0);
            }

            SensorReadoutView readout = new SensorReadoutView(requireContext());
            readout.bind(selected.get(i));
            readouts.add(readout);

            row.addView(readout, cellParams(i % READOUT_COLUMNS == 0 ? 0 : gap));
        }

        int trailing = selected.size() % READOUT_COLUMNS;
        if (trailing != 0) {
            for (int i = trailing; i < READOUT_COLUMNS; i++) {
                row.addView(new View(requireContext()), cellParams(gap));
            }
        }

        // Paint once now so the cards aren't blank until the first tick.
        for (SensorReadoutView readout : readouts) {
            readout.refresh(sensors);
        }
    }

    private LinearLayout newRow(LinearLayout container, int gap, boolean spaceAbove) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (spaceAbove) params.topMargin = gap;

        container.addView(row, params);
        return row;
    }

    private LinearLayout.LayoutParams cellParams(int startMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginStart(startMargin);
        return params;
    }

    /**
     * Sensors start listening with the screen, not with the run. Hardware takes a moment to
     * report, and a run that started them itself would substitute 0 into its first command.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (sensors != null) sensors.start(AppConstant.selectedSensors);
        if (!readouts.isEmpty()) refreshHandler.post(refreshTick);
    }

    @Override
    public void onPause() {
        refreshHandler.removeCallbacks(refreshTick);
        if (sensors != null) sensors.stop();
        super.onPause();
    }

    @Override
    public void onLog(String message) {
        Log.d(TAG, message);
    }

    /** Called from the runner's own thread, so it hops to the main looper. */
    @Override
    public void onStopped() {
        refreshHandler.post(() -> {
            if (runCode != null) runCode.setAlpha(1.0f);
        });
    }

    @Override
    public void onDestroyView() {
        refreshHandler.removeCallbacks(refreshTick);
        readouts.clear();

        if (runner != null) runner.stop();
        if (sensors != null) sensors.stop();
        if (serial != null) {
            serial.unregister();
            serial.close();
        }

        // All four are rebuilt in onViewCreated; holding them would pin the dead hierarchy.
        runCode = null;
        runner = null;
        sensors = null;
        serial = null;

        super.onDestroyView();
    }
}
