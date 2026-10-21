package com.aula.volta.data.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Endpoint de Autenticação JWT do VOLTA.
 */
public interface AuthAPI {

    @POST("auth/login")
    Call<JsonObject> login(@Body JsonObject credentials);

    @POST("auth/refresh")
    Call<JsonObject> refreshToken(@Body JsonObject refreshTokenBody);
}
