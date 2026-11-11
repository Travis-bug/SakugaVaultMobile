package ca.gbc.comp3074.sakugavault.data;

import android.content.Context;
import android.content.SharedPreferences;

import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;

/**
 * Keeps the signed-in user and their access token between launches.
 * The refresh token is not stored here: the server sends it as an HttpOnly cookie,
 * which the app's cookie jar keeps (see api/PersistentCookieJar).
 */
public final class SessionStore {

    private static final String PREFS = "session";
    private static final String KEY_TOKEN = "accessToken";
    private static final String KEY_TOKEN_EXPIRES = "accessTokenExpiresAtUtc";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_EMAIL = "email";

    private SessionStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isSignedIn(Context context) {
        SharedPreferences prefs = prefs(context);
        return prefs.contains(KEY_USER_ID) && prefs.contains(KEY_TOKEN);
    }

    /** Saves what POST /api/auth/login, /register or /refresh returned. */
    public static void saveSession(Context context, AuthResponseDto session) {
        SharedPreferences.Editor editor = prefs(context).edit()
                .putString(KEY_TOKEN, session.accessToken)
                .putString(KEY_TOKEN_EXPIRES, session.accessTokenExpiresAtUtc);
        if (session.user != null) {
            editor.putString(KEY_USER_ID, session.user.id)
                    .putString(KEY_DISPLAY_NAME, session.user.displayName)
                    .putString(KEY_USER_NAME, session.user.userName)
                    .putString(KEY_EMAIL, session.user.email);
        }
        editor.apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static String accessToken(Context context) {
        return prefs(context).getString(KEY_TOKEN, null);
    }

    /** The signed-in user's id. The watchlist rows are saved under this id. */
    public static String userId(Context context) {
        return prefs(context).getString(KEY_USER_ID, "");
    }

    public static String displayName(Context context) {
        return prefs(context).getString(KEY_DISPLAY_NAME, "Guest");
    }

    public static String userName(Context context) {
        return prefs(context).getString(KEY_USER_NAME, "guest");
    }

    public static String email(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }
}
