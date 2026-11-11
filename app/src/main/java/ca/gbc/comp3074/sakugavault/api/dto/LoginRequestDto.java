package ca.gbc.comp3074.sakugavault.api.dto;

/** Body of POST /api/auth/login. Identifier is a username or an email. */
public class LoginRequestDto {
    public String identifier;
    public String password;

    public LoginRequestDto() {
    }

    public LoginRequestDto(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
}
