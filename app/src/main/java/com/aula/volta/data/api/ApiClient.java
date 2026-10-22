package com.aula.volta.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aula.volta.data.local.SessionManager;
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
 * Ponto único de acesso à API do VOLTA conectada ao Banco de Dados Oficial:
 * https://api.54.210.1.34.sslip.io/
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";

    /** Define se as requisições forçam mock local ou utilizam a API real do banco de dados */
    public static boolean USE_MOCK = false;

    /** Base URL do mock local */
    private static final String BASE_URL_MOCK = "https://volta.mock/";

    /** Base URL do backend oficial conectado ao banco de dados */
    public static String BASE_URL_REAL = "https://api.qa.54.210.1.34.sslip.io/";

    // Credenciais oficiais e Tenant ID fornecidos pelo ambiente de banco
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
                    .connectTimeout(3500, TimeUnit.MILLISECONDS)
                    .readTimeout(4000, TimeUnit.MILLISECONDS)
                    .writeTimeout(4000, TimeUnit.MILLISECONDS);

            if (USE_MOCK) {
                http.addInterceptor(new MockInterceptor(appContext));
            } else {
                // Interceptor de Autenticação JWT, Tenant Header e Resiliência com Fallback Offline
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
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        builder.header("Host", "api.qa.54.210.1.34.sslip.io");
                        builder.header("X-Company-Id", ADMIN_COMPANY_ID);
                        builder.header("Accept", "application/json; charset=utf-8");

                        try {
                            Response response = chain.proceed(builder.build());
                            // Se a requisição deu 401 e não é login, tenta renovar o token JWT e repetir imediatamente
                            if (response.code() == 401 && !original.url().encodedPath().contains("/auth/login")) {
                                response.close();
                                String refreshed = ChatbotAuthClient.refreshQaTokenBlocking(appContext);
                                if (refreshed != null && !refreshed.isEmpty()) {
                                    Request retry = original.newBuilder()
                                            .header("Authorization", "Bearer " + refreshed)
                                            .header("Host", "api.qa.54.210.1.34.sslip.io")
                                            .header("X-Company-Id", ADMIN_COMPANY_ID)
                                            .header("Accept", "application/json; charset=utf-8")
                                            .build();
                                    Response retryResp = chain.proceed(retry);
                                    if (retryResp.code() < 500) {
                                        return retryResp;
                                    }
                                    response = retryResp;
                                }
                            }
                            // Se a API do banco respondeu (2xx, 3xx, 4xx cliente), retorna a resposta real
                            if (response.code() < 500) {
                                return response;
                            }
                            // Em caso de erro 5xx no servidor em rotas com mock disponível, fallback transparente
                            Log.w(TAG, "Servidor retornou " + response.code() + ", verificando fallback...");
                            Response fallback = new MockInterceptor(appContext).intercept(chain);
                            if (fallback != null && fallback.code() == 200) {
                                response.close();
                                return fallback;
                            }
                            return response;
                        } catch (IOException e) {
                            Log.w(TAG, "Falha de conexão com a API do banco (" + original.url() + "), usando fallback offline: " + e.getMessage());
                            Response fallback = new MockInterceptor(appContext).intercept(chain);
                            if (fallback != null) {
                                return fallback;
                            }
                            throw e;
                        }
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
