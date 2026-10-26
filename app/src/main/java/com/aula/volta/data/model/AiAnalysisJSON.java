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

    @SerializedName("mobile_summary")
    private String mobileSummary;

    @SerializedName("analysis_id")
    private String analysisId;

    @SerializedName("provenance_token")
    private String provenanceToken;

    private transient JsonObject rawAiData;

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

    public String getMobileSummary() {
        return mobileSummary;
    }

    public void setMobileSummary(String mobileSummary) {
        this.mobileSummary = mobileSummary;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getProvenanceToken() {
        return provenanceToken;
    }

    public void setProvenanceToken(String provenanceToken) {
        this.provenanceToken = provenanceToken;
    }

    public JsonObject getRawAiData() {
        return rawAiData;
    }

    public void setRawAiData(JsonObject rawAiData) {
        this.rawAiData = rawAiData;
    }

    /**
     * Converte a resposta do endpoint de visao de IA (POST /v1/occurrences/predict) para AiAnalysisJSON.
     */
    public static AiAnalysisJSON fromPredict(JsonObject json, double fallbackKg) {
        if (json == null) {
            return createOfflineFallback(null);
        }
        AiAnalysisJSON res = new AiAnalysisJSON();
        res.rawAiData = json;

        if (json.has("analysis_id") && !json.get("analysis_id").isJsonNull()) {
            res.analysisId = json.get("analysis_id").getAsString();
            res.incidentId = res.analysisId;
        }
        if (json.has("provenance_token") && !json.get("provenance_token").isJsonNull()) {
            res.provenanceToken = json.get("provenance_token").getAsString();
        }

        String rawType = json.has("detected_waste_type") && !json.get("detected_waste_type").isJsonNull()
                ? json.get("detected_waste_type").getAsString() : "Papelão A";
        if (rawType.toLowerCase(Locale.ROOT).contains("não confirmado") || rawType.trim().isEmpty()) {
            rawType = "Papelão A";
        }
        res.material = rawType.trim();

        double weight = fallbackKg > 0 ? fallbackKg : 38.0;
        if (json.has("estimated_quantity_kg") && !json.get("estimated_quantity_kg").isJsonNull()) {
            try {
                double val = json.get("estimated_quantity_kg").getAsDouble();
                if (val > 0) {
                    weight = val;
                }
            } catch (Exception ignored) {
            }
        }
        res.quantidadeEstimada = weight;
        res.unidade = "kg";
        res.confianca = 0.94;

        String rawContam = json.has("ai_contamination_level") && !json.get("ai_contamination_level").isJsonNull()
                ? json.get("ai_contamination_level").getAsString() : "BAIXO";
        res.aiContaminationLevel = rawContam;

        Contaminacao c = new Contaminacao();
        c.nivel = mapContaminationLabel(rawContam);
        c.presente = !"Baixa".equalsIgnoreCase(c.nivel);
        res.contaminacao = c;

        if (json.has("recommendations") && !json.get("recommendations").isJsonNull()) {
            res.observacoes = json.get("recommendations").getAsString().trim();
        } else {
            res.observacoes = "Guarde num lugar seco e coberto. Coleta em até 24h mantém o material valioso.";
        }

        if (json.has("report_text") && !json.get("report_text").isJsonNull()) {
            res.reportText = json.get("report_text").getAsString().trim();
        } else {
            res.reportText = res.observacoes;
        }

        if (json.has("mobile_summary") && !json.get("mobile_summary").isJsonNull()) {
            res.mobileSummary = json.get("mobile_summary").getAsString().trim();
        }

        Unidades u = new Unidades();
        String lower = rawType.toLowerCase(Locale.ROOT);
        if (lower.contains("plást") || lower.contains("plast")) {
            u.tipo = "Fardos";
            u.quantidade = 6;
        } else if (lower.contains("metal") || lower.contains("alumin") || lower.contains("ferr")) {
            u.tipo = "Tambores";
            u.quantidade = 4;
        } else if (lower.contains("vidro")) {
            u.tipo = "Engradados";
            u.quantidade = 12;
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            u.tipo = "Sacos";
            u.quantidade = 8;
        } else {
            u.tipo = "Caixas";
            u.quantidade = 11;
        }
        res.unidades = u;

        return res;
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
        String upper = rawLevel.trim().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(upper) || "ALTA".equals(upper) || "ALTO".equals(upper) || "CRITICAL".equals(upper)) {
            return "Alta";
        }
        if ("MEDIUM".equals(upper) || "MEDIA".equals(upper) || "MÉDIA".equals(upper) || "MEDIO".equals(upper) || "MÉDIO".equals(upper)) {
            return "Média";
        }
        if ("LOW".equals(upper) || "BAIXA".equals(upper) || "BAIXO".equals(upper) || "NENHUMA".equals(upper) || "NENHUM".equals(upper)) {
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

    public static String extractMaterialName(String rawMaterial) {
        if (rawMaterial == null) {
            return "Papelão";
        }
        String clean = rawMaterial
                .replaceAll("(?i)\\s*\\(Classe\\s+[ABC]\\)", "")
                .replaceAll("(?i)\\s+Classe\\s+[ABC]$", "")
                .replaceAll("\\s+[ABC]$", "")
                .trim();
        return clean.isEmpty() ? "Papelão" : clean;
    }

    public static String extractQualityClassLetter(String rawMaterial, String explicitClass) {
        String source = (explicitClass != null && !explicitClass.trim().isEmpty())
                ? explicitClass.trim()
                : (rawMaterial != null ? rawMaterial.trim() : "");
        if (source.matches("(?i).*(\\bClasse\\s+B\\b|\\s+B$|^B$).*")) {
            return "B";
        }
        if (source.matches("(?i).*(\\bClasse\\s+C\\b|\\s+C$|^C$).*")) {
            return "C";
        }
        if (rawMaterial != null) {
            String rm = rawMaterial.trim();
            if (rm.matches("(?i).*(\\bClasse\\s+B\\b|\\s+B$).*")) {
                return "B";
            }
            if (rm.matches("(?i).*(\\bClasse\\s+C\\b|\\s+C$).*")) {
                return "C";
            }
        }
        return "A";
    }

    public static String formatMaterialWithClass(String rawMaterial, String classSpec) {
        String baseMat = extractMaterialName(rawMaterial);
        String letter = extractQualityClassLetter(rawMaterial, classSpec);
        return baseMat + " (Classe " + letter + ")";
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
