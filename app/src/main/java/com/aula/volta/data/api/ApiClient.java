package com.aula.volta.data.api;

import android.content.Context;

import com.aula.volta.data.mock.MockInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Ponto único de acesso à API (Fase 0).
 *
 * <p>Mock agora, backend depois — a troca é feita em 1 ponto:
 * {@code USE_MOCK = false} + base URL real. Os Fragments chamam Retrofit
 * do mesmo jeito nos dois mundos (padrão CarrinhoDeCompras).</p>
 */
public final class ApiClient {

    /** Mock ligado: respostas vêm de data/mock (assets). Desligar na Fase 13. */
    public static final boolean USE_MOCK = true;

    /** Base URL mock (nunca chamada de verdade com USE_MOCK=true). Trocar na Fase 13. */
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

    /** Apenas para testes: permite reconstruir o client (ex.: após trocar USE_MOCK). */
    static synchronized void resetForTests() {
        instance = null;
    }
}
