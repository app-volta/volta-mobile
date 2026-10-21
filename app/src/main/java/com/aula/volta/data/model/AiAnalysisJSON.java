package com.aula.volta.data.model;

import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Contrato da Análise de IA do VOLTA.
 * Compatível com o contrato de análise direta (POST /occurrences/{id}/analysis)
 * e com o relatório de IA da API QA (GET /incidents/{id}/ai-report - AiReportResponseDTO).
 */
public class AiAnalysisJSON {

    @SerializedName(value = "material", alternate = {"detectedWasteType", "detected_waste_type"})
    private String material;

    @SerializedName(value = "quantidade_estimada", alternate = {"estimatedQuantity", "quantidadeEstimada"})
    private double quantidadeEstimada;

    private String unidade;
    private Contaminacao contaminacao;

    @SerializedName("aiContaminationLevel")
    private String aiContaminationLevel;

    private Unidades unidades;
    private double confianca;

    @SerializedName(value = "observacoes", alternate = {"recommendations", "recomendacoes"})
    private String observacoes;

    @SerializedName("reportText")
    private String reportText;

    @SerializedName("incidentId")
    private String incidentId;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public double getQuantidadeEstimada() {
        return quantidadeEstimada > 0 ? quantidadeEstimada : 38.0;
    }

    public void setQuantidadeEstimada(double quantidadeEstimada) {
        this.quantidadeEstimada = quantidadeEstimada;
    }

    public String getUnidade() {
        return unidade != null && !unidade.isEmpty() ? unidade : "kg";
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public Contaminacao getContaminacao() {
        if (contaminacao != null) {
            return contaminacao;
        }
        if (aiContaminationLevel != null && !aiContaminationLevel.isEmpty()) {
            Contaminacao mapped = new Contaminacao();
            mapped.setNivel(mapContaminationLabel(aiContaminationLevel));
            mapped.setPresente(!"LOW".equalsIgnoreCase(aiContaminationLevel)
                    && !"Baixa".equalsIgnoreCase(aiContaminationLevel));
            return mapped;
        }
        return null;
    }

    public void setContaminacao(Contaminacao contaminacao) {
        this.contaminacao = contaminacao;
    }

    public String getAiContaminationLevel() {
        return aiContaminationLevel;
    }

    public void setAiContaminationLevel(String aiContaminationLevel) {
        this.aiContaminationLevel = aiContaminationLevel;
    }

    public Unidades getUnidades() {
        return unidades;
    }

    public void setUnidades(Unidades unidades) {
        this.unidades = unidades;
    }

    public double getConfianca() {
        return confianca > 0 ? confianca : 0.88;
    }

    public void setConfianca(double confianca) {
        this.confianca = confianca;
    }

    public String getObservacoes() {
        if (observacoes != null && !observacoes.isEmpty()) {
            return observacoes;
        }
        return reportText;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getReportText() {
        return reportText;
    }

    public void setReportText(String reportText) {
        this.reportText = reportText;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    /**
     * Converte um AiReportResponseDTO da API QA (GET /incidents/{id}/ai-report) para AiAnalysisJSON.
     */
    public static AiAnalysisJSON fromAiReport(AiReportResponseDTO report, double estimatedKg) {
        AiAnalysisJSON json = new AiAnalysisJSON();
        if (report == null) {
            return createOfflineFallback(null);
        }
        json.incidentId = report.getIncidentId();
        json.material = (report.getDetectedWasteType() != null && !report.getDetectedWasteType().trim().isEmpty())
                ? report.getDetectedWasteType().trim()
                : "Papelão A";
        json.quantidadeEstimada = estimatedKg > 0 ? estimatedKg : 38.0;
        json.unidade = "kg";
        json.confianca = 0.92;
        json.aiContaminationLevel = report.getAiContaminationLevel();
        json.reportText = report.getReportText();
        json.observacoes = (report.getRecommendations() != null && !report.getRecommendations().trim().isEmpty())
                ? report.getRecommendations().trim()
                : (report.getReportText() != null && !report.getReportText().trim().isEmpty()
                ? report.getReportText().trim()
                : "Guarde num lugar seco e coberto. Coleta em até 24h mantém o material valioso.");

        Contaminacao c = new Contaminacao();
        String mappedLevel = mapContaminationLabel(report.getAiContaminationLevel());
        c.nivel = mappedLevel;
        c.presente = !"Baixa".equalsIgnoreCase(mappedLevel);
        json.contaminacao = c;

        Unidades u = new Unidades();
        u.tipo = "Caixas";
        u.quantidade = 11;
        json.unidades = u;
        return json;
    }

    public static String mapContaminationLabel(String rawLevel) {
        if (rawLevel == null || rawLevel.trim().isEmpty()) {
            return "Baixa";
        }
        String upper = rawLevel.trim().toUpperCase();
        if ("HIGH".equals(upper) || "ALTA".equals(upper) || "CRITICAL".equals(upper)) {
            return "Alta";
        }
        if ("MEDIUM".equals(upper) || "MEDIA".equals(upper) || "MÉDIA".equals(upper)) {
            return "Média";
        }
        if ("LOW".equals(upper) || "BAIXA".equals(upper)) {
            return "Baixa";
        }
        return rawLevel.trim();
    }

    public static AiAnalysisJSON createOfflineFallback(String setor) {
        AiAnalysisJSON json = new AiAnalysisJSON();
        json.material = "Papelão A";
        json.quantidadeEstimada = 38.0;
        json.unidade = "kg";
        json.confianca = 0.88;
        json.observacoes = "Guarde num lugar seco e coberto. Coleta em até 24h mantém o material valioso.";
        Contaminacao c = new Contaminacao();
        c.presente = false;
        c.nivel = "Baixa";
        json.contaminacao = c;
        Unidades u = new Unidades();
        u.tipo = "Caixas";
        u.quantidade = 11;
        json.unidades = u;
        return json;
    }

    /**
     * Converte o retorno de GET /incidents/{id}/scoring-context em AiAnalysisJSON quando aiReport está presente.
     */
    public static AiAnalysisJSON fromScoringContext(JsonObject ctx, double estimatedKg) {
        if (ctx == null) {
            return null;
        }
        String incId = ctx.has("incidentId") && !ctx.get("incidentId").isJsonNull()
                ? ctx.get("incidentId").getAsString() : null;
        if (ctx.has("aiReport") && ctx.get("aiReport").isJsonObject()) {
            JsonObject repObj = ctx.getAsJsonObject("aiReport");
            AiReportResponseDTO dto = new AiReportResponseDTO();
            dto.setIncidentId(incId);
            if (repObj.has("detectedWasteType") && !repObj.get("detectedWasteType").isJsonNull()) {
                dto.setDetectedWasteType(repObj.get("detectedWasteType").getAsString());
            }
            if (repObj.has("aiContaminationLevel") && !repObj.get("aiContaminationLevel").isJsonNull()) {
                dto.setAiContaminationLevel(repObj.get("aiContaminationLevel").getAsString());
            }
            if (repObj.has("recommendations") && !repObj.get("recommendations").isJsonNull()) {
                dto.setRecommendations(repObj.get("recommendations").getAsString());
            }
            if (repObj.has("reportText") && !repObj.get("reportText").isJsonNull()) {
                dto.setReportText(repObj.get("reportText").getAsString());
            }
            return fromAiReport(dto, estimatedKg);
        }
        return null;
    }

    /**
     * Resolve o UUID da área em GET /areas/mine ignorando acentos e diferenças de pontuação.
     */
    public static String resolveAreaId(JsonArray array, String preferredSector) {
        if (array == null || array.size() == 0) {
            return null;
        }
        String firstId = null;
        String normPref = normalize(preferredSector);
        for (JsonElement el : array) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("id") && !obj.get("id").isJsonNull()) {
                    String id = obj.get("id").getAsString();
                    if (firstId == null) {
                        firstId = id;
                    }
                    if (!normPref.isEmpty() && obj.has("sectorName") && !obj.get("sectorName").isJsonNull()) {
                        String normSec = normalize(obj.get("sectorName").getAsString());
                        if (!normSec.isEmpty() && (normPref.contains(normSec) || normSec.contains(normPref))) {
                            return id;
                        }
                    }
                }
            }
        }
        return firstId;
    }

    /**
     * Resolve o UUID do tipo de resíduo em GET /waste-types com base na descrição (default: PAPER/PAPEL).
     */
    public static String resolveWasteTypeId(JsonArray array, String description) {
        if (array == null || array.size() == 0) {
            return null;
        }
        String normDesc = normalize(description);
        String targetCategory = "PAPER";
        if (normDesc.contains("plast") || normDesc.contains("isopor") || normDesc.contains("eps") || normDesc.contains("pet")) {
            targetCategory = "PLASTIC";
        } else if (normDesc.contains("metal") || normDesc.contains("lata") || normDesc.contains("alumin")) {
            targetCategory = "METAL";
        } else if (normDesc.contains("vidro") || normDesc.contains("garrafa")) {
            targetCategory = "GLASS";
        } else if (normDesc.contains("organ") || normDesc.contains("aliment")) {
            targetCategory = "ORGANIC";
        } else if (normDesc.contains("madeira") || normDesc.contains("pallet") || normDesc.contains("palete")) {
            targetCategory = "WOOD";
        } else if (normDesc.contains("eletron") || normDesc.contains("bateria") || normDesc.contains("pilha")) {
            targetCategory = "ELECTRONIC";
        } else if (normDesc.contains("oleo") || normDesc.contains("quimic") || normDesc.contains("perigos")) {
            targetCategory = "CHEMICAL";
        }

        String firstId = null;
        String paperFallbackId = null;
        for (JsonElement el : array) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("id") && !obj.get("id").isJsonNull()) {
                    String id = obj.get("id").getAsString();
                    if (firstId == null) {
                        firstId = id;
                    }
                    String cat = obj.has("category") && !obj.get("category").isJsonNull()
                            ? normalize(obj.get("category").getAsString()) : "";
                    if (cat.contains("paper") || cat.contains("papel")) {
                        paperFallbackId = id;
                    }
                    if (cat.contains(targetCategory.toLowerCase(Locale.ROOT))) {
                        return id;
                    }
                }
            }
        }
        return paperFallbackId != null ? paperFallbackId : firstId;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static class Contaminacao {
        private boolean presente;
        private String nivel;

        public boolean isPresente() {
            return presente;
        }

        public void setPresente(boolean presente) {
            this.presente = presente;
        }

        public String getNivel() {
            return nivel;
        }

        public void setNivel(String nivel) {
            this.nivel = nivel;
        }
    }

    public static class Unidades {
        private String tipo;
        private int quantidade;

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public int getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(int quantidade) {
            this.quantidade = quantidade;
        }
    }
}
