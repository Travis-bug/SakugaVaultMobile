package ca.gbc.comp3074.sakugavault.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Keeps the signed-in user between launches.
 * Prototype only: the real app stores the refresh-token cookie here, encrypted with an Android Keystore key.
 */
public final class SessionStore {
    private static final String PREFS = "session";

    private SessionStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isSignedIn(Context context) {
        return prefs(context).contains("userName");
    }

    public static void save(Context context, String displayName, String userName, String email) {
        prefs(context).edit()
                .putString("displayName", displayName)
                .putString("userName", userName)
                .putString("email", email)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static String displayName(Context context) {
        return prefs(context).getString("displayName", "Guest");
    }

    public static String userName(Context context) {
        return prefs(context).getString("userName", "guest");
    }

    public static String email(Context context) {
        return prefs(context).getString("email", "");
    }
}
