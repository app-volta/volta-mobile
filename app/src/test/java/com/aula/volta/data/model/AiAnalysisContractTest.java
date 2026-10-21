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
}
