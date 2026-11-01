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
                    .connectTimeout(15000, TimeUnit.MILLISECONDS)
                    .readTimeout(20000, TimeUnit.MILLISECONDS)
                    .writeTimeout(20000, TimeUnit.MILLISECONDS)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder()
                                    .header("Host", "api.qa.54.210.1.34.sslip.io")
                                    .header("Accept", "application/json; charset=utf-8");

                            String companyId = SessionManager.getCompanyId(appContext);
                            if (companyId != null && !companyId.isEmpty()) {
                                builder.header("X-Company-Id", companyId);
                            }

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

                            Response response = chain.proceed(builder.build());
                            if (response.code() == 401 && !original.url().encodedPath().contains("/auth/login")) {
                                response.close();
                                String refreshed = refreshQaTokenBlocking(appContext);
                                if (refreshed != null && !refreshed.isEmpty()) {
                                    Request.Builder retryBuilder = original.newBuilder()
                                            .header("Host", "api.qa.54.210.1.34.sslip.io")
                                            .header("Accept", "application/json; charset=utf-8")
                                            .header("Authorization", "Bearer " + refreshed);
                                    String retryCompanyId = SessionManager.getCompanyId(appContext);
                                    if (retryCompanyId != null && !retryCompanyId.isEmpty()) {
                                        retryBuilder.header("X-Company-Id", retryCompanyId);
                                    }
                                    return chain.proceed(retryBuilder.build());
                                }
                            }
                            return response;
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
                                Request.Builder rb = response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newToken);
                                String cid = SessionManager.getCompanyId(appContext);
                                if (cid != null && !cid.isEmpty()) {
                                    rb.header("X-Company-Id", cid);
                                }
                                return rb.build();
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

    @Nullable
    public static synchronized String refreshQaTokenBlocking(Context appContext) {
        String email = SessionManager.email(appContext);
        String password = SessionManager.password(appContext);

        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            return null;
        }

        try {
            OkHttpClient plainHttp = new OkHttpClient.Builder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS)
                    .readTimeout(20000, TimeUnit.MILLISECONDS)
                    .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                            .header("Host", "api.qa.54.210.1.34.sslip.io")
                            .header("Accept", "application/json; charset=utf-8")
                            .build()))
                    .build();
            Retrofit authRetrofit = new Retrofit.Builder()
                    .baseUrl(CHATBOT_AUTH_BASE_URL)
                    .client(plainHttp)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            AuthAPI authApi = authRetrofit.create(AuthAPI.class);

            retrofit2.Response<LoginResponse> loginCall =
                    authApi.login(new LoginRequest(email.trim(), password)).execute();

            if (loginCall.isSuccessful() && loginCall.body() != null && loginCall.body().hasToken()) {
                String newToken = loginCall.body().getToken().trim();
                SessionManager.saveToken(appContext, newToken);
                ApiClient.saveToken(appContext, newToken);
                Log.i(TAG, "Token JWT renovado com sucesso.");
                return newToken;
            }
        } catch (Exception e) {
            Log.w(TAG, "Falha ao renovar token JWT: " + e.getMessage());
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
