package ca.gbc.comp3074.sakugavault.api;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import ca.gbc.comp3074.sakugavault.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the one Retrofit client the whole app shares.
 * <ul>
 *   <li>The server address comes from BuildConfig.API_BASE_URL (set in app/build.gradle.kts).</li>
 *   <li>PersistentCookieJar keeps the refresh cookie and the playback cookie, like a browser would.</li>
 *   <li>AuthInterceptor adds "Authorization: Bearer &lt;access token&gt;" to every call.</li>
 *   <li>TokenRefreshAuthenticator gets a new access token when the server answers 401.</li>
 * </ul>
 */
public final class ApiClient {

    private static SakugaVaultApi api;
    private static PersistentCookieJar cookieJar;

    private ApiClient() {
    }

    public static synchronized SakugaVaultApi api(Context context) {
        if (api == null) {
            Context app = context.getApplicationContext();
            cookieJar = new PersistentCookieJar(app);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);

            // Only used for POST /api/auth/refresh, so a failed refresh can never start another refresh.
            OkHttpClient refreshClient = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            OkHttpClient client = refreshClient.newBuilder()
                    .readTimeout(90, TimeUnit.SECONDS) // finding a stream through the scrapers can be slow
                    .addInterceptor(new AuthInterceptor(app))
                    .addInterceptor(logging)
                    .authenticator(new TokenRefreshAuthenticator(app, refreshClient))
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(SakugaVaultApi.class);
        }
        return api;
    }

    /** Forgets the refresh and playback cookies (used when signing out). */
    public static synchronized void clearCookies(Context context) {
        api(context);
        cookieJar.clear();
    }

    /** Some image URLs can come back relative ("/images/x.jpg"); this makes them absolute. */
    public static String absoluteUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = BuildConfig.API_BASE_URL;
        return url.startsWith("/") ? base + url.substring(1) : base + url;
    }

    /** Runs a call in the background and reports the result back on the main thread. */
    public static <T> void enqueue(Call<T> call, ApiCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(ApiError.fromResponse(response));
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable throwable) {
                if (!c.isCanceled()) {
                    callback.onError(ApiError.fromFailure(throwable));
                }
            }
        });
    }
}
