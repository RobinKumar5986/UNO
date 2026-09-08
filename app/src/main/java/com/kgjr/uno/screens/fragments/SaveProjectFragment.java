package com.kgjr.uno.screens.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.data.ProjectSession;
import com.kgjr.uno.models.project.Project;

/**
 * Names a project and writes it to disk. The builder leaves the canvas graph and a rendered
 * thumbnail on {@link AppConstant} for this screen to pick up. Saving mints a UUID the first
 * time and reuses it afterwards, so returning here updates the project in place.
 */
public class SaveProjectFragment extends Fragment {

    private EditText nameField;
    private EditText descriptionField;
    private TextView nameCounter;
    private TextView descriptionCounter;
    private ImageView preview;

    private boolean saving;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_save_project, container, false);

        nameField = root.findViewById(R.id.saveProjectName);
        descriptionField = root.findViewById(R.id.saveProjectDescription);
        nameCounter = root.findViewById(R.id.saveProjectNameCounter);
        descriptionCounter = root.findViewById(R.id.saveProjectDescriptionCounter);

        bindFields();
        bindHeader(root);
        bindPreview(root);
        bindSummary(root);

        FloatingActionButton confirm = root.findViewById(R.id.saveProjectConfirm);
        confirm.setOnClickListener(this::save);

        return root;
    }

    /** Limits are enforced by the fields, so the user cannot type past them and then be told off. */
    private void bindFields() {
        nameField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Project.NAME_MAX)});
        descriptionField.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(Project.DESCRIPTION_MAX)});

        nameField.setText(AppConstant.currentProjectName);
        descriptionField.setText(AppConstant.currentProjectDescription);
        nameField.setSelection(nameField.getText().length());

        watch(nameField, nameCounter, Project.NAME_MAX);
        watch(descriptionField, descriptionCounter, Project.DESCRIPTION_MAX);
    }

    private void watch(EditText field, TextView counter, int max) {
        updateCounter(counter, field.getText().length(), max);

        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCounter(counter, s.length(), max);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateCounter(TextView counter, int length, int max) {
        counter.setText(getString(R.string.save_project_counter, length, max));
        counter.setTextColor(getResources().getColor(
                length >= max ? R.color.accent_yellow : R.color.text_secondary_dark, null));
    }

    private void bindHeader(View root) {
        boolean updating = AppConstant.isEditingSavedProject();

        TextView subtitle = root.findViewById(R.id.saveProjectSubtitle);
        subtitle.setText(updating
                ? R.string.save_project_subtitle_update
                : R.string.save_project_subtitle_new);

        TextView badge = root.findViewById(R.id.saveProjectModeBadge);
        badge.setText(updating
                ? R.string.save_project_update_badge
                : R.string.save_project_new_badge);
    }

    private void bindPreview(View root) {
        preview = root.findViewById(R.id.saveProjectPreview);
        TextView empty = root.findViewById(R.id.saveProjectPreviewEmpty);

        Bitmap thumbnail = AppConstant.pendingThumbnail;
        boolean hasThumbnail = thumbnail != null && !thumbnail.isRecycled();

        preview.setImageBitmap(hasThumbnail ? thumbnail : null);
        preview.setVisibility(hasThumbnail ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasThumbnail ? View.GONE : View.VISIBLE);
    }

    private void bindSummary(View root) {
        TextView summary = root.findViewById(R.id.saveProjectSummary);
        summary.setText(getString(R.string.save_project_summary,
                AppConstant.canvasNodes.size(),
                AppConstant.canvasConnections.size(),
                AppConstant.selectedSensors.size()));
    }

    private void save(View view) {
        if (saving) return;

        String name = nameField.getText().toString().trim();
        if (name.isEmpty()) {
            nameField.setError(getString(R.string.save_project_name_required));
            nameField.requestFocus();
            return;
        }

        saving = true;
        hideKeyboard();

        Project saved = ProjectSession.save(requireContext(), name,
                descriptionField.getText().toString());

        if (saved == null) {
            saving = false;
            Toast.makeText(requireContext(), R.string.save_project_failed, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(requireContext(), getString(R.string.save_project_saved, saved.name),
                Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }

    private void hideKeyboard() {
        View focused = requireActivity().getCurrentFocus();
        if (focused == null) return;

        InputMethodManager manager = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        // Let the thumbnail go rather than pinning it to a dead view.
        if (preview != null) preview.setImageBitmap(null);
        preview = null;
        nameField = null;
        descriptionField = null;
        nameCounter = null;
        descriptionCounter = null;
        super.onDestroyView();
    }
}
