package com.example.monmeteo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Contrôle du rôle : l'écran refuse de s'afficher si on l'appelle sans être ADMIN.
        String role = getIntent().getStringExtra("user_role");
        if (!Role.ADMIN.name().equals(role)) {
            Toast.makeText(this, "Accès réservé à l'administrateur.", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        final long userId = getIntent().getLongExtra("user_id", 0);
        String name = getIntent().getStringExtra("user_name");
        TextView tvAdmin = findViewById(R.id.tvAdminTitle);
        tvAdmin.setText("Administration · " + (name == null ? "admin" : name));

        UserDbHelper db = new UserDbHelper(this);
        final List<UserProfile> users = db.getAllUsers();

        ListView listUsers = findViewById(R.id.listUsers);
        listUsers.setAdapter(new UserAdapter(this, users));
        listUsers.setOnItemClickListener((parent, view, position, id) -> {
            UserProfile selected = (UserProfile) parent.getItemAtPosition(position);
            Toast.makeText(this, "Sélection : " + selected.getFullName()
                    + " (" + selected.getEmail() + ")", Toast.LENGTH_SHORT).show();
        });

        TextView tvCount = findViewById(R.id.tvCount);
        tvCount.setText(users.size() + " compte(s) inscrit(s) sur l'appareil");

        Button btnMeteo = findViewById(R.id.btnAdminMeteo);
        btnMeteo.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserDashboardActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_name", name);
            intent.putExtra("user_role", Role.ADMIN.name());
            startActivity(intent);
        });

        Button btnLogout = findViewById(R.id.btnAdminLogout);
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
