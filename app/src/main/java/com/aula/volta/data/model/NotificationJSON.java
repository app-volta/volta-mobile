package com.aula.volta.data.model;

import com.google.gson.annotations.SerializedName;

/** Espelho Gson de GET /notifications (contrato futuro). */
public class NotificationJSON {

    private String id;
    private String tipo;
    private String titulo;
    private String descricao;

    @SerializedName("tempo_relativo")
    private String tempoRelativo;

    private String icone;
    private String tint;
    private String bg;
    private boolean lida;

    @SerializedName("occurrence_id")
    private String occurrenceId;

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTempoRelativo() {
        return tempoRelativo;
    }

    public String getIcone() {
        return icone;
    }

    public String getTint() {
        return tint;
    }

    public String getBg() {
        return bg;
    }

    public boolean isLida() {
        return lida;
    }

    public String getOccurrenceId() {
        return occurrenceId;
    }
}
