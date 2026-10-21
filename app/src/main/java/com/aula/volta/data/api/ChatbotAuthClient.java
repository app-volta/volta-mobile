package com.aula.volta.data.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente HTTP dedicado à API Core/Auth/IA no ambiente QA (https://api.qa.54.210.1.34.sslip.io).
 *
 * Responsabilidades:
 * - Autenticação JWT (POST /auth/login)
 * - Registro de Incidentes e Consulta de Relatório de IA (POST /incidents, GET /incidents/{id}/ai-report)
 * - Injeção automática de Authorization: Bearer <TOKEN>
 * - Renovação automática em caso de 401
 * - Mantém as demais telas operacionais do app isoladas em ApiClient
 */
public final class ChatbotAuthClient {

    private static final String TAG = "ChatbotAuthClient";
    public static final String CHATBOT_AUTH_BASE_URL = "https://api.qa.54.210.1.34.sslip.io/";

    private static Retrofit instance;

    private ChatbotAuthClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            final Context appContext = context.getApplicationContext();
            final Gson gson = new GsonBuilder().create();

            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder()
                                    .header("Accept", "application/json; charset=utf-8");

                            if (!original.url().encodedPath().contains("/auth/login")) {
                                String token = resolveToken(appContext);
                                if (token.isEmpty()) {
                                    String refreshed = refreshQaTokenBlocking(appContext);
                                    if (refreshed != null) {
                                        token = refreshed;
                                    }
                                }
                                if (!token.isEmpty()) {
                                    builder.header("Authorization", "Bearer " + token);
                                }
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    .authenticator(new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
                            if (response.request().url().encodedPath().contains("/auth/login")) {
                                return null;
                            }
                            if (responseCount(response) >= 2) {
                                return null;
                            }
                            String newToken = refreshQaTokenBlocking(appContext);
                            if (newToken != null && !newToken.isEmpty()) {
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newToken)
                                        .build();
                            }
                            return null;
                        }
                    })
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(CHATBOT_AUTH_BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    public static String resolveToken(Context appContext) {
        String token = SessionManager.getToken(appContext);
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }
        String apiToken = ApiClient.getToken(appContext);
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            return apiToken.trim();
        }
        return "";
    }

    /**
     * Renova o token JWT de forma síncrona usando as credenciais salvas no SessionManager
     * ou as credenciais de QA padrão do ambiente (EMPLOYEE_EMAIL com fallback para ADMIN_EMAIL).
     */
    @Nullable
    public static String refreshQaTokenBlocking(Context appContext) {
        String email = SessionManager.email(appContext);
        String password = SessionManager.password(appContext);

        if (email.isEmpty() || "breno@volta.com".equalsIgnoreCase(email) || email.contains("volta.mock")) {
            email = ApiClient.EMPLOYEE_EMAIL;
            password = ApiClient.EMPLOYEE_PASSWORD;
        } else if (password.isEmpty() || "1234".equals(password)) {
            password = ApiClient.EMPLOYEE_PASSWORD;
        }

        try {
            OkHttpClient plainHttp = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
            Retrofit authRetrofit = new Retrofit.Builder()
                    .baseUrl(CHATBOT_AUTH_BASE_URL)
                    .client(plainHttp)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            AuthAPI authApi = authRetrofit.create(AuthAPI.class);

            retrofit2.Response<LoginResponse> loginCall =
                    authApi.login(new LoginRequest(email, password)).execute();

            if (!loginCall.isSuccessful() || loginCall.body() == null || !loginCall.body().hasToken()) {
                loginCall = authApi.login(new LoginRequest(ApiClient.ADMIN_EMAIL, ApiClient.ADMIN_PASSWORD)).execute();
            }

            if (loginCall.isSuccessful() && loginCall.body() != null && loginCall.body().hasToken()) {
                String newToken = loginCall.body().getToken().trim();
                SessionManager.saveToken(appContext, newToken);
                ApiClient.saveToken(appContext, newToken);
                Log.i(TAG, "Token QA renovado com sucesso.");
                return newToken;
            }
        } catch (Exception e) {
            Log.w(TAG, "Falha ao renovar token QA: " + e.getMessage());
        }
        return null;
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    public static synchronized void reset() {
        instance = null;
    }
}
