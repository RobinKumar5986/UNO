package com.kgjr.uno.screens.fragments;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.adapters.SelectedSensorAdapter;
import com.kgjr.uno.adapters.SensorGridAdapter;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorCatalog;
import com.kgjr.uno.screens.fragments.sensorHelper.SensorInfoSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user pick which phone sensors a program may read. The catalog sits on the left as a
 * grid, the current selection on the right. Selection is held in
 * {@link AppConstant#selectedSensors} so it survives navigating away and back.
 */
public class SensorsFragment extends Fragment {

    private static final int GRID_COLUMNS = 3;

    private SensorGridAdapter gridAdapter;
    private SelectedSensorAdapter selectedAdapter;

    private View selectedEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sensors, container, false);

        SensorManager sensorManager = SensorCatalog.sensorManager(requireContext());
        dropUnusableSelections(sensorManager);

        selectedEmpty = root.findViewById(R.id.selectedSensorEmpty);

        setUpGrid(root, sensorManager);
        setUpSelectedPanel(root);

        FloatingActionButton nextScreenButton = root.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(view -> Navigation.findNavController(view)
                .navigate(R.id.action_sensorsFragment_to_mobileCodeFragment));

        updateEmptyState();
        return root;
    }

    private void setUpGrid(View root, SensorManager sensorManager) {
        RecyclerView grid = root.findViewById(R.id.sensorGrid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        grid.setHasFixedSize(true);

        gridAdapter = new SensorGridAdapter(SensorCatalog.all(), sensorManager,
                this::onSensorToggled, this::onSensorInfoRequested);
        grid.setAdapter(gridAdapter);
    }

    private void setUpSelectedPanel(View root) {
        RecyclerView list = root.findViewById(R.id.selectedSensorList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        selectedAdapter = new SelectedSensorAdapter(AppConstant.selectedSensors,
                this::onSensorRemoved);
        list.setAdapter(selectedAdapter);
    }

    private void onSensorToggled(PhoneSensor sensor) {
        AppConstant.toggleSensor(sensor);
        onSelectionChanged(sensor);
    }

    private void onSensorInfoRequested(PhoneSensor sensor) {
        SensorInfoSheet.show(requireContext(), sensor);
    }

    private void onSensorRemoved(PhoneSensor sensor) {
        AppConstant.deselectSensor(sensor);
        onSelectionChanged(sensor);
    }

    /** Keeps the grid card and the panel in step after either side edits. */
    private void onSelectionChanged(PhoneSensor sensor) {
        gridAdapter.refresh(sensor);
        selectedAdapter.submit(AppConstant.selectedSensors);
        updateEmptyState();
    }

    private void updateEmptyState() {
        selectedEmpty.setVisibility(
                AppConstant.selectedSensors.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * A sensor picked on an earlier run — or before a catalog change — may not be backed by this
     * phone. Drop those so the panel never lists something the program can't read.
     */
    private void dropUnusableSelections(SensorManager sensorManager) {
        List<PhoneSensor> usable = new ArrayList<>();
        for (PhoneSensor sensor : AppConstant.selectedSensors) {
            if (SensorCatalog.byName(sensor.name) != null && sensor.isAvailable(sensorManager)) {
                usable.add(sensor);
            }
        }
        AppConstant.selectedSensors = usable;
    }
}
