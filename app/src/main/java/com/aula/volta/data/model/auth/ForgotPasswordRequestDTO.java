package com.aula.volta.data.model.auth;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordRequestDTO {

    @SerializedName("email")
    private String email;

    public ForgotPasswordRequestDTO(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}