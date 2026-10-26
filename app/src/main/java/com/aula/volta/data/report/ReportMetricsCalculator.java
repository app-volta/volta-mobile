package com.aula.volta.data.report;

import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calculador de métricas e indicadores de relatórios ESG do VOLTA (PGRS).
 * Classe de lógica pura e determinística (coberta por testes unitários).
 */
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
                return "Nesta semana";
            case QUARTER:
                return "Neste trimestre";
            case MONTH:
            default:
                return "Neste mês";
        }
    }

    public static String getGoalTitle(Period period) {
        if (period == null) period = Period.MONTH;
        switch (period) {
            case WEEK:
                return "Meta da semana";
            case QUARTER:
                return "Meta do trimestre";
            case MONTH:
            default:
                return "Meta do mês";
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

    /**
     * Calcula o resumo de métricas para o período selecionado, combinando métricas corporativas
     * com ocorrências registradas em tempo real pelos operadores.
     */
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

        // 1. Métricas corporativas base (Figma 790:3322) escaladas para o período
        int baseKg = (int) Math.round(1147 * scale);
        int baseAbertas = Math.max(1, (int) Math.round(6 * scale));
        int baseResolvidas = Math.max(1, (int) Math.round(46 * scale));

        Map<String, Integer> materials = new LinkedHashMap<>();
        materials.put("Papelão", (int) Math.round(542 * scale));
        materials.put("Plástico", (int) Math.round(318 * scale));
        materials.put("Metal", (int) Math.round(176 * scale));
        materials.put("Vidro", (int) Math.round(111 * scale));

        // Se houver sumário corporativo da API, extrai seus valores base
        if (baseEsgSummary != null) {
            if (baseEsgSummary.has("kg_reciclados") && !baseEsgSummary.get("kg_reciclados").isJsonNull()) {
                baseKg = (int) Math.round(baseEsgSummary.get("kg_reciclados").getAsInt() * scale);
            }
            if (baseEsgSummary.has("abertas") && !baseEsgSummary.get("abertas").isJsonNull()) {
                baseAbertas = Math.max(1, (int) Math.round(baseEsgSummary.get("abertas").getAsInt() * scale));
            }
            if (baseEsgSummary.has("resolvidas") && !baseEsgSummary.get("resolvidas").isJsonNull()) {
                baseResolvidas = Math.max(1, (int) Math.round(baseEsgSummary.get("resolvidas").getAsInt() * scale));
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

        // 2. Setores base escalados
        Map<String, Integer> sectorKg = new HashMap<>();
        sectorKg.put("Frigorífico B2", (int) Math.round(342 * scale));
        sectorKg.put("Expedição", (int) Math.round(218 * scale));
        sectorKg.put("Manutenção", (int) Math.round(187 * scale));

        // 3. Processa ocorrências reais cadastradas
        int opAbertas = 0;
        int opResolvidas = 0;
        int opKg = 0;

        if (occurrences != null) {
            for (Occurrence occ : occurrences) {
                if (occ == null) continue;
                String id = occ.getId();
                String status = occ.getStatus() != null ? occ.getStatus().toUpperCase(Locale.ROOT) : "";

                boolean isResolved = status.contains("RESOLVID")
                        || status.contains("FINALIZAD")
                        || status.contains("APROVAD")
                        || status.contains("COLETAD");

                if (isResolved) {
                    opResolvidas++;
                } else {
                    opAbertas++;
                }

                // Determina o peso estimado
                double weight = 15.0; // Padrão razoável por lote
                if (weightsById != null && id != null && weightsById.containsKey(id) && weightsById.get(id) != null) {
                    weight = weightsById.get(id);
                }
                int weightInt = (int) Math.round(weight);
                opKg += weightInt;

                // Determina a família de material
                String mat = occ.getMaterial();
                if (materialsById != null && id != null && materialsById.containsKey(id)) {
                    mat = materialsById.get(id);
                }
                String family = resolveMaterialFamily(mat != null ? mat : occ.getTitulo());
                int currentMatKg = materials.containsKey(family) ? materials.get(family) : 0;
                materials.put(family, currentMatKg + weightInt);

                // Determina o setor
                String sector = occ.getSetor() != null ? occ.getSetor() : "Frigorífico B2";
                if (sector.contains("—")) {
                    sector = sector.substring(sector.indexOf("—") + 1).trim();
                }
                int currentSectorKg = sectorKg.containsKey(sector) ? sectorKg.get(sector) : 0;
                sectorKg.put(sector, currentSectorKg + weightInt);
            }
        }

        ReportSummary summary = new ReportSummary();
        summary.totalKg = baseKg + opKg;
        summary.openCount = baseAbertas + opAbertas;
        summary.resolvedCount = baseResolvidas + opResolvidas;
        summary.totalCount = summary.openCount + summary.resolvedCount;

        if (summary.totalCount > 0) {
            summary.recyclingRate = (int) Math.round(summary.resolvedCount * 100.0 / summary.totalCount);
        } else {
            summary.recyclingRate = 88;
        }

        summary.goalKg = goalKg;
        summary.goalPercent = Math.min(100, (int) Math.round(summary.totalKg * 100.0 / goalKg));
        summary.materialBreakdown = materials;

        // Processa top 3 setores ordenados por peso
        List<Map.Entry<String, Integer>> sortedSectors = new ArrayList<>(sectorKg.entrySet());
        sortedSectors.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int maxSectorKg = sortedSectors.isEmpty() ? 1 : Math.max(1, sortedSectors.get(0).getValue());
        for (int i = 0; i < Math.min(3, sortedSectors.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSectors.get(i);
            int pct = (int) Math.round(entry.getValue() * 100.0 / maxSectorKg);
            summary.topSectors.add(new SectorStat(entry.getKey(), entry.getValue(), pct));
        }

        return summary;
    }

    public static String resolveMaterialFamily(String raw) {
        if (raw == null) return "Papelão";
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("papel") || lower.contains("cartão") || lower.contains("cartao") || lower.contains("kraft")) {
            return "Papelão";
        }
        if (lower.contains("plást") || lower.contains("plast") || lower.contains("pet") || lower.contains("filme") || lower.contains("isopor") || lower.contains("eps")) {
            return "Plástico";
        }
        if (lower.contains("vidro") || lower.contains("caco")) {
            return "Vidro";
        }
        if (lower.contains("metal") || lower.contains("alum") || lower.contains("aço") || lower.contains("ferro") || lower.contains("lata") || lower.contains("sucata") || lower.contains(" aco") || lower.contains("aco ") || lower.equals("aco")) {
            return "Metal";
        }
        if (lower.contains("madeira") || lower.contains("palete") || lower.contains("pallet") || lower.contains("mobili")) {
            return "Madeira";
        }
        if (lower.contains("orgân") || lower.contains("organ") || lower.contains("alimento") || lower.contains("comida")) {
            return "Orgânico";
        }
        return "Papelão";
    }
}
