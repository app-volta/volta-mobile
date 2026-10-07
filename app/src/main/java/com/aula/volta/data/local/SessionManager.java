package com.aula.volta.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerenciador de Sessão do usuário VOLTA.
 * Armazena token JWT, credenciais para renovação automática, dados de perfil
 * e o ID da sessão ativa com o Chatbot.
 */
public final class SessionManager {

    private static final String PREFS = "session";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_TOKEN = "user_jwt_token";
    private static final String KEY_CHAT_SESSION_ID = "chat_session_id";

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
        login(context, email, "", name, "");
    }

    public static void login(Context context, String email, String password, String name, String token) {
        prefs(context).edit()
                .putString(KEY_EMAIL, email == null ? "" : email)
                .putString(KEY_PASSWORD, password == null ? "" : password)
                .putString(KEY_NAME, name == null ? "" : name)
                .putString(KEY_TOKEN, token == null ? "" : token)
                .apply();
    }

    public static void saveToken(Context context, String token) {
        prefs(context).edit()
                .putString(KEY_TOKEN, token == null ? "" : token)
                .apply();
    }

    public static String getToken(Context context) {
        return prefs(context).getString(KEY_TOKEN, "");
    }

    public static boolean hasToken(Context context) {
        String token = getToken(context);
        return token != null && !token.trim().isEmpty();
    }

    public static String password(Context context) {
        return prefs(context).getString(KEY_PASSWORD, "");
    }

    public static void saveChatSessionId(Context context, String sessionId) {
        prefs(context).edit()
                .putString(KEY_CHAT_SESSION_ID, sessionId == null ? "" : sessionId)
                .apply();
    }

    public static String getChatSessionId(Context context) {
        return prefs(context).getString(KEY_CHAT_SESSION_ID, "");
    }

    public static void clearChatSessionId(Context context) {
        prefs(context).edit().remove(KEY_CHAT_SESSION_ID).apply();
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
