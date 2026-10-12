package ca.gbc.comp3074.sakugavault;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.ui.Ui;

/**
 * Login / Register screen (FR-01).
 * Prototype: checks the fields locally and fakes the network call.
 * Next milestone: POST /api/auth/login and /api/auth/register with Retrofit.
 */
public class LoginActivity extends AppCompatActivity {

    private boolean registerMode = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

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

        // A saved session skips the login screen (the real app refreshes its token here).
        if (SessionStore.isSignedIn(this)) {
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

        // Same rules as the back end's LoginRequestDto / RegisterRequestDto.
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
            tvError.setText(registerMode
                    ? "Couldn't create the account. Fix the highlighted fields."
                    : "Couldn't sign in. Check your username and password.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        setLoading(true);
        handler.postDelayed(() -> {
            String userName = identifier.contains("@") ? identifier.substring(0, identifier.indexOf('@')) : identifier;
            String name = registerMode ? displayName : capitalize(userName);
            String mail = registerMode ? email : (identifier.contains("@") ? identifier : userName + "@example.com");
            SessionStore.save(this, name, userName, mail);
            openHome();
        }, 900);
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
