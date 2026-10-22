package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

/**
 * Modelo de cooperativa para a UI do fluxo de seleção e destinação.
 * Suporta tanto o JSON legado quanto a conversão de CooperativeResponseDTO do banco de dados oficial.
 */
public class CooperativeJSON {

    private String id;
    private String nome;
    private String descricao;

    @SerializedName("distancia_km")
    private double distanciaKm;

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
        if (dto == null) {
            return null;
        }
        String id = dto.getId() != null ? dto.getId() : "";
        String name = dto.getName() != null ? dto.getName() : "Cooperativa";
        double rating = dto.getAverageRating() != null ? dto.getAverageRating() : 4.8;
        String specs = dto.getSpecialties() != null ? dto.getSpecialties() : "Papelão, Plásticos";
        List<String> mats = Arrays.asList(specs.split(",\\s*"));
        return new CooperativeJSON(
                id,
                name,
                "Especialista em " + specs,
                2.8,
                "Seg à Sex • 08h às 17h",
                "Cooperativa Certificada",
                rating,
                98,
                mats,
                "Coleta rápida em até 24h"
        );
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

    public Double getAvaliacao() {
        return avaliacao;
    }

    public int getCompatibilidade() {
        return compatibilidade;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public List<String> getMateriais() {
        return materiais;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }
}
