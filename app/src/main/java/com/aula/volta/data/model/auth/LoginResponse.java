package com.aula.volta.data.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Resposta de sucesso ou erro da API de autenticação do VOLTA (POST /auth/login).
 */
public class LoginResponse {

    @SerializedName(value = "token", alternate = {"access_token", "jwt", "authToken"})
    private String token;

    @SerializedName(value = "name", alternate = {"userName", "user_name"})
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName(value = "message", alternate = {"msg", "detail", "error"})
    private String message;

    public LoginResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasToken() {
        return token != null && !token.trim().isEmpty();
    }
}
