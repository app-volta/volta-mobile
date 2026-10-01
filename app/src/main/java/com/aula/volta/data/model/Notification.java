package com.aula.volta.data.model;

/**
 * Modelo de domínio da tela de notificações.
 * Lido sempre via NotificationStore (único ponto de leitura da UI).
 */
public class Notification {

    private final String id;
    private final String tipo;
    private final String titulo;
    private final String descricao;
    private final String tempoRelativo;
    private final String icone;
    private final String tint;
    private final String bg;
    private boolean lida;
    private final String occurrenceId;

    public Notification(String id, String tipo, String titulo, String descricao,
                        String tempoRelativo, String icone, String tint, String bg,
                        boolean lida, String occurrenceId) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tempoRelativo = tempoRelativo;
        this.icone = icone;
        this.tint = tint;
        this.bg = bg;
        this.lida = lida;
        this.occurrenceId = occurrenceId;
    }

    public static Notification fromJson(NotificationJSON json) {
        return new Notification(
                json.getId(),
                json.getTipo(),
                json.getTitulo(),
                json.getDescricao(),
                json.getTempoRelativo(),
                json.getIcone(),
                json.getTint(),
                json.getBg(),
                json.isLida(),
                json.getOccurrenceId());
    }

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

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public String getOccurrenceId() {
        return occurrenceId;
    }
}
