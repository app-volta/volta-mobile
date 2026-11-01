package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

public class CompanyResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("cnpj")
    private String cnpj;

    @SerializedName("address")
    private String address;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    public CompanyResponseDTO() {
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getFormattedCnpj() {
        return formatCnpj(cnpj);
    }

    public static String formatCnpj(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 14) {
            return digits.substring(0, 2) + "."
                    + digits.substring(2, 5) + "."
                    + digits.substring(5, 8) + "/"
                    + digits.substring(8, 12) + "-"
                    + digits.substring(12, 14);
        }
        return raw.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
