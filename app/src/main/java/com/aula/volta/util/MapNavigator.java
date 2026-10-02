package com.aula.volta.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.aula.volta.R;

import java.util.Locale;

/**
 * Utilitário para navegação e rotas até cooperativas parceiras
 * via Intent nativo de mapas ou fallback web.
 * Fase 30 — Integração com Mapas & Navegação de Cooperativas.
 */
public final class MapNavigator {

    private MapNavigator() {
    }

    /**
     * Abre o local da cooperativa no aplicativo de mapas do dispositivo ou navegador.
     */
    public static void openLocation(Context context, double lat, double lng, String label) {
        String uriStr = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                lat, lng, lat, lng, Uri.encode(label));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));

        try {
            context.startActivity(mapIntent);
        } catch (Exception e) {
            // Fallback: abrir Google Maps na web
            try {
                String webUrl = String.format(Locale.US,
                        "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng);
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                context.startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(context, R.string.error_load, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
