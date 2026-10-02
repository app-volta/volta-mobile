package com.aula.volta.data.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para validar o modelo de histórico de auditoria e linha do tempo.
 * Fase 31 — Visualização Completa de Auditoria & Linha do Tempo de Ocorrências.
 */
public class OccurrenceHistoryTimelineTest {

    @Test
    public void occurrenceHistory_preservesAllTimelineProperties() {
        OccurrenceHistory entry = new OccurrenceHistory(
                "hist_101",
                "Carlos Amaral",
                "FINALIZAR",
                "ocorrencia",
                "48291",
                "EM_TRATAMENTO",
                "APROVADA",
                "02/10/2026 10:15"
        );

        assertEquals("hist_101", entry.getId());
        assertEquals("Carlos Amaral", entry.getUsuario());
        assertEquals("FINALIZAR", entry.getAcao());
        assertEquals("ocorrencia", entry.getEntidade());
        assertEquals("48291", entry.getEntidadeId());
        assertEquals("EM_TRATAMENTO", entry.getValorAnterior());
        assertEquals("APROVADA", entry.getValorNovo());
        assertEquals("02/10/2026 10:15", entry.getTimestamp());
    }

    @Test
    public void occurrenceHistory_handlesOfflineAndSyncActions() {
        OccurrenceHistory offlineEntry = new OccurrenceHistory(
                "hist_102",
                "Breno Gomes",
                "CRIAR_OFFLINE",
                "ocorrencia",
                "off_9988",
                null,
                "PENDENTE_SYNC",
                "02/10/2026 09:30"
        );

        assertEquals("CRIAR_OFFLINE", offlineEntry.getAcao());
        assertEquals("PENDENTE_SYNC", offlineEntry.getValorNovo());
        assertNull(offlineEntry.getValorAnterior());
    }
}
