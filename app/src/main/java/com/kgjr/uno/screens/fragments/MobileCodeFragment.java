package com.kgjr.uno.screens.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CanvasSnapshot;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CodeCanvasView;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.NodeDialogManager;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.Connection;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;
import com.kgjr.uno.screens.fragments.dataHelper.canvas.DataCanvasView;

import java.util.List;

public class MobileCodeFragment extends Fragment {

    private static final String TAG = "MobileCode";

    private CodeCanvasView canvas;
    private DataCanvasView dataCanvas;
    private TextView title;
    private TextView subtitle;

    private ImageView headerIcon;
    private ImageView railFlowButton;
    private ImageView railDataButton;

    private boolean showingData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mobile_code, container, false);

        canvas = root.findViewById(R.id.code_canvas);
        canvas.setOnCanvasNodeListener(this::onNodeTapped);

        dataCanvas = root.findViewById(R.id.data_canvas);

        headerIcon = root.findViewById(R.id.mobileCodeIcon);
        title = root.findViewById(R.id.mobileCodeTitle);
        subtitle = root.findViewById(R.id.mobileCodeSubtitle);

        railFlowButton = root.findViewById(R.id.railFlowButton);
        railDataButton = root.findViewById(R.id.railDataButton);
        railFlowButton.setOnClickListener(view -> showCanvas(false));
        railDataButton.setOnClickListener(view -> showCanvas(true));
        showCanvas(showingData);

        ImageView saveButton = root.findViewById(R.id.saveProjectButton);
        saveButton.setOnClickListener(this::openSaveScreen);

        FloatingActionButton nextScreenButton = root.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(this::generateAndContinue);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        showOpenProject();
    }

    @Override
    public void onDestroyView() {
        canvas = null;
        dataCanvas = null;
        headerIcon = null;
        title = null;
        subtitle = null;
        railFlowButton = null;
        railDataButton = null;
        super.onDestroyView();
    }

    /** Swaps the canvases in place, so the flow graph keeps its nodes and viewport. */
    private void showCanvas(boolean data) {
        if (canvas == null || dataCanvas == null) return;

        showingData = data;

        canvas.setVisibility(data ? View.GONE : View.VISIBLE);
        dataCanvas.setVisibility(data ? View.VISIBLE : View.GONE);

        markRailButton(railFlowButton, !data);
        markRailButton(railDataButton, data);

        showOpenProject();
    }

    private void markRailButton(ImageView button, boolean active) {
        if (button == null) return;

        button.setSelected(active);
        ImageViewCompat.setImageTintList(button, ContextCompat.getColorStateList(
                requireContext(), active ? R.color.accent_yellow : R.color.text_secondary_dark));
    }

    /** The header doubles as the "which project am I in" indicator. */
    private void showOpenProject() {
        if (title == null || subtitle == null) return;

        if (headerIcon != null) {
            headerIcon.setImageResource(showingData ? R.drawable.ic_data_chart : R.drawable.ic_code);
        }

        if (showingData) {
            title.setText(R.string.data_view_title);
            subtitle.setText(R.string.data_view_subtitle);
            return;
        }

        if (AppConstant.isEditingSavedProject()
                && !AppConstant.currentProjectName.trim().isEmpty()) {
            title.setText(AppConstant.currentProjectName);
            subtitle.setText(AppConstant.currentProjectDescription.trim().isEmpty()
                    ? getString(R.string.mobile_code_subtitle)
                    : AppConstant.currentProjectDescription);
        } else {
            title.setText(R.string.mobile_code_title);
            subtitle.setText(R.string.mobile_code_subtitle);
        }
    }

    /** Only the End block that terminates the program is configurable; the rest just close a section. */
    private void onNodeTapped(CanvasNode node) {
        if (node.type == NodeType.END
                && !FlowCode.isFinalEnd(node, canvas.nodes(), canvas.connections())) {
            if (node.data instanceof EndNodeData && ((EndNodeData) node.data).loop) {
                ((EndNodeData) node.data).loop = false;
                canvas.invalidate();
            }
            return;
        }
        NodeDialogManager.show(requireContext(), node, canvas::invalidate);
    }

    /**
     * Hands the canvas to the save screen. The graph is pushed into {@link AppConstant} here
     * rather than waiting for the view to detach, because the save screen reads it as soon as it
     * opens and the thumbnail is rendered while the nodes are still on screen.
     */
    private void openSaveScreen(View view) {
        List<CanvasNode> nodes = canvas.nodes();
        List<Connection> connections = canvas.connections();

        if (nodes.size() <= 1) {
            Toast.makeText(requireContext(), R.string.mobile_code_nothing_to_save,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AppConstant.canvasNodes = nodes;
        AppConstant.canvasConnections = connections;

        // A project without a thumbnail still saves, it just shows a placeholder.
        Bitmap thumbnail = CanvasSnapshot.of(requireContext(), nodes, connections);
        if (thumbnail == null) Log.w(TAG, "Could not render a canvas thumbnail");
        AppConstant.pendingThumbnail = thumbnail;

        Navigation.findNavController(view)
                .navigate(R.id.action_mobileCodeFragment_to_saveProjectFragment);
    }

    private void generateAndContinue(View view) {
        List<FlowBlock> tree = FlowCode.parse(canvas.nodes(), canvas.connections());
        String code = FlowCode.generate(tree);
        Log.d(TAG, "Generated code:\n" + code);

        String error = FlowCode.validate(code, tree);
        if (error != null) {
            Log.e(TAG, "Validation failed: " + error);
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            return;
        }

        AppConstant.generatedCode = code;
        AppConstant.flowTree = tree;

        Navigation.findNavController(view)
                .navigate(R.id.action_mobileCodeFragment_to_codeExeFragment);
    }
}
