package ca.gbc.comp3074.sakugavault.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers the "Demo videos" switch on the title page.
 * On (the default): the player shows a demo video straight away and never asks the scrapers.
 * Off: the player asks the back end for the live stream and only falls back to a demo video
 * when no stream is found.
 */
public final class PlaybackPrefs {

    private static final String PREFS = "playback";
    private static final String KEY_DEMO_VIDEOS = "use_demo_videos";

    private PlaybackPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean useDemoVideos(Context context) {
        return prefs(context).getBoolean(KEY_DEMO_VIDEOS, true);
    }

    public static void setUseDemoVideos(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_DEMO_VIDEOS, on).apply();
    }
}
