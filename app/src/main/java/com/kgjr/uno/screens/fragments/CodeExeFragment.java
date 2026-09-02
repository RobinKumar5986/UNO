package com.kgjr.uno.screens.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.AppConstant;
import com.kgjr.uno.R;
import com.kgjr.uno.screens.fragments.exeHelper.FlowRunner;
import com.kgjr.uno.screens.fragments.exeHelper.SerialLink;


public class CodeExeFragment extends Fragment implements FlowRunner.Listener {

    private static final String TAG = "CodeExe";

    private FloatingActionButton runCode;
    private SerialLink serial;
    private FlowRunner runner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_code_exe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        runCode = view.findViewById(R.id.run_code);

        serial = new SerialLink(requireContext(), this::onLog);
        serial.register();
        serial.connect();

        runner = new FlowRunner(serial, this);

        runCode.setOnClickListener(v -> {
            if (runner.isRunning()) {
                runner.stop();
                return;
            }
            if (!serial.isConnected()) {
                serial.connect();

                onLog("Connect the board over USB first");
                return;
            }
            runCode.setAlpha(0.5f);
            runner.start(AppConstant.flowTree);
        });
    }

    @Override
    public void onLog(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void onStopped() {
        View view = getView();
        if (view == null) return;
        view.post(() -> {
            if (runCode != null) runCode.setAlpha(1.0f);
        });
    }

    @Override
    public void onDestroyView() {
        if (runner != null) runner.stop();
        if (serial != null) {
            serial.unregister();
            serial.close();
        }
        super.onDestroyView();
    }
}