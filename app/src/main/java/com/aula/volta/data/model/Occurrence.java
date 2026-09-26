package com.aula.volta.data.model;

/** Modelo de domínio da tela (convertido de OccurrenceJSON). */
public class Occurrence {

    private final String id;
    private final String titulo;
    private final String setor;
    private final String tempoRelativo;
    private final String prioridade;
    private final String status;

    public Occurrence(String id, String titulo, String setor, String tempoRelativo,
                      String prioridade, String status) {
        this.id = id;
        this.titulo = titulo;
        this.setor = setor;
        this.tempoRelativo = tempoRelativo;
        this.prioridade = prioridade;
        this.status = status;
    }

    public static Occurrence fromJson(OccurrenceJSON json) {
        return new Occurrence(
                json.getId(),
                json.getTitulo(),
                json.getSetor(),
                json.getTempoRelativo(),
                json.getPrioridade(),
                json.getStatus());
    }

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
}
