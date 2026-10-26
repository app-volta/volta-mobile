package com.aula.volta.data.model;

import android.content.Context;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
        return fromIncident(inc, null);
    }

    public static Occurrence fromIncident(IncidentResponseDTO inc, Context context) {
        if (inc == null) {
            return null;
        }
        String id = inc.getId() != null ? inc.getId() : "";
        String desc = (inc.getEmployeeDescription() != null && !inc.getEmployeeDescription().isEmpty())
                ? inc.getEmployeeDescription() : "Incidente " + (id.length() > 8 ? id.substring(0, 8) : id);
        String sector = "Frigorífico — Setor B2";
        String mat = resolveMaterial(inc);
        String prio = (inc.getPriority() != null && !inc.getPriority().isEmpty())
                ? inc.getPriority()
                : AiAnalysisJSON.calculatePriority(mat, inc.getContaminationLevel(), desc);
        String stat = inc.getStatus() != null ? inc.getStatus() : "EM_ANALISE";
        String time = "Hoje";
        if (inc.getRegisteredAt() != null && !inc.getRegisteredAt().isEmpty()) {
            time = inc.getRegisteredAt().replace("T", " ");
            if (time.length() > 16) {
                time = time.substring(0, 16);
            }
        }
        String photoUrl = inc.getPhotoUrl();
        String photoPath = null;
        if (context != null && (photoUrl == null || photoUrl.isEmpty())) {
            try {
                String cachedStr = PrefsHelper.getJson(context, "cache_occurrence_detail", id);
                if (cachedStr != null && !cachedStr.isEmpty()) {
                    JsonObject d = JsonParser.parseString(cachedStr).getAsJsonObject();
                    if (d.has("photoUrl") && !d.get("photoUrl").isJsonNull()) {
                        photoUrl = d.get("photoUrl").getAsString();
                    } else if (d.has("imageUrl") && !d.get("imageUrl").isJsonNull()) {
                        photoUrl = d.get("imageUrl").getAsString();
                    }
                    if (d.has("photoPath") && !d.get("photoPath").isJsonNull()) {
                        photoPath = d.get("photoPath").getAsString();
                    }
                    if (d.has("prioridade") && !d.get("prioridade").isJsonNull()) {
                        prio = d.get("prioridade").getAsString();
                    }
                    if (d.has("material") && !d.get("material").isJsonNull()) {
                        mat = d.get("material").getAsString();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new Occurrence(id, desc, sector, time, prio, stat, mat, photoUrl, photoPath);
    }

    private static String resolveMaterial(IncidentResponseDTO inc) {
        String desc = inc.getEmployeeDescription() != null ? inc.getEmployeeDescription().toLowerCase(java.util.Locale.ROOT) : "";
        String wId = inc.getWasteTypeId() != null ? inc.getWasteTypeId().toLowerCase(java.util.Locale.ROOT) : "";
        if (desc.contains("cadeir") || desc.contains("mesa") || desc.contains("móvel") || desc.contains("movel") || desc.contains("mobili")) {
            return "Mobiliário / Madeira";
        }
        if (desc.contains("madeira") || desc.contains("pallet") || desc.contains("palete")) {
            return "Madeira / Palete";
        }
        if (wId.contains("4e73b22b") || desc.contains("papel") || desc.contains("papelão") || desc.contains("cartão")) {
            return "Aparas de Papelão";
        }
        if (wId.contains("bd985415") || desc.contains("plást") || desc.contains("plast") || desc.contains("pet") || desc.contains("filme")) {
            return "Plástico Filme";
        }
        if (wId.contains("7020bc8b") || desc.contains("metal") || desc.contains("alum") || desc.contains("aço") || desc.contains("lata")) {
            return "Alumínio Limpo";
        }
        if (wId.contains("976a4df6") || desc.contains("vidro") || desc.contains("garrafa")) {
            return "Vidro Misto";
        }
        if (desc.contains("orgân") || desc.contains("organ")) {
            return "Orgânico";
        }
        return "Resíduo Reciclável";
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
