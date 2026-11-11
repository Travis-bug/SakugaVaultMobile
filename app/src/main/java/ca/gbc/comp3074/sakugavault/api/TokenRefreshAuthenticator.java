package ca.gbc.comp3074.sakugavault.api;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;

import ca.gbc.comp3074.sakugavault.BuildConfig;
import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * The access token only lasts 60 minutes. When the API answers 401, this asks
 * POST /api/auth/refresh for a new one (the refresh cookie is sent automatically)
 * and retries the original request once. If the refresh fails too, the 401 reaches
 * the screen, which sends the user back to sign in.
 */
final class TokenRefreshAuthenticator implements Authenticator {

    private static final Object LOCK = new Object();

    private final Context context;
    private final OkHttpClient refreshClient;
    private final Gson gson = new Gson();

    TokenRefreshAuthenticator(Context context, OkHttpClient refreshClient) {
        this.context = context;
        this.refreshClient = refreshClient;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        Request failed = response.request();
        // A 401 from login, register or refresh means wrong details, not an old token.
        if (failed.url().encodedPath().contains("/api/auth/")) {
            return null;
        }
        if (attempts(response) >= 2) {
            return null;
        }

        synchronized (LOCK) {
            // Another request may already have refreshed the token while this one waited.
            String current = SessionStore.accessToken(context);
            String sent = failed.header("Authorization");
            if (current != null && sent != null && !sent.equals("Bearer " + current)) {
                return failed.newBuilder().header("Authorization", "Bearer " + current).build();
            }

            Request refresh = new Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "api/auth/refresh")
                    .post(RequestBody.create(new byte[0], null))
                    .build();
            try (Response refreshed = refreshClient.newCall(refresh).execute()) {
                if (!refreshed.isSuccessful() || refreshed.body() == null) {
                    return null;
                }
                AuthResponseDto session = gson.fromJson(refreshed.body().charStream(), AuthResponseDto.class);
                if (session == null || session.accessToken == null) {
                    return null;
                }
                SessionStore.saveSession(context, session);
                return failed.newBuilder().header("Authorization", "Bearer " + session.accessToken).build();
            }
        }
    }

    private static int attempts(Response response) {
        int count = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            count++;
            prior = prior.priorResponse();
        }
        return count;
    }
}
