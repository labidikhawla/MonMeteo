package com.example.monmeteo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    private EditText edtVille;
    private TextView txtStatut, txtVille, txtTemperature, txtCondition, txtRessenti, txtVent, txtHumidite;
    private View carteMeteo;
    private Button btnRecherche;
    private ListView listePrevisions;
    private String urlApi;
    private long userId;
    private UserDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        String name = getIntent().getStringExtra("user_name");
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Bienvenue, " + (name == null ? "étudiant" : name));

        db = new UserDbHelper(this);
        userId = getIntent().getLongExtra("user_id", 0);

        edtVille = findViewById(R.id.edtVille);
        btnRecherche = findViewById(R.id.btnRecherche);
        btnRecherche.setOnClickListener(v -> rechercher(edtVille.getText().toString().trim()));

        txtStatut = findViewById(R.id.txtStatut);
        carteMeteo = findViewById(R.id.carteMeteo);
        txtVille = findViewById(R.id.txtVille);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtCondition = findViewById(R.id.txtCondition);
        txtRessenti = findViewById(R.id.txtRessenti);
        txtVent = findViewById(R.id.txtVent);
        txtHumidite = findViewById(R.id.txtHumidite);
        listePrevisions = findViewById(R.id.listePrevisions);

        // GridView + ArrayAdapter : les données sont fournies par le tableau, la grille ne fait que les présenter.
        GridView grilleVilles = findViewById(R.id.grilleVilles);
        List<String> villes = Arrays.asList(getResources().getStringArray(R.array.villes_tunisiennes));
        ArrayAdapter<String> adapterVilles =
                new ArrayAdapter<>(this, R.layout.item_ville, villes);
        grilleVilles.setAdapter(adapterVilles);
        grilleVilles.setOnItemClickListener((parent, view, position, id) ->
                rechercher(parent.getItemAtPosition(position).toString()));

        findViewById(R.id.btnDocs).setOnClickListener(v -> {
            Intent intent = new Intent(this, WebViewActivity.class);
            if (urlApi != null) intent.putExtra("url", urlApi);
            startActivity(intent);
        });
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        String preferee = db.villePreferee(userId);
        if (preferee == null || preferee.isEmpty()) preferee = "Sidi Bouzid";
        edtVille.setText(preferee);
        rechercher(preferee);
    }

    private void rechercher(String ville) {
        if (ville.isEmpty()) {
            txtStatut.setText("Saisis d'abord un nom de ville.");
            return;
        }
        carteMeteo.setVisibility(View.GONE);
        txtStatut.setText("Recherche en cours…");
        btnRecherche.setEnabled(false);

        MeteoService.rechercher(ville, new MeteoService.Ecoute() {
            @Override
            public void onSuccess(Metee metee) {
                runOnUiThread(() -> {
                    btnRecherche.setEnabled(true);
                    afficher(metee);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnRecherche.setEnabled(true);
                    txtStatut.setText(message);
                });
            }
        });
    }

    private void afficher(Metee metee) {
        urlApi = metee.getUrlApi();
        txtVille.setText(metee.getVille() + " · " + metee.getRegion());
        txtTemperature.setText(metee.getTemperature() + " °C");
        txtCondition.setText("Ciel : " + metee.getCondition());
        txtRessenti.setText("Ressenti : " + metee.getRessenti() + " °C");
        txtVent.setText("Vent : " + metee.getVent() + " km/h");
        txtHumidite.setText("Humidité : " + metee.getHumidite() + " %");

        // ListView + BaseAdapter personnalisé : une ligne à 4 champs pour chaque jour.
        listePrevisions.setAdapter(new PrevisionAdapter(this, metee.getPrevisions()));

        carteMeteo.setVisibility(View.VISIBLE);
        txtStatut.setText("Données Open-Meteo · API publique sans clé");
        db.enregistrerVille(userId, metee.getVille());
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
