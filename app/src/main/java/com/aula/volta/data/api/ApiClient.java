package com.aula.volta.data.api;

import android.content.Context;

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
 * Cliente HTTP principal para a API VOLTA (QA: https://api.qa.54.210.1.34.sslip.io).
 *
 * Suporta autenticação JWT, chamadas com charset UTF-8 e cliente OkHttp dedicado.
 */
public final class ApiClient {

    /** Base URL oficial do ambiente de QA para a API VOLTA. */
    public static final String API_BASE_URL = "https://api.qa.54.210.1.34.sslip.io/";

    /** Desativado para uso real no ambiente QA. */
    public static boolean USE_MOCK = false;

    private static Retrofit instance;

    private ApiClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            final Context appContext = context.getApplicationContext();
            final Gson gson = new GsonBuilder().create();

            OkHttpClient.Builder http = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            if (USE_MOCK) {
                http.addInterceptor(new MockInterceptor(appContext));
            } else {
                http.addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json; charset=utf-8");

                        String token = SessionManager.getToken(appContext);
                        if (token != null && !token.trim().isEmpty() && !original.url().encodedPath().contains("/auth/login")) {
                            builder.header("Authorization", "Bearer " + token.trim());
                        }

                        return chain.proceed(builder.build());
                    }
                });
            }

            instance = new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .client(http.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    /** Permite reconstruir o client (ex.: em testes ou troca de ambiente). */
    public static synchronized void resetForTests() {
        instance = null;
    }
}
