package com.aula.volta.data.api;

import com.aula.volta.data.model.auth.LoginRequest;
import com.aula.volta.data.model.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Endpoints de autenticação da API VOLTA (QA: https://api.qa.54.210.1.34.sslip.io).
 */
public interface AuthAPI {

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
