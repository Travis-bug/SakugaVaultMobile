package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.LocalNetworkAccess;
import ca.gbc.comp3074.sakugavault.api.dto.CurrentUserDto;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.auth.IAuthService;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Sign In / Create Account (FR-01).
 * Calls POST /api/auth/login or /api/auth/register through IAuthService.
 * Uses the same accounts as the SakugaVault website.
 */
public class LoginActivity extends AppCompatActivity {

    private boolean registerMode = false;
    private IAuthService auth;

    /** Android 17: asks for local network access so the app can reach the back end on this computer. */
    private final ActivityResultLauncher<String> localNetworkPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onLocalNetworkResult);

    private TextInputLayout tilDisplayName;
    private TextInputLayout tilIdentifier;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etDisplayName;
    private TextInputEditText etIdentifier;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextView tvError;
    private TextView tvTagline;
    private MaterialButton btnSubmit;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = AppServices.auth(this);

        boolean needsLocalNetwork = LocalNetworkAccess.isNeeded(this) && !LocalNetworkAccess.isGranted(this);

        // Already signed in on this phone: go straight to Home (an old token is refreshed automatically).
        if (auth.isSignedIn() && !needsLocalNetwork) {
            openHome();
            return;
        }

        setContentView(R.layout.activity_login);
        Ui.padForSystemBars(findViewById(R.id.root));

        tilDisplayName = findViewById(R.id.tilDisplayName);
        tilIdentifier = findViewById(R.id.tilIdentifier);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        etIdentifier = findViewById(R.id.etIdentifier);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        tvTagline = findViewById(R.id.tvTagline);
        btnSubmit = findViewById(R.id.btnSubmit);
        progress = findViewById(R.id.progress);

        MaterialButtonToggleGroup modeToggle = findViewById(R.id.modeToggle);
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                setRegisterMode(checkedId == R.id.btnModeRegister);
            }
        });

        btnSubmit.setOnClickListener(v -> submit());

        if (needsLocalNetwork) {
            localNetworkPermission.launch(LocalNetworkAccess.PERMISSION);
        }
    }

    private void onLocalNetworkResult(boolean granted) {
        if (granted) {
            if (auth.isSignedIn()) {
                openHome();
            }
        } else {
            showError("SakugaVault needs local network access to reach the SakugaVault server on your computer. "
                    + "Allow it in Settings > Apps > SakugaVault > Permissions, then try again.");
        }
    }

    private void setRegisterMode(boolean register) {
        registerMode = register;
        tilDisplayName.setVisibility(register ? View.VISIBLE : View.GONE);
        tilEmail.setVisibility(register ? View.VISIBLE : View.GONE);
        tilIdentifier.setHint(register ? "Username" : "Username or email");
        btnSubmit.setText(register ? "Create Account" : "Sign In");
        tvTagline.setText(register ? "Create an account to save your progress" : "Sign in to pick up where you left off");
        clearErrors();
    }

    private void clearErrors() {
        tilDisplayName.setError(null);
        tilIdentifier.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void submit() {
        clearErrors();
        String identifier = text(etIdentifier);
        String password = text(etPassword);
        String displayName = text(etDisplayName);
        String email = text(etEmail);
        boolean ok = true;

        // Same rules as the back end's LoginRequestDto / RegisterRequestDto, checked here first.
        if (registerMode && displayName.length() < 2) {
            tilDisplayName.setError("Enter at least 2 characters");
            ok = false;
        }
        if (registerMode && identifier.length() < 3) {
            tilIdentifier.setError("Username must be at least 3 characters");
            ok = false;
        } else if (identifier.isEmpty()) {
            tilIdentifier.setError("Enter your username or email");
            ok = false;
        }
        if (registerMode && !email.contains("@")) {
            tilEmail.setError("Enter a valid email address");
            ok = false;
        }
        if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            ok = false;
        }
        if (!ok) {
            showError(registerMode
                    ? "Couldn't create the account. Fix the highlighted fields."
                    : "Couldn't sign in. Check your username and password.");
            return;
        }

        // Without local network access (Android 17), the request would just time out. Ask first.
        if (LocalNetworkAccess.isNeeded(this) && !LocalNetworkAccess.isGranted(this)) {
            localNetworkPermission.launch(LocalNetworkAccess.PERMISSION);
            return;
        }

        setLoading(true);
        ApiCallback<CurrentUserDto> callback = new ApiCallback<CurrentUserDto>() {
            @Override
            public void onSuccess(CurrentUserDto user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                openHome();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                // The server's own message, e.g. "Invalid username/email or password."
                showError(error.message);
            }
        };
        if (registerMode) {
            auth.register(displayName, identifier, email, password, callback);
        } else {
            auth.login(identifier, password, callback);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
