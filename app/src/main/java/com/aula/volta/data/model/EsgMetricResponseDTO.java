package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

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

    @SerializedName("weeklyTarget")
    private Double weeklyTarget;

    @SerializedName("weeklyDate")
    private String weeklyDate;

    @SerializedName("monthlyTarget")
    private Double monthlyTarget;

    @SerializedName("monthlyDate")
    private String monthlyDate;

    @SerializedName("quarterlyTarget")
    private Double quarterlyTarget;

    @SerializedName("quarterlyDate")
    private String quarterlyDate;

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

    public Double getWeeklyTarget() {
        return weeklyTarget;
    }

    public void setWeeklyTarget(Double weeklyTarget) {
        this.weeklyTarget = weeklyTarget;
    }

    public String getWeeklyDate() {
        return weeklyDate;
    }

    public void setWeeklyDate(String weeklyDate) {
        this.weeklyDate = weeklyDate;
    }

    public Double getMonthlyTarget() {
        return monthlyTarget;
    }

    public void setMonthlyTarget(Double monthlyTarget) {
        this.monthlyTarget = monthlyTarget;
    }

    public String getMonthlyDate() {
        return monthlyDate;
    }

    public void setMonthlyDate(String monthlyDate) {
        this.monthlyDate = monthlyDate;
    }

    public Double getQuarterlyTarget() {
        return quarterlyTarget;
    }

    public void setQuarterlyTarget(Double quarterlyTarget) {
        this.quarterlyTarget = quarterlyTarget;
    }

    public String getQuarterlyDate() {
        return quarterlyDate;
    }

    public void setQuarterlyDate(String quarterlyDate) {
        this.quarterlyDate = quarterlyDate;
    }
}
