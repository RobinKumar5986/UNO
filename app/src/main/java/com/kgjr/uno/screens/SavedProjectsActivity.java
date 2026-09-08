package com.kgjr.uno.screens;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.AppConstant;
import com.kgjr.uno.EdgeToEdge;
import com.kgjr.uno.R;
import com.kgjr.uno.adapters.ProjectCardAdapter;
import com.kgjr.uno.data.ProjectRepository;
import com.kgjr.uno.data.ProjectSession;
import com.kgjr.uno.models.project.Project;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Browser for saved projects, newest edit first. Tapping a card loads the project into the
 * editor session and opens the editor, which replaces whatever the session held before.
 */
public class SavedProjectsActivity extends AppCompatActivity {

    private ProjectCardAdapter adapter;
    private RecyclerView list;
    private TextView empty;

    @Nullable
    private AlertDialog openDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, true);
        setContentView(R.layout.activity_saved_projects);

        list = findViewById(R.id.savedProjectsList);
        empty = findViewById(R.id.savedProjectsEmpty);

        adapter = new ProjectCardAdapter(this::confirmOpen, this::showDetails);
        list.setLayoutManager(new GridLayoutManager(this, columnCount()));
        list.setAdapter(adapter);

        findViewById(R.id.savedProjectsClose).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // A project may have been saved or renamed since this screen opened.
        refresh();
    }

    @Override
    protected void onDestroy() {
        dismissDialog();
        super.onDestroy();
    }

    private int columnCount() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        return landscape ? 3 : 2;
    }

    private void refresh() {
        List<Project> projects = ProjectRepository.loadAll(this);
        adapter.submit(projects);

        boolean none = projects.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        list.setVisibility(none ? View.GONE : View.VISIBLE);
    }

    /**
     * The session has no dirty flag, so the check is conservative: warn whenever there is a flow
     * in the editor that is about to be replaced, unless this is the same project reopening.
     */
    private void confirmOpen(Project project) {
        boolean reopeningSameProject = project.id.equals(AppConstant.currentProjectId);

        if (!AppConstant.hasWorkInProgress() || reopeningSameProject) {
            open(project);
            return;
        }

        dismissDialog();
        openDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.project_replace_title)
                .setMessage(getString(R.string.project_replace_message, displayName(project)))
                .setNegativeButton(R.string.project_replace_cancel, null)
                .setPositiveButton(R.string.project_replace_confirm,
                        (dialog, which) -> open(project))
                .show();
    }

    private void open(Project project) {
        if (!ProjectSession.load(this, project.id)) {
            Toast.makeText(this, R.string.saved_projects_load_failed, Toast.LENGTH_LONG).show();
            refresh();
            return;
        }

        Toast.makeText(this, getString(R.string.saved_projects_loaded, displayName(project)),
                Toast.LENGTH_SHORT).show();

        // The same editor Code Mode opens, with the session already filled in.
        startActivity(new Intent(this, EditorActivity.class));
        finish();
    }

    private void showDetails(Project project) {
        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_project_details, null, false);

        TextView name = content.findViewById(R.id.detailsName);
        name.setText(displayName(project));

        TextView description = content.findViewById(R.id.detailsDescription);
        description.setText(project.description.trim().isEmpty()
                ? getString(R.string.saved_projects_no_description)
                : project.description);

        row(content, R.id.detailsBlocks, R.string.project_details_blocks,
                String.valueOf(project.nodeCount()));
        row(content, R.id.detailsLinks, R.string.project_details_links,
                String.valueOf(project.connectionCount()));
        row(content, R.id.detailsSensors, R.string.project_details_sensors,
                project.sensorCount() == 0
                        ? getString(R.string.project_details_none)
                        : joinSensors(project));
        row(content, R.id.detailsSketch, R.string.project_details_sketch,
                getString(R.string.project_details_sketch_lines, project.sourceLineCount()));
        row(content, R.id.detailsCreated, R.string.project_details_created,
                formatDate(project.createdAt));
        row(content, R.id.detailsUpdated, R.string.project_details_updated,
                formatDate(project.updatedAt));

        TextView id = content.findViewById(R.id.detailsId);
        id.setText(project.id);

        dismissDialog();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton(R.string.project_details_open,
                        (d, which) -> confirmOpen(project))
                .setNegativeButton(R.string.saved_projects_close, null)
                .show();
        openDialog = dialog;

        ImageView delete = content.findViewById(R.id.detailsDelete);
        delete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(project);
        });
    }

    private void row(View parent, int containerId, int label, String value) {
        View container = parent.findViewById(containerId);
        ((TextView) container.findViewById(R.id.detailLabel)).setText(label);
        ((TextView) container.findViewById(R.id.detailValue)).setText(value);
    }

    private void confirmDelete(Project project) {
        dismissDialog();
        openDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.project_details_delete_title)
                .setMessage(getString(R.string.project_details_delete_message,
                        displayName(project)))
                .setNegativeButton(R.string.project_replace_cancel, null)
                .setPositiveButton(R.string.project_details_delete,
                        (dialog, which) -> delete(project))
                .show();
    }

    private void delete(Project project) {
        if (!ProjectRepository.delete(this, project.id)) {
            Toast.makeText(this, R.string.project_details_delete_failed, Toast.LENGTH_LONG).show();
            return;
        }

        // Detach the editor from a deleted id, so a later save creates a new project.
        if (project.id.equals(AppConstant.currentProjectId)) AppConstant.clearCurrentProject();

        Toast.makeText(this, getString(R.string.project_details_deleted, displayName(project)),
                Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void dismissDialog() {
        if (openDialog != null && openDialog.isShowing()) openDialog.dismiss();
        openDialog = null;
    }

    private String displayName(Project project) {
        return project.name.trim().isEmpty()
                ? getString(R.string.saved_projects_untitled)
                : project.name;
    }

    private String joinSensors(Project project) {
        StringBuilder builder = new StringBuilder();
        for (String sensor : project.sensorNames) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(sensor);
        }
        return builder.toString();
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0L) return getString(R.string.project_details_none);
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(timestamp));
    }
}
