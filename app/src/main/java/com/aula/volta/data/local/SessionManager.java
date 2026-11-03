package com.aula.volta.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {

    private static final String PREFS = "session";
    private static final String PREFS_PERSISTENT = "user_profiles_cache";

    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_TOKEN = "user_jwt_token";
    private static final String KEY_CHAT_SESSION_ID = "chat_session_id";
    private static final String KEY_SECTOR = "user_sector";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_COMPANY_ID = "company_id";
    private static final String KEY_PROFILE_PHOTO_URL = "user_profile_photo_url";
    private static final String KEY_PROFILE_PHOTO_LOCAL = "user_profile_photo_local";
    private static final String KEY_FIRST_LOGIN_PENDING = "first_login_pending";

    private SessionManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences persistentPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_PERSISTENT, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(Context context) {
        String email = email(context);
        return email != null && !email.isEmpty() && !isFirstLoginPending(context);
    }

    public static void setFirstLoginPending(Context context, boolean pending) {
        prefs(context).edit()
                .putBoolean(KEY_FIRST_LOGIN_PENDING, pending)
                .apply();
    }

    public static boolean isFirstLoginPending(Context context) {
        return prefs(context).getBoolean(KEY_FIRST_LOGIN_PENDING, false);
    }

    public static void login(Context context, String email, String name) {
        login(context, email, "", name, "");
    }

    public static void login(Context context, String email, String password, String name, String token) {
        String cleanEmail = email == null ? "" : email.trim();
        String cleanName = name == null ? "" : name.trim();
        prefs(context).edit()
                .putString(KEY_EMAIL, cleanEmail)
                .putString(KEY_PASSWORD, password == null ? "" : password)
                .putString(KEY_NAME, cleanName)
                .putString(KEY_TOKEN, token == null ? "" : token)
                .apply();

        if (!cleanEmail.isEmpty()) {
            String lowerEmail = cleanEmail.toLowerCase(java.util.Locale.ROOT);
            persistentPrefs(context).edit()
                    .putString("active_user_email", cleanEmail)
                    .putString("user_name_" + cleanEmail, cleanName)
                    .putString("user_name_" + lowerEmail, cleanName)
                    .apply();

            String cachedPhotoUrl = persistentPrefs(context).getString("photo_url_" + lowerEmail, "");
            if (cachedPhotoUrl == null || cachedPhotoUrl.isEmpty()) {
                cachedPhotoUrl = persistentPrefs(context).getString("photo_url_" + cleanEmail, "");
            }
            String cachedPhotoLocal = persistentPrefs(context).getString("photo_local_" + lowerEmail, "");
            if (cachedPhotoLocal == null || cachedPhotoLocal.isEmpty()) {
                cachedPhotoLocal = persistentPrefs(context).getString("photo_local_" + cleanEmail, "");
            }

            if (cachedPhotoUrl == null || cachedPhotoUrl.isEmpty()) {
                boolean removed = persistentPrefs(context).getBoolean("photo_removed_" + lowerEmail, false)
                        || persistentPrefs(context).getBoolean("photo_removed_" + cleanEmail, false);
                if (!removed) {
                    cachedPhotoUrl = com.aula.volta.data.upload.CloudinaryUploader.getDeterministicProfileUrl(lowerEmail);
                }
            }

            if (cachedPhotoLocal == null || cachedPhotoLocal.isEmpty() || !new java.io.File(cachedPhotoLocal).exists()) {
                java.io.File hashed = new java.io.File(context.getFilesDir(), "profile_photo_" + Math.abs(lowerEmail.hashCode()) + ".jpg");
                if (hashed.exists()) {
                    cachedPhotoLocal = hashed.getAbsolutePath();
                }
            }

            prefs(context).edit()
                    .putString(KEY_PROFILE_PHOTO_URL, cachedPhotoUrl == null ? "" : cachedPhotoUrl)
                    .putString(KEY_PROFILE_PHOTO_LOCAL, cachedPhotoLocal == null ? "" : cachedPhotoLocal)
                    .apply();
        }
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

    public static void savePassword(Context context, String password) {
        prefs(context).edit().putString(KEY_PASSWORD, password == null ? "" : password).apply();
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
        String currentEmail = email(context);
        String currentPhotoUrl = getProfilePhotoUrl(context);
        String currentPhotoLocal = getProfilePhotoLocal(context);
        String currentName = name(context);
        if (currentEmail != null && !currentEmail.isEmpty()) {
            String lowerEmail = currentEmail.trim().toLowerCase(java.util.Locale.ROOT);
            persistentPrefs(context).edit()
                    .putString("photo_url_" + currentEmail, currentPhotoUrl)
                    .putString("photo_url_" + lowerEmail, currentPhotoUrl)
                    .putString("photo_local_" + currentEmail, currentPhotoLocal)
                    .putString("photo_local_" + lowerEmail, currentPhotoLocal)
                    .putString("user_name_" + currentEmail, currentName)
                    .putString("user_name_" + lowerEmail, currentName)
                    .apply();
        }
        persistentPrefs(context).edit().remove("active_user_email").apply();
        prefs(context).edit().clear().apply();

        try {
            Context app = context.getApplicationContext();
            app.getSharedPreferences("cache_occurrences", Context.MODE_PRIVATE).edit().clear().apply();
            app.getSharedPreferences("cache_occurrence_detail", Context.MODE_PRIVATE).edit().clear().apply();
            app.getSharedPreferences("cache_reports", Context.MODE_PRIVATE).edit().clear().apply();
            app.getSharedPreferences("cache_notifications", Context.MODE_PRIVATE).edit().clear().apply();
            try {
                java.io.File legacyPhoto = new java.io.File(app.getFilesDir(), "profile_photo.jpg");
                if (legacyPhoto.exists()) {
                    legacyPhoto.delete();
                }
            } catch (Exception ignored) {
            }
            try {
                java.io.File[] cacheFiles = app.getCacheDir().listFiles();
                if (cacheFiles != null) {
                    for (java.io.File cf : cacheFiles) {
                        if (cf.getName().startsWith("volta_") || cf.getName().startsWith("profile_")) {
                            cf.delete();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            com.aula.volta.data.api.ApiClient.clearToken(app);
            com.aula.volta.data.api.ApiClient.reset();
            com.aula.volta.data.api.ChatbotAuthClient.reset();
        } catch (Exception ignored) {
        }
    }

    public static String email(Context context) {
        String e = prefs(context).getString(KEY_EMAIL, "");
        if (e != null && !e.isEmpty()) {
            return e;
        }
        String persistent = persistentPrefs(context).getString("active_user_email", "");
        if (persistent != null && !persistent.isEmpty()) {
            prefs(context).edit().putString(KEY_EMAIL, persistent).apply();
            return persistent;
        }
        return "";
    }

    public static String getEmail(Context context) {
        return email(context);
    }

    public static void saveName(Context context, String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_NAME, name.trim()).apply();
        String currentEmail = email(context);
        if (currentEmail != null && !currentEmail.isEmpty()) {
            persistentPrefs(context).edit()
                    .putString("user_name_" + currentEmail, name.trim())
                    .apply();
        }
    }

    public static void saveEmail(Context context, String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_EMAIL, email.trim()).apply();
    }

    public static String name(Context context) {
        String n = prefs(context).getString(KEY_NAME, "");
        if (n != null && !n.trim().isEmpty()) {
            return n.trim();
        }
        String currentEmail = email(context);
        if (currentEmail != null && !currentEmail.isEmpty()) {
            String cachedName = persistentPrefs(context).getString("user_name_" + currentEmail, "");
            if (cachedName != null && !cachedName.trim().isEmpty()) {
                return cachedName.trim();
            }
            if (currentEmail.contains("@")) {
                String prefix = currentEmail.split("@")[0].replace('.', ' ').replace('_', ' ').trim();
                if (!prefix.isEmpty()) {
                    String[] words = prefix.split("\\s+");
                    StringBuilder sb = new StringBuilder();
                    for (String w : words) {
                        if (!w.isEmpty()) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(Character.toUpperCase(w.charAt(0)));
                            if (w.length() > 1) {
                                sb.append(w.substring(1).toLowerCase());
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        return sb.toString();
                    }
                }
            }
        }
        return "Colaborador";
    }

    public static String getName(Context context) {
        return name(context);
    }

    public static void saveSector(Context context, String sector) {
        prefs(context).edit()
                .putString(KEY_SECTOR, sector == null ? "" : sector)
                .apply();
    }

    public static String sector(Context context) {
        return prefs(context).getString(KEY_SECTOR, "");
    }

    public static String getSector(Context context) {
        return sector(context);
    }

    public static void saveUserId(Context context, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_USER_ID, userId.trim()).apply();
    }

    public static String getUserId(Context context) {
        return prefs(context).getString(KEY_USER_ID, "");
    }

    public static void saveCompanyId(Context context, String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return;
        }
        prefs(context).edit().putString(KEY_COMPANY_ID, companyId.trim()).apply();
    }

    public static String getCompanyId(Context context) {
        return prefs(context).getString(KEY_COMPANY_ID, "");
    }

    public static void saveProfilePhotoUrl(Context context, String url) {
        saveProfilePhotoUrl(context, email(context), url);
    }

    public static void saveProfilePhotoUrl(Context context, String userEmail, String url) {
        String cleanUrl = url == null ? "" : url;
        prefs(context).edit().putString(KEY_PROFILE_PHOTO_URL, cleanUrl).apply();
        if (userEmail != null && !userEmail.isEmpty()) {
            String cleanEmail = userEmail.trim().toLowerCase(java.util.Locale.ROOT);
            persistentPrefs(context).edit()
                    .putString("photo_url_" + cleanEmail, cleanUrl)
                    .putString("photo_url_" + userEmail, cleanUrl)
                    .remove("photo_removed_" + cleanEmail)
                    .remove("photo_removed_" + userEmail)
                    .apply();
        }
    }

    public static String getProfilePhotoUrl(Context context) {
        return getProfilePhotoUrl(context, email(context));
    }

    public static String getProfilePhotoUrl(Context context, String userEmail) {
        String url = prefs(context).getString(KEY_PROFILE_PHOTO_URL, "");
        if (url != null && !url.isEmpty()) {
            return url;
        }
        if (userEmail != null && !userEmail.isEmpty()) {
            String cleanEmail = userEmail.trim().toLowerCase(java.util.Locale.ROOT);
            String persistent = persistentPrefs(context).getString("photo_url_" + cleanEmail, "");
            if (persistent == null || persistent.isEmpty()) {
                persistent = persistentPrefs(context).getString("photo_url_" + userEmail, "");
            }
            if (persistent != null && !persistent.isEmpty()) {
                return persistent;
            }
            boolean removed = persistentPrefs(context).getBoolean("photo_removed_" + cleanEmail, false)
                    || persistentPrefs(context).getBoolean("photo_removed_" + userEmail, false);
            if (!removed) {
                String deterministic = com.aula.volta.data.upload.CloudinaryUploader.getDeterministicProfileUrl(cleanEmail);
                if (deterministic != null && !deterministic.isEmpty()) {
                    return deterministic;
                }
            }
        }
        return "";
    }

    public static void saveProfilePhotoLocal(Context context, String localPath) {
        saveProfilePhotoLocal(context, email(context), localPath);
    }

    public static void saveProfilePhotoLocal(Context context, String userEmail, String localPath) {
        String cleanPath = localPath == null ? "" : localPath;
        prefs(context).edit().putString(KEY_PROFILE_PHOTO_LOCAL, cleanPath).apply();
        if (userEmail != null && !userEmail.isEmpty()) {
            String cleanEmail = userEmail.trim().toLowerCase(java.util.Locale.ROOT);
            persistentPrefs(context).edit()
                    .putString("photo_local_" + cleanEmail, cleanPath)
                    .putString("photo_local_" + userEmail, cleanPath)
                    .remove("photo_removed_" + cleanEmail)
                    .remove("photo_removed_" + userEmail)
                    .apply();
        }
    }

    public static String getProfilePhotoLocal(Context context) {
        return getProfilePhotoLocal(context, email(context));
    }

    public static String getProfilePhotoLocal(Context context, String userEmail) {
        if (userEmail != null && !userEmail.isEmpty()) {
            String cleanEmail = userEmail.trim().toLowerCase(java.util.Locale.ROOT);
            String pPath = persistentPrefs(context).getString("photo_local_" + cleanEmail, "");
            if (pPath != null && !pPath.isEmpty() && new java.io.File(pPath).exists()) {
                prefs(context).edit().putString(KEY_PROFILE_PHOTO_LOCAL, pPath).apply();
                return pPath;
            }
            String altPath = persistentPrefs(context).getString("photo_local_" + userEmail, "");
            if (altPath != null && !altPath.isEmpty() && new java.io.File(altPath).exists()) {
                prefs(context).edit().putString(KEY_PROFILE_PHOTO_LOCAL, altPath).apply();
                return altPath;
            }
            java.io.File hashed = new java.io.File(context.getFilesDir(), "profile_photo_" + Math.abs(cleanEmail.hashCode()) + ".jpg");
            if (hashed.exists()) {
                String abs = hashed.getAbsolutePath();
                prefs(context).edit().putString(KEY_PROFILE_PHOTO_LOCAL, abs).apply();
                persistentPrefs(context).edit().putString("photo_local_" + cleanEmail, abs).apply();
                return abs;
            }
        }
        String path = prefs(context).getString(KEY_PROFILE_PHOTO_LOCAL, "");
        if (path != null && !path.isEmpty()) {
            if (new java.io.File(path).exists()) {
                return path;
            } else {
                prefs(context).edit().remove(KEY_PROFILE_PHOTO_LOCAL).apply();
            }
        }
        java.io.File legacy = new java.io.File(context.getFilesDir(), "profile_photo.jpg");
        if (legacy.exists()) {
            String abs = legacy.getAbsolutePath();
            prefs(context).edit().putString(KEY_PROFILE_PHOTO_LOCAL, abs).apply();
            return abs;
        }
        return "";
    }

    public static void saveCompany(Context context, String name, String cnpj, String address) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (name != null && !name.trim().isEmpty()) {
            editor.putString("company_name", name.trim());
        }
        if (cnpj != null && !cnpj.trim().isEmpty()) {
            editor.putString("company_cnpj", cnpj.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            editor.putString("company_address", address.trim());
        }
        editor.apply();
    }

    public static String getCompanyName(Context context) {
        return prefs(context).getString("company_name", "");
    }

    public static String getCompanyCnpj(Context context) {
        return prefs(context).getString("company_cnpj", "");
    }

    public static String getCompanyAddress(Context context) {
        return prefs(context).getString("company_address", "");
    }

    public static void clearProfilePhoto(Context context) {
        String currentEmail = email(context);
        prefs(context).edit()
                .remove(KEY_PROFILE_PHOTO_URL)
                .remove(KEY_PROFILE_PHOTO_LOCAL)
                .apply();
        if (currentEmail != null && !currentEmail.isEmpty()) {
            String cleanEmail = currentEmail.trim().toLowerCase(java.util.Locale.ROOT);
            persistentPrefs(context).edit()
                    .remove("photo_url_" + cleanEmail)
                    .remove("photo_url_" + currentEmail)
                    .remove("photo_local_" + cleanEmail)
                    .remove("photo_local_" + currentEmail)
                    .putBoolean("photo_removed_" + cleanEmail, true)
                    .putBoolean("photo_removed_" + currentEmail, true)
                    .apply();
        }
    }
}
