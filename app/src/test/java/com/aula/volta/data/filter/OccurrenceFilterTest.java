package com.aula.volta.data.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aula.volta.data.model.Occurrence;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OccurrenceFilterTest {

    private List<Occurrence> sampleOccurrences;

    @Before
    public void setUp() {
        sampleOccurrences = new ArrayList<>();
        sampleOccurrences.add(new Occurrence("48291", "Palete de Papelão Danificado", "Frigorífico B2", "há 10 min", "ALTA", "NOVO", "Papelão Ondulado"));
        sampleOccurrences.add(new Occurrence("48292", "Sobras de Plástico Filme", "Expedição Central", "há 25 min", "MÉDIA", "EM_ANALISE", "Plástico Filme"));
        sampleOccurrences.add(new Occurrence("48293", "Sucata Metálica de Manutenção", "Oficina Mecânica", "há 1 hora", "BAIXA", "RESOLVIDO", "Metal / Aço"));
        sampleOccurrences.add(new Occurrence("48294", "Caixas de Vidro para Coleta", "Refeitório Principal", "há 2 horas", "BAIXA", "APROVADA", "Vidro"));
    }

    @Test
    public void testEmptyFilterReturnsAll() {
        List<Occurrence> result = OccurrenceFilter.filter(sampleOccurrences, null, null, null);
        assertEquals(4, result.size());

        result = OccurrenceFilter.filter(sampleOccurrences, "", Collections.emptyList(), Collections.emptyList());
        assertEquals(4, result.size());
    }

    @Test
    public void testSearchByIdWithAndWithoutHash() {
        List<Occurrence> byId = OccurrenceFilter.filter(sampleOccurrences, "48291", null, null);
        assertEquals(1, byId.size());
        assertEquals("48291", byId.get(0).getId());

        List<Occurrence> byHashId = OccurrenceFilter.filter(sampleOccurrences, "#48291", null, null);
        assertEquals(1, byHashId.size());
        assertEquals("48291", byHashId.get(0).getId());

        List<Occurrence> partialId = OccurrenceFilter.filter(sampleOccurrences, "29", null, null);
        assertEquals(4, partialId.size());
    }

    @Test
    public void testSearchByTitleAndSectorCaseInsensitive() {
        List<Occurrence> byTitle = OccurrenceFilter.filter(sampleOccurrences, "palete", null, null);
        assertEquals(1, byTitle.size());
        assertEquals("48291", byTitle.get(0).getId());

        List<Occurrence> bySectorWithoutAccent = OccurrenceFilter.filter(sampleOccurrences, "frigorifico", null, null);
        assertEquals(1, bySectorWithoutAccent.size());
        assertEquals("Frigorífico B2", bySectorWithoutAccent.get(0).getSetor());

        List<Occurrence> bySectorWithAccent = OccurrenceFilter.filter(sampleOccurrences, "Expedição", null, null);
        assertEquals(1, bySectorWithAccent.size());
        assertEquals("Expedição Central", bySectorWithAccent.get(0).getSetor());
    }

    @Test
    public void testFilterByMaterial() {
        List<String> materials = Collections.singletonList("papel");
        List<Occurrence> result = OccurrenceFilter.filter(sampleOccurrences, null, materials, null);
        assertEquals(1, result.size());
        assertEquals("Papelão Ondulado", result.get(0).getMaterial());

        List<String> multipleMaterials = Arrays.asList("papel", "plast");
        result = OccurrenceFilter.filter(sampleOccurrences, null, multipleMaterials, null);
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterByStatus() {
        List<String> statuses = Collections.singletonList("NOVO");
        List<Occurrence> result = OccurrenceFilter.filter(sampleOccurrences, null, null, statuses);
        assertEquals(1, result.size());
        assertEquals("48291", result.get(0).getId());

        statuses = Collections.singletonList("RESOLVIDO");
        result = OccurrenceFilter.filter(sampleOccurrences, null, null, statuses);
        assertEquals(2, result.size());
    }

    @Test
    public void testCompositeFilter_Text_Material_And_Status() {

        List<Occurrence> result = OccurrenceFilter.filter(
                sampleOccurrences,
                "central",
                Collections.singletonList("plast"),
                Collections.singletonList("EM_ANALISE")
        );
        assertEquals(1, result.size());
        assertEquals("48292", result.get(0).getId());

        result = OccurrenceFilter.filter(
                sampleOccurrences,
                "central",
                Collections.singletonList("plast"),
                Collections.singletonList("NOVO")
        );
        assertTrue(result.isEmpty());
    }

    @Test
    public void testHasActiveFilters() {
        assertFalse(OccurrenceFilter.hasActiveFilters(null, null, null));
        assertFalse(OccurrenceFilter.hasActiveFilters("   ", Collections.emptyList(), Collections.emptyList()));

        assertTrue(OccurrenceFilter.hasActiveFilters("palete", null, null));
        assertTrue(OccurrenceFilter.hasActiveFilters(null, Collections.singletonList("papel"), null));
        assertTrue(OccurrenceFilter.hasActiveFilters(null, null, Collections.singletonList("NOVO")));
    }

    @Test
    public void testFilterByPrimaryMaterialAndSpecificItem() {
        List<Occurrence> extended = new ArrayList<>(sampleOccurrences);
        extended.add(new Occurrence("48295", "Cadeira de escritório", "Almoxarifado", "há 3 horas", "BAIXA", "NOVO", "Plástico — Cadeira de escritório"));
        extended.add(new Occurrence("48296", "Palete PBR Quebrado", "Expedição", "há 4 horas", "BAIXA", "NOVO", "Madeira — Palete PBR"));

        List<String> woodFilter = Collections.singletonList("madeira");
        List<Occurrence> woodResult = OccurrenceFilter.filter(extended, null, woodFilter, null);
        assertEquals(1, woodResult.size());
        assertEquals("48296", woodResult.get(0).getId());

        List<String> plasticFilter = Collections.singletonList("plast");
        List<Occurrence> plasticResult = OccurrenceFilter.filter(extended, null, plasticFilter, null);
        assertEquals(2, plasticResult.size());

        List<Occurrence> chairResult = OccurrenceFilter.filter(extended, "cadeira", null, null);
        assertEquals(1, chairResult.size());
        assertEquals("48295", chairResult.get(0).getId());
    }

    @Test
    public void testFilterByPriority() {
        List<String> alta = Collections.singletonList("ALTA");
        List<Occurrence> altaResult = OccurrenceFilter.filter(sampleOccurrences, null, null, null, alta);
        assertEquals(1, altaResult.size());
        assertEquals("48291", altaResult.get(0).getId());

        List<String> media = Collections.singletonList("MÉDIA");
        List<Occurrence> mediaResult = OccurrenceFilter.filter(sampleOccurrences, null, null, null, media);
        assertEquals(1, mediaResult.size());
        assertEquals("48292", mediaResult.get(0).getId());

        List<String> baixa = Collections.singletonList("BAIXA");
        List<Occurrence> baixaResult = OccurrenceFilter.filter(sampleOccurrences, null, null, null, baixa);
        assertEquals(2, baixaResult.size());
    }

    @Test
    public void testFilterByAbertaAndAprovadaStatus() {

        List<String> aberta = Collections.singletonList("ABERTA");
        List<Occurrence> abertaResult = OccurrenceFilter.filter(sampleOccurrences, null, null, aberta);
        assertEquals(2, abertaResult.size());

        List<String> aprovada = Collections.singletonList("APROVADA");
        List<Occurrence> aprovadaResult = OccurrenceFilter.filter(sampleOccurrences, null, null, aprovada);
        assertEquals(2, aprovadaResult.size());
    }
}
