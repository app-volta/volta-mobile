package com.aula.volta.data.model;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Validação do contrato do AIC de Cooperativas recomendadas (Fase 15).
 */
public class CooperativeContractTest {

    @Test
    public void deserialize_validCooperativeJson() {
        String jsonStr = "{"
                + "\"nome\":\"CoopRecicla SP\","
                + "\"descricao\":\"Especializada em papelão e plásticos industriais\","
                + "\"distancia_km\":4.2,"
                + "\"coleta\":\"Coleta no local\","
                + "\"tag\":\"Certificada ISO 14001\","
                + "\"avaliacao\":4.9,"
                + "\"compatibilidade\":95,"
                + "\"materiais\":[\"Papelão\", \"Plástico\", \"Metal\"],"
                + "\"disponibilidade\":\"Disponível hoje\""
                + "}";

        CooperativeJSON coop = new Gson().fromJson(jsonStr, CooperativeJSON.class);
        assertNotNull(coop);
        assertEquals("CoopRecicla SP", coop.getNome());
        assertEquals("Especializada em papelão e plásticos industriais", coop.getDescricao());
        assertEquals(4.2, coop.getDistanciaKm(), 0.001);
        assertEquals("Coleta no local", coop.getColeta());
        assertEquals("Certificada ISO 14001", coop.getTag());
        assertEquals(4.9, coop.getAvaliacao(), 0.001);
        assertEquals(95, coop.getCompatibilidade());
        assertNotNull(coop.getMateriais());
        assertEquals(3, coop.getMateriais().size());
        assertTrue(coop.getMateriais().contains("Papelão"));
        assertEquals("Disponível hoje", coop.getDisponibilidade());
    }
}
