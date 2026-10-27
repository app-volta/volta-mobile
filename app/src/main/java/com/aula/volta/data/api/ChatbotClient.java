package com.aula.volta.data.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 * Cliente HTTP dedicado para o Chatbot VOLTA e Servico de Visao por IA no ambiente QA (https://chat.qa.54.210.1.34.sslip.io).
 *
 * Características:
 * - Cliente separado da API principal de backend
 * - Injeção automática do header "Authorization: Bearer <TOKEN>"
 * - Renovação automática transparente de token JWT via POST {api}/auth/login ao receber HTTP 401
 * - Força charset UTF-8 e Accept JSON
 * - Timeouts calibrados para chamadas generativas e análise de visão de IA (Gemini Vision)
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
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    // Interceptor para injeção de Headers (Host + Bearer Token + UTF-8)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder()
                                    .header("Host", "chat.qa.54.210.1.34.sslip.io")
                                    .header("Accept", "application/json; charset=utf-8");

                            String token = ChatbotAuthClient.resolveToken(appContext);
                            if (token.isEmpty()) {
                                String refreshed = ChatbotAuthClient.refreshQaTokenBlocking(appContext);
                                if (refreshed != null) {
                                    token = refreshed;
                                }
                            }
                            if (!token.isEmpty()) {
                                builder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    // Authenticator para renovação automática de login ao receber 401
                    .authenticator(new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
                            if (responseCount(response) >= 2) {
                                Log.w(TAG, "Tentativa de autenticação falhou mais de 2 vezes. Abortando renovação.");
                                return null;
                            }

                            Log.i(TAG, "Token expirado (401). Tentando renovação de login via API QA...");
                            String newToken = ChatbotAuthClient.refreshQaTokenBlocking(appContext);
                            if (newToken != null && !newToken.isEmpty()) {
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newToken)
                                        .build();
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
