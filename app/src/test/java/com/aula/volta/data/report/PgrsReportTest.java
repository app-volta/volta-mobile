package com.aula.volta.data.report;

import com.aula.volta.data.model.Occurrence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Cobre o HTML do PGRS (função pura, sem Android).
 */
public class PgrsReportTest {

    @Test
    public void buildHtml_containsSummaryAndRows() {
        List<Occurrence> occurrences = new ArrayList<>();
        occurrences.add(new Occurrence("1", "Papelão molhado", "Frigorífico",
                "agora", "ALTA", "NOVO", "Papelão"));
        occurrences.add(new Occurrence("2", "Plástico", "Expedição",
                "ontem", "BAIXA", "RESOLVIDO", "Plástico"));

        String html = PgrsReport.buildHtml(9, 43, 1147, occurrences, "01/10/2026 08:00");

        assertTrue(html.contains("Relatório PGRS"));
        assertTrue(html.contains("Ocorrências: 52"));
        assertTrue(html.contains("Kg reciclados: 1147"));
        assertTrue(html.contains("#1"));
        assertTrue(html.contains("Papelão molhado"));
        assertTrue(html.contains("01/10/2026 08:00"));
    }

    @Test
    public void buildHtml_escapesHtml() {
        List<Occurrence> occurrences = new ArrayList<>();
        occurrences.add(new Occurrence("1", "<b>X</b> & Y", null,
                null, null, null, null));

        String html = PgrsReport.buildHtml(0, 0, 0, occurrences, null);

        assertFalse(html.contains("<b>X</b>"));
        assertTrue(html.contains("&lt;b&gt;X&lt;/b&gt; &amp; Y"));
    }

    @Test
    public void buildHtml_emptyList_hasEmptyTable() {
        String html = PgrsReport.buildHtml(0, 0, 0, new ArrayList<>(), "x");

        assertTrue(html.contains("<table"));
        assertFalse(html.contains("<td>"));
    }
}
