package com.kgjr.uno.screens.fragments;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CanvasSnapshot;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CodeCanvasView;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.FlowScope;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.NodeDialogManager;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.TriggerStartDialog;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.FlowDocument;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;
import com.kgjr.uno.screens.fragments.codeHelper.model.TriggerFlow;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.PayloadFormat;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerFormats;
import com.kgjr.uno.screens.fragments.codeHelper.trigger.TriggerProgram;
import com.kgjr.uno.screens.fragments.dataHelper.canvas.DataCanvasView;

import java.util.ArrayList;
import java.util.List;

public class MobileCodeFragment extends Fragment {

    private static final String TAG = "MobileCode";

    private enum Mode { FLOW, TRIGGERS, DATA }

    private CodeCanvasView canvas;
    private CodeCanvasView triggerCanvas;
    private DataCanvasView dataCanvas;
    private View triggerPane;
    private View triggerEmpty;
    private LinearLayout triggerChips;
    private TextView title;
    private TextView subtitle;

    private ImageView headerIcon;
    private ImageView railFlowButton;
    private ImageView railTriggerButton;
    private ImageView railDataButton;

    private Mode mode = Mode.FLOW;
    private int selectedTrigger;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mobile_code, container, false);

        canvas = root.findViewById(R.id.code_canvas);
        canvas.setOnCanvasNodeListener(node -> onNodeTapped(canvas, node, FlowScope.MAIN));

        triggerCanvas = root.findViewById(R.id.trigger_canvas);
        triggerCanvas.setOnCanvasNodeListener(node ->
                onNodeTapped(triggerCanvas, node, currentTriggerScope()));

        triggerPane = root.findViewById(R.id.triggerPane);
        triggerEmpty = root.findViewById(R.id.triggerEmpty);
        triggerChips = root.findViewById(R.id.triggerChips);
        dataCanvas = root.findViewById(R.id.data_canvas);

        headerIcon = root.findViewById(R.id.mobileCodeIcon);
        title = root.findViewById(R.id.mobileCodeTitle);
        subtitle = root.findViewById(R.id.mobileCodeSubtitle);

        railFlowButton = root.findViewById(R.id.railFlowButton);
        railTriggerButton = root.findViewById(R.id.railTriggerButton);
        railDataButton = root.findViewById(R.id.railDataButton);
        railFlowButton.setOnClickListener(view -> showMode(Mode.FLOW));
        railTriggerButton.setOnClickListener(view -> showMode(Mode.TRIGGERS));
        railDataButton.setOnClickListener(view -> showMode(Mode.DATA));

        ImageView saveButton = root.findViewById(R.id.saveProjectButton);
        saveButton.setOnClickListener(this::openSaveScreen);

        FloatingActionButton nextScreenButton = root.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(this::generateAndContinue);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rebound every time: loading a project replaces these documents while we're away.
        canvas.bind(AppConstant.mainFlow);
        selectTrigger(selectedTrigger);
        showMode(mode);
    }

    /** Keeps AppConstant current for anything that checks for unsaved work while we're away. */
    @Override
    public void onPause() {
        if (canvas != null) flushCanvases();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        canvas = null;
        triggerCanvas = null;
        dataCanvas = null;
        triggerPane = null;
        triggerEmpty = null;
        triggerChips = null;
        headerIcon = null;
        title = null;
        subtitle = null;
        railFlowButton = null;
        railTriggerButton = null;
        railDataButton = null;
        super.onDestroyView();
    }

    /** Swaps the panes in place, so each canvas keeps its nodes and viewport. */
    private void showMode(Mode next) {
        if (canvas == null) return;
        mode = next;

        canvas.setVisibility(next == Mode.FLOW ? View.VISIBLE : View.GONE);
        triggerPane.setVisibility(next == Mode.TRIGGERS ? View.VISIBLE : View.GONE);
        dataCanvas.setVisibility(next == Mode.DATA ? View.VISIBLE : View.GONE);

        markRailButton(railFlowButton, next == Mode.FLOW);
        markRailButton(railTriggerButton, next == Mode.TRIGGERS);
        markRailButton(railDataButton, next == Mode.DATA);

        showHeader();
    }

    private void markRailButton(ImageView button, boolean active) {
        if (button == null) return;

        button.setSelected(active);
        ImageViewCompat.setImageTintList(button, ContextCompat.getColorStateList(
                requireContext(), active ? R.color.accent_yellow : R.color.text_secondary_dark));
    }

    /** The header doubles as the "which project am I in" indicator on the flow canvas. */
    private void showHeader() {
        if (title == null || subtitle == null) return;

        switch (mode) {
            case DATA:
                headerIcon.setImageResource(R.drawable.ic_data_chart);
                title.setText(R.string.data_view_title);
                subtitle.setText(R.string.data_view_subtitle);
                return;

            case TRIGGERS:
                headerIcon.setImageResource(R.drawable.ic_trigger);
                title.setText(R.string.triggers_title);
                subtitle.setText(R.string.triggers_subtitle);
                return;

            default:
                headerIcon.setImageResource(R.drawable.ic_code);
                break;
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

    // -------------------------------------------------------------- triggers

    private FlowScope currentTriggerScope() {
        List<TriggerFlow> triggers = AppConstant.triggers;
        if (selectedTrigger < 0 || selectedTrigger >= triggers.size()) return FlowScope.MAIN;
        return FlowScope.of(triggers.get(selectedTrigger), triggers);
    }

    private void selectTrigger(int index) {
        if (triggerCanvas == null) return;

        List<TriggerFlow> triggers = AppConstant.triggers;
        boolean empty = triggers.isEmpty();
        selectedTrigger = empty ? 0 : Math.max(0, Math.min(index, triggers.size() - 1));

        triggerCanvas.bind(empty ? null : triggers.get(selectedTrigger));
        triggerCanvas.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
        triggerEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        renderTriggerChips();
    }

    /** New triggers open straight on their format, since that has to be set before anything else. */
    private void addTrigger() {
        TriggerFlow trigger = new TriggerFlow();
        AppConstant.triggers.add(trigger);
        selectTrigger(AppConstant.triggers.size() - 1);

        TriggerStartDialog.show(requireContext(), currentTriggerScope(), () -> {
            triggerCanvas.invalidate();
            renderTriggerChips();
        });
    }

    private void confirmDeleteTrigger(int index) {
        String label = TriggerFormats.label(index);
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.triggers_delete_title, label))
                .setMessage(R.string.triggers_delete_message)
                .setNegativeButton(R.string.triggers_cancel, null)
                .setPositiveButton(R.string.triggers_delete, (d, which) -> {
                    triggerCanvas.bind(null);
                    AppConstant.triggers.remove(index);
                    selectTrigger(Math.min(index, AppConstant.triggers.size() - 1));
                })
                .show();
    }

    /** One chip per trigger (red while its format is unusable), then a + chip. Long-press deletes. */
    private void renderTriggerChips() {
        if (triggerChips == null) return;
        triggerChips.removeAllViews();

        List<TriggerFlow> triggers = AppConstant.triggers;
        for (int i = 0; i < triggers.size(); i++) {
            TriggerFlow trigger = triggers.get(i);
            boolean broken = TriggerFormats.problemOf(trigger.receive()) != null
                    || TriggerFormats.conflictMessage(trigger, triggers) != null;

            TextView chip = chip(TriggerFormats.label(i), i == selectedTrigger, broken);
            int index = i;
            chip.setOnClickListener(v -> selectTrigger(index));
            chip.setOnLongClickListener(v -> {
                confirmDeleteTrigger(index);
                return true;
            });
            triggerChips.addView(chip, chipParams(i > 0));
        }

        TextView add = chip("+", false, false);
        add.setContentDescription(getString(R.string.triggers_add));
        add.setOnClickListener(v -> addTrigger());
        triggerChips.addView(add, chipParams(!triggers.isEmpty()));
    }

    private TextView chip(String text, boolean selected, boolean broken) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(40));
        view.setBackgroundResource(selected ? R.drawable.bg_channel_chip : R.drawable.bg_condition_input);

        int color = broken ? R.color.accent_red
                : selected ? R.color.accent_blue : R.color.text_secondary_light;
        view.setTextColor(ContextCompat.getColor(requireContext(), color));
        view.setPadding(dp(14), dp(9), dp(14), dp(9));
        return view;
    }

    private LinearLayout.LayoutParams chipParams(boolean gapBefore) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (gapBefore) params.setMarginStart(dp(8));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ----------------------------------------------------------------- nodes

    /** Only the End block that terminates the program is configurable; the rest just close a section. */
    private void onNodeTapped(CodeCanvasView source, CanvasNode node, FlowScope scope) {
        if (node.type == NodeType.END
                && !FlowCode.isFinalEnd(node, source.nodes(), source.connections())) {
            if (node.data instanceof EndNodeData && ((EndNodeData) node.data).loop) {
                ((EndNodeData) node.data).loop = false;
                source.invalidate();
            }
            return;
        }
        NodeDialogManager.show(requireContext(), node, scope, () -> {
            source.invalidate();
            if (scope.isTrigger()) renderTriggerChips();
        });
    }

    // ------------------------------------------------------------ navigation

    private void flushCanvases() {
        canvas.flush();
        triggerCanvas.flush();
    }

    /**
     * Hands the canvases to the save screen. They are flushed into {@link AppConstant} here
     * rather than waiting for the views to detach, because the save screen reads them as soon
     * as it opens and the thumbnail is rendered while the nodes are still on screen.
     */
    private void openSaveScreen(View view) {
        flushCanvases();
        FlowDocument main = AppConstant.mainFlow;

        if (main.isEmpty() && AppConstant.triggers.isEmpty()) {
            Toast.makeText(requireContext(), R.string.mobile_code_nothing_to_save,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // A project without a thumbnail still saves, it just shows a placeholder.
        Bitmap thumbnail = CanvasSnapshot.of(requireContext(), main.nodes, main.connections);
        if (thumbnail == null) Log.w(TAG, "Could not render a canvas thumbnail");
        AppConstant.pendingThumbnail = thumbnail;

        Navigation.findNavController(view)
                .navigate(R.id.action_mobileCodeFragment_to_saveProjectFragment);
    }

    private void generateAndContinue(View view) {
        flushCanvases();

        FlowDocument main = AppConstant.mainFlow;
        List<FlowBlock> tree = FlowCode.parse(main.nodes, main.connections);
        String code = FlowCode.generate(tree);
        Log.d(TAG, "Generated code:\n" + code);

        // With triggers in place, the Stage 1 flow is optional.
        boolean skipMain = !FlowCode.hasWork(tree) && !AppConstant.triggers.isEmpty();
        String error = skipMain ? null : FlowCode.validate(code, tree);
        if (error != null) {
            fail(error, Mode.FLOW, selectedTrigger);
            return;
        }

        List<TriggerProgram> programs = new ArrayList<>();
        List<TriggerFlow> triggers = AppConstant.triggers;

        for (int i = 0; i < triggers.size(); i++) {
            TriggerFlow trigger = triggers.get(i);
            String label = TriggerFormats.label(i);

            String problem = TriggerFormats.problemOf(trigger.receive());
            if (problem == null) problem = TriggerFormats.conflictMessage(trigger, triggers);
            if (problem != null) {
                fail(label + ": " + problem, Mode.TRIGGERS, i);
                return;
            }

            List<FlowBlock> triggerTree = FlowCode.parse(trigger.nodes, trigger.connections);
            String triggerCode = FlowCode.generate(triggerTree);
            PayloadFormat format = PayloadFormat.parse(trigger.receive().payload);

            String triggerError = FlowCode.validateTrigger(triggerCode, triggerTree, format);
            if (triggerError != null) {
                fail(label + ": " + triggerError, Mode.TRIGGERS, i);
                return;
            }
            Log.d(TAG, label + " code:\n" + triggerCode);
            programs.add(new TriggerProgram(trigger.id, label, trigger.receive(), triggerTree));
        }

        AppConstant.generatedCode = code;
        AppConstant.flowTree = tree;
        AppConstant.triggerPrograms = programs;

        Navigation.findNavController(view)
                .navigate(R.id.action_mobileCodeFragment_to_codeExeFragment);
    }

    /** Shows the error and jumps to the canvas it is about. */
    private void fail(String error, Mode where, int trigger) {
        Log.e(TAG, "Validation failed: " + error);
        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();

        if (where == Mode.TRIGGERS) selectTrigger(trigger);
        showMode(where);
    }
}
