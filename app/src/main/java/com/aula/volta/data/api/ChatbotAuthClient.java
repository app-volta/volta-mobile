package com.aula.volta.data.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente HTTP exclusivo para autenticação na API do Chatbot VOLTA (https://api.qa.54.210.1.34.sslip.io).
 * Mantém o serviço de autenticação do chatbot completamente isolado do resto do aplicativo.
 */
public final class ChatbotAuthClient {

    public static final String CHATBOT_AUTH_BASE_URL = "https://api.qa.54.210.1.34.sslip.io/";

    private static Retrofit instance;

    private ChatbotAuthClient() {
    }

    public static synchronized Retrofit get(Context context) {
        if (instance == null) {
            Gson gson = new GsonBuilder().create();

            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(CHATBOT_AUTH_BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }
}
