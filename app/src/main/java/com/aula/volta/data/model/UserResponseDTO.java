package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de resposta de perfil de usuário na API VOLTA (GET /users/me e GET /users/{id}).
 */
public class UserResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("position")
    private String position;

    @SerializedName("companyId")
    private String companyId;

    @SerializedName("role")
    private String role;

    @SerializedName(value = "photoUrl", alternate = {
            "avatarUrl", "imageUrl", "foto_url", "photo_url", "foto", "picture", "pictureUrl",
            "profile_photo", "profilePhoto", "image_url", "avatar_url", "urlFoto", "url_foto"
    })
    private String photoUrl;

    public UserResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
