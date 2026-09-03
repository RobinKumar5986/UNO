package com.kgjr.uno.adapters;

import android.hardware.SensorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;

import java.util.ArrayList;
import java.util.List;

/**
 * The sensor catalog as a grid of cards. Selection lives in {@link AppConstant}, so the adapter
 * reads it rather than keeping a copy — the panel on the right edits the same list.
 */
public class SensorGridAdapter extends RecyclerView.Adapter<SensorGridAdapter.SensorViewHolder> {

    public interface OnSensorToggled {
        void onSensorToggled(PhoneSensor sensor);
    }

    public interface OnSensorInfoRequested {
        void onSensorInfoRequested(PhoneSensor sensor);
    }

    private static final float UNAVAILABLE_ALPHA = 0.45f;

    private final List<PhoneSensor> sensors;
    private final SensorManager sensorManager;
    private final OnSensorToggled toggleListener;
    private final OnSensorInfoRequested infoListener;

    public SensorGridAdapter(List<PhoneSensor> sensors, SensorManager sensorManager,
                             OnSensorToggled toggleListener,
                             OnSensorInfoRequested infoListener) {
        this.sensors = new ArrayList<>(sensors);
        this.sensorManager = sensorManager;
        this.toggleListener = toggleListener;
        this.infoListener = infoListener;
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor_card, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        PhoneSensor sensor = sensors.get(position);
        boolean available = sensor.isAvailable(sensorManager);

        holder.name.setText(sensor.displayName);

        int image = sensor.imageRes(holder.itemView.getContext());
        holder.image.setImageResource(image != 0 ? image : R.drawable.ic_sensors);

        bindState(holder, sensor, available);

        if (available) {
            holder.itemView.setOnClickListener(view -> {
                if (toggleListener != null) toggleListener.onSensorToggled(sensor);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }

        holder.infoButton.setOnClickListener(view -> {
            if (infoListener != null) infoListener.onSensorInfoRequested(sensor);
        });
    }

    private void bindState(SensorViewHolder holder, PhoneSensor sensor, boolean available) {
        boolean selected = available && AppConstant.isSensorSelected(sensor);

        holder.itemView.setActivated(selected);
        holder.itemView.setEnabled(available);
        holder.itemView.setClickable(available);
        holder.itemView.setAlpha(available ? 1f : UNAVAILABLE_ALPHA);

        holder.selectedTick.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.unavailableBadge.setVisibility(available ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    /** Repaints one card after its selection changed elsewhere. */
    public void refresh(PhoneSensor sensor) {
        int index = sensors.indexOf(sensor);
        if (index >= 0) notifyItemChanged(index);
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        final TextView name;
        final ImageView selectedTick;
        final ImageView infoButton;
        final TextView unavailableBadge;

        SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.sensorImage);
            name = itemView.findViewById(R.id.sensorName);
            selectedTick = itemView.findViewById(R.id.sensorSelectedTick);
            infoButton = itemView.findViewById(R.id.sensorInfoButton);
            unavailableBadge = itemView.findViewById(R.id.sensorUnavailableBadge);
        }
    }
}
