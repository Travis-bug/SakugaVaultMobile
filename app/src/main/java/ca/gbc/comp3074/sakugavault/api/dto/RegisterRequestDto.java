package ca.gbc.comp3074.sakugavault.api.dto;

/** Body of POST /api/auth/register. */
public class RegisterRequestDto {
    public String displayName;
    public String userName;
    public String email;
    public String password;

    public RegisterRequestDto() {
    }

    public RegisterRequestDto(String displayName, String userName, String email, String password) {
        this.displayName = displayName;
        this.userName = userName;
        this.email = email;
        this.password = password;
    }
}
