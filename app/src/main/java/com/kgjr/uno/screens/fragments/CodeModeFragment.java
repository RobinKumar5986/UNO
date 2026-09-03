package com.kgjr.uno.screens.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kgjr.uno.R;
import com.kgjr.uno.flash.Stk500Programmer;
import com.kgjr.uno.flash.Uploader;
import com.kgjr.uno.ide.ArduinoCompiler;
import com.kgjr.uno.ide.AvrSdk;
import com.kgjr.uno.ide.Board;
import com.kgjr.uno.ide.BuildResult;
import com.kgjr.uno.screens.AiChatActivity;
import com.kgjr.uno.screens.fragments.helpers.CodeEditorView;
import com.kgjr.uno.screens.fragments.helpers.CodeFoldManager;
import com.kgjr.uno.screens.fragments.helpers.CodeGutterView;
import com.kgjr.uno.screens.fragments.helpers.CodeModeHelper;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CodeModeFragment extends Fragment {

    private final Board board = Board.UNO;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final StringBuilder pendingLog = new StringBuilder();

    private Activity host;

    private CodeEditorView editor;
    private CodeGutterView gutter;
    private TextView logView;
    private ScrollView outputScroll;
    private ImageButton compileButton;
    private ImageButton uploadButton;
    private ProgressBar progressBar;

    private View contentRow;
    private View editorFrame;
    private View outputFrame;
    private View divider;

    private float dragLastX;
    private boolean busy;
    FloatingActionButton nextScreenButton;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        host = requireActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        gutter = view.findViewById(R.id.codeGutter);
        logView = view.findViewById(R.id.buildLog);
        outputScroll = view.findViewById(R.id.outputPanel);
        compileButton = view.findViewById(R.id.toolbarCompileButton);
        uploadButton = view.findViewById(R.id.toolbarUploadButton);
        progressBar = view.findViewById(R.id.uploadProgress);
        contentRow = view.findViewById(R.id.contentRow);
        editorFrame = view.findViewById(R.id.editorFrame);
        outputFrame = view.findViewById(R.id.outputFrame);
        divider = view.findViewById(R.id.divider);
        FloatingActionButton transferButton = view.findViewById(R.id.transferButton);

        gutter.attach(editor);
        editor.setText(CodeModeHelper.loadSource(requireContext()));
        host.setTitle(board.displayName);

        setupAiSiteButtons(view);
        setupSplitView();
        setupKeyboardDismiss(view);

        compileButton.setOnClickListener(v -> compile());
        uploadButton.setOnClickListener(v -> compileThenUpload());
        transferButton.setOnClickListener(v -> openChat(defaultAiUrl(), true));

        setBusy(busy);
        if (pendingLog.length() > 0) {
            logView.append(pendingLog);
            pendingLog.setLength(0);
            scrollLogToBottom();
        }
        nextScreenButton = view.findViewById(R.id.nextScreenButton);
        nextScreenButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_codeModeFragment_to_sensorsFragment));
    }

    @Override
    public void onPause() {
        super.onPause();
        saveSource();
    }

    // Folded regions render as placeholders, so the visible text is not the sketch.
    // Everything that leaves this screen goes through the expanded source.
    private String currentSource() {
        if (editor == null) return "";
        return CodeFoldManager.expandForCompile(editor.getText());
    }

    private void saveSource() {
        if (editor == null || !isAdded()) return;
        CodeModeHelper.saveSource(requireContext(), currentSource());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editor = null;
        gutter = null;
        logView = null;
        outputScroll = null;
        compileButton = null;
        uploadButton = null;
        progressBar = null;
        contentRow = null;
        editorFrame = null;
        outputFrame = null;
        divider = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }

    private void setupAiSiteButtons(View view) {
        List<CodeModeHelper.AiSite> sites = CodeModeHelper.aiSites();
        int[] ids = {R.id.aiSiteButton1, R.id.aiSiteButton2, R.id.aiSiteButton3};
        for (int i = 0; i < ids.length && i < sites.size(); i++) {
            final String url = sites.get(i).url;
            ImageView button = view.findViewById(ids[i]);
            if (button != null) button.setOnClickListener(v -> openChat(url, false));
        }
    }

    private String defaultAiUrl() {
        List<CodeModeHelper.AiSite> sites = CodeModeHelper.aiSites();
        return sites.isEmpty() ? "https://chatgpt.com" : sites.get(0).url;
    }

    private void openChat(String url, boolean withSketch) {
        String code = null;
        if (withSketch && editor != null) {
            code = currentSource();
            CodeModeHelper.copyToClipboard(host, "sketch", code);
        }
        saveSource();
        hideKeyboard();
        startActivity(AiChatActivity.intentFor(host, url, code));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSplitView() {
        if (contentRow == null || editorFrame == null || outputFrame == null || divider == null) {
            return;
        }

        contentRow.post(() -> {
            if (contentRow == null) return;
            int total = contentRow.getWidth() - divider.getWidth();
            if (total <= 0) return;
            int left = (int) (total * 0.62f);
            setWidth(editorFrame, left);
            setWidth(outputFrame, total - left);
        });

        divider.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragLastX = event.getRawX();
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float delta = event.getRawX() - dragLastX;
                    dragLastX = event.getRawX();
                    int minWidth = dpToPx(100);
                    int newLeft = editorFrame.getWidth() + (int) delta;
                    int newRight = outputFrame.getWidth() - (int) delta;
                    if (newLeft < minWidth || newRight < minWidth) return true;
                    setWidth(editorFrame, newLeft);
                    setWidth(outputFrame, newRight);
                    return true;
                }

                default:
                    return true;
            }
        });
    }

    // layout_weight beats an explicit width, so it has to be cleared here.
    private void setWidth(View v, int px) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = px;
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).weight = 0f;
        }
        v.setLayoutParams(lp);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // IME_FLAG_NO_ENTER_ACTION keeps Enter as a newline while still showing a Done key.
    private void setupKeyboardDismiss(View root) {
        if (editor != null) {
            editor.setRawInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editor.setImeOptions(EditorInfo.IME_ACTION_DONE
                    | EditorInfo.IME_FLAG_NO_ENTER_ACTION
                    | EditorInfo.IME_FLAG_NO_FULLSCREEN);
            editor.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    return true;
                }
                return false;
            });
        }
        View doneButton = root.findViewById(R.id.keyboardDoneButton);
        if (doneButton != null) doneButton.setOnClickListener(v -> hideKeyboard());
    }

    private void hideKeyboard() {
        View target = host != null ? host.getCurrentFocus() : null;
        if (target == null) target = getView();
        if (target == null) return;

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(target);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        } else {
            InputMethodManager imm =
                    (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
        if (target == editor) target.clearFocus();
    }

    private void compile() {
        if (busy || editor == null) return;
        setBusy(true);
        clearLog();
        saveSource();

        final String source = currentSource();
        worker.execute(() -> {
            ArduinoCompiler compiler = new ArduinoCompiler(host);
            BuildResult result = compiler.build(source, board, this::appendLogAsync);
            ui.post(() -> {
                setBusy(false);
                toast(result.ok ? "Compiled: " + result.programBytes + " bytes" : result.error);
            });
        });
    }

    private void compileThenUpload() {
        if (busy || editor == null) return;
        setBusy(true);
        clearLog();
        saveSource();

        final String source = currentSource();
        worker.execute(() -> {
            ArduinoCompiler compiler = new ArduinoCompiler(host);
            BuildResult result = compiler.build(source, board, this::appendLogAsync);

            if (result.ok) {
                doUpload(result.hexFile);
                return;
            }
            if (!compiler.tools().isComplete()) {
                File fallback = compiler.sdk().exampleBlinkHex();
                if (fallback.isFile()) {
                    appendLogAsync("Falling back to the prebuilt blink.hex in assets.");
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
            @Override public void onLog(String message) { appendLogAsync(message); }
            @Override public void onProgress(int percent) {
                ui.post(() -> { if (progressBar != null) progressBar.setProgress(percent); });
            }
        };

        try {
            appendLogAsync("--- upload ---");
            new Uploader(host).upload(hex, board, true, progress);
            ui.post(() -> {
                setBusy(false);
                toast("Uploaded");
            });
        } catch (Exception e) {
            appendLogAsync("Upload failed: " + e.getMessage());
            ui.post(() -> {
                setBusy(false);
                toast("Upload failed");
            });
        }
    }

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
        final ScrollView scroll = outputScroll;
        if (scroll == null) return;
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String message) {
        if (!isAdded()) return;
        Toast.makeText(host, message, Toast.LENGTH_SHORT).show();
    }
}