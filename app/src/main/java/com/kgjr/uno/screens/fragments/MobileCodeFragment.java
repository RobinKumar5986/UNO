package com.kgjr.uno.screens.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.codeHelper.canvas.CodeCanvasView;
import com.kgjr.uno.screens.fragments.codeHelper.dialogs.NodeDialogManager;

public class MobileCodeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mobile_code, container, false);

        CodeCanvasView canvas = root.findViewById(R.id.code_canvas);
        canvas.setOnCanvasNodeListener(node ->
                NodeDialogManager.show(requireContext(), node, canvas::invalidate));

        FloatingActionButton nextScreenButton = root.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_mobileCodeFragment_to_codeExeFragment);
        });

        return root;
    }
}