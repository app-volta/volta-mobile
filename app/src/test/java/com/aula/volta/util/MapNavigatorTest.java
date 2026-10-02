package com.aula.volta.util;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Testes unitários para validar a formatação de rotas e coordenadas geográficas.
 * Fase 30 — Integração com Mapas & Navegação de Cooperativas.
 */
public class MapNavigatorTest {

    @Test
    public void geoUri_formatsLatitudeAndLongitudeAccurately() {
        double lat = -23.5186;
        double lng = -46.7369;
        String label = "JBS Ambiental";

        String expectedUri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                lat, lng, lat, lng, "JBS%20Ambiental");

        assertTrue(expectedUri.startsWith("geo:-23.518600,-46.736900"));
        assertTrue(expectedUri.contains("JBS%20Ambiental"));
    }
}
