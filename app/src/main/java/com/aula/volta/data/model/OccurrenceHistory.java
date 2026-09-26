package com.aula.volta.data.model;

/**
 * Entrada de auditoria (regra transversal, item 5 do plano).
 * Mock: lista local por ocorrência. Backend: tabela imutável. App nunca apaga log.
 */
public class OccurrenceHistory {

    private String id;
    private String usuario;
    private String acao;
    private String entidade;
    private String entidadeId;
    private String valorAnterior;
    private String valorNovo;
    private String timestamp;

    public OccurrenceHistory() {
    }

    public OccurrenceHistory(String id, String usuario, String acao, String entidade,
                             String entidadeId, String valorAnterior, String valorNovo,
                             String timestamp) {
        this.id = id;
        this.usuario = usuario;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
