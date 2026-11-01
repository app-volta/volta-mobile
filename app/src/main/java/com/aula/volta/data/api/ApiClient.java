package com.aula.volta.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.aula.volta.data.local.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String TAG = "ApiClient";

    public static String BASE_URL_REAL = "https://api.qa.54.210.1.34.sslip.io/";

    public static final String ADMIN_COMPANY_ID = "24032a1d-3e08-4f2f-ad50-590446459df3";
    public static final String ADMIN_EMAIL = "volta.brasil.2026@gmail.com";
    public static final String ADMIN_PASSWORD = "Volta@2026";
    public static final String EMPLOYEE_EMAIL = "breno@volta.com";
    public static final String EMPLOYEE_PASSWORD = "Volta@2026";
    public static final String SWAGGER_USERNAME = "volta-dev";
    public static final String SWAGGER_PASSWORD = "Volta@2026";

    private static final String PREF_NAME = "volta_auth";
    private static final String PREF_TOKEN = "jwt_token";

    private static Retrofit instance;

    private ApiClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            final Context appContext = context.getApplicationContext();
            Gson gson = new GsonBuilder().create();
            OkHttpClient.Builder http = new OkHttpClient.Builder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS)
                    .readTimeout(25000, TimeUnit.MILLISECONDS)
                    .writeTimeout(20000, TimeUnit.MILLISECONDS);

            http.addInterceptor(new Interceptor() {
                @NonNull
                @Override
                public Response intercept(@NonNull Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder();

                    String token = getToken(appContext);
                    if ((token == null || token.isEmpty()) && SessionManager.hasToken(appContext)) {
                        token = SessionManager.getToken(appContext);
                    }
                    if ((token == null || token.isEmpty()) && !original.url().encodedPath().contains("/auth/login")) {
                        String refreshed = ChatbotAuthClient.refreshQaTokenBlocking(appContext);
                        if (refreshed != null && !refreshed.isEmpty()) {
                            token = refreshed;
                        }
                    }
                    if (token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    builder.header("Host", "api.qa.54.210.1.34.sslip.io");
                    String userEmail = SessionManager.email(appContext);
                    boolean isBrenoOrAdmin = userEmail == null || userEmail.isEmpty()
                            || ADMIN_EMAIL.equalsIgnoreCase(userEmail)
                            || EMPLOYEE_EMAIL.equalsIgnoreCase(userEmail);
                    if (isBrenoOrAdmin) {
                        builder.header("X-Company-Id", ADMIN_COMPANY_ID);
                    }
                    builder.header("Accept", "application/json; charset=utf-8");

                    Response response = chain.proceed(builder.build());
                    if (response.code() == 401 && !original.url().encodedPath().contains("/auth/login")) {
                        response.close();
                        String refreshed = ChatbotAuthClient.refreshQaTokenBlocking(appContext);
                        if (refreshed != null && !refreshed.isEmpty()) {
                            Request.Builder retryBuilder = original.newBuilder()
                                    .header("Authorization", "Bearer " + refreshed)
                                    .header("Host", "api.qa.54.210.1.34.sslip.io")
                                    .header("Accept", "application/json; charset=utf-8");
                            if (isBrenoOrAdmin) {
                                retryBuilder.header("X-Company-Id", ADMIN_COMPANY_ID);
                            }
                            Response retryResp = chain.proceed(retryBuilder.build());
                            if (retryResp.code() < 500) {
                                return retryResp;
                            }
                            response = retryResp;
                        }
                    }
                    return response;
                }
            });

            String baseUrl = BASE_URL_REAL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            instance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(http.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_TOKEN, null);
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(PREF_TOKEN).apply();
    }

    public static synchronized void reset() {
        instance = null;
    }
}
