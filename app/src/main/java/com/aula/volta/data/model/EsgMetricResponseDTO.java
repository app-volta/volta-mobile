package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de métrica ESG mensal (GET /esg/metrics e POST /esg/metrics).
 */
public class EsgMetricResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("companyId")
    private String companyId;

    @SerializedName("period")
    private String period;

    @SerializedName("totalWasteKg")
    private Double totalWasteKg;

    @SerializedName("totalRecycledKg")
    private Double totalRecycledKg;

    @SerializedName("recyclingPercentage")
    private Double recyclingPercentage;

    @SerializedName("calculatedAt")
    private String calculatedAt;

    public EsgMetricResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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

    public String getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(String calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
