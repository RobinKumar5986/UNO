package com.kgjr.uno.screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kgjr.uno.R;

import java.util.ArrayList;
import java.util.List;

/** Full-screen AI chat. The WebView is the only focusable view here, so typing works. */
public class AiChatActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "com.kgjr.uno.extra.URL";
    public static final String EXTRA_CODE = "com.kgjr.uno.extra.CODE";

    private WebView webView;
    private String sketch;

    private ValueCallback<Uri[]> pendingFileCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private PermissionRequest pendingWebPermission;
    private ActivityResultLauncher<String[]> devicePermissionLauncher;

    public static Intent intentFor(Context context, String url, @Nullable String code) {
        Intent intent = new Intent(context, AiChatActivity.class);
        intent.putExtra(EXTRA_URL, url);
        if (code != null) intent.putExtra(EXTRA_CODE, code);
        return intent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        sketch = getIntent().getStringExtra(EXTRA_CODE);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) url = "https://chatgpt.com";

        webView = findViewById(R.id.chatWebView);

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingFileCallback == null) return;
                    Uri[] uris = null;
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            uris = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                uris[i] = data.getClipData().getItemAt(i).getUri();
                            }
                        } else if (data.getData() != null) {
                            uris = new Uri[]{data.getData()};
                        }
                    }
                    pendingFileCallback.onReceiveValue(uris);
                    pendingFileCallback = null;
                });

        devicePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                grants -> {
                    PermissionRequest request = pendingWebPermission;
                    pendingWebPermission = null;
                    if (request == null) return;
                    if (!grants.containsValue(Boolean.FALSE)) {
                        request.grant(request.getResources());
                    } else {
                        request.deny();
                    }
                });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setBackgroundColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            s.setForceDark(WebSettings.FORCE_DARK_ON);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingWebPermission == request) pendingWebPermission = null;
            }

            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
                pendingFileCallback = callback;
                try {
                    fileChooserLauncher.launch(params.createIntent());
                    return true;
                } catch (Exception e) {
                    pendingFileCallback = null;
                    return false;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        setupTopBar();
        webView.loadUrl(url);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupTopBar() {
        int[] ids = {R.id.chatSiteButton1, R.id.chatSiteButton2, R.id.chatSiteButton3};
        String[] urls = {"https://claude.ai", "https://chatgpt.com", "https://gemini.google.com"};
        for (int i = 0; i < ids.length; i++) {
            ImageView button = findViewById(ids[i]);
            final String target = urls[i];
            if (button != null) button.setOnClickListener(v -> webView.loadUrl(target));
        }

        ImageButton insert = findViewById(R.id.chatInsertCodeButton);
        if (insert != null) {
            insert.setVisibility(sketch == null ? ImageButton.GONE : ImageButton.VISIBLE);
            insert.setOnClickListener(v -> insertSketchIntoComposer());
        }

        ImageButton close = findViewById(R.id.chatCloseButton);
        if (close != null) close.setOnClickListener(v -> finish());
    }

    /** Types the sketch into whatever composer the page is using. */
    private void insertSketchIntoComposer() {
        if (sketch == null || webView == null) return;
        String payload = org.json.JSONObject.quote(sketch);
        String js = "(function(){"
                + "var box = document.querySelector('div[contenteditable=\"true\"]')"
                + " || document.querySelector('textarea');"
                + "if(!box){return 'no-composer';}"
                + "box.focus();"
                + "if(box.tagName === 'TEXTAREA'){"
                + "  var setter = Object.getOwnPropertyDescriptor("
                + "     window.HTMLTextAreaElement.prototype, 'value').set;"
                + "  setter.call(box, box.value + " + payload + ");"
                + "} else {"
                + "  document.execCommand('insertText', false, " + payload + ");"
                + "}"
                + "box.dispatchEvent(new Event('input', {bubbles:true}));"
                + "return 'ok';"
                + "})();";
        webView.evaluateJavascript(js, value -> {
            if (value != null && value.contains("no-composer")) {
                toast("Open a chat first, then tap insert");
            }
        });
    }

    private void handleWebPermission(@NonNull PermissionRequest request) {
        List<String> needed = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                needed.add(Manifest.permission.RECORD_AUDIO);
            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                needed.add(Manifest.permission.CAMERA);
            }
        }
        if (needed.isEmpty()) {
            request.deny();
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String permission : needed) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            request.grant(request.getResources());
            return;
        }
        if (pendingWebPermission != null) pendingWebPermission.deny();
        pendingWebPermission = request;
        devicePermissionLauncher.launch(missing.toArray(new String[0]));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (pendingWebPermission != null) {
            pendingWebPermission.deny();
            pendingWebPermission = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(new WebViewClient());
            android.view.ViewGroup parent = (android.view.ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}