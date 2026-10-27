package com.aula.volta.data.model;

import android.content.Context;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

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
        String title = (json.getTitulo() != null && !json.getTitulo().trim().isEmpty())
                ? json.getTitulo()
                : AiAnalysisJSON.generateSingleWordTitle(json.getMaterial(), null, null);
        return new Occurrence(
                json.getId(),
                title,
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
        String sector = resolveSector(inc, context);
        String mat = resolveMaterial(inc);
        String singleTitle = AiAnalysisJSON.generateSingleWordTitle(mat, inc.getEmployeeDescription(), inc.getWasteTypeId());
        String prio = (inc.getPriority() != null && !inc.getPriority().isEmpty())
                ? inc.getPriority()
                : AiAnalysisJSON.calculatePriority(mat, inc.getContaminationLevel(), singleTitle);
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
        if (context != null) {
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
                    if (d.has("material_principal") && !d.get("material_principal").isJsonNull()) {
                        mat = d.get("material_principal").getAsString();
                    } else if (d.has("material") && !d.get("material").isJsonNull()) {
                        mat = d.get("material").getAsString();
                    }
                    if (d.has("setor") && !d.get("setor").isJsonNull()) {
                        sector = d.get("setor").getAsString();
                    }
                    if (d.has("titulo") && !d.get("titulo").isJsonNull()) {
                        singleTitle = AiAnalysisJSON.generateSingleWordTitle(mat, d.get("titulo").getAsString(), null);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new Occurrence(id, singleTitle, sector, time, prio, stat, mat, photoUrl, photoPath);
    }

    private static String resolveSector(IncidentResponseDTO inc, Context context) {
        if (inc == null) {
            return "Frigorífico — Setor B2";
        }
        String areaId = inc.getAreaId();
        if (areaId != null && !areaId.isEmpty()) {
            String lower = areaId.toLowerCase(Locale.ROOT);
            if (lower.contains("3059a21a")) return "Frigorífico — Setor B2";
            if (lower.contains("9c98cf54")) return "Expedição";
            if (lower.contains("86901188")) return "Manutenção";
            if (lower.contains("1f7e4e53")) return "Armazenamento";
            if (context != null) {
                String cachedArea = PrefsHelper.getJson(context, "cache_areas", areaId);
                if (cachedArea != null && !cachedArea.isEmpty()) {
                    try {
                        JsonObject obj = JsonParser.parseString(cachedArea).getAsJsonObject();
                        if (obj.has("sectorName") && !obj.get("sectorName").isJsonNull()) {
                            return obj.get("sectorName").getAsString();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        String desc = inc.getEmployeeDescription();
        if (desc != null) {
            String lowerDesc = desc.toLowerCase(Locale.ROOT);
            if (lowerDesc.contains("expedi")) return "Expedição";
            if (lowerDesc.contains("manuten")) return "Manutenção";
            if (lowerDesc.contains("armazen")) return "Armazenamento";
            if (lowerDesc.contains("frigor")) return "Frigorífico — Setor B2";
        }
        return "Frigorífico — Setor B2";
    }

    private static String resolveMaterial(IncidentResponseDTO inc) {
        if (inc == null) {
            return "Resíduo Reciclável";
        }
        String desc = inc.getEmployeeDescription() != null ? inc.getEmployeeDescription().toLowerCase(Locale.ROOT) : "";
        String wId = inc.getWasteTypeId() != null ? inc.getWasteTypeId().toLowerCase(Locale.ROOT) : "";

        // 1. Mapeamento por UUID da API QA (tabela waste_types)
        if (wId.contains("bd985415") || wId.contains("4e73b22b")) {
            return "Aparas de Papelão";
        }
        if (wId.contains("8b3f6a14")) {
            return "Plástico Filme";
        }
        if (wId.contains("2605dca7") || wId.contains("976a4df6")) {
            return "Vidro Misto";
        }
        if (wId.contains("5ee946da") || wId.contains("7020bc8b")) {
            return "Alumínio Limpo";
        }
        if (wId.contains("5cb7e3c8")) {
            return "Orgânico";
        }
        if (wId.contains("e9e077a8")) {
            return "Madeira / Palete";
        }
        if (wId.contains("7807fd3c")) {
            return "Eletrônico";
        }
        if (wId.contains("db21c099")) {
            return "Químico";
        }
        if (wId.contains("9221e10b")) {
            return "Têxtil";
        }
        if (wId.contains("dc9b7d17")) {
            return "Resíduo Misto";
        }

        // 2. Classificação semântica por descrição do operador
        if (desc.contains("cadeir") || desc.contains("mesa") || desc.contains("móvel") || desc.contains("movel") || desc.contains("mobili")) {
            return "Mobiliário / Madeira";
        }
        if (desc.contains("madeira") || desc.contains("pallet") || desc.contains("palete")) {
            return "Madeira / Palete";
        }
        if (desc.contains("papel") || desc.contains("papelão") || desc.contains("cartão") || desc.contains("caixa")) {
            return "Aparas de Papelão";
        }
        if (desc.contains("plást") || desc.contains("plast") || desc.contains("pet") || desc.contains("filme")) {
            return "Plástico Filme";
        }
        if (desc.contains("metal") || desc.contains("alum") || desc.contains("aço") || desc.contains("lata")) {
            return "Alumínio Limpo";
        }
        if (desc.contains("vidro") || desc.contains("garrafa")) {
            return "Vidro Misto";
        }
        if (desc.contains("orgân") || desc.contains("organ") || desc.contains("peotta")) {
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
