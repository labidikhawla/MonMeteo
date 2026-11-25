package com.example.monmeteo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "monmeteo_auth.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "users";
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    public UserDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "full_name TEXT NOT NULL, " +
                "email TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "salt TEXT NOT NULL, " +
                "ville_preferee TEXT, " +
                "role TEXT NOT NULL CHECK (role IN ('ADMIN', 'USER')))");
        insertAccount(db, "Administrateur", "admin@meteo.tn", "Admin123!", Role.ADMIN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void insertAccount(SQLiteDatabase db, String name, String email,
                               String password, Role role) {
        try {
            String salt = createSalt();
            String hash = hashPassword(password, salt);
            ContentValues values = new ContentValues();
            values.put("full_name", name.trim());
            values.put("email", email.trim().toLowerCase());
            values.put("password_hash", hash);
            values.put("salt", salt);
            values.put("role", role.name());
            db.insertOrThrow(TABLE, null, values);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de créer le compte initial", e);
        }
    }

    public boolean register(String name, String email, String password, String favoriteCity) {
        SQLiteDatabase db = getWritableDatabase();
        String salt = createSalt();
        String hash;
        try {
            hash = hashPassword(password, salt);
        } catch (Exception e) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("full_name", name.trim());
        values.put("email", email.trim().toLowerCase());
        values.put("password_hash", hash);
        values.put("salt", salt);
        values.put("ville_preferee", favoriteCity == null ? "" : favoriteCity.trim());
        values.put("role", Role.USER.name());
        long result = db.insert(TABLE, null, values);
        return result != -1;
    }

    public UserProfile login(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {"id", "full_name", "email", "password_hash", "salt", "role"};
        String[] args = {email.trim()};
        try (Cursor c = db.query(TABLE, columns, "email = ?", args,
                null, null, null, "1")) {
            if (!c.moveToFirst()) return null;
            String storedHash = c.getString(c.getColumnIndexOrThrow("password_hash"));
            String salt = c.getString(c.getColumnIndexOrThrow("salt"));
            String candidateHash = hashPassword(password, salt);
            if (!constantTimeEquals(storedHash, candidateHash)) return null;
            return new UserProfile(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("full_name")),
                    c.getString(c.getColumnIndexOrThrow("email")),
                    Role.valueOf(c.getString(c.getColumnIndexOrThrow("role"))));
        } catch (Exception e) {
            return null;
        }
    }

    public List<UserProfile> getAllUsers() {
        List<UserProfile> users = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE,
                new String[]{"id", "full_name", "email", "role"},
                null, null, null, null, "full_name COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                users.add(new UserProfile(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("full_name")),
                        c.getString(c.getColumnIndexOrThrow("email")),
                        Role.valueOf(c.getString(c.getColumnIndexOrThrow("role")))));
            }
        }
        return users;
    }

    public String villePreferee(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE, new String[]{"ville_preferee"}, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null, "1")) {
            if (c.moveToFirst()) return c.getString(0);
        }
        return "";
    }

    public boolean enregistrerVille(long id, String ville) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ville_preferee", ville);
        return db.update(TABLE, values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Un sel aléatoire par utilisateur : deux mots de passe identiques donnent deux hachés différents.
    private String createSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    // PBKDF2, 120 000 itérations : on ralentit volontairement le calcul contre la force brute.
    private String hashPassword(String password, String saltText) throws Exception {
        byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    // equals() s'arrête au premier caractère différent : la durée de comparaison fuiterait l'emplacement du premier écart.
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] y = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }
}
