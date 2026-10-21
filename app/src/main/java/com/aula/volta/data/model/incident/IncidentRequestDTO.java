package com.aula.volta.data.model.incident;

import com.google.gson.annotations.SerializedName;

/**
 * Dados para registro de incidente na API QA (POST /incidents).
 * O campo photoUrl recebe a secure_url gerada no Cloudinary após clicar em "Enviar para análise".
 */
public class IncidentRequestDTO {

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

    public IncidentRequestDTO() {
    }

    public IncidentRequestDTO(String areaId, String wasteTypeId, String photoUrl,
                              String employeeDescription, String contaminationLevel,
                              Double estimatedQuantity, String priority) {
        this.areaId = areaId;
        this.wasteTypeId = wasteTypeId;
        this.photoUrl = photoUrl;
        this.employeeDescription = employeeDescription;
        this.contaminationLevel = contaminationLevel;
        this.estimatedQuantity = estimatedQuantity;
        this.priority = priority;
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
}
