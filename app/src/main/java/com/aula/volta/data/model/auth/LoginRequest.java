package com.aula.volta.data.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Payload de login para a API de autenticação do VOLTA (POST /auth/login).
 */
public class LoginRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
