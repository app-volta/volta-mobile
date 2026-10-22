package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de área/setor da empresa (GET /areas/mine e GET /areas).
 */
public class AreaResponseDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("companyId")
    private String companyId;

    @SerializedName("sectorName")
    private String sectorName;

    @SerializedName("locationDescription")
    private String locationDescription;

    public AreaResponseDTO() {
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

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }
}
