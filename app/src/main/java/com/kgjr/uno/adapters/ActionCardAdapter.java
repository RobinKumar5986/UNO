package com.kgjr.uno.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.R;
import com.kgjr.uno.models.ActionItem;

import java.util.List;

public class ActionCardAdapter extends RecyclerView.Adapter<ActionCardAdapter.CardViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position, ActionItem item);
    }

    private final List<ActionItem> items;
    private final OnItemClickListener listener;

    public ActionCardAdapter(List<ActionItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        ActionItem item = items.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.icon.setColorFilter(holder.itemView.getContext()
                .getColor(item.getTintColorResId()));
        holder.title.setText(item.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(holder.getAdapterPosition(), item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(ActionItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.itemIcon);
            title = itemView.findViewById(R.id.itemTitle);
        }
    }
}