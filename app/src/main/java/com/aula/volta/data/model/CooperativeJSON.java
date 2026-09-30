package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Espelho Gson de GET /cooperatives/recommended (contrato futuro).
 * O app apenas apresenta a lista ordenada que o backend/AIC devolve.
 */
public class CooperativeJSON {

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
