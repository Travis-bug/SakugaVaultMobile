package ca.gbc.comp3074.sakugavault.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Keeps cookies between app launches, the way a browser does for the website.
 * The back end uses two: "SakugaVault.Refresh" (7-day sign-in refresh) and the
 * 30-minute playback cookie that the stream proxy checks.
 */
final class PersistentCookieJar implements CookieJar {

    private static final String PREFS = "cookies";

    private final SharedPreferences prefs;
    private final Map<String, Cookie> cookies = new LinkedHashMap<>();

    PersistentCookieJar(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> saved : prefs.getAll().entrySet()) {
            if (!(saved.getValue() instanceof String)) {
                continue;
            }
            // Saved as "<url>\n<Set-Cookie value>" so the cookie can be parsed again for the same host.
            String[] parts = ((String) saved.getValue()).split("\n", 2);
            HttpUrl url = parts.length == 2 ? HttpUrl.parse(parts[0]) : null;
            Cookie cookie = url == null ? null : Cookie.parse(url, parts[1]);
            if (cookie != null && cookie.expiresAt() > now) {
                cookies.put(saved.getKey(), cookie);
            }
        }
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> received) {
        SharedPreferences.Editor editor = prefs.edit();
        long now = System.currentTimeMillis();
        for (Cookie cookie : received) {
            String key = cookie.name() + "|" + cookie.domain() + "|" + cookie.path();
            if (cookie.expiresAt() <= now) {
                // The server deleted it (for example on sign-out).
                cookies.remove(key);
                editor.remove(key);
            } else {
                cookies.put(key, cookie);
                if (cookie.persistent()) {
                    editor.putString(key, url + "\n" + cookie);
                }
            }
        }
        editor.apply();
    }

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> matching = new ArrayList<>();
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Cookie>> iterator = cookies.entrySet().iterator();
        while (iterator.hasNext()) {
            Cookie cookie = iterator.next().getValue();
            if (cookie.expiresAt() <= now) {
                iterator.remove();
            } else if (cookie.matches(url)) {
                matching.add(cookie);
            }
        }
        return matching;
    }

    synchronized void clear() {
        cookies.clear();
        prefs.edit().clear().apply();
    }
}
