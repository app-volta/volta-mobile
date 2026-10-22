package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para cálculo de percentual de reciclagem (POST /esg/recycling-percentage).
 */
public class RecyclingPercentageResponseDTO {

    @SerializedName("totalWasteKg")
    private Double totalWasteKg;

    @SerializedName("totalRecycledKg")
    private Double totalRecycledKg;

    @SerializedName("recyclingPercentage")
    private Double recyclingPercentage;

    public RecyclingPercentageResponseDTO() {
    }

    public Double getTotalWasteKg() {
        return totalWasteKg;
    }

    public void setTotalWasteKg(Double totalWasteKg) {
        this.totalWasteKg = totalWasteKg;
    }

    public Double getTotalRecycledKg() {
        return totalRecycledKg;
    }

    public void setTotalRecycledKg(Double totalRecycledKg) {
        this.totalRecycledKg = totalRecycledKg;
    }

    public Double getRecyclingPercentage() {
        return recyclingPercentage;
    }

    public void setRecyclingPercentage(Double recyclingPercentage) {
        this.recyclingPercentage = recyclingPercentage;
    }
}
