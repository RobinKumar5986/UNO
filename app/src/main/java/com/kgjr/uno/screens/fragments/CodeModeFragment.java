package com.kgjr.uno.screens.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kgjr.uno.R;
import com.kgjr.uno.flash.Stk500Programmer;
import com.kgjr.uno.flash.Uploader;
import com.kgjr.uno.ide.ArduinoCompiler;
import com.kgjr.uno.ide.AvrSdk;
import com.kgjr.uno.ide.Board;
import com.kgjr.uno.ide.BuildResult;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Code mode: write a sketch, compile it on the phone, flash it over USB.
 *
 * If the native toolchain is not bundled yet, Compile explains what is missing
 * and Upload falls back to the prebuilt blink.hex from the SDK assets, so the
 * USB half of the pipeline can be exercised on its own.
 *
 * Lifecycle notes for the port from EditorActivity:
 *  - {@link #host} is captured in onAttach so the worker thread never calls
 *    requireContext()/requireActivity(), which throw once the fragment detaches.
 *    It is the same object the activity used to pass to itself as {@code this}.
 *  - View fields are cleared in onDestroyView; every UI post null-checks them, so a
 *    build that finishes after the view is gone lands harmlessly.
 *  - Log lines produced before the view exists (the SDK unpack starts in onCreate)
 *    are buffered in {@link #pendingLog} and flushed in onViewCreated.
 *  - {@code busy} and {@link #pendingLog} survive *view* recreation (navigating back
 *    to this destination), not a full config change — rotation builds a new fragment
 *    instance, exactly as rotation used to build a new EditorActivity.
 */
public class CodeModeFragment extends Fragment {

    private static final String PREF = "editor";
    private static final String KEY_SOURCE = "source";

    private static final String DEFAULT_SKETCH =
            "// Blink the built-in LED on pin 13.\n"
                    + "\n"
                    + "#define LED_PIN LED_BUILTIN\n"
                    + "\n"
                    + "void setup() {\n"
                    + "  pinMode(LED_PIN, OUTPUT);\n"
                    + "}\n"
                    + "\n"
                    + "void loop() {\n"
                    + "  digitalWrite(LED_PIN, HIGH);\n"
                    + "  delay(500);\n"
                    + "  digitalWrite(LED_PIN, LOW);\n"
                    + "  delay(500);\n"
                    + "}\n";

    private final Board board = Board.UNO;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    /** Log text emitted before the view was inflated. Main thread only. */
    private final StringBuilder pendingLog = new StringBuilder();

    private Activity host;

    private EditText editor;
    private TextView logView;
    private ScrollView logScroll;
    private ProgressBar progressBar;
    private Button compileButton;
    private Button uploadButton;

    private File lastHex;
    private boolean busy;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        host = requireActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Unpack the SDK in the background so the first Compile is not slow.
        worker.execute(() -> {
            try {
                new AvrSdk(host).installIfNeeded(this::appendLogAsync);
            } catch (Exception e) {
                appendLogAsync("Could not unpack the AVR SDK: " + e);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_code_mode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editor = view.findViewById(R.id.sketchEditor);
        logView = view.findViewById(R.id.buildLog);
        logScroll = view.findViewById(R.id.logScroll);
        progressBar = view.findViewById(R.id.progressBar);
        compileButton = view.findViewById(R.id.compileButton);
        uploadButton = view.findViewById(R.id.uploadButton);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        editor.setText(prefs.getString(KEY_SOURCE, DEFAULT_SKETCH));

        host.setTitle(board.displayName);

        compileButton.setOnClickListener(v -> compile(false));
        uploadButton.setOnClickListener(v -> compile(true));
        view.findViewById(R.id.resetButton).setOnClickListener(v -> {
            editor.setText(DEFAULT_SKETCH);
            clearLog();
        });

        // Re-apply the button state and replay anything logged while the view was away
        // (SDK unpack, or a build still running behind the back stack).
        setBusy(busy);
        if (pendingLog.length() > 0) {
            logView.append(pendingLog);
            pendingLog.setLength(0);
            scrollLogToBottom();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (editor == null) return;
        requireContext().getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SOURCE, editor.getText().toString())
                .apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release the views; every ui.post() below null-checks them, so a late
        // callback from an in-flight build becomes a no-op instead of a crash.
        //
        // Deliberately NOT calling ui.removeCallbacksAndMessages(null): that would
        // also discard a queued setBusy(false), leaving busy stuck true and both
        // buttons permanently disabled when the view comes back off the back stack.
        editor = null;
        logView = null;
        logScroll = null;
        progressBar = null;
        compileButton = null;
        uploadButton = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }

    // ------------------------------------------------------------------

    private void compile(boolean thenUpload) {
        if (busy || editor == null) return;
        setBusy(true);
        clearLog();

        final String source = editor.getText().toString();

        worker.execute(() -> {
            ArduinoCompiler compiler = new ArduinoCompiler(host);
            BuildResult result = compiler.build(source, board, this::appendLogAsync);

            if (result.ok) {
                lastHex = result.hexFile;
                if (thenUpload) {
                    doUpload(result.hexFile);
                    return;
                }
                ui.post(() -> {
                    setBusy(false);
                    toast("Compiled: " + result.programBytes + " bytes");
                });
                return;
            }

            // Toolchain not installed yet? Still let the user prove the USB path.
            if (thenUpload && !compiler.tools().isComplete()) {
                File fallback = compiler.sdk().exampleBlinkHex();
                if (fallback.isFile()) {
                    appendLogAsync("");
                    appendLogAsync("Falling back to the prebuilt blink.hex that ships in assets,");
                    appendLogAsync("so the upload path can be tested without the compiler.");
                    doUpload(fallback);
                    return;
                }
            }

            ui.post(() -> {
                setBusy(false);
                toast(result.error);
            });
        });
    }

    private void doUpload(File hex) {
        Stk500Programmer.Progress progress = new Stk500Programmer.Progress() {
            @Override
            public void onLog(String message) {
                appendLogAsync(message);
            }

            @Override
            public void onProgress(int percent) {
                ui.post(() -> {
                    if (progressBar != null) progressBar.setProgress(percent);
                });
            }
        };

        try {
            appendLogAsync("");
            appendLogAsync("--- upload ---");
            new Uploader(host).upload(hex, board, true, progress);
            ui.post(() -> {
                setBusy(false);
                toast("Uploaded");
            });
        } catch (Exception e) {
            appendLogAsync("");
            appendLogAsync("Upload failed: " + e.getMessage());
            ui.post(() -> {
                setBusy(false);
                toast("Upload failed");
            });
        }
    }

    // ------------------------------------------------------------------

    private void setBusy(boolean value) {
        busy = value;
        if (compileButton == null || uploadButton == null || progressBar == null) return;
        compileButton.setEnabled(!value);
        uploadButton.setEnabled(!value);
        progressBar.setVisibility(value ? View.VISIBLE : View.GONE);
        progressBar.setProgress(0);
    }

    private void clearLog() {
        pendingLog.setLength(0);
        if (logView != null) logView.setText("");
    }

    private void appendLogAsync(@NonNull String line) {
        ui.post(() -> {
            if (logView == null) {
                pendingLog.append(line).append('\n');
                return;
            }
            logView.append(line);
            logView.append("\n");
            scrollLogToBottom();
        });
    }

    private void scrollLogToBottom() {
        final ScrollView scroll = logScroll;
        if (scroll == null) return;
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String message) {
        if (!isAdded()) return;
        Toast.makeText(host, message, Toast.LENGTH_SHORT).show();
    }
}