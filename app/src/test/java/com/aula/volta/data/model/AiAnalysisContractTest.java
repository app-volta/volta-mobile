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
}
