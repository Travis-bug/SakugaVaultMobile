package ca.gbc.comp3074.sakugavault.services.auth;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiClient;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.SakugaVaultApi;
import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CurrentUserDto;
import ca.gbc.comp3074.sakugavault.api.dto.LoginRequestDto;
import ca.gbc.comp3074.sakugavault.api.dto.RegisterRequestDto;
import ca.gbc.comp3074.sakugavault.data.SessionStore;

public final class AuthService implements IAuthService {

    private final Context context;
    private final SakugaVaultApi api;

    public AuthService(Context context) {
        this.context = context;
        this.api = ApiClient.api(context);
    }

    @Override
    public void login(String identifier, String password, ApiCallback<CurrentUserDto> callback) {
        ApiClient.enqueue(api.login(new LoginRequestDto(identifier, password)), saveSession(callback));
    }

    @Override
    public void register(String displayName, String userName, String email, String password,
                         ApiCallback<CurrentUserDto> callback) {
        RegisterRequestDto request = new RegisterRequestDto(displayName, userName, email, password);
        ApiClient.enqueue(api.register(request), saveSession(callback));
    }

    /** Saves the access token and user from the response, then reports the user to the screen. */
    private ApiCallback<AuthResponseDto> saveSession(ApiCallback<CurrentUserDto> callback) {
        return new ApiCallback<AuthResponseDto>() {
            @Override
            public void onSuccess(AuthResponseDto session) {
                if (session == null || session.accessToken == null || session.user == null) {
                    callback.onError(new ApiError(500, "The server sent an empty sign-in response."));
                    return;
                }
                SessionStore.saveSession(context, session);
                callback.onSuccess(session.user);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        };
    }

    @Override
    public void logout(Runnable done) {
        ApiClient.enqueue(api.logout(), new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                finish();
            }

            @Override
            public void onError(ApiError error) {
                finish();
            }

            private void finish() {
                clearLocalSession();
                done.run();
            }
        });
    }

    @Override
    public boolean isSignedIn() {
        return SessionStore.isSignedIn(context);
    }

    @Override
    public void clearLocalSession() {
        SessionStore.clear(context);
        ApiClient.clearCookies(context);
    }
}
