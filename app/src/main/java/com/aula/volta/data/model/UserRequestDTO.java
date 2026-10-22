package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para cadastro de usuário na API VOLTA (POST /users).
 */
public class UserRequestDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("position")
    private String position;

    @SerializedName("companyId")
    private String companyId;

    public UserRequestDTO() {
    }

    public UserRequestDTO(String name, String email, String password, String position, String companyId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.position = position;
        this.companyId = companyId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
}
