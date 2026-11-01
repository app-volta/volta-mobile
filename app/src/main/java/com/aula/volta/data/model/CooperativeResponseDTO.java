package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

public class CooperativeResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("cnpj")
    private String cnpj;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("averageRating")
    private Double averageRating;

    @SerializedName("specialties")
    private com.google.gson.JsonElement specialties;

    public CooperativeResponseDTO() {
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

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public String getSpecialties() {
        if (specialties == null || specialties.isJsonNull()) {
            return "Resíduos Recicláveis";
        }
        if (specialties.isJsonPrimitive()) {
            return formatSpecialty(specialties.getAsString());
        }
        if (specialties.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (com.google.gson.JsonElement el : specialties.getAsJsonArray()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(formatSpecialty(el.getAsString()));
            }
            return sb.toString();
        }
        return specialties.toString();
    }

    private static String formatSpecialty(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Resíduos";
        String upper = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (upper.contains("PAPEL")) return "Papelão";
        if (upper.contains("PLAST")) return "Plástico";
        if (upper.contains("MET")) return "Metal";
        if (upper.contains("VIDR")) return "Vidro";
        if (upper.contains("MADEIR")) return "Madeira";
        if (upper.contains("ORGAN")) return "Orgânico";
        if (upper.contains("ELETR")) return "Eletrônico";
        String val = raw.replace("_", " ").toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(val.charAt(0)) + val.substring(1);
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties != null ? new com.google.gson.JsonPrimitive(specialties) : null;
    }
}
