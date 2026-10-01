package com.aula.volta.data.model;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Validação do contrato da IA (Fase 15).
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
}
