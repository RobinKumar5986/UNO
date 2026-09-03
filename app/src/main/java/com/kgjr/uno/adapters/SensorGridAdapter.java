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
import com.kgjr.uno.models.sensors.SensorType;

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

    /** Cards for sensors the phone can't back are shown, but dimmed and not selectable. */
    private static final float UNAVAILABLE_ALPHA = 0.45f;

    private final List<PhoneSensor> sensors;
    private final SensorManager sensorManager;
    private final OnSensorToggled listener;

    public SensorGridAdapter(List<PhoneSensor> sensors, SensorManager sensorManager,
                             OnSensorToggled listener) {
        this.sensors = new ArrayList<>(sensors);
        this.sensorManager = sensorManager;
        this.listener = listener;
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
        holder.description.setText(sensor.description);
        holder.channels.setText(sensor.channelSummary());

        holder.typeBadge.setText(sensor.type.label);
        holder.typeBadge.setBackgroundResource(sensor.type == SensorType.OUTPUT
                ? R.drawable.bg_sensor_badge_output
                : R.drawable.bg_sensor_badge_input);

        int image = sensor.imageRes(holder.itemView.getContext());
        holder.image.setImageResource(image != 0 ? image : R.drawable.ic_sensors);

        bindState(holder, sensor, available);

        if (available) {
            holder.itemView.setOnClickListener(view -> {
                if (listener != null) listener.onSensorToggled(sensor);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
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
        final TextView description;
        final TextView channels;
        final TextView typeBadge;
        final ImageView selectedTick;
        final TextView unavailableBadge;

        SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.sensorImage);
            name = itemView.findViewById(R.id.sensorName);
            description = itemView.findViewById(R.id.sensorDescription);
            channels = itemView.findViewById(R.id.sensorChannels);
            typeBadge = itemView.findViewById(R.id.sensorTypeBadge);
            selectedTick = itemView.findViewById(R.id.sensorSelectedTick);
            unavailableBadge = itemView.findViewById(R.id.sensorUnavailableBadge);
        }
    }
}
