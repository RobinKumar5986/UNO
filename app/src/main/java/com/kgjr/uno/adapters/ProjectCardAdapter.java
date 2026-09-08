package com.kgjr.uno.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.R;
import com.kgjr.uno.data.ProjectRepository;
import com.kgjr.uno.models.project.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The saved-project grid. Tapping a card opens the project, the info icon opens its details.
 * Thumbnails are decoded once and cached by project id — the list is short and each bitmap is
 * subsampled to the card, so this is cheaper than pulling in an image loader.
 */
public class ProjectCardAdapter extends RecyclerView.Adapter<ProjectCardAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClicked(Project project);
    }

    public interface OnProjectInfoListener {
        void onProjectInfoClicked(Project project);
    }

    private static final int THUMBNAIL_TARGET_WIDTH = 320;
    private static final int THUMBNAIL_TARGET_HEIGHT = 200;

    private final List<Project> projects = new ArrayList<>();
    private final Map<String, Bitmap> thumbnails = new HashMap<>();

    private final OnProjectClickListener clickListener;
    private final OnProjectInfoListener infoListener;

    public ProjectCardAdapter(OnProjectClickListener clickListener,
                              OnProjectInfoListener infoListener) {
        this.clickListener = clickListener;
        this.infoListener = infoListener;
    }

    /** Replaces the list, keeping cached thumbnails for projects that are still present. */
    public void submit(List<Project> updated) {
        projects.clear();
        if (updated != null) projects.addAll(updated);

        Set<String> live = new HashSet<>();
        for (Project project : projects) live.add(project.id);
        thumbnails.keySet().retainAll(live);

        notifyDataSetChanged();
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(project.name.trim().isEmpty()
                ? context.getString(R.string.saved_projects_untitled)
                : project.name);

        holder.updated.setText(context.getString(R.string.saved_projects_updated,
                relativeTime(project.updatedAt)));

        Bitmap thumbnail = thumbnail(context, project.id);
        holder.thumbnail.setImageBitmap(thumbnail);
        holder.thumbnail.setVisibility(thumbnail == null ? View.GONE : View.VISIBLE);
        holder.thumbnailEmpty.setVisibility(thumbnail == null ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onProjectClicked(project);
        });
        holder.info.setOnClickListener(v -> {
            if (infoListener != null) infoListener.onProjectInfoClicked(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    private Bitmap thumbnail(Context context, String id) {
        if (thumbnails.containsKey(id)) return thumbnails.get(id);

        Bitmap bitmap = ProjectRepository.loadThumbnail(context, id,
                THUMBNAIL_TARGET_WIDTH, THUMBNAIL_TARGET_HEIGHT);
        thumbnails.put(id, bitmap);
        return bitmap;
    }

    private static CharSequence relativeTime(long timestamp) {
        if (timestamp <= 0L) return "—";
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView thumbnailEmpty;
        final TextView name;
        final TextView updated;
        final ImageView info;

        ProjectViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.projectThumbnail);
            thumbnailEmpty = itemView.findViewById(R.id.projectThumbnailEmpty);
            name = itemView.findViewById(R.id.projectName);
            updated = itemView.findViewById(R.id.projectUpdated);
            info = itemView.findViewById(R.id.projectInfo);
        }
    }
}
