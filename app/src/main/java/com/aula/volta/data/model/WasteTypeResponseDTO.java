package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de tipo de resíduo (GET /waste-types).
 */
public class WasteTypeResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("defaultRiskLevel")
    private String defaultRiskLevel;

    @SerializedName("color")
    private String color;

    @SerializedName("wasteClass")
    private String wasteClass;

    public WasteTypeResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultRiskLevel() {
        return defaultRiskLevel;
    }

    public void setDefaultRiskLevel(String defaultRiskLevel) {
        this.defaultRiskLevel = defaultRiskLevel;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWasteClass() {
        return wasteClass;
    }

    public void setWasteClass(String wasteClass) {
        this.wasteClass = wasteClass;
    }
}
