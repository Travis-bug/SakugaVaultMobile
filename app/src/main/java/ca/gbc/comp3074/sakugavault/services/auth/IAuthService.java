package ca.gbc.comp3074.sakugavault.services.auth;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.dto.CurrentUserDto;

/** Signing in and out. Mirrors the back end's Services/Auth/IAuthService. */
public interface IAuthService {

    /** POST /api/auth/login with a username or email. On success the session is saved on the phone. */
    void login(String identifier, String password, ApiCallback<CurrentUserDto> callback);

    /** POST /api/auth/register. Creates the account and signs in. */
    void register(String displayName, String userName, String email, String password,
                  ApiCallback<CurrentUserDto> callback);

    /** POST /api/auth/logout, then forgets the session on this phone (even if the server can't be reached). */
    void logout(Runnable done);

    boolean isSignedIn();

    /** Forgets the session on this phone only, for when the server says the session is over. */
    void clearLocalSession();
}
