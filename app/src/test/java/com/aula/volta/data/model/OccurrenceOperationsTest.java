package com.aula.volta.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Testes unitários para as ações operacionais e trilha de auditoria (Fase 33).
 */
public class OccurrenceOperationsTest {

    private final Gson gson = new Gson();

    @Test
    public void testAddObservationAuditEntry() {
        String obsText = "Material molhado pela chuva, aguardando nova caçamba.";
        OccurrenceHistory observationEntry = new OccurrenceHistory(
                "audit-obs-1",
                "Breno Gomes",
                "ADICIONAR_OBSERVACAO",
                "ocorrencia",
                "48291",
                null,
                obsText,
                "02/10/2026 10:30"
        );

        assertEquals("ADICIONAR_OBSERVACAO", observationEntry.getAcao());
        assertNull(observationEntry.getValorAnterior());
        assertEquals(obsText, observationEntry.getValorNovo());

        String json = gson.toJson(observationEntry);
        OccurrenceHistory parsed = gson.fromJson(json, OccurrenceHistory.class);
        assertEquals(obsText, parsed.getValorNovo());
        assertEquals("Breno Gomes", parsed.getUsuario());
    }

    @Test
    public void testForwardSectorAuditEntry() {
        String setorAnterior = "Frigorífico B2";
        String novoSetor = "Expedição Central";
        OccurrenceHistory forwardEntry = new OccurrenceHistory(
                "audit-fwd-2",
                "Breno Gomes",
                "ENCAMINHAR_SETOR",
                "ocorrencia",
                "48291",
                setorAnterior,
                novoSetor,
                "02/10/2026 10:35"
        );

        assertEquals("ENCAMINHAR_SETOR", forwardEntry.getAcao());
        assertEquals(setorAnterior, forwardEntry.getValorAnterior());
        assertEquals(novoSetor, forwardEntry.getValorNovo());

        // Validar atualização do modelo Occurrence
        Occurrence original = new Occurrence("48291", "Palete Danificado", setorAnterior, "há 5 min", "ALTA", "NOVO", "Papelão");
        Occurrence updated = new Occurrence(original.getId(), original.getTitulo(), novoSetor, original.getTempoRelativo(), original.getPrioridade(), original.getStatus(), original.getMaterial());

        assertEquals(novoSetor, updated.getSetor());
        assertEquals(original.getId(), updated.getId());
    }

    @Test
    public void testEditClassificationAuditEntry() {
        String matAnterior = "Papelão Ondulado";
        String novoMat = "Plástico Filme";
        OccurrenceHistory editClassEntry = new OccurrenceHistory(
                "audit-cls-3",
                "Breno Gomes",
                "ALTERAR_CLASSIFICACAO",
                "ocorrencia",
                "48291",
                matAnterior,
                novoMat,
                "02/10/2026 10:40"
        );

        assertEquals("ALTERAR_CLASSIFICACAO", editClassEntry.getAcao());
        assertEquals(matAnterior, editClassEntry.getValorAnterior());
        assertEquals(novoMat, editClassEntry.getValorNovo());

        // Validar atualização do modelo Occurrence
        Occurrence original = new Occurrence("48291", "Resíduo Descartado", "Expedição", "há 5 min", "ALTA", "NOVO", matAnterior);
        Occurrence updated = new Occurrence(original.getId(), original.getTitulo(), original.getSetor(), original.getTempoRelativo(), original.getPrioridade(), original.getStatus(), novoMat);

        assertEquals(novoMat, updated.getMaterial());
        assertEquals(original.getId(), updated.getId());
    }
}
