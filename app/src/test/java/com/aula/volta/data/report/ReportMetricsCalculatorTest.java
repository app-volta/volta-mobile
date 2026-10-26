package com.aula.volta.data.report;

import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ReportMetricsCalculatorTest {

    @Test
    public void testGoalsAndScales() {
        assertEquals(375, ReportMetricsCalculator.getGoalKg(ReportMetricsCalculator.Period.WEEK));
        assertEquals(1500, ReportMetricsCalculator.getGoalKg(ReportMetricsCalculator.Period.MONTH));
        assertEquals(4500, ReportMetricsCalculator.getGoalKg(ReportMetricsCalculator.Period.QUARTER));

        assertEquals(0.25, ReportMetricsCalculator.getPeriodScale(ReportMetricsCalculator.Period.WEEK), 0.001);
        assertEquals(1.0, ReportMetricsCalculator.getPeriodScale(ReportMetricsCalculator.Period.MONTH), 0.001);
        assertEquals(3.0, ReportMetricsCalculator.getPeriodScale(ReportMetricsCalculator.Period.QUARTER), 0.001);

        assertEquals("Nesta semana", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.WEEK));
        assertEquals("Neste mês", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.MONTH));
        assertEquals("Neste trimestre", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.QUARTER));

        assertEquals("Meta da semana", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.WEEK));
        assertEquals("Meta do mês", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.MONTH));
        assertEquals("Meta do trimestre", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.QUARTER));
    }

    @Test
    public void testResolveMaterialFamily() {
        assertEquals("Papelão", ReportMetricsCalculator.resolveMaterialFamily("Papelão ondulado"));
        assertEquals("Papelão", ReportMetricsCalculator.resolveMaterialFamily("Cartão reciclável"));
        assertEquals("Plástico", ReportMetricsCalculator.resolveMaterialFamily("Plástico filme"));
        assertEquals("Plástico", ReportMetricsCalculator.resolveMaterialFamily("Garrafa PET"));
        assertEquals("Metal", ReportMetricsCalculator.resolveMaterialFamily("Alumínio"));
        assertEquals("Metal", ReportMetricsCalculator.resolveMaterialFamily("Sucata de aço"));
        assertEquals("Vidro", ReportMetricsCalculator.resolveMaterialFamily("Vidro âmbar"));
        assertEquals("Madeira", ReportMetricsCalculator.resolveMaterialFamily("Palete de madeira"));
        assertEquals("Orgânico", ReportMetricsCalculator.resolveMaterialFamily("Resíduo orgânico"));
        assertEquals("Papelão", ReportMetricsCalculator.resolveMaterialFamily(null));
    }

    @Test
    public void testCalculateMonth_withNoOccurrences() {
        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.MONTH, null, null, null, null);

        assertNotNull(summary);
        assertEquals(1147, summary.totalKg);
        assertEquals(6, summary.openCount);
        assertEquals(46, summary.resolvedCount);
        assertEquals(52, summary.totalCount);
        assertEquals(1500, summary.goalKg);
        assertEquals(76, summary.goalPercent); // 1147 / 1500 = 76.46% -> 76%
        assertTrue(summary.recyclingRate > 80);
        assertTrue(summary.materialBreakdown.containsKey("Papelão"));
        assertEquals(Integer.valueOf(542), summary.materialBreakdown.get("Papelão"));
        assertEquals(Integer.valueOf(318), summary.materialBreakdown.get("Plástico"));
        assertFalse(summary.topSectors.isEmpty());
    }

    @Test
    public void testCalculateWeek_scalesMetrics() {
        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.WEEK, null, null, null, null);

        assertNotNull(summary);
        assertEquals(375, summary.goalKg);
        assertEquals(287, summary.totalKg); // 1147 * 0.25 = 286.75 -> 287
        assertEquals(2, summary.openCount);  // 6 * 0.25 = 1.5 -> 2
        assertEquals(12, summary.resolvedCount); // 46 * 0.25 = 11.5 -> 12
        assertEquals(77, summary.goalPercent); // 287 / 375 = 76.5% -> 77%
    }

    @Test
    public void testCalculateWithRealOccurrences() {
        List<Occurrence> occurrences = new ArrayList<>();
        occurrences.add(new Occurrence("occ-1", "Papelão molhado", "Frigorífico B2",
                "10 min", "ALTA", "EM_ANALISE", "Papelão"));
        occurrences.add(new Occurrence("occ-2", "Plástico", "Expedição",
                "1h", "MEDIA", "APROVADA", "Plástico"));

        Map<String, Double> weights = new HashMap<>();
        weights.put("occ-1", 50.0);
        weights.put("occ-2", 30.0);

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.MONTH, null, occurrences, weights, null);

        assertNotNull(summary);
        // Base 1147 + 50 + 30 = 1227
        assertEquals(1227, summary.totalKg);
        // Base abertas (6) + occ-1 (1) = 7
        assertEquals(7, summary.openCount);
        // Base resolvidas (46) + occ-2 (1) = 47
        assertEquals(47, summary.resolvedCount);
        assertEquals(54, summary.totalCount);

        // Material Papelão = 542 + 50 = 592
        assertEquals(Integer.valueOf(592), summary.materialBreakdown.get("Papelão"));
        // Material Plástico = 318 + 30 = 348
        assertEquals(Integer.valueOf(348), summary.materialBreakdown.get("Plástico"));

        JsonObject json = summary.toJsonObject();
        assertEquals(1227, json.get("kg_reciclados").getAsInt());
        assertEquals(7, json.get("abertas").getAsInt());
        assertEquals(47, json.get("resolvidas").getAsInt());
    }
}
