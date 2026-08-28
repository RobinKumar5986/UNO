package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kgjr.uno.R;

/**
 * Shared shell for every node dialog: one card at one fixed size, a header with title and
 * subtitle, a Done button, and a blurred backdrop. Callers either hand over a content layout or
 * add views to {@link #contentOf}.
 */
public class NodeDialogFrame {

    /** Card size; the window is padded on top of this to leave room for the card shadow. */
    private static final float WIDTH_DP = 336f;
    private static final float HEIGHT_DP = 468f;
    private static final float SHADOW_INSET_DP = 8f;
    private static final int BLUR_RADIUS_PX = 56;
    private static final float DIM_AMOUNT = 0.55f;

    public static Dialog create(Context context, String title, String subtitle, Runnable onClose) {
        Dialog dialog = new Dialog(context, R.style.NodeDialogTheme);
        dialog.setContentView(R.layout.dialog_node_frame);

        ((TextView) dialog.findViewById(R.id.node_dialog_title)).setText(title);

        TextView subtitleView = dialog.findViewById(R.id.node_dialog_subtitle);
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }

        View.OnClickListener dismiss = v -> dialog.dismiss();
        ((ImageView) dialog.findViewById(R.id.node_dialog_close)).setOnClickListener(dismiss);
        dialog.findViewById(R.id.node_dialog_done).setOnClickListener(dismiss);
        dialog.setOnDismissListener(d -> {
            if (onClose != null) onClose.run();
        });

        applyWindow(dialog, context);
        return dialog;
    }

    public static Dialog create(Context context, String title, String subtitle,
                                int contentLayout, Runnable onClose) {
        Dialog dialog = create(context, title, subtitle, onClose);
        // The dialog's own inflater carries NodeDialogTheme; the host context may not be a
        // MaterialComponents theme, which the Material views reject outright.
        dialog.getLayoutInflater().inflate(contentLayout, contentOf(dialog), true);
        return dialog;
    }

    public static LinearLayout contentOf(Dialog dialog) {
        return dialog.findViewById(R.id.node_dialog_content);
    }

    private static void applyWindow(Dialog dialog, Context context) {
        Window window = dialog.getWindow();
        if (window == null) return;

        float density = context.getResources().getDisplayMetrics().density;
        float inset = SHADOW_INSET_DP * 2f;
        window.setLayout((int) ((WIDTH_DP + inset) * density),
                (int) ((HEIGHT_DP + inset) * density));
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = DIM_AMOUNT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            attributes.setBlurBehindRadius(BLUR_RADIUS_PX);
        }
        window.setAttributes(attributes);
    }

    private NodeDialogFrame() {
    }
}
