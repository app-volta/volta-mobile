package com.aula.volta.data.report;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.aula.volta.R;

import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Relatório PGRS em HTML (Fase 14). Gera do cache local e compartilha via FileProvider.
 * Backend de relatórios pluga aqui depois; o HTML é montado por função pura testável.
 */
public final class PgrsReport {

    private PgrsReport() {
    }

    /** Gera o arquivo no cache e devolve; null se não há dados. */
    public static File generate(Context context) {
        Context app = context.getApplicationContext();
        String summaryJson = PrefsHelper.getJson(app, "cache_reports", "summary");
        List<Occurrence> occurrences = OccurrenceStore.cached(app);
        if (summaryJson == null && (occurrences == null || occurrences.isEmpty())) {
            return null;
        }
        int abertas = 0;
        int resolvidas = 0;
        int kg = 0;
        try {
            if (summaryJson != null) {
                JsonObject summary = JsonParser.parseString(summaryJson).getAsJsonObject();
                abertas = intOpt(summary, "abertas");
                resolvidas = intOpt(summary, "resolvidas");
                kg = intOpt(summary, "kg_reciclados");
            }
        } catch (Exception ignored) {
            // usa zeros
        }
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        String html = buildHtml(abertas, resolvidas, kg, occurrences, date);
        try {
            File out = new File(app.getCacheDir(),
                    "volta_pgrs_" + System.currentTimeMillis() + ".html");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(html.getBytes(StandardCharsets.UTF_8));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /** Abre o chooser de compartilhamento do HTML gerado. */
    public static void share(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent,
                context.getString(R.string.reports_share)));
    }

    /** Monta o HTML (pura — coberta por teste unitário). */
    static String buildHtml(int abertas, int resolvidas, int kg,
                            List<Occurrence> occurrences, String date) {
        int total = abertas + resolvidas;
        StringBuilder rows = new StringBuilder();
        if (occurrences != null) {
            for (Occurrence o : occurrences) {
                rows.append("<tr><td>#").append(esc(o.getId())).append("</td><td>")
                        .append(esc(o.getTitulo())).append("</td><td>")
                        .append(esc(o.getSetor())).append("</td><td>")
                        .append(esc(o.getStatus())).append("</td><td>")
                        .append(esc(o.getMaterial())).append("</td></tr>");
            }
        }
        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<title>VOLTA — Relatório PGRS</title></head><body>"
                + "<h1>VOLTA — Relatório PGRS</h1>"
                + "<p>Gerado em " + esc(date) + "</p>"
                + "<h2>Resumo da unidade</h2>"
                + "<ul><li>Ocorrências: " + total + "</li>"
                + "<li>Abertas: " + abertas + "</li>"
                + "<li>Resolvidas: " + resolvidas + "</li>"
                + "<li>Kg reciclados: " + kg + "</li></ul>"
                + "<h2>Ocorrências</h2>"
                + "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">"
                + "<tr><th>ID</th><th>Título</th><th>Setor</th>"
                + "<th>Status</th><th>Material</th></tr>"
                + rows
                + "</table></body></html>";
    }

    private static int intOpt(JsonObject obj, String member) {
        try {
            return obj.has(member) && !obj.get(member).isJsonNull()
                    ? obj.get(member).getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
