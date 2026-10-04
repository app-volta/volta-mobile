package com.aula.volta.data.api;

import android.content.Context;

import com.aula.volta.data.mock.MockInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Ponto único de acesso à API principal do app (Fase 0).
 *
 * <p>Mantém as operações do app (Home, Ocorrências, Cooperativas, Relatórios)
 * isoladas e funcionais, sem interferência de serviços externos.</p>
 */
public final class ApiClient {

    /** Mock ligado para as APIs operacionais do app. */
    public static final boolean USE_MOCK = true;

    /** Base URL mock para as operações do app. */
    private static final String BASE_URL_MOCK = "https://volta.mock/";

    private static Retrofit instance;

    private ApiClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            Gson gson = new GsonBuilder().create();

            OkHttpClient.Builder http = new OkHttpClient.Builder();
            if (USE_MOCK) {
                http.addInterceptor(new MockInterceptor(context.getApplicationContext()));
            }

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL_MOCK)
                    .client(http.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    /** Apenas para testes: permite reconstruir o client. */
    public static synchronized void resetForTests() {
        instance = null;
    }
}
