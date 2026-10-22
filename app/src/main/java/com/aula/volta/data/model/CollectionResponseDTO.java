package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de resposta de coleta na API VOLTA (GET /collections e POST /collections).
 */
public class CollectionResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("incidentId")
    private String incidentId;

    @SerializedName("cooperativeId")
    private String cooperativeId;

    @SerializedName("requestedAt")
    private String requestedAt;

    @SerializedName("scheduledAt")
    private String scheduledAt;

    @SerializedName("currentStatus")
    private String currentStatus;

    @SerializedName("collectionType")
    private String collectionType;

    @SerializedName("urgent")
    private Boolean urgent;

    public CollectionResponseDTO() {
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

    public String getCooperativeId() {
        return cooperativeId;
    }

    public void setCooperativeId(String cooperativeId) {
        this.cooperativeId = cooperativeId;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }
}
