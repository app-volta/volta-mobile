package com.aula.volta.data.api;

import com.aula.volta.data.model.UserRequestDTO;
import com.aula.volta.data.model.UserResponseDTO;
import com.aula.volta.data.model.auth.ChangePasswordRequestDTO;
import com.aula.volta.data.model.auth.ForgotPasswordRequestDTO;
import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;
import com.aula.volta.data.model.auth.ResetPasswordRequestDTO;
import com.aula.volta.data.model.auth.VerifyCodeRequestDTO;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

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
    @PUT("users/me/password")
    Call<Void> changePassword(@Body ChangePasswordRequestDTO request);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("auth/forgot-password")
    Call<JsonObject> forgotPassword(@Body ForgotPasswordRequestDTO request);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("auth/verify-code")
    Call<Void> verifyCode(@Body VerifyCodeRequestDTO request);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequestDTO request);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("users")
    Call<UserResponseDTO> createUser(@Body UserRequestDTO request);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("users")
    Call<List<UserResponseDTO>> getUsers();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("companies/me")
    Call<com.aula.volta.data.model.CompanyResponseDTO> getMyCompany();
}