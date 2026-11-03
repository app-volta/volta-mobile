package com.aula.volta.data.model;

import android.content.Context;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

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
        String title = json.getTitulo();
        if (title != null) {
            title = title.replaceAll("(?i)\\s*\\(Classe\\s+[A-Za-z0-9]+\\)", "").trim();
        }
        if (title == null || title.trim().isEmpty()) {
            title = json.getMaterial() != null ? json.getMaterial() : "Nenhum";
        }
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
        String mat = resolveMaterial(inc, context);
        String singleTitle;
        if (inc.getTitle() != null && !inc.getTitle().trim().isEmpty() && !AiAnalysisJSON.isGenericLabel(inc.getTitle())) {
            singleTitle = inc.getTitle().trim();
        } else if (inc.getEmployeeDescription() != null && !inc.getEmployeeDescription().trim().isEmpty() && !AiAnalysisJSON.isGenericLabel(inc.getEmployeeDescription())) {
            singleTitle = inc.getEmployeeDescription().trim();
        } else if (inc.getTitle() != null && !inc.getTitle().trim().isEmpty()) {
            singleTitle = inc.getTitle().trim();
        } else if (inc.getEmployeeDescription() != null && !inc.getEmployeeDescription().trim().isEmpty()) {
            singleTitle = inc.getEmployeeDescription().trim();
        } else {
            singleTitle = (mat != null && !mat.trim().isEmpty() && !AiAnalysisJSON.isGenericLabel(mat)) ? mat : "Nenhum";
        }
        String prio = (inc.getPriority() != null && !inc.getPriority().isEmpty())
                ? inc.getPriority()
                : AiAnalysisJSON.calculatePriority(mat, inc.getContaminationLevel(), singleTitle);
        String stat = inc.getStatus() != null ? inc.getStatus() : "EM_ANALISE";
        String time = formatBrazilianDateTime(inc.getRegisteredAt());
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
                        String t = d.get("titulo").getAsString().trim();
                        if (!t.isEmpty()) {
                            singleTitle = t;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new Occurrence(id, singleTitle, sector, time, prio, stat, mat, photoUrl, photoPath);
    }

    public static String resolveSector(IncidentResponseDTO inc, Context context) {
        if (inc == null) {
            return "Planta Principal";
        }
        String areaId = inc.getAreaId();
        if (areaId != null && !areaId.isEmpty()) {
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
            if (lowerDesc.contains("frigor")) return "Planta Principal";
        }
        return "Planta Principal";
    }

    private static String resolveMaterial(IncidentResponseDTO inc) {
        return resolveMaterial(inc, null);
    }

    private static String resolveMaterial(IncidentResponseDTO inc, Context context) {
        if (inc == null) {
            return "Nenhum";
        }
        String wId = inc.getWasteTypeId();
        if (wId != null && !wId.isEmpty() && context != null) {
            try {
                String listStr = PrefsHelper.getJson(context, "cache_waste_types", "list");
                if (listStr != null && !listStr.isEmpty()) {
                    JsonArray arr = JsonParser.parseString(listStr).getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (el.isJsonObject()) {
                            JsonObject obj = el.getAsJsonObject();
                            if (obj.has("id") && !obj.get("id").isJsonNull()
                                    && wId.equalsIgnoreCase(obj.get("id").getAsString())) {
                                if (obj.has("description") && !obj.get("description").isJsonNull()) {
                                    return obj.get("description").getAsString();
                                }
                                if (obj.has("category") && !obj.get("category").isJsonNull()) {
                                    return obj.get("category").getAsString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        String desc = inc.getEmployeeDescription() != null ? inc.getEmployeeDescription().toLowerCase(Locale.ROOT) : "";
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
        return "Nenhum";
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

    public boolean isResolved() {
        if (status == null) {
            return false;
        }
        String s = status.toUpperCase(Locale.ROOT);
        return s.contains("RESOLVID")
                || s.contains("FINALIZAD")
                || s.contains("APROVAD")
                || s.contains("COLETAD")
                || s.contains("CONCLUID")
                || s.contains("CLOSED");
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

    public static String formatBrazilianDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Hoje";
        }
        String clean = raw.trim();
        if (clean.equalsIgnoreCase("hoje") || clean.equalsIgnoreCase("ontem")
                || clean.startsWith("há ") || clean.contains("às")) {
            return clean;
        }

        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "^(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2})");
            java.util.regex.Matcher m = p.matcher(clean);
            if (m.find()) {
                String year = m.group(1);
                String month = m.group(2);
                String day = m.group(3);
                String hour = m.group(4);
                String minute = m.group(5);

                java.text.SimpleDateFormat sdfToday = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
                String todayStr = sdfToday.format(new java.util.Date());
                if (todayStr.equals(year + "-" + month + "-" + day)) {
                    return "Hoje às " + hour + ":" + minute;
                }
                return day + "/" + month + "/" + year + " às " + hour + ":" + minute;
            }

            java.util.regex.Pattern pDateOnly = java.util.regex.Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
            java.util.regex.Matcher mDate = pDateOnly.matcher(clean);
            if (mDate.find()) {
                return mDate.group(3) + "/" + mDate.group(2) + "/" + mDate.group(1);
            }
        } catch (Exception ignored) {
        }

        return clean;
    }
}
