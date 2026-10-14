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
    private Unidades unidades;
    private double confianca;
    private String observacoes;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public double getQuantidadeEstimada() {
        return quantidadeEstimada;
    }

    public void setQuantidadeEstimada(double quantidadeEstimada) {
        this.quantidadeEstimada = quantidadeEstimada;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public Contaminacao getContaminacao() {
        return contaminacao;
    }

    public void setContaminacao(Contaminacao contaminacao) {
        this.contaminacao = contaminacao;
    }

    public Unidades getUnidades() {
        return unidades;
    }

    public void setUnidades(Unidades unidades) {
        this.unidades = unidades;
    }

    public double getConfianca() {
        return confianca;
    }

    public void setConfianca(double confianca) {
        this.confianca = confianca;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public static AiAnalysisJSON createOfflineFallback(String setor) {
        AiAnalysisJSON json = new AiAnalysisJSON();
        json.material = "Papelão A";
        json.quantidadeEstimada = 38.0;
        json.unidade = "kg";
        json.confianca = 0.88;
        json.observacoes = "Guarde num lugar seco e coberto. Coleta em até 24h mantém o material valioso.";
        Contaminacao c = new Contaminacao();
        c.presente = false;
        c.nivel = "Baixa";
        json.contaminacao = c;
        Unidades u = new Unidades();
        u.tipo = "Caixas";
        u.quantidade = 11;
        json.unidades = u;
        return json;
    }

    public static class Contaminacao {
        private boolean presente;
        private String nivel;

        public boolean isPresente() {
            return presente;
        }

        public void setPresente(boolean presente) {
            this.presente = presente;
        }

        public String getNivel() {
            return nivel;
        }

        public void setNivel(String nivel) {
            this.nivel = nivel;
        }
    }

    public static class Unidades {
        private String tipo;
        private int quantidade;

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public int getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(int quantidade) {
            this.quantidade = quantidade;
        }
    }
}
