package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Contrato da IA (Fase 0). O mock obedece; a API real só pluga.
 * A tela depende deste contrato, nunca do mock.
 */
public class AiAnalysisJSON {

    private String material;

    @SerializedName("quantidade_estimada")
    private double quantidadeEstimada;

    private String unidade;
    private Contaminacao contaminacao;
    private double confianca;
    private String observacoes;

    public String getMaterial() {
        return material;
    }

    public double getQuantidadeEstimada() {
        return quantidadeEstimada;
    }

    public String getUnidade() {
        return unidade;
    }

    public Contaminacao getContaminacao() {
        return contaminacao;
    }

    public double getConfianca() {
        return confianca;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public static class Contaminacao {
        private boolean presente;
        private String nivel;

        public boolean isPresente() {
            return presente;
        }

        public String getNivel() {
            return nivel;
        }
    }
}
