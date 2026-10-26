package com.aula.volta.data.model;

import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Validação do contrato da IA (Fase 15 + Integração QA AiReportResponseDTO).
 * O mock e o backend obedecem a esta estrutura.
 */
public class AiAnalysisContractTest {

    @Test
    public void deserialize_validAiAnalysisJson() {
        String jsonStr = "{"
                + "\"material\":\"Papelão ondulado\","
                + "\"quantidade_estimada\":45.5,"
                + "\"unidade\":\"kg\","
                + "\"contaminacao\":{\"presente\":true,\"nivel\":\"MÉDIA\"},"
                + "\"unidades\":{\"tipo\":\"caixas\",\"quantidade\":12},"
                + "\"confianca\":0.94,"
                + "\"observacoes\":\"Material acumulado próximo à área úmida\""
                + "}";

        AiAnalysisJSON ai = new Gson().fromJson(jsonStr, AiAnalysisJSON.class);
        assertNotNull(ai);
        assertEquals("Papelão ondulado", ai.getMaterial());
        assertEquals(45.5, ai.getQuantidadeEstimada(), 0.001);
        assertEquals("kg", ai.getUnidade());
        assertNotNull(ai.getContaminacao());
        assertTrue(ai.getContaminacao().isPresente());
        assertEquals("MÉDIA", ai.getContaminacao().getNivel());
        assertNotNull(ai.getUnidades());
        assertEquals("caixas", ai.getUnidades().getTipo());
        assertEquals(12, ai.getUnidades().getQuantidade());
        assertEquals(0.94, ai.getConfianca(), 0.001);
        assertEquals("Material acumulado próximo à área úmida", ai.getObservacoes());
    }

    @Test
    public void fromAiReport_mapsQaDtoAndEdgeCasesCorrectly() {
        AiReportResponseDTO dto = new AiReportResponseDTO();
        dto.setId("rep-1");
        dto.setIncidentId("inc-1");
        dto.setDetectedWasteType("Óleo lubrificante");
        dto.setAiContaminationLevel("HIGH");
        dto.setRecommendations("Isolar a área e acionar coleta especializada");
        dto.setReportText("Foi identificado vazamento de óleo próximo à máquina 3.");

        AiAnalysisJSON mapped = AiAnalysisJSON.fromAiReport(dto, 25.5);
        assertEquals("inc-1", mapped.getIncidentId());
        assertEquals("Óleo lubrificante", mapped.getMaterial());
        assertEquals(25.5, mapped.getQuantidadeEstimada(), 0.001);
        assertEquals("kg", mapped.getUnidade());
        assertNotNull(mapped.getContaminacao());
        assertTrue(mapped.getContaminacao().isPresente());
        assertEquals("Alta", mapped.getContaminacao().getNivel());
        assertEquals("Isolar a área e acionar coleta especializada", mapped.getObservacoes());
        assertEquals("Foi identificado vazamento de óleo próximo à máquina 3.", mapped.getReportText());

        // Edge case: null report returns valid offline fallback
        AiAnalysisJSON fallback = AiAnalysisJSON.fromAiReport(null, 0);
        assertNotNull(fallback);
        assertEquals("Papelão A", fallback.getMaterial());
        assertEquals("Baixa", fallback.getContaminacao().getNivel());
        assertFalse(fallback.getContaminacao().isPresente());
    }

    @Test
    public void resolveAreaAndWasteType_matchesAccentsAndCategoriesCorrectly() {
        com.google.gson.JsonArray areas = com.google.gson.JsonParser.parseString("["
                + "{\"id\":\"area-b2\",\"sectorName\":\"Frigorifico - Setor B2\"},"
                + "{\"id\":\"area-exp\",\"sectorName\":\"Expedicao\"},"
                + "{\"id\":\"area-man\",\"sectorName\":\"Manutencao\"}"
                + "]").getAsJsonArray();

        assertEquals("area-exp", AiAnalysisJSON.resolveAreaId(areas, "Expedição"));
        assertEquals("area-man", AiAnalysisJSON.resolveAreaId(areas, "Manutenção"));
        assertEquals("area-b2", AiAnalysisJSON.resolveAreaId(areas, "Frigorífico — Setor B2"));

        com.google.gson.JsonArray wasteTypes = com.google.gson.JsonParser.parseString("["
                + "{\"id\":\"wt-plastic\",\"category\":\"PLASTIC\"},"
                + "{\"id\":\"wt-paper\",\"category\":\"PAPER\"},"
                + "{\"id\":\"wt-metal\",\"category\":\"METAL\"}"
                + "]").getAsJsonArray();

        // Default description (or empty) selects PAPER instead of index 0 (PLASTIC)
        assertEquals("wt-paper", AiAnalysisJSON.resolveWasteTypeId(wasteTypes, ""));
        assertEquals("wt-paper", AiAnalysisJSON.resolveWasteTypeId(wasteTypes, "Caixas de papelão molhadas"));
        assertEquals("wt-plastic", AiAnalysisJSON.resolveWasteTypeId(wasteTypes, "Bandejas de isopor EPS"));
        assertEquals("wt-metal", AiAnalysisJSON.resolveWasteTypeId(wasteTypes, "Latas de alumínio"));
    }

    @Test
    public void fromScoringContext_parsesAiReportWhenPresentAndReturnsNullWhenAbsent() {
        com.google.gson.JsonObject ctxNoReport = com.google.gson.JsonParser.parseString("{"
                + "\"incidentId\":\"inc-123\","
                + "\"priority\":\"MEDIUM\","
                + "\"aiReport\":null"
                + "}").getAsJsonObject();
        assertNull(AiAnalysisJSON.fromScoringContext(ctxNoReport, 38.0));

        com.google.gson.JsonObject ctxWithReport = com.google.gson.JsonParser.parseString("{"
                + "\"incidentId\":\"inc-456\","
                + "\"priority\":\"HIGH\","
                + "\"aiReport\":{"
                + "  \"detectedWasteType\":\"Plástico Filme\","
                + "  \"aiContaminationLevel\":\"LOW\","
                + "  \"recommendations\":\"Prensar em fardo limpo\""
                + "}"
                + "}").getAsJsonObject();
        AiAnalysisJSON parsed = AiAnalysisJSON.fromScoringContext(ctxWithReport, 30.0);
        assertNotNull(parsed);
        assertEquals("inc-456", parsed.getIncidentId());
        assertEquals("Plástico Filme", parsed.getMaterial());
        assertEquals("Baixa", parsed.getContaminacao().getNivel());
        assertEquals("Prensar em fardo limpo", parsed.getObservacoes());
    }

    @Test
    public void materialAndClassFormatting_handlesTrailingLettersAndExplicitClassesWithoutDuplication() {
        assertEquals("Papelão", AiAnalysisJSON.extractMaterialName("Papelão A"));
        assertEquals("Papelão", AiAnalysisJSON.extractMaterialName("Papelão (Classe B)"));
        assertEquals("Plástico Filme", AiAnalysisJSON.extractMaterialName("Plástico Filme C"));
        assertEquals("Papelão", AiAnalysisJSON.extractMaterialName(null));
        assertEquals("Papelão", AiAnalysisJSON.extractMaterialName("   "));

        assertEquals("A", AiAnalysisJSON.extractQualityClassLetter("Papelão A", null));
        assertEquals("B", AiAnalysisJSON.extractQualityClassLetter("Papelão B", null));
        assertEquals("C", AiAnalysisJSON.extractQualityClassLetter("Papelão (Classe C)", null));
        assertEquals("B", AiAnalysisJSON.extractQualityClassLetter("Papelão A", "Classe B"));
        assertEquals("C", AiAnalysisJSON.extractQualityClassLetter("Plástico", "C"));

        assertEquals("Papelão (Classe A)", AiAnalysisJSON.formatMaterialWithClass("Papelão A", "Classe A"));
        assertEquals("Papelão (Classe B)", AiAnalysisJSON.formatMaterialWithClass("Papelão A", "Classe B"));
        assertEquals("Plástico (Classe C)", AiAnalysisJSON.formatMaterialWithClass("Plástico (Classe A)", "C"));
        assertEquals("Papelão (Classe A)", AiAnalysisJSON.formatMaterialWithClass("Papelão", "Classe A"));
    }

    @Test
    public void fromPredict_parsesGeminiVisionResponseCorrectly() {
        com.google.gson.JsonObject predict = com.google.gson.JsonParser.parseString("{\n"
                + "  \"detected_waste_type\": \"Papelão contaminado\",\n"
                + "  \"ai_contamination_level\": \"MEDIO\",\n"
                + "  \"estimated_quantity_kg\": 42.0,\n"
                + "  \"recommendations\": \"Segregar em recipientes herméticos\",\n"
                + "  \"report_text\": \"Presença de três caixas de papelão no setor B2\",\n"
                + "  \"mobile_summary\": \"Papelão com contaminação média\",\n"
                + "  \"requires_human_validation\": true,\n"
                + "  \"analysis_id\": \"632555cc-b451-450a-8ae4-ea6cf9e3784d\",\n"
                + "  \"provenance_token\": \"d64fe8b8d5e28ae0ee0365b49a6d816bda132c688df67e15d56dce7d8ab04558\"\n"
                + "}").getAsJsonObject();

        AiAnalysisJSON ai = AiAnalysisJSON.fromPredict(predict, 38.0);
        assertNotNull(ai);
        assertEquals("Papelão contaminado", ai.getMaterial());
        assertEquals("Média", ai.getContaminacao().getNivel());
        assertTrue(ai.getContaminacao().isPresente());
        assertEquals(42.0, ai.getQuantidadeEstimada(), 0.001);
        assertEquals("Segregar em recipientes herméticos", ai.getObservacoes());
        assertEquals("Presença de três caixas de papelão no setor B2", ai.getReportText());
        assertEquals("Papelão com contaminação média", ai.getMobileSummary());
        assertEquals("632555cc-b451-450a-8ae4-ea6cf9e3784d", ai.getAnalysisId());
        assertEquals("d64fe8b8d5e28ae0ee0365b49a6d816bda132c688df67e15d56dce7d8ab04558", ai.getProvenanceToken());
        assertNotNull(ai.getUnidades());
        assertEquals("Caixas", ai.getUnidades().getTipo());
        assertNotNull(ai.getRawAiData());
    }

    @Test
    public void fromPredict_adaptsUnitsBasedOnWasteType() {
        com.google.gson.JsonObject plastic = new com.google.gson.JsonObject();
        plastic.addProperty("detected_waste_type", "Plástico Filme Stretch");
        plastic.addProperty("ai_contamination_level", "BAIXO");
        AiAnalysisJSON aiPlastic = AiAnalysisJSON.fromPredict(plastic, 20.0);
        assertEquals("Fardos", aiPlastic.getUnidades().getTipo());

        com.google.gson.JsonObject metal = new com.google.gson.JsonObject();
        metal.addProperty("detected_waste_type", "Sucata de Metal / Alumínio");
        metal.addProperty("ai_contamination_level", "BAIXO");
        AiAnalysisJSON aiMetal = AiAnalysisJSON.fromPredict(metal, 50.0);
        assertEquals("Tambores", aiMetal.getUnidades().getTipo());
    }

    @Test
    public void classifyPrimaryMaterial_mapsSpecificItemsToEcoCategoriesCorrectly() {
        assertEquals("Plástico", AiAnalysisJSON.classifyPrimaryMaterial("Cadeira de escritório", "", ""));
        assertEquals("Plástico", AiAnalysisJSON.classifyPrimaryMaterial("Bandejas de isopor EPS", "", ""));
        assertEquals("Madeira", AiAnalysisJSON.classifyPrimaryMaterial("Palete de madeira PBR", "", ""));
        assertEquals("Papelão", AiAnalysisJSON.classifyPrimaryMaterial("Caixas de papelão ondulado", "", ""));
        assertEquals("Metal", AiAnalysisJSON.classifyPrimaryMaterial("Sucata de ferro e perfis metálicos", "", ""));
        assertEquals("Vidro", AiAnalysisJSON.classifyPrimaryMaterial("Garrafas de vidro âmbar", "", ""));
        assertEquals("Orgânico", AiAnalysisJSON.classifyPrimaryMaterial("Restos de comida do refeitório", "", ""));
        assertEquals("Perigoso", AiAnalysisJSON.classifyPrimaryMaterial("Estopas contaminadas com óleo e graxa", "", ""));
    }

    @Test
    public void calculatePriority_chairsAndFurnitureAlwaysLowPriorityUnlessHazardous() {
        // Cadeiras e móveis inertes secos com contaminação baixa recebem prioridade BAIXA determinística
        assertEquals("BAIXA", AiAnalysisJSON.calculatePriority("Cadeira de escritório", "Baixa", "Cadeira quebrada"));
        assertEquals("BAIXA", AiAnalysisJSON.calculatePriority("Plástico — Cadeira de escritório (Classe A)", "LOW", ""));
        assertEquals("BAIXA", AiAnalysisJSON.calculatePriority("Palete PBR de madeira", "Baixa", ""));
        assertEquals("BAIXA", AiAnalysisJSON.calculatePriority("Papelão A", "LOW", ""));

        // Alimentos e materiais perecíveis recebem prioridade MEDIA
        assertEquals("MEDIA", AiAnalysisJSON.calculatePriority("Restos de alimentos", "Baixa", ""));
        assertEquals("MEDIA", AiAnalysisJSON.calculatePriority("Papelão molhado", "MÉDIA", ""));

        // Materiais perigosos e contaminados com óleo recebem prioridade ALTA
        assertEquals("ALTA", AiAnalysisJSON.calculatePriority("Óleo lubrificante usado", "Baixa", ""));
        assertEquals("ALTA", AiAnalysisJSON.calculatePriority("Sucata com graxa", "HIGH", ""));
    }

    @Test
    public void formatMaterialWithClass_primaryAndSpecificItemComposition() {
        assertEquals("Plástico — Cadeira de escritório (Classe A)",
                AiAnalysisJSON.formatMaterialWithClass("Plástico", "Cadeira de escritório", "A"));
        assertEquals("Madeira — Palete PBR (Classe B)",
                AiAnalysisJSON.formatMaterialWithClass("Madeira", "Palete PBR", "B"));
        assertEquals("Papelão (Classe A)",
                AiAnalysisJSON.formatMaterialWithClass("Papelão", "Papelão", "A"));

        // Extração reversa de primary e specific item
        assertEquals("Plástico", AiAnalysisJSON.extractPrimaryMaterial("Plástico — Cadeira de escritório (Classe A)"));
        assertEquals("Cadeira de escritório", AiAnalysisJSON.extractSpecificItem("Plástico — Cadeira de escritório (Classe A)"));
    }

    @Test
    public void fromPredict_chairsAssignedRealisticUnitsAndWeight() {
        com.google.gson.JsonObject chairJson = new com.google.gson.JsonObject();
        chairJson.addProperty("detected_waste_type", "Cadeira de escritório");
        chairJson.addProperty("ai_contamination_level", "BAIXO");

        AiAnalysisJSON aiChair = AiAnalysisJSON.fromPredict(chairJson, 0);
        assertEquals("Plástico", aiChair.getPrimaryMaterial());
        assertEquals("Cadeira de escritório", aiChair.getSpecificItem());
        assertNotNull(aiChair.getUnidades());
        assertEquals("Unidades", aiChair.getUnidades().getTipo());
        assertEquals(1, aiChair.getUnidades().getQuantidade());
        assertEquals(5.0, aiChair.getQuantidadeEstimada(), 0.001);
    }
}
