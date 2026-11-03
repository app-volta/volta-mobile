package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

public class AreaResponseDTO {

    @SerializedName(value = "id", alternate = {"areaId", "area_id"})
    private String id;

    @SerializedName(value = "companyId", alternate = {"company_id"})
    private String companyId;

    @SerializedName(value = "sectorName", alternate = {"name", "sector", "nome", "setor"})
    private String sectorName;

    @SerializedName(value = "locationDescription", alternate = {"location", "description", "descricao"})
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
