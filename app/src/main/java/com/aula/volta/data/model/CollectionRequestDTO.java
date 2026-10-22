package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para solicitar uma coleta na API VOLTA (POST /collections).
 */
public class CollectionRequestDTO {

    @SerializedName("incidentId")
    private String incidentId;

    @SerializedName("cooperativeId")
    private String cooperativeId;

    @SerializedName("collectionType")
    private String collectionType;

    @SerializedName("urgent")
    private Boolean urgent;

    public CollectionRequestDTO() {
    }

    public CollectionRequestDTO(String incidentId, String cooperativeId, String collectionType, Boolean urgent) {
        this.incidentId = incidentId;
        this.cooperativeId = cooperativeId;
        this.collectionType = collectionType;
        this.urgent = urgent;
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
