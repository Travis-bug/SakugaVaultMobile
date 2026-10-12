package ca.gbc.comp3074.sakugavault.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ca.gbc.comp3074.sakugavault.data.Anime;

/** Small shared helpers for the prototype screens. */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Placeholder artwork: a two-colour gradient per title (real cover art is loaded with Glide later). */
    public static GradientDrawable art(Context context, Anime anime, float cornerDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{anime.colorStart, anime.colorEnd});
        drawable.setCornerRadius(dp(context, cornerDp));
        return drawable;
    }

    /**
     * Android 15+ draws apps edge to edge, so pad the root view by the status bar,
     * navigation bar and keyboard sizes.
     */
    public static void padForSystemBars(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
