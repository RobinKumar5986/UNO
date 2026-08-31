package com.kgjr.uno.screens.fragments.codeHelper.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kgjr.uno.R;

/**
 * Shared shell for every node dialog: a large centred card, a header with title and subtitle,
 * a close chip pinned to the card's top-right corner, and a scrolling content area. Callers
 * either hand over a content layout or add views to {@link #contentOf}.
 *
 * <p>There is no Done button — the close chip and a tap outside both dismiss.
 */
public class NodeDialogFrame {


    private static final int BLUR_RADIUS_PX = 56;
    private static final float DIM_AMOUNT = 0.55f;

    /** The card fills most of the screen; these are fractions of the display, not fixed dp. */
    private static final float WIDTH_FRACTION = 0.86f;
    private static final float HEIGHT_FRACTION = 0.80f;
    /** Lifts the card off the bottom edge — the gap below ends up larger than the one above. */
    private static final float BOTTOM_LIFT_DP = 28f;
    /** Room inside the window for the card's shadow and the close chip's overhang. */
    private static final float WINDOW_INSET_DP = 14f;

    public static Dialog create(Context context, String title, String subtitle, Runnable onClose) {
        Dialog dialog = new Dialog(context, R.style.NodeDialogTheme);
        dialog.setContentView(R.layout.dialog_node_frame);
        dialog.setCanceledOnTouchOutside(true);

        ((TextView) dialog.findViewById(R.id.node_dialog_title)).setText(title);

        TextView subtitleView = dialog.findViewById(R.id.node_dialog_subtitle);
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.node_dialog_close).setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            if (onClose != null) onClose.run();
        });

        applyWindow(dialog, context);
        return dialog;
    }

    public static Dialog create(Context context, String title, String subtitle,
                                int contentLayout, Runnable onClose) {
        Dialog dialog = create(context, title, subtitle, onClose);
        LinearLayout content = contentOf(dialog);
        // The dialog's own inflater carries NodeDialogTheme; the host context may not be a
        // MaterialComponents theme, which the Material views reject outright.
        dialog.getLayoutInflater().inflate(contentLayout, content, true);
        coerceScrollableHeights(content);
        return dialog;
    }

    public static LinearLayout contentOf(Dialog dialog) {
        return dialog.findViewById(R.id.node_dialog_content);
    }

    /**
     * A content layout whose root declares match_parent height measures to exactly the
     * ScrollView's viewport, so the ScrollView has nothing to scroll and the overflow is
     * silently clipped. Content layouts are written by other screens, so we defend here rather
     * than relying on every one of them getting it right.
     */
    private static void coerceScrollableHeights(LinearLayout content) {
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params != null && params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                child.setLayoutParams(params);
            }
        }
    }

    private static void applyWindow(Dialog dialog, Context context) {
        Window window = dialog.getWindow();
        if (window == null) return;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int inset = (int) (WINDOW_INSET_DP * 2f * metrics.density);

        window.setLayout(
                (int) (metrics.widthPixels * WIDTH_FRACTION) + inset,
                (int) (metrics.heightPixels * HEIGHT_FRACTION) + inset);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = DIM_AMOUNT;
        attributes.y = -(int) (BOTTOM_LIFT_DP * metrics.density);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            attributes.setBlurBehindRadius(BLUR_RADIUS_PX);
        }
        window.setAttributes(attributes);
    }

    private NodeDialogFrame() {
    }
}