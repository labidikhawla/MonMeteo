package com.example.monmeteo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

	private EditText champVille;
	private Button boutonMeteo;
	private TextView statut;
	private View carte;
	private TextView txtVille, txtTemperature, txtCondition, txtRessenti, txtVent, txtHumidite, txtJournee;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		champVille = findViewById(R.id.edtVille);
		boutonMeteo = findViewById(R.id.btnMeteo);
		statut = findViewById(R.id.txtStatut);
		carte = findViewById(R.id.carteMeteo);
		txtVille = findViewById(R.id.txtVille);
		txtTemperature = findViewById(R.id.txtTemperature);
		txtCondition = findViewById(R.id.txtCondition);
		txtRessenti = findViewById(R.id.txtRessenti);
		txtVent = findViewById(R.id.txtVent);
		txtHumidite = findViewById(R.id.txtHumidite);
		txtJournee = findViewById(R.id.txtJournee);

		boutonMeteo.setOnClickListener(v -> lancerRecherche());
	}

	private void lancerRecherche() {
		final String ville = champVille.getText().toString().trim();
		if (ville.isEmpty()) {
			statut.setText(R.string.hint_ville);
			return;
		}

		carte.setVisibility(View.GONE);
		statut.setText(R.string.statut_recherche);
		boutonMeteo.setEnabled(false);

		// Les appels réseau sont interdits sur le thread UI : on passe en arrière-plan.
		new Thread(() -> {
			String erreur = null;
			Metee metee = null;
			try {
				metee = chargerMetee(ville);
			} catch (VilleIntrouvable e) {
				erreur = getString(R.string.statut_ville_inconnue);
			} catch (IOException e) {
				erreur = getString(R.string.statut_hors_ligne);
			}

			final Metee resultat = metee;
			final String messageErreur = erreur;
			runOnUiThread(() -> {
				boutonMeteo.setEnabled(true);
				if (messageErreur != null) {
					statut.setText(messageErreur);
				} else {
					afficher(resultat);
				}
			});
		}).start();
	}

	private Metee chargerMetee(String ville) throws IOException {
		String urlVille = "https://geocoding-api.open-meteo.com/v1/search?name="
				+ URLEncoder.encode(ville, "UTF-8") + "&count=1&language=fr&format=json";

		JSONArray resultats;
		try {
			resultats = new JSONObject(lireJson(urlVille)).optJSONArray("results");
		} catch (Exception e) {
			throw new VilleIntrouvable();
		}
		if (resultats == null || resultats.length() == 0) {
			throw new VilleIntrouvable();
		}

		JSONObject lieu = resultats.optJSONObject(0);
		String lat = lieu.optString("latitude");
		String lon = lieu.optString("longitude");

		String urlMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
				+ "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
				+ "&daily=temperature_2m_max,temperature_2m_min&timezone=auto";

		Metee metee = new Metee();
		metee.ville = lieu.optString("name");
		try {
			JSONObject reponse = new JSONObject(lireJson(urlMeteo));
			JSONObject courant = reponse.getJSONObject("current");
			JSONObject jour = reponse.getJSONObject("daily");

			metee.temperature = arrondi(courant.getDouble("temperature_2m"));
			metee.condition = decrire(courant.getInt("weather_code"));
			metee.ressenti = arrondi(courant.getDouble("apparent_temperature"));
			metee.vent = arrondi(courant.getDouble("wind_speed_10m"));
			metee.humidite = String.valueOf(courant.getInt("relative_humidity_2m"));
			metee.min = arrondi(jour.getJSONArray("temperature_2m_min").getDouble(0));
			metee.max = arrondi(jour.getJSONArray("temperature_2m_max").getDouble(0));
		} catch (Exception e) {
			throw new IOException("Réponse météo illisible", e);
		}
		return metee;
	}

	private String lireJson(String url) throws IOException {
		HttpURLConnection connexion = (HttpURLConnection) new URL(url).openConnection();
		connexion.setConnectTimeout(8000);
		connexion.setReadTimeout(8000);
		connexion.setRequestProperty("Accept", "application/json");
		try {
			if (connexion.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP " + connexion.getResponseCode());
			}
			StringBuilder corps = new StringBuilder();
			try (BufferedReader lecteur = new BufferedReader(
					new InputStreamReader(connexion.getInputStream(), "UTF-8"))) {
				String ligne;
				while ((ligne = lecteur.readLine()) != null) {
					corps.append(ligne);
				}
			}
			return corps.toString();
		} finally {
			connexion.disconnect();
		}
	}

	private void afficher(Metee metee) {
		txtVille.setText(metee.ville);
		txtTemperature.setText(metee.temperature + " °C");
		txtCondition.setText("Ciel : " + metee.condition);
		txtRessenti.setText("Ressenti : " + metee.ressenti + " °C");
		txtVent.setText("Vent : " + metee.vent + " km/h");
		txtHumidite.setText("Humidité : " + metee.humidite + " %");
		txtJournee.setText("Aujourd'hui : " + metee.min + " °C  /  " + metee.max + " °C");

		carte.setVisibility(View.VISIBLE);
		statut.setText(R.string.statut_source);
	}

	private String arrondi(double valeur) {
		return String.valueOf(Math.round(valeur));
	}

	// Codes météo WMO, la convention utilisée par Open-Meteo.
	private String decrire(int code) {
		if (code == 0) return "Dégagé";
		if (code <= 2) return "Peu nuageux";
		if (code == 3) return "Couvert";
		if (code <= 48) return "Brouillard";
		if (code <= 57) return "Bruine";
		if (code <= 67) return "Pluie";
		if (code <= 77) return "Neige";
		if (code <= 82) return "Averses";
		if (code <= 86) return "Averses de neige";
		if (code <= 99) return "Orage";
		return "Indéterminé";
	}

	private static class Metee {
		String ville, temperature, condition, ressenti, vent, humidite, min, max;
	}

	private static class VilleIntrouvable extends IOException {
	}
}
