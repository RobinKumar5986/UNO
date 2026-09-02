package com.kgjr.uno.screens.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CodeCanvasView;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.NodeDialogManager;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowBlock;
import com.kgjr.uno.screens.fragments.codeHelper.flow.FlowCode;
import com.kgjr.uno.screens.fragments.codeHelper.model.CanvasNode;
import com.kgjr.uno.screens.fragments.codeHelper.model.EndNodeData;
import com.kgjr.uno.screens.fragments.codeHelper.model.NodeType;

import java.util.List;

public class MobileCodeFragment extends Fragment {

    private static final String TAG = "MobileCode";

    private CodeCanvasView canvas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mobile_code, container, false);

        canvas = root.findViewById(R.id.code_canvas);
        canvas.setOnCanvasNodeListener(this::onNodeTapped);

        FloatingActionButton nextScreenButton = root.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(this::generateAndContinue);

        return root;
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