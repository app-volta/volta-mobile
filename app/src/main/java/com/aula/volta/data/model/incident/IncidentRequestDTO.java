package com.aula.volta.data.model.incident;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class IncidentRequestDTO {

    @SerializedName("areaId")
    private String areaId;

    @SerializedName("wasteTypeId")
    private String wasteTypeId;

    @SerializedName("wasteTypeIds")
    private List<String> wasteTypeIds;

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("title")
    private String title;

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
        setWasteTypeId(wasteTypeId);
        this.photoUrl = normalizePhotoUrl(photoUrl);
        this.employeeDescription = normalizeDescription(employeeDescription);
        this.title = this.employeeDescription;
        this.contaminationLevel = normalizeContamination(contaminationLevel);
        this.estimatedQuantity = normalizeQuantity(estimatedQuantity);
        this.priority = normalizePriority(priority);
    }

    public IncidentRequestDTO(String areaId, List<String> wasteTypeIds, String photoUrl,
                              String employeeDescription, String contaminationLevel,
                              Double estimatedQuantity, String priority) {
        this.areaId = areaId;
        this.wasteTypeIds = wasteTypeIds != null ? new ArrayList<>(wasteTypeIds) : new ArrayList<>();
        this.wasteTypeId = (!this.wasteTypeIds.isEmpty()) ? this.wasteTypeIds.get(0) : null;
        this.photoUrl = normalizePhotoUrl(photoUrl);
        this.employeeDescription = normalizeDescription(employeeDescription);
        this.title = this.employeeDescription;
        this.contaminationLevel = normalizeContamination(contaminationLevel);
        this.estimatedQuantity = normalizeQuantity(estimatedQuantity);
        this.priority = normalizePriority(priority);
    }

    public static String normalizePriority(String p) {
        if (p == null) {
            return "MEDIUM";
        }
        String upper = p.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("BAIX") || "LOW".equals(upper)) {
            return "LOW";
        }
        if (upper.contains("MÉD") || upper.contains("MED") || "MEDIUM".equals(upper)) {
            return "MEDIUM";
        }
        if (upper.contains("ALT") || "HIGH".equals(upper)) {
            return "HIGH";
        }
        if (upper.contains("CRÍT") || upper.contains("CRIT") || "CRITICAL".equals(upper)) {
            return "CRITICAL";
        }
        return "MEDIUM";
    }

    public static String normalizeContamination(String c) {
        if (c == null) {
            return "LOW";
        }
        String upper = c.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("ALT") || "HIGH".equals(upper)) {
            return "HIGH";
        }
        if (upper.contains("MÉD") || upper.contains("MED") || "MEDIUM".equals(upper)) {
            return "MEDIUM";
        }
        if (upper.contains("BAIX") || "LOW".equals(upper)) {
            return "LOW";
        }
        return "LOW";
    }

    public static String normalizePhotoUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
            return null;
        }
        return trimmed;
    }

    public static String normalizeDescription(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return "Ocorrência registrada no app";
        }
        return desc.trim();
    }

    public static Double normalizeQuantity(Double qty) {
        if (qty == null || qty <= 0.0) {
            return 5.0;
        }
        return Math.round(qty * 100.0) / 100.0;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getWasteTypeId() {
        if (wasteTypeId != null && !wasteTypeId.isEmpty()) {
            return wasteTypeId;
        }
        return (wasteTypeIds != null && !wasteTypeIds.isEmpty()) ? wasteTypeIds.get(0) : null;
    }

    public void setWasteTypeId(String wasteTypeId) {
        this.wasteTypeId = wasteTypeId != null ? wasteTypeId.trim() : null;
        if (this.wasteTypeId != null && !this.wasteTypeId.isEmpty()) {
            this.wasteTypeIds = new ArrayList<>(Collections.singletonList(this.wasteTypeId));
        } else {
            this.wasteTypeIds = new ArrayList<>();
        }
    }

    public List<String> getWasteTypeIds() {
        return wasteTypeIds;
    }

    public void setWasteTypeIds(List<String> wasteTypeIds) {
        this.wasteTypeIds = wasteTypeIds != null ? new ArrayList<>(wasteTypeIds) : new ArrayList<>();
        if (!this.wasteTypeIds.isEmpty()) {
            this.wasteTypeId = this.wasteTypeIds.get(0);
        }
    }

    public String getTitle() {
        return title != null ? title : employeeDescription;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
        if (this.employeeDescription == null || this.employeeDescription.isEmpty()) {
            this.employeeDescription = this.title;
        }
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = normalizePhotoUrl(photoUrl);
    }

    public String getEmployeeDescription() {
        return employeeDescription;
    }

    public void setEmployeeDescription(String employeeDescription) {
        this.employeeDescription = normalizeDescription(employeeDescription);
    }

    public String getContaminationLevel() {
        return contaminationLevel;
    }

    public void setContaminationLevel(String contaminationLevel) {
        this.contaminationLevel = normalizeContamination(contaminationLevel);
    }

    public Double getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public void setEstimatedQuantity(Double estimatedQuantity) {
        this.estimatedQuantity = normalizeQuantity(estimatedQuantity);
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = normalizePriority(priority);
    }
}
