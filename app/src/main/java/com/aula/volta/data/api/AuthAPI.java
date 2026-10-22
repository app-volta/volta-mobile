package com.aula.volta.data.api;

import com.aula.volta.data.model.UserRequestDTO;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * Endpoints de autenticação e gestão de usuários da API VOLTA (https://api.54.210.1.34.sslip.io).
 */
public interface AuthAPI {

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<JsonObject> refreshToken(@Body JsonObject refreshTokenBody);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("users/me")
    Call<UserResponseDTO> getMe();

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PUT("users/me")
    Call<UserResponseDTO> updateMe(@Body JsonObject body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("users")
    Call<UserResponseDTO> createUser(@Body UserRequestDTO request);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("users")
    Call<List<UserResponseDTO>> getUsers();
}
