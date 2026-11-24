# Météo Poche

Application Android native (Java + XML) qui répond à un problème réel : **savoir en deux secondes quel temps il fait réellement dans une ville, avant de sortir, de voyager ou de travailler dehors**, sans installer une application gonflée de publicité et sans créer de compte.

En Tunisie, beaucoup d'étudiants, d'agriculteurs et de voyageurs ont besoin d'une météo fiable et instantanée pour une ville précise (Sousse, Kairouan, Djerba…). Les applications existantes imposent un compte, de la géolocalisation permanente et des écrans publicitaires. **Météo Poche** fait l'inverse : un champ de texte, un bouton, une réponse.

---

## 1. Fonctionnalités

| Fonctionnalité | Détail |
| --- | --- |
| Recherche par ville | Saisie libre du nom, accents et espaces acceptés |
| Température actuelle | En degrés Celsius, arrondie |
| Condition du ciel | Traduite en français à partir du code WMO officiel |
| Température ressentie | `apparent_temperature` |
| Vent | Vitesse à 10 m, en km/h |
| Humidité | En pourcentage |
| Min / max du jour | Plage thermique de la journée |
| Gestion d'erreurs | Ville introuvable, serveur injoignable, aucune connexion |

L'application tient en **un seul écran** : c'est voulu, c'est ce qui la rend rapide.

---

## 2. Stack technique

- **Langage** : Java (aucun Kotlin)
- **Interface** : XML uniquement (`activity_main.xml`), aucune interface dessinée en code
- **IDE** : Android Studio Quail 4 (2026.1.4 Patch 1)
- **Android Gradle Plugin** : 9.4.1 — **Gradle** : 9.6.0
- **SDK** : compile/target 37, `minSdk` 24 (compatible Android 7.0 et plus)
- **Thème** : Material 3 (`Theme.Material3.DayNight.NoActionBar`), clair et sombre
- **Dépendances tierces** : **aucune**. L'appel réseau utilise `HttpURLConnection` et le parsing utilise `org.json`, deux classes déjà intégrées au SDK Android.

---

## 3. Intégration et consommation d'une API externe

C'est le cœur technique du projet. L'application consomme **deux points d'entrée de l'API publique Open-Meteo**, gratuite et **sans clé API** — donc le démonstrateur fonctionne sur n'importe quel téléphone, sans compte à créer et sans quota à surveiller.

### 3.1 Les deux appels

| Étape | Endpoint | Paramètres | Ce qu'on en tire |
| --- | --- | --- | --- |
| 1. Géocodage | `https://geocoding-api.open-meteo.com/v1/search` | `name`, `count=1`, `language=fr`, `format=json` | `latitude`, `longitude`, `name` (nom officiel de la ville) |
| 2. Prévisions | `https://api.open-meteo.com/v1/forecast` | `latitude`, `longitude`, `current=…`, `daily=…`, `timezone=auto` | température, ressenti, humidité, code météo, vent, min/max du jour |

Le géocodage est indispensable : l'API météo ne prend **que** des coordonnées. L'application convertit donc d'abord le texte saisi par l'utilisateur en position géographique, puis interroge la météo avec cette position. `timezone=auto` garantit que les min/max correspondent bien à la journée locale de la ville, pas à UTC.

### 3.2 Extrait de code de la consommation

```java
private Metee chargerMetee(String ville) throws IOException {
    String urlVille = "https://geocoding-api.open-meteo.com/v1/search?name="
            + URLEncoder.encode(ville, "UTF-8") + "&count=1&language=fr&format=json";

    JSONArray resultats = new JSONObject(lireJson(urlVille)).optJSONArray("results");
    if (resultats == null || resultats.length() == 0) throw new VilleIntrouvable();

    JSONObject lieu = resultats.optJSONObject(0);
    String lat = lieu.optString("latitude");
    String lon = lieu.optString("longitude");

    String urlMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
            + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto";

    JSONObject reponse = new JSONObject(lireJson(urlMeteo));
    JSONObject courant = reponse.getJSONObject("current");
    JSONObject jour    = reponse.getJSONObject("daily");
    // ... lecture des valeurs et construction de l'objet Metee
}
```

La couche HTTP est isolée dans une seule méthode, réutilisable pour n'importe quelle API :

```java
private String lireJson(String url) throws IOException {
    HttpURLConnection connexion = (HttpURLConnection) new URL(url).openConnection();
    connexion.setConnectTimeout(8000);
    connexion.setReadTimeout(8000);
    connexion.setRequestProperty("Accept", "application/json");
    try {
        if (connexion.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IOException("HTTP " + connexion.getResponseCode());
        // lecture ligne à ligne du flux ...
    } finally {
        connexion.disconnect();
    }
}
```

### 3.3 Réponse réelle de l'API (vérifiée le 24/11/2026)

Requête : `GET https://api.open-meteo.com/v1/forecast?latitude=35.82539&longitude=10.63699&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto`

```json
{
  "timezone": "Africa/Tunis",
  "current": {
    "time": "2026-11-24T23:45",
    "temperature_2m": 17.4,
    "apparent_temperature": 17.2,
    "relative_humidity_2m": 82,
    "weather_code": 0,
    "wind_speed_10m": 11.3
  },
  "daily": { "temperature_2m_min": [ ... ], "temperature_2m_max": [ ... ] }
}
```

Affichage produit par l'application pour Sousse : **17 °C**, **Ciel : Dégagé**, **Ressenti : 17 °C**, **Vent : 11 km/h**, **Humidité : 82 %**.

### 3.4 Points techniques à savoir expliquer

- **Permission** : `<uses-permission android:name="android.permission.INTERNET" />` dans `AndroidManifest.xml`. Sans elle, chaque appel échoue silencieusement.
- **Interdiction du réseau sur le thread UI** : Android lève une `NetworkOnMainThreadException` si un appel HTTP part du thread principal. Les deux requêtes sont donc confiées à un `new Thread(...)`, et l'affichage revient sur le thread d'interface avec `runOnUiThread(...)`.
- **Encodage de l'URL** : `URLEncoder.encode(ville, "UTF-8")` transforme « Sousse El Medina » ou « Kairouan » en paramètre valide ; sans ça, les espaces cassent la requête.
- **Codes WMO** : `weather_code` n'est pas un texte mais un code normalisé par l'Organisation météorologique mondiale (0 = dégagé, 3 = couvert, 61 = pluie, 95 = orage). La méthode `decrire(int code)` les traduit en français.
- **Séparation des responsabilités** : la classe interne `Metee` sert de DTO — le thread de fond construit un objet simple dont toutes les valeurs sont déjà formatées en texte, l'interface n'a plus qu'à l'afficher. Elle ne touche jamais au JSON.

---

## 4. Arborescence

```
MonMeteo/
├── app/src/main/
│   ├── AndroidManifest.xml            // permission INTERNET + activity principale
│   ├── java/com/example/monmeteo/
│   │   └── MainActivity.java          // UI, appels API, parsing JSON, erreurs
│   └── res/
│       ├── layout/activity_main.xml   // toute l'interface, en XML
│       ├── drawable/fond_carte.xml    // carte arrondie blanche
│       ├── values/{colors,dimens,strings,themes}.xml
│       └── values-night/themes.xml    // thème sombre
├── build.gradle.kts / settings.gradle.kts / gradle/libs.versions.toml
└── README.md
```

---

## 5. Générer et livrer l'APK

Deux méthodes, toutes deux testées :

**Dans Android Studio** : `Build → Generate Signed App Bundle / APKs… → APK → Next → debug → Finish`

**En ligne de commande** (depuis la racine du projet) :
```
./gradlew assembleDebug
```

Le fichier installable est produit ici :
```
app/build/outputs/apk/debug/app-debug.apk
```

Le projet est livré avec l'APK déjà généré (5,5 Mo), hors dépôt car `*.apk` est ignoré par Git ; il est transmis séparément comme demandé.

### Installation sur un téléphone

Méthode 1 — copier `app-debug.apk` sur le téléphone, l'ouvrir, autoriser « sources inconnues ».

Méthode 2 — via ADB, téléphone branché en USB et « débogage USB » activé :
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. Limites assumées

- Une seule ville à la fois, pas de liste de favoris ni d'historique.
- Pas d'écran de prévisions sur plusieurs jours, alors que l'API les renvoie déjà : c'est la première évolution prévue.
- Pas de mise en cache : chaque appui sur le bouton relance deux requêtes réseau.
- L'application nécessite une connexion ; elle n'annonce pas explicitement le mode hors ligne par une icône dédiée, seulement par un message.

## 7. Évolutions possibles

1. Afficher les 7 jours déjà reçus dans `daily` (aucun nouvel appel nécessaire).
2. Ville mémorisée dans `SharedPreferences` au lancement.
3. Icônes météo dessinées en `VectorDrawable` selon le code WMO.
4. Géolocalisation automatique par `FusedLocationProviderClient` en complément de la saisie manuelle.
