package com.example.monmeteo;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Spinner spinnerVille;
    private UserDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new UserDbHelper(this);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        spinnerVille = findViewById(R.id.spinnerVille);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (name.length() < 2) {
            etName.setError("Saisis un nom valide.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Adresse e-mail invalide.");
            return;
        }
        if (password.length() < 8) {
            etPassword.setError("Utilise au moins 8 caractères.");
            return;
        }

        String ville = spinnerVille.getSelectedItem().toString();
        if (db.register(name, email, password, ville)) {
            Toast.makeText(this, "Compte créé. Tu peux te connecter.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Inscription impossible : e-mail déjà utilisé ?",
                    Toast.LENGTH_LONG).show();
        }
    }
}
