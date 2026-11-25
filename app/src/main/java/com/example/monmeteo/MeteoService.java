package com.example.monmeteo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MeteoService {

    public interface Ecoute {
        void onSuccess(Metee metee);

        void onError(String message);
    }

    public static void rechercher(final String ville, final Ecoute listener) {
        new Thread(() -> {
            String erreur = null;
            Metee metee = null;
            try {
                metee = charger(ville);
            } catch (VilleIntrouvable e) {
                erreur = "Ville introuvable. Vérifie l'orthographe.";
            } catch (IOException e) {
                erreur = "Impossible de joindre Open-Meteo. Vérifie ta connexion.";
            }
            final Metee resultat = metee;
            final String messageErreur = erreur;
            if (messageErreur != null) {
                listener.onError(messageErreur);
            } else {
                listener.onSuccess(resultat);
            }
        }).start();
    }

    // Appel 1 : l'API de géocodage transforme le nom de la ville en coordonnées.
    // Appel 2 : l'API de prévisions renvoie le temps actuel et les 7 prochains jours.
    public static Metee charger(String ville) throws IOException {
        String urlGeocodage = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(ville, "UTF-8") + "&count=1&language=fr&format=json";

        JSONArray resultats;
        try {
            resultats = new JSONObject(lireJson(urlGeocodage)).optJSONArray("results");
        } catch (JSONException e) {
            throw new VilleIntrouvable();
        }
        if (resultats == null || resultats.length() == 0) {
            throw new VilleIntrouvable();
        }

        JSONObject lieu = resultats.optJSONObject(0);

        String urlPrevisions = "https://api.open-meteo.com/v1/forecast?latitude="
                + lieu.optString("latitude") + "&longitude=" + lieu.optString("longitude")
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto";

        Metee metee = new Metee();
        metee.setVille(lieu.optString("name"));
        metee.setRegion(lieu.optString("admin1"));
        metee.setUrlApi(urlPrevisions);
        try {
            JSONObject reponse = new JSONObject(lireJson(urlPrevisions));
            JSONObject courant = reponse.getJSONObject("current");
            JSONObject quotidien = reponse.getJSONObject("daily");

            metee.setTemperature(arrondi(courant.getDouble("temperature_2m")));
            metee.setCondition(decrire(courant.getInt("weather_code")));
            metee.setRessenti(arrondi(courant.getDouble("apparent_temperature")));
            metee.setVent(arrondi(courant.getDouble("wind_speed_10m")));
            metee.setHumidite(String.valueOf(courant.getInt("relative_humidity_2m")));

            JSONArray dates = quotidien.getJSONArray("time");
            JSONArray codes = quotidien.getJSONArray("weather_code");
            JSONArray maxs = quotidien.getJSONArray("temperature_2m_max");
            JSONArray mins = quotidien.getJSONArray("temperature_2m_min");

            for (int i = 0; i < dates.length(); i++) {
                int code = codes.getInt(i);
                metee.getPrevisions().add(new Prevision(
                        libelleJour(i, dates.getString(i)),
                        symbole(code),
                        decrire(code),
                        arrondi(mins.getDouble(i)),
                        arrondi(maxs.getDouble(i))));
            }

            metee.setMin(metee.getPrevisions().get(0).getMin());
            metee.setMax(metee.getPrevisions().get(0).getMax());
        } catch (Exception e) {
            throw new IOException("Réponse météo illisible", e);
        }
        return metee;
    }

    public static String lireJson(String url) throws IOException {
        HttpURLConnection connexion = (HttpURLConnection) new URL(url).openConnection();
        connexion.setConnectTimeout(15000);
        connexion.setReadTimeout(20000);
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

    private static String libelleJour(int index, String dateIso) {
        if (index == 0) return "Aujourd'hui";
        if (index == 1) return "Demain";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso);
            return new SimpleDateFormat("EEEE", Locale.FRANCE).format(d);
        } catch (Exception e) {
            return dateIso;
        }
    }

    private static String arrondi(double valeur) {
        return String.valueOf(Math.round(valeur));
    }

    // Codes météo WMO : la convention publique employée par Open-Meteo.
    public static String decrire(int code) {
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

    public static String symbole(int code) {
        if (code == 0) return "☀";
        if (code <= 2) return "⛅";
        if (code == 3) return "☁";
        if (code <= 48) return "🌫";
        if (code <= 67) return "🌧";
        if (code <= 77) return "🌨";
        if (code <= 82) return "🌦";
        if (code <= 86) return "❄";
        if (code <= 99) return "⛈";
        return "•";
    }

    public static class VilleIntrouvable extends IOException {
    }
}
