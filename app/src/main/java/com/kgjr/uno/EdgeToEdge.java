package com.kgjr.uno;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Edge-to-edge only: the layout extends behind the status and navigation bars, but the bars stay
 * visible and interactive. Nothing is hidden and there is no immersive/swipe behaviour.
 *
 * <p>Call {@link #enable} before setContentView, then {@link #padForSystemBars} on the content
 * root so the UI isn't sitting under the clock or the gesture pill.
 */
public final class EdgeToEdge {

    /**
     * @param lightIcons true for dark backgrounds (draws white status/nav icons), false for
     *                   light backgrounds (draws dark icons).
     */
    public static void enable(Activity activity, boolean lightIcons) {
        Window window = activity.getWindow();

        // The one line that actually turns edge-to-edge on: stop the decor view from insetting
        // the layout for the system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Android 10+ paints a translucent scrim behind transparent bars unless told not to.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!lightIcons);
        controller.setAppearanceLightNavigationBars(!lightIcons);
    }

    /** Pads the view by the system bar and cutout insets so no content hides underneath. */
    public static void padForSystemBars(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    private EdgeToEdge() {
    }
}