package com.aula.volta.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.aula.volta.data.mock.MockInterceptor;
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

/**
 * Ponto único de acesso à API do VOLTA.
 *
 * Suporta modo mock (para desenvolvimento e testes) e modo real (backend Spring Boot).
 * Quando o backend for ligado, basta alternar USE_MOCK para false e definir a BASE_URL_REAL.
 */
public final class ApiClient {

    /** Define se as requisições usam mock local ou backend real */
    public static boolean USE_MOCK = true;

    /** Base URL do mock local */
    private static final String BASE_URL_MOCK = "https://volta.mock/";

    /** Base URL do backend real (quando ligado, ex: servidor cloud ou http://10.0.2.2:8080/api/v1/) */
    public static String BASE_URL_REAL = "http://10.0.2.2:8080/api/v1/";

    // Credenciais oficiais e Tenant ID fornecidos pelo ambiente QA
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
            Gson gson = new GsonBuilder().create();
            OkHttpClient.Builder http = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            if (USE_MOCK) {
                http.addInterceptor(new MockInterceptor(context.getApplicationContext()));
            } else {
                // Interceptor de Autenticação JWT e Tenant
                http.addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        String token = getToken(context);
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        builder.header("X-Company-Id", ADMIN_COMPANY_ID);
                        builder.header("Accept", "application/json");

                        return chain.proceed(builder.build());
                    }
                });
            }

            String baseUrl = USE_MOCK ? BASE_URL_MOCK : BASE_URL_REAL;
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

    /** Permite reconstruir o client (ex.: após trocar USE_MOCK ou BASE_URL_REAL). */
    public static synchronized void reset() {
        instance = null;
    }

    public static synchronized void resetForTests() {
        instance = null;
    }
}
