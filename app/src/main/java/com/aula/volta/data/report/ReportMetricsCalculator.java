package com.aula.volta.data.report;

import androidx.annotation.NonNull;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.EsgMetricResponseDTO;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ReportMetricsCalculator {

    public enum Period {
        WEEK, MONTH, QUARTER
    }

    public static int getGoalKg(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return 375;
            case QUARTER:
                return 4500;
            case MONTH:
            default:
                return 1500;
        }
    }

    public static double getPeriodScale(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return 0.25;
            case QUARTER:
                return 3.0;
            case MONTH:
            default:
                return 1.0;
        }
    }

    public static String getPeriodTitle(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return "Nesta Semana";
            case QUARTER:
                return "Neste Trimestre";
            case MONTH:
            default:
                return "Neste Mês";
        }
    }

    public static String getGoalTitle(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return "Meta da Semana";
            case QUARTER:
                return "Meta do Trimestre";
            case MONTH:
            default:
                return "Meta do Mês";
        }
    }

    public static String getVsPeriodTitle(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return "vs. semana passada";
            case QUARTER:
                return "vs. trimestre passado";
            case MONTH:
            default:
                return "vs. mês passado";
        }
    }

    public static class SectorStat {
        public final String name;
        public final int kg;
        public final int percentOfMax;

        public SectorStat(String name, int kg, int percentOfMax) {
            this.name = name;
            this.kg = kg;
            this.percentOfMax = percentOfMax;
        }
    }

    public static class ReportSummary {
        public int totalKg;
        public int openCount;
        public int resolvedCount;
        public int totalCount;
        public int recyclingRate;
        public int goalKg;
        public int goalPercent;
        public Map<String, Integer> materialBreakdown = new LinkedHashMap<>();
        public List<SectorStat> topSectors = new ArrayList<>();

        public JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            obj.addProperty("kg_reciclados", totalKg);
            obj.addProperty("abertas", openCount);
            obj.addProperty("resolvidas", resolvedCount);
            obj.addProperty("total", totalCount);
            obj.addProperty("taxa_reciclagem", recyclingRate);
            obj.addProperty("meta_kg", goalKg);
            obj.addProperty("meta_percent", goalPercent);
            obj.addProperty("monthlyTarget", (double) goalKg);
            obj.addProperty("targetKg", goalKg);
            obj.addProperty("goalPercent", goalPercent);
            obj.addProperty("corporateTotalKg", totalKg);
            obj.addProperty("corporateRecyclingPercentage", recyclingRate);

            JsonObject porTipo = new JsonObject();
            for (Map.Entry<String, Integer> entry : materialBreakdown.entrySet()) {
                porTipo.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("por_tipo", porTipo);
            return obj;
        }
    }

    private ReportMetricsCalculator() {
    }

    public static ReportSummary calculate(
            Period period,
            JsonObject baseEsgSummary,
            List<Occurrence> occurrences,
            Map<String, Double> weightsById,
            Map<String, String> materialsById) {

        if (period == null) {
            period = Period.MONTH;
        }
        double scale = getPeriodScale(period);
        int goalKg = getGoalKg(period);
        if (baseEsgSummary != null) {
            if (period == Period.MONTH && baseEsgSummary.has("monthlyTarget") && !baseEsgSummary.get("monthlyTarget").isJsonNull()) {
                double mt = baseEsgSummary.get("monthlyTarget").getAsDouble();
                if (mt > 0) goalKg = (int) Math.round(mt);
            } else if (period == Period.WEEK && baseEsgSummary.has("weeklyTarget") && !baseEsgSummary.get("weeklyTarget").isJsonNull()) {
                double wt = baseEsgSummary.get("weeklyTarget").getAsDouble();
                if (wt > 0) goalKg = (int) Math.round(wt);
            } else if (period == Period.QUARTER && baseEsgSummary.has("quarterlyTarget") && !baseEsgSummary.get("quarterlyTarget").isJsonNull()) {
                double qt = baseEsgSummary.get("quarterlyTarget").getAsDouble();
                if (qt > 0) goalKg = (int) Math.round(qt);
            }
        }

        int opAbertas = 0;
        int opResolvidas = 0;
        int opKg = 0;

        Map<String, Integer> materials = new LinkedHashMap<>();
        Map<String, Integer> sectorKg = new HashMap<>();

        if (occurrences != null) {
            for (Occurrence occ : occurrences) {
                if (occ == null) continue;
                String id = occ.getId();
                String status = occ.getStatus() != null ? occ.getStatus().toUpperCase(Locale.ROOT) : "";

                boolean isResolved = status.contains("RESOLVID")
                        || status.contains("FINALIZAD")
                        || status.contains("APROVAD")
                        || status.contains("COLETAD")
                        || status.contains("CONCLUID")
                        || status.contains("CLOSED");

                if (isResolved) {
                    opResolvidas++;
                } else {
                    opAbertas++;
                }

                double weight = 0.0;
                if (weightsById != null && id != null && weightsById.containsKey(id) && weightsById.get(id) != null) {
                    weight = weightsById.get(id);
                }
                int weightInt = (int) Math.round(weight);
                opKg += weightInt;

                String mat = occ.getMaterial();
                if (materialsById != null && id != null && materialsById.containsKey(id)) {
                    mat = materialsById.get(id);
                }
                String family = resolveMaterialFamily(mat != null ? mat : occ.getTitulo());
                int currentMatKg = materials.containsKey(family) ? materials.get(family) : 0;
                materials.put(family, currentMatKg + weightInt);

                String sector = occ.getSetor() != null ? occ.getSetor() : "Planta Principal";
                if (sector.contains("—")) {
                    sector = sector.substring(sector.indexOf("—") + 1).trim();
                }
                int currentSectorKg = sectorKg.containsKey(sector) ? sectorKg.get(sector) : 0;
                sectorKg.put(sector, currentSectorKg + weightInt);
            }
        }

        ReportSummary summary = new ReportSummary();

        int baseKg = 0;
        int baseAbertas = 0;
        int baseResolvidas = 0;

        if (baseEsgSummary != null) {
            if (baseEsgSummary.has("corporateTotalKg") && !baseEsgSummary.get("corporateTotalKg").isJsonNull()) {
                baseKg = (int) Math.round(baseEsgSummary.get("corporateTotalKg").getAsInt() * scale);
            } else if (baseEsgSummary.has("kg_reciclados") && !baseEsgSummary.get("kg_reciclados").isJsonNull()) {
                baseKg = (int) Math.round(baseEsgSummary.get("kg_reciclados").getAsInt() * scale);
            }
            if (baseEsgSummary.has("abertas") && !baseEsgSummary.get("abertas").isJsonNull()) {
                baseAbertas = (int) Math.round(baseEsgSummary.get("abertas").getAsInt() * scale);
            }
            if (baseEsgSummary.has("resolvidas") && !baseEsgSummary.get("resolvidas").isJsonNull()) {
                baseResolvidas = (int) Math.round(baseEsgSummary.get("resolvidas").getAsInt() * scale);
            }
            if (baseEsgSummary.has("por_tipo") && baseEsgSummary.get("por_tipo").isJsonObject()) {
                JsonObject pt = baseEsgSummary.getAsJsonObject("por_tipo");
                for (Map.Entry<String, JsonElement> e : pt.entrySet()) {
                    if (!e.getValue().isJsonNull()) {
                        int v = (int) Math.round(e.getValue().getAsInt() * scale);
                        materials.put(e.getKey(), v);
                    }
                }
            }
        }

        summary.totalKg = baseKg > 0 ? baseKg : opKg;
        summary.openCount = opAbertas > 0 ? opAbertas : baseAbertas;
        summary.resolvedCount = opResolvidas > 0 ? opResolvidas : baseResolvidas;
        summary.totalCount = summary.openCount + summary.resolvedCount;

        if (baseEsgSummary != null && baseEsgSummary.has("corporateRecyclingPercentage") && !baseEsgSummary.get("corporateRecyclingPercentage").isJsonNull()) {
            summary.recyclingRate = baseEsgSummary.get("corporateRecyclingPercentage").getAsInt();
        } else if (summary.totalCount > 0) {
            summary.recyclingRate = (int) Math.round(summary.resolvedCount * 100.0 / summary.totalCount);
        } else {
            summary.recyclingRate = 0;
        }

        summary.materialBreakdown = materials;
        summary.goalKg = goalKg;
        summary.goalPercent = goalKg > 0 ? Math.min(100, (int) Math.round(summary.totalKg * 100.0 / goalKg)) : 0;

        List<Map.Entry<String, Integer>> sortedSectors = new ArrayList<>(sectorKg.entrySet());
        sortedSectors.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int totalSectorKg = 0;
        for (int k : sectorKg.values()) {
            totalSectorKg += k;
        }
        int totalForPct = totalSectorKg > 0 ? totalSectorKg : 1;
        for (int i = 0; i < Math.min(3, sortedSectors.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSectors.get(i);
            int pct = (int) Math.round(entry.getValue() * 100.0 / totalForPct);
            summary.topSectors.add(new SectorStat(entry.getKey(), entry.getValue(), Math.max(2, Math.min(100, pct))));
        }

        return summary;
    }

    public interface SummaryCallback {
        void onSummaryReady(ReportSummary summary, List<Occurrence> occurrences);
    }

    public static void loadOfficialSummary(android.content.Context context, Period period, SummaryCallback callback) {
        if (context == null) return;
        final android.content.Context appContext = context.getApplicationContext();
        final Period finalPeriod = period != null ? period : Period.MONTH;

        OccurrenceStore.refresh(appContext, freshOccurrences -> {
            final List<Occurrence> occs = freshOccurrences != null ? freshOccurrences : OccurrenceStore.cached(appContext);
            ReportAPI api = ApiClient.get(appContext).create(ReportAPI.class);
            api.getEsgMetrics().enqueue(new Callback<List<EsgMetricResponseDTO>>() {
                @Override
                public void onResponse(@NonNull Call<List<EsgMetricResponseDTO>> call,
                                       @NonNull Response<List<EsgMetricResponseDTO>> response) {
                    JsonObject baseEsg = new JsonObject();
                    baseEsg.addProperty("isLiveDatabase", true);
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        EsgMetricResponseDTO metric = response.body().get(0);
                        if (metric.getTotalRecycledKg() != null) {
                            baseEsg.addProperty("corporateTotalKg", metric.getTotalRecycledKg().intValue());
                        }
                        if (metric.getRecyclingPercentage() != null) {
                            baseEsg.addProperty("corporateRecyclingPercentage", metric.getRecyclingPercentage().intValue());
                        }
                        if (metric.getMonthlyTarget() != null && metric.getMonthlyTarget().doubleValue() > 0) {
                            baseEsg.addProperty("monthlyTarget", metric.getMonthlyTarget().doubleValue());
                        }
                        if (metric.getWeeklyTarget() != null && metric.getWeeklyTarget().doubleValue() > 0) {
                            baseEsg.addProperty("weeklyTarget", metric.getWeeklyTarget().doubleValue());
                        }
                        if (metric.getQuarterlyTarget() != null && metric.getQuarterlyTarget().doubleValue() > 0) {
                            baseEsg.addProperty("quarterlyTarget", metric.getQuarterlyTarget().doubleValue());
                        }
                    }
                    computeAndNotify(appContext, finalPeriod, occs, baseEsg, callback);
                }

                @Override
                public void onFailure(@NonNull Call<List<EsgMetricResponseDTO>> call, @NonNull Throwable t) {
                    JsonObject baseEsg = new JsonObject();
                    baseEsg.addProperty("isLiveDatabase", true);
                    computeAndNotify(appContext, finalPeriod, occs, baseEsg, callback);
                }
            });
        });
    }

    private static void computeAndNotify(android.content.Context context, Period period,
                                         List<Occurrence> occurrences, JsonObject baseEsg,
                                         SummaryCallback callback) {
        Map<String, Double> weightsById = new HashMap<>();
        Map<String, String> materialsById = new HashMap<>();

        if (occurrences != null) {
            for (Occurrence occ : occurrences) {
                if (occ == null || occ.getId() == null) continue;
                String cachedStr = PrefsHelper.getJson(context, "cache_occurrence_detail", occ.getId());
                if (cachedStr != null && !cachedStr.isEmpty()) {
                    try {
                        JsonObject d = com.google.gson.JsonParser.parseString(cachedStr).getAsJsonObject();
                        if (d.has("peso_estimado") && !d.get("peso_estimado").isJsonNull()) {
                            try {
                                weightsById.put(occ.getId(), d.get("peso_estimado").getAsDouble());
                            } catch (Exception ignored) {
                                String wStr = d.get("peso_estimado").getAsString().replaceAll("[^0-9.]", "");
                                if (!wStr.isEmpty()) {
                                    weightsById.put(occ.getId(), Double.parseDouble(wStr));
                                }
                            }
                        } else if (d.has("quantidade") && !d.get("quantidade").isJsonNull()) {
                            weightsById.put(occ.getId(), d.get("quantidade").getAsDouble());
                        }
                        if (d.has("material") && !d.get("material").isJsonNull()) {
                            materialsById.put(occ.getId(), d.get("material").getAsString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        ReportSummary summary = calculate(period, baseEsg, occurrences, weightsById, materialsById);
        PrefsHelper.putJson(context, "cache_reports", "summary", summary.toJsonObject().toString());
        if (callback != null) {
            callback.onSummaryReady(summary, occurrences);
        }
    }

    public static String resolveMaterialFamily(String raw) {
        if (raw == null) return "Papelão";
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        if (lower.isEmpty()) {
            return "Papelão";
        }
        if (lower.equals("nenhum")) {
            return "Nenhum";
        }
        if (lower.contains("papel") || lower.contains("cartão") || lower.contains("cartao") || lower.contains("kraft")) {
            return "Papelão";
        }
        if (lower.contains("plást") || lower.contains("plast") || lower.contains("pet") || lower.contains("filme") || lower.contains("isopor") || lower.contains("eps")) {
            return "Plástico";
        }
        if (lower.contains("vidro") || lower.contains("caco") || lower.contains("garrafa")) {
            return "Vidro";
        }
        if (lower.contains("metal") || lower.contains("alum") || lower.contains("aço") || lower.contains("ferro") || lower.contains("lata") || lower.contains("sucata") || lower.contains(" aco") || lower.contains("aco ") || lower.equals("aco")) {
            return "Metal";
        }
        if (lower.contains("madeira") || lower.contains("palete") || lower.contains("pallet") || lower.contains("mobili")) {
            return "Madeira";
        }
        if (lower.contains("orgân") || lower.contains("organ") || lower.contains("alimento") || lower.contains("comida") || lower.contains("peotta")) {
            return "Orgânico";
        }
        if (lower.contains("perig") || lower.contains("quimic") || lower.contains("oleo") || lower.contains("graxa") || lower.contains("tinta")) {
            return "Perigoso";
        }
        if (lower.contains("eletron") || lower.contains("bateria") || lower.contains("pilha")) {
            return "Eletrônico";
        }
        if (lower.contains("textil") || lower.contains("tecido") || lower.contains("pano")) {
            return "Têxtil";
        }
        return "Papelão";
    }
}
