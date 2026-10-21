package com.aula.volta.data.model.incident;

import com.google.gson.annotations.SerializedName;

/**
 * Resposta de incidente registrado na API QA (POST /incidents e GET /incidents/{id}).
 */
public class IncidentResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("companyId")
    private String companyId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("areaId")
    private String areaId;

    @SerializedName("wasteTypeId")
    private String wasteTypeId;

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("employeeDescription")
    private String employeeDescription;

    @SerializedName("contaminationLevel")
    private String contaminationLevel;

    @SerializedName("estimatedQuantity")
    private Double estimatedQuantity;

    @SerializedName("priority")
    private String priority;

    @SerializedName("status")
    private String status;

    @SerializedName("registeredAt")
    private String registeredAt;

    public IncidentResponseDTO() {
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getWasteTypeId() {
        return wasteTypeId;
    }

    public void setWasteTypeId(String wasteTypeId) {
        this.wasteTypeId = wasteTypeId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getEmployeeDescription() {
        return employeeDescription;
    }

    public void setEmployeeDescription(String employeeDescription) {
        this.employeeDescription = employeeDescription;
    }

    public String getContaminationLevel() {
        return contaminationLevel;
    }

    public void setContaminationLevel(String contaminationLevel) {
        this.contaminationLevel = contaminationLevel;
    }

    public Double getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public void setEstimatedQuantity(Double estimatedQuantity) {
        this.estimatedQuantity = estimatedQuantity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }
}
