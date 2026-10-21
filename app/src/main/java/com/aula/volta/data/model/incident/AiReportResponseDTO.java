package com.aula.volta.data.model.incident;

import com.google.gson.annotations.SerializedName;

/**
 * Relatório gerado pela IA para um incidente na API QA (GET /incidents/{id}/ai-report).
 */
public class AiReportResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("incidentId")
    private String incidentId;

    @SerializedName("detectedWasteType")
    private String detectedWasteType;

    @SerializedName("aiContaminationLevel")
    private String aiContaminationLevel;

    @SerializedName("recommendations")
    private String recommendations;

    @SerializedName("reportText")
    private String reportText;

    @SerializedName("generatedAt")
    private String generatedAt;

    public AiReportResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getDetectedWasteType() {
        return detectedWasteType;
    }

    public void setDetectedWasteType(String detectedWasteType) {
        this.detectedWasteType = detectedWasteType;
    }

    public String getAiContaminationLevel() {
        return aiContaminationLevel;
    }

    public void setAiContaminationLevel(String aiContaminationLevel) {
        this.aiContaminationLevel = aiContaminationLevel;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getReportText() {
        return reportText;
    }

    public void setReportText(String reportText) {
        this.reportText = reportText;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
