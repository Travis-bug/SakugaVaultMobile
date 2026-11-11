package ca.gbc.comp3074.sakugavault.api;

/**
 * How a service hands back the result of a call to the SakugaVault API.
 * Both methods run on the main (UI) thread, so screens can update views directly.
 */
public interface ApiCallback<T> {
    void onSuccess(T result);

    void onError(ApiError error);
}
