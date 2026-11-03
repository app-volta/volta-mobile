package com.aula.volta.data.model.auth;

import com.google.gson.annotations.SerializedName;

public class VerifyCodeRequestDTO {

    @SerializedName("email")
    private String email;

    @SerializedName("code")
    private String code;

    public VerifyCodeRequestDTO(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}