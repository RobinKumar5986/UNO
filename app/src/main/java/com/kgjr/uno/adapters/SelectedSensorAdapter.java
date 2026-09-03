package com.kgjr.uno.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.R;
import com.kgjr.uno.models.sensors.PhoneSensor;
import com.kgjr.uno.models.sensors.SensorChannel;

import java.util.ArrayList;
import java.util.List;

/** The right-hand panel: the sensors the user picked, newest last, each removable. */
public class SelectedSensorAdapter
        extends RecyclerView.Adapter<SelectedSensorAdapter.SelectedViewHolder> {

    public interface OnSensorRemoved {
        void onSensorRemoved(PhoneSensor sensor);
    }

    private final List<PhoneSensor> sensors = new ArrayList<>();
    private final OnSensorRemoved listener;

    public SelectedSensorAdapter(List<PhoneSensor> sensors, OnSensorRemoved listener) {
        this.sensors.addAll(sensors);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SelectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_sensor, parent, false);
        return new SelectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedViewHolder holder, int position) {
        PhoneSensor sensor = sensors.get(position);

        holder.name.setText(sensor.displayName);
        holder.channels.setText(channelLabel(sensor));

        int image = sensor.imageRes(holder.itemView.getContext());
        holder.image.setImageResource(image != 0 ? image : R.drawable.ic_sensors);

        holder.remove.setOnClickListener(v -> {
            if (listener != null) listener.onSensorRemoved(sensor);
        });
    }

    /** "Azimuth (0–360°)" for one channel, "X, Y, Z" once there are several. */
    private static String channelLabel(PhoneSensor sensor) {
        if (sensor.channelCount() == 1) return sensor.channels.get(0).detailLabel();

        StringBuilder names = new StringBuilder();
        for (SensorChannel channel : sensor.channels) {
            if (names.length() > 0) names.append(", ");
            names.append(channel.displayName);
        }
        return names.toString();
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    /** Replaces the panel contents with the current selection. */
    public void submit(List<PhoneSensor> updated) {
        sensors.clear();
        sensors.addAll(updated);
        notifyDataSetChanged();
    }

    static class SelectedViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        final TextView name;
        final TextView channels;
        final ImageView remove;

        SelectedViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.selectedSensorImage);
            name = itemView.findViewById(R.id.selectedSensorName);
            channels = itemView.findViewById(R.id.selectedSensorChannels);
            remove = itemView.findViewById(R.id.selectedSensorRemove);
        }
    }
}
