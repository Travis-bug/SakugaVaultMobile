package ca.gbc.comp3074.sakugavault.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.time.Duration;
import java.time.OffsetDateTime;

import ca.gbc.comp3074.sakugavault.LoginActivity;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.services.AppServices;

/** Small helpers shared by the screens. */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Placeholder artwork: a two-colour gradient per title, shown until the real image loads. */
    public static GradientDrawable art(Context context, Anime anime, float cornerDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{anime.colorStart, anime.colorEnd});
        drawable.setCornerRadius(dp(context, cornerDp));
        return drawable;
    }

    /** Sets the gradient as the background and clips the image inside to its rounded corners. */
    public static void setArt(View container, Anime anime, float cornerDp) {
        container.setBackground(art(container.getContext(), anime, cornerDp));
        container.setClipToOutline(true);
    }

    /** Loads a poster or backdrop with Glide. With no URL the view stays empty and the gradient shows. */
    public static void loadImage(ImageView view, String url) {
        String absolute = ApiClient.absoluteUrl(url);
        if (absolute == null) {
            Glide.with(view).clear(view);
            view.setImageDrawable(null);
            return;
        }
        Glide.with(view)
                .load(absolute)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view);
    }

    /**
     * If the server says the session is over (401 even after a token refresh),
     * forget it and go back to sign in. Returns true when it handled the error.
     */
    public static boolean handleSessionExpired(Activity activity, ApiError error) {
        if (activity == null || error == null || !error.isUnauthorized()) {
            return false;
        }
        AppServices.auth(activity).clearLocalSession();
        Toast.makeText(activity, "Your session ended. Please sign in again.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        return true;
    }

    /** "now", "5m", "2h", "3d" from an API date such as 2026-11-08T17:20:00+00:00. */
    public static String timeAgo(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime when = OffsetDateTime.parse(isoDate);
            long minutes = Math.max(0, Duration.between(when, OffsetDateTime.now()).toMinutes());
            if (minutes < 1) {
                return "now";
            }
            if (minutes < 60) {
                return minutes + "m";
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + "h";
            }
            long days = hours / 24;
            return days < 30 ? days + "d" : when.toLocalDate().toString();
        } catch (RuntimeException e) {
            return "";
        }
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
