package com.aula.volta.data.model;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para modelo de histórico de auditoria (Fase 15).
 */
public class AuditLogModelTest {

    @Test
    public void occurrenceHistory_gettersAndSerialization() {
        OccurrenceHistory history = new OccurrenceHistory(
                "uuid-123", "Breno Gomes", "ALTERAR_STATUS", "ocorrencia",
                "48291", "EM_ANALISE", "EM_TRATAMENTO", "01/10/2026 10:45");

        assertEquals("uuid-123", history.getId());
        assertEquals("Breno Gomes", history.getUsuario());
        assertEquals("ALTERAR_STATUS", history.getAcao());
        assertEquals("ocorrencia", history.getEntidade());
        assertEquals("48291", history.getEntidadeId());
        assertEquals("EM_ANALISE", history.getValorAnterior());
        assertEquals("EM_TRATAMENTO", history.getValorNovo());
        assertEquals("01/10/2026 10:45", history.getTimestamp());

        String json = new Gson().toJson(history);
        OccurrenceHistory fromJson = new Gson().fromJson(json, OccurrenceHistory.class);

        assertEquals(history.getId(), fromJson.getId());
        assertEquals(history.getUsuario(), fromJson.getUsuario());
        assertEquals(history.getAcao(), fromJson.getAcao());
        assertEquals(history.getEntidadeId(), fromJson.getEntidadeId());
        assertEquals(history.getValorAnterior(), fromJson.getValorAnterior());
        assertEquals(history.getValorNovo(), fromJson.getValorNovo());
    }
}
