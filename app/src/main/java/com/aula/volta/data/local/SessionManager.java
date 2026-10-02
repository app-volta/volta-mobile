package com.aula.volta.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Sessão mock (Fase 13). Guarda quem está logado; backend de auth pluga aqui.
 * MainActivity barra quem não tem sessão; Sair limpa e volta ao Auth.
 */
public final class SessionManager {

    private static final String PREFS = "session";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";

    private SessionManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(Context context) {
        String email = prefs(context).getString(KEY_EMAIL, null);
        return email != null && !email.isEmpty();
    }

    public static void login(Context context, String email, String name) {
        prefs(context).edit()
                .putString(KEY_EMAIL, email == null ? "" : email)
                .putString(KEY_NAME, name == null ? "" : name)
                .apply();
    }

    public static void logout(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static String email(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }

    public static String name(Context context) {
        return prefs(context).getString(KEY_NAME, "");
    }
}
