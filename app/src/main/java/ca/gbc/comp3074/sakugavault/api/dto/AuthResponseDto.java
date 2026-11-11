package ca.gbc.comp3074.sakugavault.api.dto;

/** Answer from POST /api/auth/login, /register and /refresh (Contracts/Auth). */
public class AuthResponseDto {
    public String accessToken;
    public String accessTokenExpiresAtUtc;
    public CurrentUserDto user;
}
