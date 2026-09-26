package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/** Espelho Gson de GET /occurrences (contrato futuro). */
public class OccurrenceJSON {

    private String id;
    private String titulo;
    private String setor;

    @SerializedName("tempo_relativo")
    private String tempoRelativo;

    private String prioridade;
    private String status;
    private String material;

    @SerializedName("quantidade_estimada")
    private double quantidadeEstimada;

    private String unidade;

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSetor() {
        return setor;
    }

    public String getTempoRelativo() {
        return tempoRelativo;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public String getStatus() {
        return status;
    }

    public String getMaterial() {
        return material;
    }

    public double getQuantidadeEstimada() {
        return quantidadeEstimada;
    }

    public String getUnidade() {
        return unidade;
    }
}
