package com.aula.volta.data.model;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testes unitários para validar os modelos e contratos de Ocorrência.
 * Fase 15 — Testes.
 */
public class OccurrenceModelTest {

    @Test
    public void fromJson_mapsAllFieldsCorrectly() {
        String jsonStr = "{"
                + "\"id\":\"48291\","
                + "\"titulo\":\"Papelão molhado\","
                + "\"setor\":\"Frigorífico\","
                + "\"tempo_relativo\":\"12 min\","
                + "\"prioridade\":\"ALTA\","
                + "\"status\":\"EM_ANALISE\","
                + "\"material\":\"Papelão\""
                + "}";

        OccurrenceJSON json = new Gson().fromJson(jsonStr, OccurrenceJSON.class);
        assertNotNull(json);

        Occurrence occurrence = Occurrence.fromJson(json);
        assertNotNull(occurrence);
        assertEquals("48291", occurrence.getId());
        assertEquals("Papelão molhado", occurrence.getTitulo());
        assertEquals("Frigorífico", occurrence.getSetor());
        assertEquals("12 min", occurrence.getTempoRelativo());
        assertEquals("ALTA", occurrence.getPrioridade());
        assertEquals("EM_ANALISE", occurrence.getStatus());
        assertEquals("Papelão", occurrence.getMaterial());
    }

    @Test
    public void occurrenceConstructor_preservesValues() {
        Occurrence o = new Occurrence("100", "Sucata", "Manutenção", "ontem", "BAIXA", "RESOLVIDO", "Metal");
        assertEquals("100", o.getId());
        assertEquals("Sucata", o.getTitulo());
        assertEquals("Manutenção", o.getSetor());
        assertEquals("ontem", o.getTempoRelativo());
        assertEquals("BAIXA", o.getPrioridade());
        assertEquals("RESOLVIDO", o.getStatus());
        assertEquals("Metal", o.getMaterial());
    }
}
