package com.aula.volta.data.model;

import com.aula.volta.data.model.incident.IncidentResponseDTO;

/**
 * Modelo de domínio da ocorrência (utilizado em toda a UI da Home, Lista e Detalhe).
 * Suporta conversão tanto de OccurrenceJSON legado quanto de IncidentResponseDTO da API Oficial do banco.
 */
public class Occurrence {

    private final String id;
    private final String titulo;
    private final String setor;
    private final String tempoRelativo;
    private final String prioridade;
    private final String status;
    private final String material;
    private final String photoUrl;
    private final String photoPath;

    public Occurrence(String id, String titulo, String setor, String tempoRelativo,
                      String prioridade, String status, String material) {
        this(id, titulo, setor, tempoRelativo, prioridade, status, material, null, null);
    }

    public Occurrence(String id, String titulo, String setor, String tempoRelativo,
                      String prioridade, String status, String material,
                      String photoUrl, String photoPath) {
        this.id = id;
        this.titulo = titulo;
        this.setor = setor;
        this.tempoRelativo = tempoRelativo;
        this.prioridade = prioridade;
        this.status = status;
        this.material = material;
        this.photoUrl = photoUrl;
        this.photoPath = photoPath;
    }

    public static Occurrence fromJson(OccurrenceJSON json) {
        if (json == null) {
            return null;
        }
        return new Occurrence(
                json.getId(),
                json.getTitulo(),
                json.getSetor(),
                json.getTempoRelativo(),
                json.getPrioridade(),
                json.getStatus(),
                json.getMaterial(),
                json.getPhotoUrl(),
                null);
    }

    public static Occurrence fromIncident(IncidentResponseDTO inc) {
        if (inc == null) {
            return null;
        }
        String id = inc.getId() != null ? inc.getId() : "";
        String desc = (inc.getEmployeeDescription() != null && !inc.getEmployeeDescription().isEmpty())
                ? inc.getEmployeeDescription() : "Incidente " + (id.length() > 8 ? id.substring(0, 8) : id);
        String sector = "Frigorífico — Setor B2";
        String prio = inc.getPriority() != null ? inc.getPriority() : "MEDIA";
        String stat = inc.getStatus() != null ? inc.getStatus() : "EM_ANALISE";
        String mat = "Aparas de Papelão";
        String time = "Hoje";
        if (inc.getRegisteredAt() != null && !inc.getRegisteredAt().isEmpty()) {
            time = inc.getRegisteredAt().replace("T", " ");
            if (time.length() > 16) {
                time = time.substring(0, 16);
            }
        }
        String photoUrl = inc.getPhotoUrl();
        return new Occurrence(id, desc, sector, time, prio, stat, mat, photoUrl, null);
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

    public String getMaterial() {
        return material;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}
