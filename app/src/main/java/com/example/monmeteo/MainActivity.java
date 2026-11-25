package com.example.monmeteo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_DARK = "is_dark_mode";

    private EditText etEmail, etPassword;
    private Button btnToggleTheme;
    private UserDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // La préférence est lue AVANT setContentView : sinon l'écran s'affiche en clair puis clignote.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_IS_DARK, isSystemInDarkMode());
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new UserDbHelper(this);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoRegister = findViewById(R.id.btnGoRegister);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        updateToggleButtonText(isDark);

        btnToggleTheme.setOnClickListener(v -> {
            boolean currentDark = prefs.getBoolean(KEY_IS_DARK, isSystemInDarkMode());
            boolean newDark = !currentDark;
            prefs.edit().putBoolean(KEY_IS_DARK, newDark).apply();
            if (newDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnLogin.setOnClickListener(v -> login());
        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private boolean isSystemInDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateToggleButtonText(boolean isDark) {
        if (btnToggleTheme != null) {
            btnToggleTheme.setText(isDark ? "Mode Clair" : "Mode Sombre");
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.isEmpty()) {
            Toast.makeText(this, "Vérifie l'e-mail et le mot de passe.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfile user = db.login(email, password);
        if (user == null) {
            Toast.makeText(this, "Identifiants incorrects.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        if (user.getRole() == Role.ADMIN) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, UserDashboardActivity.class);
        }
        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_name", user.getFullName());
        intent.putExtra("user_role", user.getRole().name());
        startActivity(intent);
        finish();
    }
}
