package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

public class CooperativeJSON {

    private String id;
    private String nome;
    private String descricao;

    @SerializedName("distancia_km")
    private double distanciaKm;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    private String coleta;
    private String tag;
    private Double avaliacao;
    private int compatibilidade;
    private List<String> materiais;
    private String disponibilidade;

    public CooperativeJSON() {
    }

    public CooperativeJSON(String id, String nome, String descricao, double distanciaKm,
                           String coleta, String tag, Double avaliacao, int compatibilidade,
                           List<String> materiais, String disponibilidade) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.distanciaKm = distanciaKm;
        this.coleta = coleta;
        this.tag = tag;
        this.avaliacao = avaliacao;
        this.compatibilidade = compatibilidade;
        this.materiais = materiais;
        this.disponibilidade = disponibilidade;
    }

    public static CooperativeJSON fromDTO(CooperativeResponseDTO dto) {
        return fromDTO(dto, -23.5320, -46.6850);
    }

    public static CooperativeJSON fromDTO(CooperativeResponseDTO dto, double userLat, double userLng) {
        if (dto == null) {
            return null;
        }
        String id = dto.getId() != null ? dto.getId() : "";
        String name = dto.getName() != null ? dto.getName() : "Cooperativa";
        String specs = dto.getSpecialties() != null ? dto.getSpecialties() : "";
        List<String> mats = (!specs.isEmpty())
                ? Arrays.asList(specs.split(",\\s*"))
                : java.util.Collections.emptyList();

        double dist = 0.0;
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            dist = calculateDistanceKm(userLat, userLng, dto.getLatitude(), dto.getLongitude());
        }

        CooperativeJSON coop = new CooperativeJSON(
                id,
                name,
                specs.isEmpty() ? "" : ("Recebe: " + specs),
                dist,
                null,
                null,
                null,
                0,
                mats,
                null
        );
        coop.setLatitude(dto.getLatitude());
        coop.setLongitude(dto.getLongitude());
        return coop;
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double km = 6371.0 * c;
        return Math.round(km * 10.0) / 10.0;
    }

    public String getId() {
        return id != null ? id : (nome != null ? String.valueOf(Math.abs(nome.hashCode())) : "");
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getColeta() {
        return coleta;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Double getAvaliacao() {
        return avaliacao;
    }

    public int getCompatibilidade() {
        return compatibilidade;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public List<String> getMateriais() {
        return materiais;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
