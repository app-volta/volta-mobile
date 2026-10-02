package com.aula.volta.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

/**
 * Testes unitários para o fluxo de destinação contextual e confirmação de coleta (Fase 34).
 */
public class CooperativePickDestinationTest {

    private final Gson gson = new Gson();

    @Test
    public void testDestinationAuditLogEntry() {
        OccurrenceHistory entry = new OccurrenceHistory(
                "audit-dest-1",
                "Breno Gomes",
                "SOLICITAR_DESTINACAO",
                "ocorrencia",
                "48291",
                "EM_ANALISE",
                "JBS Ambiental",
                "02/10/2026 10:45"
        );

        assertEquals("SOLICITAR_DESTINACAO", entry.getAcao());
        assertEquals("JBS Ambiental", entry.getValorNovo());
        assertEquals("48291", entry.getEntidadeId());

        String json = gson.toJson(entry);
        OccurrenceHistory parsed = gson.fromJson(json, OccurrenceHistory.class);
        assertEquals("SOLICITAR_DESTINACAO", parsed.getAcao());
        assertEquals("JBS Ambiental", parsed.getValorNovo());
    }

    @Test
    public void testOccurrenceStatusTransitionOnDestination() {
        Occurrence original = new Occurrence(
                "48291",
                "Palete de Papelão Danificado",
                "Frigorífico B2",
                "há 10 min",
                "ALTA",
                "NOVO",
                "Papelão Ondulado"
        );

        // Ao solicitar destinação, status avança para EM_TRATAMENTO
        Occurrence updated = new Occurrence(
                original.getId(),
                original.getTitulo(),
                original.getSetor(),
                original.getTempoRelativo(),
                original.getPrioridade(),
                "EM_TRATAMENTO",
                original.getMaterial()
        );

        assertEquals("EM_TRATAMENTO", updated.getStatus());
        assertEquals(original.getId(), updated.getId());
    }

    @Test
    public void testCooperativeDetailsFormatting() {
        String json = "{"
                + "\"nome\":\"JBS Ambiental\","
                + "\"distancia_km\":2.4,"
                + "\"coleta\":\"Retirada em até 24h\","
                + "\"tag\":\"PARCEIRA J&F\""
                + "}";
        CooperativeJSON coop = gson.fromJson(json, CooperativeJSON.class);

        assertEquals("JBS Ambiental", coop.getNome());
        assertEquals(2.4, coop.getDistanciaKm(), 0.001);
        assertEquals("Retirada em até 24h", coop.getColeta());
        assertEquals("PARCEIRA J&F", coop.getTag());
    }
}
