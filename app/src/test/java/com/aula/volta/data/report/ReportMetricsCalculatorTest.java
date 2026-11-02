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

        assertEquals("Nesta Semana", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.WEEK));
        assertEquals("Neste Mês", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.MONTH));
        assertEquals("Neste Trimestre", ReportMetricsCalculator.getPeriodTitle(ReportMetricsCalculator.Period.QUARTER));

        assertEquals("Meta da Semana", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.WEEK));
        assertEquals("Meta do Mês", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.MONTH));
        assertEquals("Meta do Trimestre", ReportMetricsCalculator.getGoalTitle(ReportMetricsCalculator.Period.QUARTER));
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
        assertEquals(0, summary.totalKg);
        assertEquals(0, summary.openCount);
        assertEquals(0, summary.resolvedCount);
        assertEquals(0, summary.totalCount);
        assertEquals(1500, summary.goalKg);
        assertEquals(0, summary.goalPercent);
        assertEquals(0, summary.recyclingRate);
        assertTrue(summary.materialBreakdown.isEmpty());
        assertTrue(summary.topSectors.isEmpty());
    }

    @Test
    public void testCalculateWeek_scalesMetrics() {
        JsonObject baseEsg = new JsonObject();
        baseEsg.addProperty("kg_reciclados", 400);
        baseEsg.addProperty("abertas", 4);
        baseEsg.addProperty("resolvidas", 16);

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.WEEK, baseEsg, null, null, null);

        assertNotNull(summary);
        assertEquals(375, summary.goalKg);
        assertEquals(100, summary.totalKg);
        assertEquals(1, summary.openCount);
        assertEquals(4, summary.resolvedCount);
        assertEquals(27, summary.goalPercent);
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
        assertEquals(80, summary.totalKg);
        assertEquals(1, summary.openCount);
        assertEquals(1, summary.resolvedCount);
        assertEquals(2, summary.totalCount);
        assertEquals(Integer.valueOf(50), summary.materialBreakdown.get("Papelão"));
        assertEquals(Integer.valueOf(30), summary.materialBreakdown.get("Plástico"));

        JsonObject json = summary.toJsonObject();
        assertEquals(80, json.get("kg_reciclados").getAsInt());
        assertEquals(1, json.get("abertas").getAsInt());
        assertEquals(1, json.get("resolvidas").getAsInt());
    }

    @Test
    public void testCalculateLiveDatabase() {
        List<Occurrence> occurrences = new ArrayList<>();

        occurrences.add(new Occurrence("inc-1", "Caixa de Papelão", "Frigorífico B2",
                "10 min", "BAIXA", "CLOSED", "Papelão"));

        occurrences.add(new Occurrence("inc-2", "Bobina plástica", "Expedição",
                "1h", "MEDIA", "PENDING", "Plástico"));
        occurrences.add(new Occurrence("inc-3", "Aparas de metal", "Manutenção",
                "2h", "ALTA", "AGUARDANDO_VALIDACAO", "Metal"));

        Map<String, Double> weights = new HashMap<>();
        weights.put("inc-1", 40.0);
        weights.put("inc-2", 20.0);
        weights.put("inc-3", 15.0);

        JsonObject baseEsg = new JsonObject();
        baseEsg.addProperty("isLiveDatabase", true);

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.MONTH, baseEsg, occurrences, weights, null);

        assertNotNull(summary);

        assertEquals(75, summary.totalKg);

        assertEquals(2, summary.openCount);

        assertEquals(1, summary.resolvedCount);
        assertEquals(3, summary.totalCount);

        assertEquals(33, summary.recyclingRate);

        assertEquals(Integer.valueOf(40), summary.materialBreakdown.get("Papelão"));
        assertEquals(Integer.valueOf(20), summary.materialBreakdown.get("Plástico"));
        assertEquals(Integer.valueOf(15), summary.materialBreakdown.get("Metal"));
        assertNull(summary.materialBreakdown.get("Vidro"));

        assertEquals(3, summary.topSectors.size());
        assertEquals("Frigorífico B2", summary.topSectors.get(0).name);
        assertEquals(40, summary.topSectors.get(0).kg);
    }

    @Test
    public void testCalculateLiveDatabase_withCorporateTotal() {
        List<Occurrence> occurrences = new ArrayList<>();
        occurrences.add(new Occurrence("inc-1", "Papelão", "Frigorífico B2",
                "10 min", "BAIXA", "CLOSED", "Papelão"));
        occurrences.add(new Occurrence("inc-2", "Plástico", "Expedição",
                "1h", "MEDIA", "PENDING", "Plástico"));

        Map<String, Double> weights = new HashMap<>();
        weights.put("inc-1", 50.0);
        weights.put("inc-2", 30.0);

        JsonObject baseEsg = new JsonObject();
        baseEsg.addProperty("isLiveDatabase", true);
        baseEsg.addProperty("corporateTotalKg", 500);
        baseEsg.addProperty("corporateRecyclingPercentage", 75);

        ReportMetricsCalculator.ReportSummary summary = ReportMetricsCalculator.calculate(
                ReportMetricsCalculator.Period.MONTH, baseEsg, occurrences, weights, null);

        assertNotNull(summary);
        assertEquals(500, summary.totalKg);
        assertEquals(75, summary.recyclingRate);
        assertEquals(1, summary.openCount);
        assertEquals(1, summary.resolvedCount);
        assertEquals(2, summary.totalCount);
        assertEquals(Integer.valueOf(50), summary.materialBreakdown.get("Papelão"));
        assertEquals(Integer.valueOf(30), summary.materialBreakdown.get("Plástico"));
    }
}
