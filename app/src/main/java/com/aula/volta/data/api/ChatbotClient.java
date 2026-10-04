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
 * Cliente HTTP dedicado para o Chatbot VOLTA no ambiente QA (https://chat.qa.54.210.1.34.sslip.io).
 *
 * Características:
 * - Cliente separado da API principal de backend
 * - Injeção automática do header "Authorization: Bearer <TOKEN>"
 * - Renovação automática transparente de token JWT via POST {api}/auth/login ao receber HTTP 401
 * - Força charset UTF-8 e Content-Type JSON
 * - Timeouts calibrados para chamadas generativas de IA (45 segundos)
 */
public final class ChatbotClient {

    private static final String TAG = "ChatbotClient";
    public static final String CHATBOT_BASE_URL = "https://chat.qa.54.210.1.34.sslip.io/";

    private static Retrofit instance;

    private ChatbotClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            final Context appContext = context.getApplicationContext();
            final Gson gson = new GsonBuilder().create();

            OkHttpClient.Builder http = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    // Interceptor para injeção de Headers (Bearer Token + UTF-8)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder()
                                    .header("Accept", "application/json; charset=utf-8");

                            String token = SessionManager.getToken(appContext);
                            if (token != null && !token.trim().isEmpty()) {
                                builder.header("Authorization", "Bearer " + token.trim());
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    // Authenticator para renovação automática de login ao receber 401
                    .authenticator(new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
                            // Evita loop infinito se a reautenticação já falhou anteriormente
                            if (responseCount(response) >= 2) {
                                Log.w(TAG, "Tentativa de autenticação falhou mais de 2 vezes. Abortando renovação.");
                                return null;
                            }

                            String email = SessionManager.email(appContext);
                            String password = SessionManager.password(appContext);

                            if (email.isEmpty() || password.isEmpty()) {
                                Log.w(TAG, "Sem credenciais salvas no SessionManager para renovação do token.");
                                return null;
                            }

                            Log.i(TAG, "Token expirado (401). Tentando renovação de login via API...");
                            try {
                                AuthAPI authApi = ApiClient.get(appContext).create(AuthAPI.class);
                                retrofit2.Response<LoginResponse> loginCall =
                                        authApi.login(new LoginRequest(email, password)).execute();

                                if (loginCall.isSuccessful() && loginCall.body() != null && loginCall.body().hasToken()) {
                                    String newToken = loginCall.body().getToken();
                                    SessionManager.saveToken(appContext, newToken);
                                    Log.i(TAG, "Token renovado com sucesso!");

                                    return response.request().newBuilder()
                                            .header("Authorization", "Bearer " + newToken.trim())
                                            .build();
                                } else {
                                    Log.e(TAG, "Falha na renovação do token: " + loginCall.code());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erro durante renovação do token", e);
                            }

                            return null;
                        }
                    });

            instance = new Retrofit.Builder()
                    .baseUrl(CHATBOT_BASE_URL)
                    .client(http.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    /** Permite resetar o client para testes ou logout. */
    public static synchronized void reset() {
        instance = null;
    }
}
