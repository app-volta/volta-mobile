package com.aula.volta.data.model;

import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.text.Normalizer;
import java.util.Locale;

public class AiAnalysisJSON {

    @SerializedName(value = "material", alternate = {"detectedWasteType", "detected_waste_type"})
    private String material;

    @SerializedName(value = "primaryMaterial", alternate = {"primary_material", "material_principal"})
    private String primaryMaterial;

    @SerializedName(value = "specificItem", alternate = {"specific_item", "item_especifico"})
    private String specificItem;

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

    public String getPrimaryMaterial() {
        if (primaryMaterial != null && !primaryMaterial.trim().isEmpty()) {
            return primaryMaterial.trim();
        }
        return classifyPrimaryMaterial(material, rawAiData != null ? rawAiData.toString() : "", "");
    }

    public void setPrimaryMaterial(String primaryMaterial) {
        this.primaryMaterial = primaryMaterial;
    }

    public String getSpecificItem() {
        if (specificItem != null && !specificItem.trim().isEmpty()) {
            return specificItem.trim();
        }
        return extractSpecificItem(material);
    }

    public void setSpecificItem(String specificItem) {
        this.specificItem = specificItem;
    }

    public double getQuantidadeEstimada() {
        return quantidadeEstimada;
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
        return confianca;
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
                ? json.get("detected_waste_type").getAsString() : "Outros";
        if (rawType.toLowerCase(Locale.ROOT).contains("não confirmado") || rawType.trim().isEmpty()) {
            rawType = "Outros";
        }
        res.material = extractMaterialName(rawType);
        res.specificItem = res.material;
        res.primaryMaterial = classifyPrimaryMaterial(rawType, json.toString(), "");

        boolean hasExplicitKg = false;
        double weight = fallbackKg > 0 ? fallbackKg : 0.0;
        if (json.has("estimated_quantity_kg") && !json.get("estimated_quantity_kg").isJsonNull()) {
            try {
                double val = json.get("estimated_quantity_kg").getAsDouble();
                if (val > 0) {
                    weight = val;
                    hasExplicitKg = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!hasExplicitKg && json.has("quantity_kg") && !json.get("quantity_kg").isJsonNull()) {
            try {
                double val = json.get("quantity_kg").getAsDouble();
                if (val > 0) {
                    weight = val;
                    hasExplicitKg = true;
                }
            } catch (Exception ignored) {
            }
        }

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
        if (lower.contains("cadeira") || lower.contains("mesa") || lower.contains("móvel")
                || lower.contains("movel") || lower.contains("mobili") || lower.contains("assento")) {
            u.tipo = "Unidades";
            u.quantidade = 1;
            if (!hasExplicitKg && weight <= 0) weight = 5.0;
        } else if (lower.contains("madeira") || lower.contains("pallet") || lower.contains("palete") || lower.contains("estrado")) {
            u.tipo = "Paletes";
            u.quantidade = 0;
        } else if (lower.contains("plást") || lower.contains("plast") || lower.contains("isopor") || lower.contains("eps") || lower.contains("pet")) {
            u.tipo = "Fardos";
            u.quantidade = 0;
        } else if (lower.contains("metal") || lower.contains("alumin") || lower.contains("ferr")
                || lower.contains("aço") || lower.contains("aco")) {
            u.tipo = "Tambores";
            u.quantidade = 0;
        } else if (lower.contains("vidro")) {
            u.tipo = "Engradados";
            u.quantidade = 0;
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            u.tipo = "Sacos";
            u.quantidade = 0;
        } else if (lower.contains("papel") || lower.contains("papelão") || lower.contains("cartao") || lower.contains("cartão")) {
            u.tipo = "Caixas";
            u.quantidade = 0;
        } else {
            u.tipo = "Unidades";
            u.quantidade = 0;
        }

        if (json.has("units_count") && !json.get("units_count").isJsonNull()) {
            try {
                int count = json.get("units_count").getAsInt();
                if (count > 0) u.quantidade = count;
            } catch (Exception ignored) {
            }
        }
        if (json.has("unit_type") && !json.get("unit_type").isJsonNull()) {
            String ut = json.get("unit_type").getAsString();
            if (!ut.trim().isEmpty()) u.tipo = ut.trim();
        }

        res.quantidadeEstimada = weight > 0 ? weight : (fallbackKg > 0 ? fallbackKg : 0.0);
        res.unidade = "kg";
        if (json.has("confidence") && !json.get("confidence").isJsonNull()) {
            try {
                res.confianca = json.get("confidence").getAsDouble();
            } catch (Exception ignored) {
                res.confianca = 0.0;
            }
        } else if (json.has("confianca") && !json.get("confianca").isJsonNull()) {
            try {
                res.confianca = json.get("confianca").getAsDouble();
            } catch (Exception ignored) {
                res.confianca = 0.0;
            }
        } else {
            res.confianca = 0.0;
        }
        res.unidades = u;

        return res;
    }

    public static AiAnalysisJSON fromAiReport(AiReportResponseDTO report, double estimatedKg) {
        AiAnalysisJSON json = new AiAnalysisJSON();
        if (report == null) {
            AiAnalysisJSON fallback = createOfflineFallback("Papelão");
            fallback.material = "Papelão";
            return fallback;
        }
        json.incidentId = report.getIncidentId();
        String detectedType = (report.getDetectedWasteType() != null && !report.getDetectedWasteType().trim().isEmpty())
                ? report.getDetectedWasteType().trim()
                : "Papelão";
        json.material = extractMaterialName(detectedType);
        json.specificItem = json.material;
        json.primaryMaterial = classifyPrimaryMaterial(detectedType, report.getReportText(), "");

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
        String lower = detectedType.toLowerCase(Locale.ROOT);
        if (lower.contains("cadeira") || lower.contains("mesa") || lower.contains("móvel")
                || lower.contains("movel") || lower.contains("mobili") || lower.contains("assento")) {
            u.tipo = "Unidades";
            u.quantidade = 1;
        } else if (lower.contains("madeira") || lower.contains("pallet") || lower.contains("palete") || lower.contains("estrado")) {
            u.tipo = "Paletes";
            u.quantidade = 0;
        } else if (lower.contains("plást") || lower.contains("plast") || lower.contains("isopor") || lower.contains("eps") || lower.contains("pet")) {
            u.tipo = "Fardos";
            u.quantidade = 0;
        } else if (lower.contains("metal") || lower.contains("alumin") || lower.contains("ferr")
                || lower.contains("aço") || lower.contains("aco")) {
            u.tipo = "Tambores";
            u.quantidade = 0;
        } else if (lower.contains("vidro")) {
            u.tipo = "Engradados";
            u.quantidade = 0;
        } else if (lower.contains("orgân") || lower.contains("organ")) {
            u.tipo = "Sacos";
            u.quantidade = 0;
        } else {
            u.tipo = "Caixas";
            u.quantidade = 0;
        }
        json.unidades = u;

        json.quantidadeEstimada = estimatedKg > 0 ? estimatedKg : 0.0;
        json.unidade = "kg";
        json.confianca = 0.0;
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

    public static String calculatePriority(String wasteType, String contaminationLevel, String description) {
        String text = ((wasteType != null ? wasteType : "") + " "
                + (description != null ? description : "")).toLowerCase(Locale.ROOT);
        String contam = contaminationLevel != null
                ? contaminationLevel.toUpperCase(Locale.ROOT)
                : "LOW";

        boolean isPaper = text.contains("papel") || text.contains("cartao") || text.contains("caixa");
        boolean hasHazard = text.contains("óleo") || text.contains("oleo") || text.contains("graxa")
                || text.contains("inflam") || text.contains("bateria") || text.contains("pilha")
                || text.contains("lâmpada") || text.contains("lampada") || text.contains("tóxic")
                || text.contains("toxic") || text.contains("solvente")
                || (!isPaper && (text.contains("perig") || text.contains("químic") || text.contains("quimic")))
                || (isPaper && (text.contains("papelão contaminado com óleo") || text.contains("impregnado com óleo") || text.contains("resíduo perigoso")));

        if (contam.contains("HIGH") || contam.contains("ALTA") || contam.contains("CRITICAL") || hasHazard) {
            return "ALTA";
        }

        if (contam.contains("MED") || contam.contains("MÉD")
                || text.contains("organ") || text.contains("orgân") || text.contains("alimento")
                || text.contains("comida") || text.contains("perecivel") || text.contains("perecível")
                || (!isPaper && text.contains("molhado"))) {
            return "MEDIA";
        }

        return "BAIXA";
    }

    public static AiAnalysisJSON createOfflineFallback(String setor) {
        return createOfflineFallback(setor, null);
    }

    public static AiAnalysisJSON createOfflineFallback(String setor, String description) {
        AiAnalysisJSON json = new AiAnalysisJSON();
        String desc = description != null ? description.toLowerCase(Locale.ROOT) : "";
        if (desc.contains("cadeira") || desc.contains("mesa") || desc.contains("móvel") || desc.contains("movel")) {
            json.primaryMaterial = "Outros";
            json.specificItem = "Mobiliário";
            json.material = "Mobiliário";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Unidades";
            u.quantidade = 0;
            json.unidades = u;
        } else if (desc.contains("madeira") || desc.contains("pallet") || desc.contains("palete")) {
            json.primaryMaterial = "Madeira";
            json.specificItem = "Palete PBR";
            json.material = "Palete PBR";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Paletes";
            u.quantidade = 0;
            json.unidades = u;
        } else if (desc.contains("manuten") || desc.contains("usinagem") || desc.contains("oficina")) {
            json.primaryMaterial = "Metal";
            json.specificItem = "Sucata Metálica";
            json.material = "Sucata Metálica";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Lotes";
            u.quantidade = 0;
            json.unidades = u;
        } else if (desc.contains("plast") || desc.contains("plást") || desc.contains("pet") || desc.contains("filme")) {
            json.primaryMaterial = "Plástico";
            json.specificItem = "Plástico Filme";
            json.material = "Plástico Filme";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Fardos";
            u.quantidade = 0;
            json.unidades = u;
        } else if (desc.contains("papel") || desc.contains("cartao") || desc.contains("kraft")) {
            json.primaryMaterial = "Papelão";
            json.specificItem = "Caixas de Papelão";
            json.material = "Caixas de Papelão";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Caixas";
            u.quantidade = 0;
            json.unidades = u;
        } else {
            json.primaryMaterial = "Nenhum";
            json.specificItem = "Nenhum";
            json.material = "Nenhum";
            json.quantidadeEstimada = 0.0;
            Unidades u = new Unidades();
            u.tipo = "Unidades";
            u.quantidade = 0;
            json.unidades = u;
        }
        json.unidade = "kg";
        json.confianca = 0.0;
        json.observacoes = "Guarde num lugar seco e coberto. Coleta em até 24h mantém o material valioso.";
        Contaminacao c = new Contaminacao();
        c.presente = false;
        c.nivel = "Baixa";
        json.contaminacao = c;
        return json;
    }

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

    public static boolean isPureCategoryOrGeneric(String label) {
        if (label == null) {
            return true;
        }
        String s = normalize(label);
        return s.isEmpty()
                || s.equals("nenhum")
                || s.equals("indefinido")
                || s.equals("residuo")
                || s.equals("residuo reciclavel")
                || s.equals("reciclavel")
                || s.equals("outros")
                || s.equals("outro")
                || s.equals("nao confirmado")
                || s.equals("plastico")
                || s.equals("papelao")
                || s.equals("metal")
                || s.equals("vidro")
                || s.equals("madeira")
                || s.equals("organico")
                || s.equals("perigoso");
    }

    public static boolean isGenericLabel(String label) {
        if (label == null) {
            return true;
        }
        String s = normalize(label);
        return s.isEmpty()
                || s.equals("nenhum")
                || s.equals("indefinido")
                || s.equals("residuo")
                || s.equals("residuo reciclavel")
                || s.equals("reciclavel")
                || s.equals("outros")
                || s.equals("outro")
                || s.equals("nao confirmado");
    }

    public static String resolveAiTitle(AiAnalysisJSON analysis, String userDescription, JsonObject predictJson) {
        if (userDescription != null && !userDescription.trim().isEmpty() && !isPureCategoryOrGeneric(userDescription)) {
            return userDescription.trim();
        }

        if (predictJson != null) {
            for (String key : new String[]{"title", "specific_item", "item", "detected_item", "detected_waste_type"}) {
                if (predictJson.has(key) && !predictJson.get(key).isJsonNull() && predictJson.get(key).isJsonPrimitive()) {
                    String val = predictJson.get(key).getAsString().trim();
                    if (!val.isEmpty() && !isPureCategoryOrGeneric(val)) {
                        return val;
                    }
                }
            }

            for (String arrKey : new String[]{"detected_waste_types", "waste_types", "possibilities", "possible_waste_types", "candidates", "classes"}) {
                if (predictJson.has(arrKey) && predictJson.get(arrKey).isJsonArray()) {
                    JsonArray arr = predictJson.getAsJsonArray(arrKey);
                    for (JsonElement el : arr) {
                        String candidate = "";
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            candidate = el.getAsString().trim();
                        } else if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            for (String p : new String[]{"name", "title", "item", "description", "material", "type"}) {
                                if (o.has(p) && !o.get(p).isJsonNull()) {
                                    candidate = o.get(p).getAsString().trim();
                                    break;
                                }
                            }
                        }
                        if (!candidate.isEmpty() && !isPureCategoryOrGeneric(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        if (analysis != null) {
            if (analysis.getSpecificItem() != null && !analysis.getSpecificItem().trim().isEmpty() && !isPureCategoryOrGeneric(analysis.getSpecificItem())) {
                return analysis.getSpecificItem().trim();
            }
            if (analysis.getMaterial() != null && !analysis.getMaterial().trim().isEmpty() && !isPureCategoryOrGeneric(analysis.getMaterial())) {
                return analysis.getMaterial().trim();
            }
            if (analysis.getPrimaryMaterial() != null && !analysis.getPrimaryMaterial().trim().isEmpty() && !isPureCategoryOrGeneric(analysis.getPrimaryMaterial())) {
                return analysis.getPrimaryMaterial().trim();
            }
        }

        if (predictJson != null) {
            for (String key : new String[]{"title", "specific_item", "item", "detected_item", "detected_waste_type"}) {
                if (predictJson.has(key) && !predictJson.get(key).isJsonNull() && predictJson.get(key).isJsonPrimitive()) {
                    String val = predictJson.get(key).getAsString().trim();
                    if (!val.isEmpty() && !isGenericLabel(val)) {
                        return val;
                    }
                }
            }
        }

        if (userDescription != null && !userDescription.trim().isEmpty() && !isGenericLabel(userDescription)) {
            return userDescription.trim();
        }
        if (analysis != null && analysis.getMaterial() != null && !analysis.getMaterial().trim().isEmpty() && !isGenericLabel(analysis.getMaterial())) {
            return analysis.getMaterial().trim();
        }
        if (analysis != null && analysis.getPrimaryMaterial() != null && !analysis.getPrimaryMaterial().trim().isEmpty() && !isGenericLabel(analysis.getPrimaryMaterial())) {
            return analysis.getPrimaryMaterial().trim();
        }

        return "Nenhum";
    }

    public static java.util.List<String> extractAiCandidateWasteNames(JsonObject predictJson, AiAnalysisJSON analysis, String userDescription) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (predictJson != null) {
            String[] arrayKeys = new String[]{
                    "detected_waste_types", "waste_types", "possibilities", "possible_waste_types",
                    "candidates", "alternatives", "classes", "detected_types", "types",
                    "categories", "category", "labels", "predicoes", "possibilidades"
            };
            for (String key : arrayKeys) {
                if (predictJson.has(key) && predictJson.get(key).isJsonArray()) {
                    JsonArray arr = predictJson.getAsJsonArray(key);
                    for (JsonElement el : arr) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            String val = el.getAsString().trim();
                            if (!val.isEmpty() && !list.contains(val)) {
                                list.add(val);
                            }
                        } else if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            for (String prop : new String[]{"name", "type", "category", "description", "waste_type", "material", "label", "title", "item"}) {
                                if (o.has(prop) && !o.get(prop).isJsonNull()) {
                                    String v = o.get(prop).getAsString().trim();
                                    if (!v.isEmpty() && !list.contains(v)) {
                                        list.add(v);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (String singleKey : new String[]{"detected_waste_type", "waste_type", "material", "title", "specific_item", "category"}) {
                if (predictJson.has(singleKey) && !predictJson.get(singleKey).isJsonNull() && predictJson.get(singleKey).isJsonPrimitive()) {
                    String val = predictJson.get(singleKey).getAsString().trim();
                    if (!val.isEmpty() && !list.contains(val)) {
                        list.add(0, val);
                    }
                }
            }
        }
        if (analysis != null) {
            if (analysis.getSpecificItem() != null && !analysis.getSpecificItem().trim().isEmpty()) {
                String s = analysis.getSpecificItem().trim();
                if (!list.contains(s)) list.add(0, s);
            }
            if (analysis.getMaterial() != null && !analysis.getMaterial().trim().isEmpty()) {
                String m = analysis.getMaterial().trim();
                if (!list.contains(m)) list.add(0, m);
            }
            if (analysis.getPrimaryMaterial() != null && !analysis.getPrimaryMaterial().trim().isEmpty()) {
                String p = analysis.getPrimaryMaterial().trim();
                if (!list.contains(p)) list.add(p);
            }
        }
        if (userDescription != null && !userDescription.trim().isEmpty() && !isGenericLabel(userDescription)) {
            String u = userDescription.trim();
            if (!list.contains(u)) list.add(0, u);
        }
        return list;
    }

    public static java.util.List<String> resolveAllWasteTypeIds(JsonArray catalog, java.util.List<String> candidates) {
        java.util.List<String> resultIds = new java.util.ArrayList<>();
        if (catalog == null || catalog.size() == 0) {
            return resultIds;
        }

        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate == null || candidate.trim().isEmpty() || isGenericLabel(candidate)) continue;
                String singleId = resolveWasteTypeId(catalog, candidate);
                if (singleId != null && !resultIds.contains(singleId)) {
                    resultIds.add(singleId);
                }
            }
        }

        if (resultIds.isEmpty()) {
            if (candidates != null) {
                for (String candidate : candidates) {
                    if (candidate == null || candidate.trim().isEmpty()) continue;
                    String singleId = resolveWasteTypeId(catalog, candidate);
                    if (singleId != null && !resultIds.contains(singleId)) {
                        resultIds.add(singleId);
                    }
                }
            }
        }

        if (resultIds.isEmpty()) {
            String defaultId = resolveWasteTypeId(catalog, "");
            if (defaultId != null) {
                resultIds.add(defaultId);
            }
        }
        return resultIds;
    }

    public static String getStaticWasteTypeId(String material) {
        if (material == null) return "bd985415-4b25-4c07-beca-cc086dce9b13";
        String norm = normalize(material);
        if (norm.contains("papel") || norm.contains("cartao") || norm.contains("kraft")) {
            return "bd985415-4b25-4c07-beca-cc086dce9b13";
        }
        if (norm.contains("plast") || norm.contains("pet") || norm.contains("pvc")) {
            return "8b3f6a14-9a27-462f-b414-24329d927af7";
        }
        if (norm.contains("metal") || norm.contains("alumin") || norm.contains("ferr") || norm.contains("aco")) {
            return "5ee946da-2975-45e7-80d7-a5a8e49b4b2d";
        }
        if (norm.contains("vidro")) {
            return "2605dca7-b392-4835-a2a7-99a1aa68a7ab";
        }
        if (norm.contains("madeira") || norm.contains("pallet") || norm.contains("palete")) {
            return "e9e077a8-7464-422d-a3be-44bc770fd5d2";
        }
        if (norm.contains("organ") || norm.contains("alimento") || norm.contains("comida")) {
            return "5cb7e3c8-6dac-45e4-870f-92cc64a2a452";
        }
        if (norm.contains("eletron") || norm.contains("bateria") || norm.contains("pilha")) {
            return "7807fd3c-7c11-42c3-95be-48884968d3c1";
        }
        if (norm.contains("quimic") || norm.contains("oleo") || norm.contains("tinta") || norm.contains("solvente")) {
            return "db21c099-511a-4dbc-82dc-2090b4b6d0a5";
        }
        if (norm.contains("textil") || norm.contains("tecido") || norm.contains("pano")) {
            return "9221e10b-b15b-40d8-a798-49f372ab9efb";
        }
        return "dc9b7d17-cd48-475a-a313-d5de98505854";
    }

    public static String resolveWasteTypeId(JsonArray array, String description) {
        if (array == null || array.size() == 0) {
            return getStaticWasteTypeId(description);
        }
        String normDesc = normalize(description);
        String primary = classifyPrimaryMaterial(description, "", "");
        String normPrimary = normalize(primary);

        String[] targetKeywords;
        if (normPrimary.contains("papel")) {
            targetKeywords = new String[]{"papel", "paper"};
        } else if (normPrimary.contains("plast")) {
            targetKeywords = new String[]{"plast", "plastic"};
        } else if (normPrimary.contains("met")) {
            targetKeywords = new String[]{"metal", "alumin", "lata"};
        } else if (normPrimary.contains("vidr")) {
            targetKeywords = new String[]{"vidro", "glass"};
        } else if (normPrimary.contains("org") || normDesc.contains("organ")) {
            targetKeywords = new String[]{"organ", "biodegrad"};
        } else if (normPrimary.contains("madeir") || normDesc.contains("pallet") || normDesc.contains("palete") || normDesc.contains("mobil")) {
            targetKeywords = new String[]{"madeira", "wood"};
        } else if (normPrimary.contains("perig") || normDesc.contains("quimic") || normDesc.contains("oleo")) {
            targetKeywords = new String[]{"quimic", "chemical", "perigo"};
        } else if (normDesc.contains("eletron") || normDesc.contains("bateria") || normDesc.contains("pilha")) {
            targetKeywords = new String[]{"eletron", "electronic"};
        } else if (normDesc.contains("textil") || normDesc.contains("tecido")) {
            targetKeywords = new String[]{"textil", "fabric"};
        } else {
            targetKeywords = new String[]{"outro", "misto", "geral", "indefinido", "nenhum"};
        }

        String firstId = null;
        String fallbackPaperId = null;
        String fallbackOtherId = null;
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
                    String desc = obj.has("description") && !obj.get("description").isJsonNull()
                            ? normalize(obj.get("description").getAsString()) : "";

                    if (cat.contains("papel") || cat.contains("paper")) {
                        fallbackPaperId = id;
                    }
                    if (cat.contains("outro") || cat.contains("geral") || cat.contains("misto") || desc.contains("outro") || desc.contains("geral")) {
                        fallbackOtherId = id;
                    }

                    for (String kw : targetKeywords) {
                        if (cat.contains(kw) || desc.contains(kw)) {
                            return id;
                        }
                    }
                }
            }
        }
        if (normDesc.isEmpty() && fallbackPaperId != null) {
            return fallbackPaperId;
        }
        return fallbackOtherId != null ? fallbackOtherId : (fallbackPaperId != null ? fallbackPaperId : (firstId != null ? firstId : getStaticWasteTypeId(description)));
    }

    public static String classifyPrimaryMaterial(String detectedItem, String rawText, String userDescription) {
        String combined = normalize((detectedItem != null ? detectedItem : "") + " "
                + (rawText != null ? rawText : "") + " "
                + (userDescription != null ? userDescription : ""));

        if (combined.isEmpty() || combined.equals("nenhum") || combined.equals("indefinido")) {
            return "Outros";
        }

        if (combined.contains("perig") || combined.contains("quimic") || combined.contains("oleo")
                || combined.contains("graxa") || combined.contains("solvente") || combined.contains("tinta")
                || combined.contains("bateria") || combined.contains("pilha") || combined.contains("lampada")
                || combined.contains("toxic") || combined.contains("estopa contaminada")) {
            return "Perigoso";
        }

        if (combined.contains("madeira") || combined.contains("pallet") || combined.contains("palete")
                || combined.contains("estrado") || combined.contains("caixote") || combined.contains("tabua")
                || combined.contains("compensado") || combined.contains("mdf") || combined.contains("pbr")
                || combined.contains("cadeira de madeira") || combined.contains("mesa de madeira")) {
            return "Madeira";
        }

        if (combined.contains("vidro") || combined.contains("garrafa de vidro") || combined.contains("caco")
                || combined.contains("frasco de vidro") || combined.contains("pote de vidro") || combined.contains("espelho")) {
            return "Vidro";
        }

        if (combined.contains("metal") || combined.contains("alumin") || combined.contains("ferro")
                || combined.contains("aco") || combined.contains("cobre") || combined.contains("lata")
                || combined.contains("latinha") || combined.contains("sucata") || combined.contains("perfil")
                || combined.contains("tambor metalico") || combined.contains("cadeira de ferro") || combined.contains("cadeira de metal")) {
            return "Metal";
        }

        if (combined.contains("plast") || combined.contains("isopor") || combined.contains("eps")
                || combined.contains("pet") || combined.contains("polietileno") || combined.contains("polipropileno")
                || combined.contains("pvc") || combined.contains("filme stretch") || combined.contains("bombona")
                || combined.contains("copo plastico") || combined.contains("sacola plastica") || combined.contains("sacola de plastico")
                || combined.contains("cadeira") || combined.contains("assento") || combined.contains("poltrona")
                || combined.contains("garrafa pet")) {
            return "Plástico";
        }

        if (combined.contains("papel") || combined.contains("papelao") || combined.contains("cartao")
                || combined.contains("kraft") || combined.contains("jornal") || combined.contains("revista")
                || combined.contains("documento") || combined.contains("caixa de papel") || combined.contains("caixas de papel")) {
            return "Papelão";
        }

        if (combined.contains("organ") || combined.contains("alimento") || combined.contains("comida")
                || combined.contains("refeitorio") || combined.contains("refeicao") || combined.contains("fruta")
                || combined.contains("casca") || combined.contains("resto")) {
            return "Orgânico";
        }

        return "Outros";
    }

    public static String extractPrimaryMaterial(String rawMaterial) {
        if (rawMaterial == null || rawMaterial.trim().isEmpty() || isGenericLabel(rawMaterial)) {
            return "Outros";
        }
        if (rawMaterial.contains(" — ")) {
            String[] parts = rawMaterial.split(" — ");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                return parts[0].trim();
            }
        } else if (rawMaterial.contains(" - ")) {
            String[] parts = rawMaterial.split(" - ");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                return parts[0].trim();
            }
        }
        return classifyPrimaryMaterial(rawMaterial, "", "");
    }

    public static String extractSpecificItem(String rawMaterial) {
        if (rawMaterial == null || rawMaterial.trim().isEmpty() || isGenericLabel(rawMaterial)) {
            return "Nenhum";
        }
        String base;
        if (rawMaterial.contains(" — ")) {
            String[] parts = rawMaterial.split(" — ");
            base = parts.length > 1 ? parts[1] : parts[0];
        } else if (rawMaterial.contains(" - ")) {
            String[] parts = rawMaterial.split(" - ");
            base = parts.length > 1 ? parts[1] : parts[0];
        } else {
            base = rawMaterial;
        }
        String clean = extractMaterialName(base);
        return isGenericLabel(clean) ? "Nenhum" : clean;
    }

    public static String extractMaterialName(String rawMaterial) {
        if (rawMaterial == null) {
            return "Papelão";
        }
        String clean = rawMaterial
                .replaceAll("(?i)\\s*\\(Classe\\s+[^)]+\\)", "")
                .replaceAll("(?i)\\s*\\(Resíduo[^)]+\\)", "")
                .replaceAll("(?i)\\s+Classe\\s+[A-Za-z0-9_]+$", "")
                .replaceAll("(?i)\\s+[ABC]$", "")
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

    public static String formatMaterialWithClass(String primaryMaterial, String specificItem, String classSpec) {
        String pm = (primaryMaterial != null && !primaryMaterial.trim().isEmpty() && !isGenericLabel(primaryMaterial))
                ? primaryMaterial.trim() : "Nenhum";
        pm = extractMaterialName(pm);

        if (specificItem != null && !specificItem.trim().isEmpty() && !isGenericLabel(specificItem)) {
            String cleanItem = extractMaterialName(specificItem);
            String normItem = normalize(cleanItem);
            String normPm = normalize(pm);
            if (normItem.equals(normPm)) {
                return pm;
            }
            if (normItem.startsWith(normPm) || normItem.contains(normPm)) {
                return cleanItem;
            }
            return pm + " — " + cleanItem;
        }
        return pm;
    }

    public static String formatMaterialWithClass(String rawMaterial, String classSpec) {
        if (rawMaterial == null || rawMaterial.trim().isEmpty() || isGenericLabel(rawMaterial)) {
            return "Nenhum";
        }
        if (rawMaterial.contains(" — ") || rawMaterial.contains(" - ")) {
            String primary = extractPrimaryMaterial(rawMaterial);
            String item = extractSpecificItem(rawMaterial);
            return formatMaterialWithClass(primary, item, classSpec);
        }
        return extractMaterialName(rawMaterial);
    }

    public static String generateSingleWordTitle(String material, String description, String rawText) {
        String combined = normalize((material != null ? material : "") + " "
                + (description != null ? description : "") + " "
                + (rawText != null ? rawText : ""));

        if (combined.isEmpty() || combined.equals("nenhum") || combined.equals("indefinido")) {
            return "Nenhum";
        }

        if (combined.contains("cadeir") || combined.contains("mesa") || combined.contains("mobilia") || combined.contains("mobiliari") || combined.contains("moveis") || combined.contains("movel")
                || combined.contains("sofa") || combined.contains("armario") || combined.contains("poltrona")
                || combined.contains("estante") || combined.contains("banco")) {
            return "Mobiliário";
        }

        if (combined.contains("computad") || combined.contains("tela") || combined.contains("monitor")
                || combined.contains("eletron") || combined.contains("placa") || combined.contains("teclado")
                || combined.contains("mouse") || combined.contains("cabo") || combined.contains("fio")
                || combined.contains("bateria") || combined.contains("pilha")) {
            return "Eletrônico";
        }

        if (combined.contains("oleo") || combined.contains("graxa") || combined.contains("solvent")
                || combined.contains("tinta") || combined.contains("quimic") || combined.contains("toxic")
                || combined.contains("perig")) {
            return "Químico";
        }

        if (combined.contains("pallet") || combined.contains("palete")) {
            return "Palete";
        }
        if (combined.contains("madeira") || combined.contains("estrado") || combined.contains("tabua")
                || combined.contains("caixote") || combined.contains("mdf")) {
            return "Madeira";
        }

        if (combined.contains("vidro") || combined.contains("garrafa") || combined.contains("caco")
                || combined.contains("espelho") || combined.contains("frasco") || combined.contains("pote")) {
            return "Vidro";
        }

        if (combined.contains("metal") || combined.contains("alumin") || combined.contains("ferro")
                || combined.contains("aco") || combined.contains("lata") || combined.contains("latinha")
                || combined.contains("sucata") || combined.contains("cobre")) {
            return "Metal";
        }

        if (combined.contains("plast") || combined.contains("pet") || combined.contains("filme")
                || combined.contains("isopor") || combined.contains("eps") || combined.contains("sacola plastica")
                || combined.contains("bombona") || combined.contains("copo plastico") || combined.contains("pvc")) {
            return "Plástico";
        }

        if (combined.contains("papel") || combined.contains("cartao") || combined.contains("kraft")
                || combined.contains("caixa de papel") || combined.contains("caixas de papel") || combined.contains("jornal") || combined.contains("revista")) {
            return "Papelão";
        }

        if (combined.contains("organ") || combined.contains("alimento") || combined.contains("comida")
                || combined.contains("peotta") || combined.contains("casca") || combined.contains("fruta")
                || combined.contains("refeic") || combined.contains("refeit")) {
            return "Orgânico";
        }

        if (combined.contains("textil") || combined.contains("tecido") || combined.contains("pano")
                || combined.contains("uniforme") || combined.contains("estopa")) {
            return "Têxtil";
        }

        String primary = classifyPrimaryMaterial(material, rawText, description);
        if (primary != null && !primary.trim().isEmpty() && !isGenericLabel(primary)) {
            String[] words = primary.trim().split("\\s+");
            return words[0];
        }

        return "Nenhum";
    }

    public static String normalize(String text) {
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
