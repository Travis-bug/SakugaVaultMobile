package ca.gbc.comp3074.sakugavault.api;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import ca.gbc.comp3074.sakugavault.data.SessionStore;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds "Authorization: Bearer <access token>" to each request while the user is signed in. */
final class AuthInterceptor implements Interceptor {

    private final Context context;

    AuthInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String token = SessionStore.accessToken(context);
        if (token != null && request.header("Authorization") == null) {
            request = request.newBuilder().header("Authorization", "Bearer " + token).build();
        }
        return chain.proceed(request);
    }
}
