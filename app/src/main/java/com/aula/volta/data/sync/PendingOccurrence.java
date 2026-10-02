package com.aula.volta.data.sync;

/**
 * Representa uma ocorrência registrada sem conexão de rede,
 * enfileirada para sincronização posterior com a API do VOLTA.
 */
public class PendingOccurrence {

    private String localId;
    private String fotoPath;
    private String descricao;
    private String setor;
    private String material;
    private double quantidadeEstimada;
    private String unidade;
    private long timestamp;

    public PendingOccurrence() {
    }

    public PendingOccurrence(String localId, String fotoPath, String descricao,
                             String setor, String material, double quantidadeEstimada,
                             String unidade, long timestamp) {
        this.localId = localId;
        this.fotoPath = fotoPath;
        this.descricao = descricao;
        this.setor = setor;
        this.material = material;
        this.quantidadeEstimada = quantidadeEstimada;
        this.unidade = unidade;
        this.timestamp = timestamp;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getFotoPath() {
        return fotoPath;
    }

    public void setFotoPath(String fotoPath) {
        this.fotoPath = fotoPath;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
