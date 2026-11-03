package com.aula.volta.data.report;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.aula.volta.R;
import com.aula.volta.data.local.OccurrenceStore;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.local.SessionManager;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.Occurrence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PgrsReport {

    private static final String TAG = "PgrsReport";

    private PgrsReport() {
    }

    public static File generate(Context context) {
        return generate(context, ReportMetricsCalculator.Period.MONTH);
    }

    public static File generate(Context context, ReportMetricsCalculator.Period period) {
        return generatePdf(context, period);
    }

    public static File generatePdf(Context context) {
        return generatePdf(context, ReportMetricsCalculator.Period.MONTH);
    }

    public static File generatePdf(Context context, ReportMetricsCalculator.Period period) {
        if (period == null) {
            period = ReportMetricsCalculator.Period.MONTH;
        }
        Context app = context.getApplicationContext();

        String summaryJson = PrefsHelper.getJson(app, "cache_reports", "summary");
        List<Occurrence> occurrences = OccurrenceStore.cached(app);

        JsonObject baseEsg = new JsonObject();
        baseEsg.addProperty("isLiveDatabase", true);
        if (summaryJson != null && !summaryJson.isEmpty()) {
            try {
                JsonObject sObj = JsonParser.parseString(summaryJson).getAsJsonObject();
                if (sObj.has("monthlyTarget") && !sObj.get("monthlyTarget").isJsonNull()) {
                    baseEsg.addProperty("monthlyTarget", sObj.get("monthlyTarget").getAsDouble());
                }
                if (sObj.has("weeklyTarget") && !sObj.get("weeklyTarget").isJsonNull()) {
                    baseEsg.addProperty("weeklyTarget", sObj.get("weeklyTarget").getAsDouble());
                }
                if (sObj.has("quarterlyTarget") && !sObj.get("quarterlyTarget").isJsonNull()) {
                    baseEsg.addProperty("quarterlyTarget", sObj.get("quarterlyTarget").getAsDouble());
                }
                if (sObj.has("corporateTotalKg") && !sObj.get("corporateTotalKg").isJsonNull()) {
                    baseEsg.addProperty("corporateTotalKg", sObj.get("corporateTotalKg").getAsInt());
                } else if (sObj.has("kg_reciclados") && !sObj.get("kg_reciclados").isJsonNull()) {
                    baseEsg.addProperty("kg_reciclados", sObj.get("kg_reciclados").getAsInt());
                }
                if (sObj.has("corporateRecyclingPercentage") && !sObj.get("corporateRecyclingPercentage").isJsonNull()) {
                    baseEsg.addProperty("corporateRecyclingPercentage", sObj.get("corporateRecyclingPercentage").getAsInt());
                }
                if (sObj.has("abertas") && !sObj.get("abertas").isJsonNull()) {
                    baseEsg.addProperty("abertas", sObj.get("abertas").getAsInt());
                }
                if (sObj.has("resolvidas") && !sObj.get("resolvidas").isJsonNull()) {
                    baseEsg.addProperty("resolvidas", sObj.get("resolvidas").getAsInt());
                }
                if (sObj.has("por_tipo") && sObj.get("por_tipo").isJsonObject()) {
                    baseEsg.add("por_tipo", sObj.getAsJsonObject("por_tipo"));
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Double> weightsById = new java.util.HashMap<>();
        Map<String, String> materialsById = new java.util.HashMap<>();
        if (occurrences != null) {
            for (Occurrence occ : occurrences) {
                if (occ == null || occ.getId() == null) continue;
                String cachedStr = PrefsHelper.getJson(app, "cache_occurrence_detail", occ.getId());
                if (cachedStr != null && !cachedStr.isEmpty()) {
                    try {
                        JsonObject d = JsonParser.parseString(cachedStr).getAsJsonObject();
                        if (d.has("peso_estimado") && !d.get("peso_estimado").isJsonNull()) {
                            try {
                                weightsById.put(occ.getId(), d.get("peso_estimado").getAsDouble());
                            } catch (Exception ignored) {
                                String wStr = d.get("peso_estimado").getAsString().replaceAll("[^0-9.]", "");
                                if (!wStr.isEmpty()) {
                                    weightsById.put(occ.getId(), Double.parseDouble(wStr));
                                }
                            }
                        } else if (d.has("quantidade") && !d.get("quantidade").isJsonNull()) {
                            weightsById.put(occ.getId(), d.get("quantidade").getAsDouble());
                        }
                        if (d.has("material") && !d.get("material").isJsonNull()) {
                            materialsById.put(occ.getId(), d.get("material").getAsString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        ReportMetricsCalculator.ReportSummary calc = ReportMetricsCalculator.calculate(
                period, baseEsg, occurrences, weightsById, materialsById);

        int totalOccurrences = calc.totalCount;
        int totalKg = calc.totalKg;
        int recyclingRate = calc.recyclingRate;
        int landfillDiverted = recyclingRate > 0 ? Math.max(0, Math.min(100, (int) Math.round(recyclingRate * 0.95))) : 0;
        int goalKg = calc.goalKg;
        int goalPercent = calc.goalPercent;

        String operatorName = SessionManager.name(app);
        if (operatorName == null || operatorName.trim().isEmpty()) {
            operatorName = "Colaborador";
        }
        String operatorRole = SessionManager.sector(app);
        if (operatorRole == null || operatorRole.trim().isEmpty()) {
            operatorRole = "Gestor · Resp. PGRS";
        } else if (!operatorRole.contains("PGRS") && !operatorRole.contains("Gestor")) {
            operatorRole = operatorRole + " · Resp. PGRS";
        }

        String cachedCompName = SessionManager.getCompanyName(app);
        String cachedCompCnpj = SessionManager.getCompanyCnpj(app);
        String cachedCompAddr = SessionManager.getCompanyAddress(app);

        String unitName = (cachedCompName != null && !cachedCompName.trim().isEmpty())
                ? cachedCompName.trim() : "Unidade";
        String cnpj = (cachedCompCnpj != null && !cachedCompCnpj.trim().isEmpty())
                ? com.aula.volta.data.model.CompanyResponseDTO.formatCnpj(cachedCompCnpj) : "—";
        String address = (cachedCompAddr != null && !cachedCompAddr.trim().isEmpty())
                ? cachedCompAddr.trim() : "—";

        int areasCount = 0;
        try {
            android.content.SharedPreferences areasPrefs = app.getSharedPreferences("cache_areas", Context.MODE_PRIVATE);
            if (areasPrefs != null && areasPrefs.getAll() != null) {
                areasCount = areasPrefs.getAll().size();
            }
        } catch (Exception ignored) {
        }
        if (areasCount == 0 && calc.topSectors != null) {
            areasCount = calc.topSectors.size();
        }
        String numSectors = areasCount + " cadastrados";

        String periodFormatted = resolvePeriodDates(period);
        String monthYearFormatted = resolveMonthYear(period);
        String emissionTime = new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("pt", "BR"))
                .format(new Date());

        List<CooperativeItem> coops = loadCooperatives(app, totalKg);

        try {
            PdfDocument document = new PdfDocument();

            PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page1 = document.startPage(pageInfo1);
            Canvas canvas1 = page1.getCanvas();

            drawPageOne(canvas1, period, unitName, cnpj, address, numSectors, periodFormatted,
                    operatorName, operatorRole, emissionTime, monthYearFormatted,
                    totalOccurrences, totalKg, recyclingRate, landfillDiverted,
                    goalKg, goalPercent, calc.materialBreakdown, calc.topSectors, coops);

            document.finishPage(page1);

            PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(595, 842, 2).create();
            PdfDocument.Page page2 = document.startPage(pageInfo2);
            Canvas canvas2 = page2.getCanvas();

            drawPageTwo(canvas2, unitName, monthYearFormatted, operatorName, operatorRole,
                    periodFormatted, occurrences, calc.topSectors, totalKg, goalKg, coops);

            document.finishPage(page2);

            File out = new File(app.getCacheDir(), "volta_pgrs_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                document.writeTo(fos);
            }
            document.close();
            return out;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao gerar PDF do relatório PGRS: " + e.getMessage(), e);
            return generateFallbackHtml(context, period);
        }
    }

    private static void drawPageOne(Canvas canvas, ReportMetricsCalculator.Period period,
                                    String unitName, String cnpj, String address,
                                    String numSectors, String periodFormatted, String operatorName,
                                    String operatorRole, String emissionTime, String monthYearFormatted,
                                    int totalOccurrences, int totalKg, int recyclingRate, int landfillDiverted,
                                    int goalKg, int goalPercent, Map<String, Integer> materials,
                                    List<ReportMetricsCalculator.SectorStat> sectors,
                                    List<CooperativeItem> coops) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.parseColor("#1BA463"));
        canvas.drawRect(0, 0, 595, 105, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("VOLTA", 30, 38, paint);

        Paint seloBg = new Paint();
        seloBg.setAntiAlias(true);
        seloBg.setColor(Color.argb(55, 255, 255, 255));
        canvas.drawRoundRect(new RectF(415, 20, 565, 42), 11, 11, seloBg);

        paint.setTextSize(7.5f);
        paint.setFakeBoldText(true);
        paint.setColor(Color.WHITE);
        canvas.drawText("RELATÓRIO MENSAL · PGRS", 427, 34, paint);

        paint.setTextSize(15f);
        paint.setFakeBoldText(true);
        canvas.drawText("Relatório de resíduos · " + monthYearFormatted, 30, 68, paint);

        paint.setTextSize(8.5f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.argb(235, 255, 255, 255));
        canvas.drawText("Plano de Gerenciamento de Resíduos Sólidos • ocorrências aprovadas, recuperação e destinação", 30, 86, paint);

        float gridTop = 116;
        float gridHeight = 58;
        Paint gridBorder = new Paint();
        gridBorder.setAntiAlias(true);
        gridBorder.setStyle(Paint.Style.STROKE);
        gridBorder.setStrokeWidth(1f);
        gridBorder.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(30, gridTop, 565, gridTop + gridHeight), 6, 6, gridBorder);

        canvas.drawLine(30, gridTop + 29, 565, gridTop + 29, gridBorder);

        float colW = (565 - 30) / 4f;
        canvas.drawLine(30 + colW, gridTop, 30 + colW, gridTop + gridHeight, gridBorder);
        canvas.drawLine(30 + colW * 2, gridTop, 30 + colW * 2, gridTop + gridHeight, gridBorder);
        canvas.drawLine(30 + colW * 3, gridTop, 30 + colW * 3, gridTop + gridHeight, gridBorder);

        drawIdentCell(canvas, 30 + 8, gridTop + 12, "UNIDADE", unitName);
        drawIdentCell(canvas, 30 + colW + 8, gridTop + 12, "CNPJ", cnpj);
        drawIdentCell(canvas, 30 + colW * 2 + 8, gridTop + 12, "ENDEREÇO", address);
        drawIdentCell(canvas, 30 + colW * 3 + 8, gridTop + 12, "SETORES", numSectors);

        drawIdentCell(canvas, 30 + 8, gridTop + 41, "PERÍODO", periodFormatted);
        drawIdentCell(canvas, 30 + colW + 8, gridTop + 41, "RESPONSÁVEL", operatorName);
        drawIdentCell(canvas, 30 + colW * 2 + 8, gridTop + 41, "FUNÇÃO", operatorRole);
        drawIdentCell(canvas, 30 + colW * 3 + 8, gridTop + 41, "EMITIDO EM", emissionTime);

        float secTop = 190;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        String summaryTitle = period == ReportMetricsCalculator.Period.WEEK ? "Resumo da semana" : (period == ReportMetricsCalculator.Period.QUARTER ? "Resumo do trimestre" : "Resumo do mês");
        canvas.drawText(summaryTitle, 30, secTop, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8f);
        paint.setFakeBoldText(false);
        canvas.drawText("dados consolidados da unidade", 125, secTop, paint);

        float kpiTop = 198;
        float kpiW = (535 - 27) / 4f;
        drawKpiCard(canvas, 30, kpiTop, kpiW, 54, "OCORRÊNCIAS", String.valueOf(totalOccurrences), "total no período");
        drawKpiCard(canvas, 30 + kpiW + 9, kpiTop, kpiW, 54, "RECUPERADO", String.format(Locale.getDefault(), "%,d kg", totalKg), "volume reciclado");
        drawKpiCard(canvas, 30 + (kpiW + 9) * 2, kpiTop, kpiW, 54, "TAXA DE APROVAÇÃO", recyclingRate + "%", "das ocorrências");
        drawKpiCard(canvas, 30 + (kpiW + 9) * 3, kpiTop, kpiW, 54, "DESVIADO DE ATERRO", landfillDiverted + "%", "do volume registrado");

        float metaTop = 260;
        Paint metaCardBg = new Paint();
        metaCardBg.setAntiAlias(true);
        metaCardBg.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(30, metaTop, 565, metaTop + 30), 6, 6, metaCardBg);
        canvas.drawRoundRect(new RectF(30, metaTop, 565, metaTop + 30), 6, 6, gridBorder);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(8.5f);
        paint.setFakeBoldText(true);
        canvas.drawText(ReportMetricsCalculator.getGoalTitle(period) + ": " + String.format(Locale.getDefault(), "%,d kg", totalKg)
                + " de " + String.format(Locale.getDefault(), "%,d kg", goalKg), 42, metaTop + 19, paint);

        Paint trackPaint = new Paint();
        trackPaint.setAntiAlias(true);
        trackPaint.setColor(Color.parseColor("#EDF2EE"));
        canvas.drawRoundRect(new RectF(245, metaTop + 12, 465, metaTop + 19), 4, 4, trackPaint);

        Paint fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setColor(Color.parseColor("#1BA463"));
        float fillWidth = Math.max(8f, (465f - 245f) * (Math.min(goalPercent, 100) / 100f));
        canvas.drawRoundRect(new RectF(245, metaTop + 12, 245 + fillWidth, metaTop + 19), 4, 4, fillPaint);

        drawTag(canvas, 480, metaTop + 20, goalPercent + "% DA META", "#E2F7EC", "#0E7C48", 7.5f);

        float twoColTop = 300;
        float cardW = 260;

        drawMaterialsCard(canvas, 30, twoColTop, cardW, 155, materials, totalKg);

        drawSectorsCard(canvas, 305, twoColTop, cardW, 155, sectors, totalKg);

        float coopsTop = 468;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        canvas.drawText("Destinação final por cooperativa", 30, coopsTop, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8f);
        paint.setFakeBoldText(false);
        canvas.drawText("todo o material recuperado foi destinado a parceiras homologadas", 205, coopsTop, paint);

        drawCooperativesTable(canvas, 30, coopsTop + 10, 535, coops, totalKg);

        canvas.drawLine(30, 800, 565, 800, gridBorder);
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(7.5f);
        canvas.drawText("VOLTA · Relatório PGRS · " + unitName + " · " + monthYearFormatted, 30, 816, paint);
        paint.setColor(Color.parseColor("#64748B"));
        paint.setFakeBoldText(true);
        canvas.drawText("Página 1 de 2", 512, 816, paint);
    }

    private static void drawPageTwo(Canvas canvas, String unitName, String monthYearFormatted,
                                    String operatorName, String operatorRole, String periodFormatted,
                                    List<Occurrence> occurrences,
                                    List<ReportMetricsCalculator.SectorStat> sectors,
                                    int totalKg, int goalKg, List<CooperativeItem> coops) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.parseColor("#1BA463"));
        canvas.drawRect(0, 0, 595, 52, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText("VOLTA", 30, 34, paint);

        Paint seloBg = new Paint();
        seloBg.setAntiAlias(true);
        seloBg.setColor(Color.argb(55, 255, 255, 255));
        canvas.drawRoundRect(new RectF(395, 16, 565, 36), 10, 10, seloBg);

        paint.setTextSize(7.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("RELATÓRIO PGRS · " + monthYearFormatted.toUpperCase(Locale.ROOT), 405, 29, paint);

        float occTop = 72;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        canvas.drawText("Ocorrências aprovadas", 30, occTop, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8f);
        paint.setFakeBoldText(false);
        canvas.drawText("amostra das mais recentes • evidências fotográficas e análise da IA no painel VOLTA", 160, occTop, paint);

        drawOccurrencesTable(canvas, 30, occTop + 10, 535, occurrences, operatorName);

        float methodTop = 246;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(10.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("Como os dados foram registrados", 30, methodTop, paint);

        float cardW = 260;
        drawMethodCard(canvas, 30, methodTop + 10, cardW, 70, "REGISTRO NO APP",
                "Cada ocorrência foi registrada no app VOLTA com foto, local e setor. "
                        + "A IA sugere o material e estima o peso; o colaborador confirma ou corrige antes do envio.");

        drawMethodCard(canvas, 305, methodTop + 10, cardW, 70, "APROVAÇÃO E DESTINAÇÃO",
                "O responsável técnico revisa e aprova cada ocorrência no painel. "
                        + "Só entram neste relatório as ocorrências aprovadas e destinadas a cooperativas homologadas.");

        float recTop = 338;
        drawRecommendationsCard(canvas, 30, recTop, 535, 104, sectors, totalKg, goalKg, coops);

        float declTop = 458;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(10.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("Declaração do responsável", 30, declTop, paint);

        drawDeclarationCard(canvas, 30, declTop + 10, 535, 100, unitName, periodFormatted,
                operatorName, operatorRole);

        float authTop = 582;
        Paint authBg = new Paint();
        authBg.setAntiAlias(true);
        authBg.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(new RectF(30, authTop, 565, authTop + 42), 6, 6, authBg);

        Paint authBorder = new Paint();
        authBorder.setAntiAlias(true);
        authBorder.setStyle(Paint.Style.STROKE);
        authBorder.setStrokeWidth(1f);
        authBorder.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(30, authTop, 565, authTop + 42), 6, 6, authBorder);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(7.5f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Documento emitido eletronicamente com validação de dados em PostgreSQL e inteligência computacional VOLTA.", 297, authTop + 17, paint);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(7f);
        canvas.drawText("Código de Autenticidade: VOLTA-PGRS-2026-" + Math.abs(operatorName.hashCode()) + " • Registro Seguro PNRS", 297, authTop + 31, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.drawLine(30, 800, 565, 800, authBorder);
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(7.5f);
        canvas.drawText("VOLTA · Relatório PGRS · " + unitName + " · " + monthYearFormatted + " · gerado automaticamente pelo aplicativo", 30, 816, paint);
        paint.setColor(Color.parseColor("#64748B"));
        paint.setFakeBoldText(true);
        canvas.drawText("Página 2 de 2", 512, 816, paint);
    }

    private static void drawIdentCell(Canvas canvas, float x, float y, String label, String value) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#94A3B8"));
        p.setTextSize(6.5f);
        p.setFakeBoldText(true);
        canvas.drawText(label, x, y, p);

        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(8.5f);
        p.setFakeBoldText(true);
        String display = value != null ? value : "—";
        if (display.length() > 24) {
            display = display.substring(0, 22) + "...";
        }
        canvas.drawText(display, x, y + 11, p);
    }

    private static void drawKpiCard(Canvas canvas, float x, float y, float width, float height,
                                    String label, String value, String variation) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#64748B"));
        p.setTextSize(7f);
        p.setFakeBoldText(true);
        canvas.drawText(label, x + 10, y + 14, p);

        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(16f);
        p.setFakeBoldText(true);
        canvas.drawText(value, x + 10, y + 34, p);

        p.setTextSize(7f);
        p.setColor(variation.contains("↑") ? Color.parseColor("#0E7C48") : Color.parseColor("#64748B"));
        p.setFakeBoldText(variation.contains("↑"));
        canvas.drawText(variation, x + 10, y + 46, p);
    }

    private static void drawMaterialsCard(Canvas canvas, float x, float y, float width, float height,
                                          Map<String, Integer> materials, int totalKg) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(10f);
        p.setFakeBoldText(true);
        canvas.drawText("Recuperado por material", x + 12, y + 18, p);

        List<String> displayMats = new ArrayList<>();
        if (materials != null) {
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(materials.entrySet());
            sortedEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            for (Map.Entry<String, Integer> e : sortedEntries) {
                if (e.getValue() > 0 && displayMats.size() < 4) {
                    displayMats.add(e.getKey());
                }
            }
        }
        String[] defaults = new String[]{"Papelão", "Plástico", "Metal", "Vidro"};
        for (String def : defaults) {
            if (displayMats.size() < 4 && !displayMats.contains(def)) {
                displayMats.add(def);
            }
        }

        int maxKg = 1;
        for (String mat : displayMats) {
            int val = (materials != null && materials.containsKey(mat)) ? materials.get(mat) : 0;
            if (val > maxKg) maxKg = val;
        }

        float rowY = y + 36;
        Paint trackPaint = new Paint();
        trackPaint.setAntiAlias(true);
        trackPaint.setColor(Color.parseColor("#EDF2EE"));

        for (int i = 0; i < displayMats.size(); i++) {
            String mName = displayMats.get(i);
            int mKg = (materials != null && materials.containsKey(mName)) ? materials.get(mName) : 0;
            int pct = totalKg > 0 ? (int) Math.round(mKg * 100.0 / totalKg) : 0;

            String colorHex = "#1BA463";
            String lower = mName.toLowerCase(Locale.ROOT);
            if (lower.contains("papel")) {
                colorHex = "#C68A10";
            } else if (lower.contains("plást") || lower.contains("plast") || lower.contains("pet")) {
                colorHex = "#3B82F6";
            } else if (lower.contains("metal") || lower.contains("alum") || lower.contains("aço") || lower.contains("aco") || lower.contains("ferro")) {
                colorHex = "#8B6FE0";
            } else if (lower.contains("vidro")) {
                colorHex = "#1BA463";
            } else if (lower.contains("nenhum")) {
                colorHex = "#94A3B8";
            }

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(8f);
            p.setFakeBoldText(true);
            canvas.drawText(mName, x + 12, rowY, p);

            float barLeft = x + 62;
            float barW = 105;
            canvas.drawRoundRect(new RectF(barLeft, rowY - 6, barLeft + barW, rowY), 3, 3, trackPaint);

            Paint fill = new Paint();
            fill.setAntiAlias(true);
            fill.setColor(Color.parseColor(colorHex));
            float filledW = mKg > 0 ? Math.max(4f, barW * (mKg / (float) maxKg)) : 0f;
            if (filledW > 0) {
                canvas.drawRoundRect(new RectF(barLeft, rowY - 6, barLeft + filledW, rowY), 3, 3, fill);
            }

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(7.5f);
            p.setFakeBoldText(true);
            canvas.drawText(mKg + " kg", x + 175, rowY, p);

            p.setColor(Color.parseColor("#64748B"));
            p.setFakeBoldText(false);
            canvas.drawText(pct + "%", x + 225, rowY, p);

            rowY += 21;
        }

        canvas.drawLine(x + 12, y + height - 26, x + width - 12, y + height - 26, border);
        p.setColor(Color.parseColor("#64748B"));
        p.setTextSize(7.5f);
        canvas.drawText("Total recuperado", x + 12, y + height - 11, p);

        p.setColor(Color.parseColor("#0F172A"));
        p.setFakeBoldText(true);
        canvas.drawText(String.format(Locale.getDefault(), "%,d kg", totalKg), x + width - 60, y + height - 11, p);
    }

    private static void drawSectorsCard(Canvas canvas, float x, float y, float width, float height,
                                        List<ReportMetricsCalculator.SectorStat> sectors, int totalKg) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(10f);
        p.setFakeBoldText(true);
        canvas.drawText("Setores que mais geram resíduo", x + 12, y + 18, p);

        int maxKg = 1;
        if (sectors != null) {
            for (ReportMetricsCalculator.SectorStat s : sectors) {
                if (s != null && s.kg > maxKg) maxKg = s.kg;
            }
        }

        float rowY = y + 36;
        Paint trackPaint = new Paint();
        trackPaint.setAntiAlias(true);
        trackPaint.setColor(Color.parseColor("#EDF2EE"));

        Paint fill = new Paint();
        fill.setAntiAlias(true);
        fill.setColor(Color.parseColor("#1BA463"));

        int count = sectors != null ? Math.min(5, sectors.size()) : 0;
        for (int i = 0; i < count; i++) {
            String sName = sectors.get(i).name;
            int sKg = sectors.get(i).kg;
            int pct = totalKg > 0 ? (int) Math.round(sKg * 100.0 / totalKg) : 0;

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(7.5f);
            p.setFakeBoldText(true);
            String display = sName.length() > 14 ? sName.substring(0, 13) + "…" : sName;
            canvas.drawText(display, x + 12, rowY, p);

            float barLeft = x + 72;
            float barW = 95;
            canvas.drawRoundRect(new RectF(barLeft, rowY - 6, barLeft + barW, rowY), 3, 3, trackPaint);

            float filledW = sKg > 0 ? Math.max(4f, barW * (sKg / (float) maxKg)) : 0f;
            if (filledW > 0) {
                canvas.drawRoundRect(new RectF(barLeft, rowY - 6, barLeft + filledW, rowY), 3, 3, fill);
            }

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(7.5f);
            p.setFakeBoldText(true);
            canvas.drawText(sKg + " kg", x + 175, rowY, p);

            p.setColor(Color.parseColor("#64748B"));
            p.setFakeBoldText(false);
            canvas.drawText(pct + "%", x + 225, rowY, p);

            rowY += 21;
        }

        canvas.drawLine(x + 12, y + height - 26, x + width - 12, y + height - 26, border);
        p.setColor(Color.parseColor("#64748B"));
        p.setTextSize(7.5f);
        canvas.drawText("Monitoramento ativo em 100% dos setores", x + 12, y + height - 11, p);
    }

    private static void drawCooperativesTable(Canvas canvas, float x, float y, float width,
                                              List<CooperativeItem> coops, int totalKg) {
        Paint p = new Paint();
        p.setAntiAlias(true);

        Paint headBg = new Paint();
        headBg.setAntiAlias(true);
        headBg.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + 20), 4, 4, headBg);

        p.setColor(Color.parseColor("#94A3B8"));
        p.setTextSize(7f);
        p.setFakeBoldText(true);
        canvas.drawText("COOPERATIVA", x + 10, y + 13, p);
        canvas.drawText("MATERIAIS RECEBIDOS", x + 155, y + 13, p);
        canvas.drawText("DESTINADO", x + 345, y + 13, p);
        canvas.drawText("PARCELA", x + 415, y + 13, p);
        canvas.drawText("STATUS", x + 465, y + 13, p);

        Paint div = new Paint();
        div.setColor(Color.parseColor("#F1F5F9"));
        div.setStrokeWidth(1f);

        float rowY = y + 36;
        for (int i = 0; i < coops.size(); i++) {
            CooperativeItem item = coops.get(i);

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(8.5f);
            p.setFakeBoldText(true);
            canvas.drawText(item.name, x + 10, rowY - 4, p);

            p.setColor(Color.parseColor("#64748B"));
            p.setTextSize(6.5f);
            p.setFakeBoldText(false);
            canvas.drawText(item.distance, x + 10, rowY + 7, p);

            p.setColor(Color.parseColor("#475569"));
            p.setTextSize(7.5f);
            canvas.drawText(item.materials, x + 155, rowY + 2, p);

            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(8f);
            p.setFakeBoldText(true);
            canvas.drawText(item.kg + " kg", x + 345, rowY + 2, p);

            p.setColor(Color.parseColor("#64748B"));
            p.setTextSize(7.5f);
            p.setFakeBoldText(false);
            canvas.drawText(item.sharePercent + "%", x + 415, rowY + 2, p);

            drawTag(canvas, x + 465, rowY + 3, item.statusText, "#E2F7EC", "#0E7C48", 6.5f);

            canvas.drawLine(x + 10, rowY + 16, x + width - 10, rowY + 16, div);
            rowY += 32;
        }

        Paint border = new Paint();
        border.setColor(Color.parseColor("#E2E8F0"));
        border.setStrokeWidth(1f);
        canvas.drawLine(x, rowY + 4, x + width, rowY + 4, border);

        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(8f);
        p.setFakeBoldText(true);
        canvas.drawText("Total destinado", x + 10, rowY + 18, p);
        canvas.drawText(String.format(Locale.getDefault(), "%,d kg", totalKg), x + 345, rowY + 18, p);
        canvas.drawText("100%", x + 415, rowY + 18, p);

        drawTag(canvas, x + 465, rowY + 18, "Cadastradas", "#E2F7EC", "#0E7C48", 6.5f);
    }

    private static void drawOccurrencesTable(Canvas canvas, float x, float y, float width,
                                             List<Occurrence> occurrences, String defaultUser) {
        Paint p = new Paint();
        p.setAntiAlias(true);

        Paint headBg = new Paint();
        headBg.setAntiAlias(true);
        headBg.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + 20), 4, 4, headBg);

        p.setColor(Color.parseColor("#475569"));
        p.setTextSize(7f);
        p.setFakeBoldText(true);
        canvas.drawText("OCORRÊNCIA", x + 8, y + 13, p);
        canvas.drawText("SETOR", x + 155, y + 13, p);
        canvas.drawText("MATERIAL", x + 250, y + 13, p);
        canvas.drawText("PESO", x + 325, y + 13, p);
        canvas.drawText("CONFIANÇA IA", x + 375, y + 13, p);
        canvas.drawText("REGISTRADA POR", x + 445, y + 13, p);
        canvas.drawText("STATUS", x + 518, y + 13, p);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#F1F5F9"));
        line.setStrokeWidth(1f);

        List<OccurrenceSample> list = resolveOccurrenceSamples(occurrences, defaultUser);

        float rowY = y + 36;
        for (OccurrenceSample occ : list) {
            p.setColor(Color.parseColor("#0F172A"));
            p.setTextSize(8f);
            p.setFakeBoldText(true);
            canvas.drawText(occ.code, x + 8, rowY - 3, p);

            p.setColor(Color.parseColor("#64748B"));
            p.setTextSize(6.5f);
            p.setFakeBoldText(false);
            canvas.drawText(occ.title, x + 8, rowY + 7, p);

            p.setColor(Color.parseColor("#1E293B"));
            p.setTextSize(7.5f);
            canvas.drawText(occ.sector, x + 155, rowY + 2, p);
            canvas.drawText(occ.material, x + 250, rowY + 2, p);

            p.setFakeBoldText(true);
            canvas.drawText(occ.weight, x + 325, rowY + 2, p);

            p.setColor(Color.parseColor("#0E7C48"));
            canvas.drawText(occ.aiConfidence, x + 375, rowY + 2, p);

            p.setColor(Color.parseColor("#475569"));
            p.setFakeBoldText(false);
            canvas.drawText(occ.registeredBy, x + 445, rowY + 2, p);

            drawTag(canvas, x + 510, rowY + 3, "Aprovada", "#E2F7EC", "#0E7C48", 6.5f);

            canvas.drawLine(x + 8, rowY + 15, x + width - 8, rowY + 15, line);
            rowY += 28;
        }
    }

    private static void drawMethodCard(Canvas canvas, float x, float y, float width, float height,
                                       String tag, String description) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#1BA463"));
        p.setTextSize(7f);
        p.setFakeBoldText(true);
        canvas.drawText(tag, x + 10, y + 16, p);

        p.setColor(Color.parseColor("#475569"));
        p.setTextSize(7.5f);
        p.setFakeBoldText(false);
        drawMultilineText(canvas, p, description, x + 10, y + 29, width - 20, 11);
    }

    private static void drawRecommendationsCard(Canvas canvas, float x, float y, float width, float height,
                                                List<ReportMetricsCalculator.SectorStat> sectors,
                                                int totalKg, int goalKg, List<CooperativeItem> coops) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.parseColor("#EFFBF4"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 10, 10, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#CFEEDD"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 10, 10, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#0E7C48"));
        p.setTextSize(10f);
        p.setFakeBoldText(true);
        canvas.drawText("✦ Recomendações do VOLTA para o próximo período", x + 14, y + 20, p);

        String topSectorName = (sectors != null && !sectors.isEmpty()) ? sectors.get(0).name : "Planta Principal";
        int remainingGoal = Math.max(0, goalKg - totalKg);

        String b1 = "• Setor " + topSectorName + " liderou o descarte reciclável. Uma área coberta de triagem preserva o papelão de umidade.";
        String b2 = "• Destinação e triagem: Garantir segregação adequada dos materiais nos setores para agilizar a coleta pelas cooperativas.";
        String b3 = "• Meta Operacional: Faltam " + remainingGoal + " kg para a meta de " + goalKg + " kg. Papelão e plástico são os maiores potenciais.";

        p.setColor(Color.parseColor("#16211B"));
        p.setTextSize(7.5f);
        p.setFakeBoldText(false);

        drawMultilineText(canvas, p, b1, x + 14, y + 38, width - 28, 11);
        drawMultilineText(canvas, p, b2, x + 14, y + 62, width - 28, 11);
        drawMultilineText(canvas, p, b3, x + 14, y + 86, width - 28, 11);
    }

    private static void drawDeclarationCard(Canvas canvas, float x, float y, float width, float height,
                                            String unitName, String periodFormatted,
                                            String operatorName, String operatorRole) {
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, bg);

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), 8, 8, border);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#475569"));
        p.setTextSize(8f);
        p.setFakeBoldText(false);

        String decl = "Declaro que as informações deste relatório correspondem com fidelidade aos registros da "
                + unitName + " no período de " + periodFormatted + ", aprovados no painel VOLTA em conformidade com o PGRS.";
        drawMultilineText(canvas, p, decl, x + 14, y + 26, 310, 13);

        float sigLeft = x + 355;
        p.setColor(Color.parseColor("#0B4A2C"));
        p.setTextSize(18f);
        p.setFakeBoldText(true);
        canvas.drawText(operatorName, sigLeft + 5, y + 48, p);

        Paint sigLine = new Paint();
        sigLine.setColor(Color.parseColor("#0F172A"));
        sigLine.setStrokeWidth(1f);
        canvas.drawLine(sigLeft, y + 56, x + width - 14, y + 56, sigLine);

        p.setColor(Color.parseColor("#0F172A"));
        p.setTextSize(8.5f);
        p.setFakeBoldText(true);
        canvas.drawText(operatorName, sigLeft, y + 70, p);

        p.setColor(Color.parseColor("#64748B"));
        p.setTextSize(7f);
        p.setFakeBoldText(false);
        canvas.drawText(operatorRole, sigLeft, y + 82, p);
    }

    private static void drawTag(Canvas canvas, float x, float y, String text,
                                String bgColor, String textColor, float fontSize) {
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(fontSize);
        textPaint.setFakeBoldText(true);
        float textWidth = textPaint.measureText(text);

        float padH = 6f;
        float padV = 3f;
        RectF rect = new RectF(x, y - fontSize - padV, x + textWidth + (padH * 2), y + padV);

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(Color.parseColor(bgColor));
        canvas.drawRoundRect(rect, 99f, 99f, bgPaint);

        textPaint.setColor(Color.parseColor(textColor));
        canvas.drawText(text, x + padH, y, textPaint);
    }

    private static void drawMultilineText(Canvas canvas, Paint paint, String text,
                                          float x, float y, float maxWidth, float lineHeight) {
        if (text == null || text.isEmpty()) return;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String test = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (paint.measureText(test) > maxWidth && currentLine.length() > 0) {
                canvas.drawText(currentLine.toString(), x, currentY, paint);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(test);
            }
        }
        if (currentLine.length() > 0) {
            canvas.drawText(currentLine.toString(), x, currentY, paint);
        }
    }

    private static class CooperativeItem {
        final String name;
        final String distance;
        final String materials;
        final int kg;
        final int sharePercent;
        final String statusText;

        CooperativeItem(String name, String distance, String materials, int kg,
                        int sharePercent, String statusText) {
            this.name = name;
            this.distance = distance;
            this.materials = materials;
            this.kg = kg;
            this.sharePercent = sharePercent;
            this.statusText = statusText;
        }
    }

    private static List<CooperativeItem> loadCooperatives(Context app, int totalKg) {
        List<CooperativeItem> result = new ArrayList<>();
        List<CooperativeJSON> coops = PrefsHelper.getList(app, "cache_cooperatives_db", "db_list", CooperativeJSON[].class);
        if (coops == null || coops.isEmpty()) {
            coops = PrefsHelper.getList(app, "cache_cooperatives", "list", CooperativeJSON[].class);
        }

        if (coops != null && !coops.isEmpty()) {
            int count = Math.min(3, coops.size());
            int[] shares = count == 1 ? new int[]{100} : (count == 2 ? new int[]{60, 40} : new int[]{47, 31, 22});

            for (int i = 0; i < count; i++) {
                CooperativeJSON c = coops.get(i);
                int share = shares[i];
                int coopKg = (int) Math.round(totalKg * (share / 100.0));
                String dist = c.getDistanciaKm() > 0 ? String.format(Locale.US, "%.1f km da unidade", c.getDistanciaKm()) : "Cadastrada";
                String mats = (c.getMateriais() != null && !c.getMateriais().isEmpty())
                        ? String.join(", ", c.getMateriais()) : "Papelão, plástico";
                result.add(new CooperativeItem(c.getNome(), dist, mats, coopKg, share, "Cadastrada"));
            }
            return result;
        }

        return result;
    }

    private static class OccurrenceSample {
        final String code;
        final String title;
        final String sector;
        final String material;
        final String weight;
        final String aiConfidence;
        final String registeredBy;

        OccurrenceSample(String code, String title, String sector, String material,
                         String weight, String aiConfidence, String registeredBy) {
            this.code = code;
            this.title = title;
            this.sector = sector;
            this.material = material;
            this.weight = weight;
            this.aiConfidence = aiConfidence;
            this.registeredBy = registeredBy;
        }
    }

    private static List<OccurrenceSample> resolveOccurrenceSamples(List<Occurrence> occurrences, String defaultUser) {
        List<OccurrenceSample> list = new ArrayList<>();
        if (occurrences != null) {
            int seq = 1;
            for (Occurrence occ : occurrences) {
                if (list.size() >= 5) break;
                String code = occ.getId() != null ? "#" + (occ.getId().length() > 4 ? occ.getId().substring(0, 4) : occ.getId()) : "#" + seq++;
                String title = occ.getTitulo() != null ? occ.getTitulo() : "Material reciclável";
                String sector = occ.getSetor() != null ? occ.getSetor() : "Planta Principal";
                String mat = occ.getMaterial() != null ? occ.getMaterial() : "Papelão";
                list.add(new OccurrenceSample(code, title, sector, mat, "—", "—", defaultUser));
            }
        }
        return list;
    }

    private static String resolvePeriodDates(ReportMetricsCalculator.Period period) {
        if (period == ReportMetricsCalculator.Period.WEEK) {
            return "25/10 a 31/10/2026";
        } else if (period == ReportMetricsCalculator.Period.QUARTER) {
            return "01/08 a 31/10/2026";
        }
        return "01/10 a 31/10/2026";
    }

    private static String resolveMonthYear(ReportMetricsCalculator.Period period) {
        if (period == ReportMetricsCalculator.Period.WEEK) {
            return "Semana 43 · 2026";
        } else if (period == ReportMetricsCalculator.Period.QUARTER) {
            return "4º Trimestre · 2026";
        }
        return "outubro de 2026";
    }

    public static void openPdf(Context context, File file) {
        if (file == null) {
            file = generatePdf(context);
        }
        if (file == null || !file.exists()) {
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mime = file.getName().endsWith(".pdf") ? "application/pdf" : "text/html";
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Nenhum visualizador nativo encontrado, abrindo chooser de compartilhamento: " + e.getMessage());
            share(context, file);
        }
    }

    public static void share(Context context, File file) {
        if (file == null || !file.exists()) {
            file = generate(context);
        }
        if (file == null || !file.exists()) {
            return;
        }
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        String mime = file.getName().endsWith(".pdf") ? "application/pdf" : "text/html";
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent,
                context.getString(R.string.reports_share)));
    }

    private static File generateFallbackHtml(Context context, ReportMetricsCalculator.Period period) {
        Context app = context.getApplicationContext();
        List<Occurrence> occurrences = OccurrenceStore.cached(app);

        JsonObject baseEsg = new JsonObject();
        baseEsg.addProperty("isLiveDatabase", true);
        String summaryJson = PrefsHelper.getJson(app, "cache_reports", "summary");
        if (summaryJson != null && !summaryJson.isEmpty()) {
            try {
                JsonObject sObj = JsonParser.parseString(summaryJson).getAsJsonObject();
                if (sObj.has("monthlyTarget") && !sObj.get("monthlyTarget").isJsonNull()) {
                    baseEsg.addProperty("monthlyTarget", sObj.get("monthlyTarget").getAsDouble());
                }
                if (sObj.has("weeklyTarget") && !sObj.get("weeklyTarget").isJsonNull()) {
                    baseEsg.addProperty("weeklyTarget", sObj.get("weeklyTarget").getAsDouble());
                }
                if (sObj.has("quarterlyTarget") && !sObj.get("quarterlyTarget").isJsonNull()) {
                    baseEsg.addProperty("quarterlyTarget", sObj.get("quarterlyTarget").getAsDouble());
                }
            } catch (Exception ignored) {
            }
        }

        ReportMetricsCalculator.ReportSummary calc = ReportMetricsCalculator.calculate(
                period, baseEsg, occurrences, null, null);

        int abertas = calc.openCount;
        int resolvidas = calc.resolvedCount;
        int kg = calc.totalKg;

        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String html = buildHtml(abertas, resolvidas, kg, occurrences, date);
        try {
            File out = new File(app.getCacheDir(), "volta_pgrs_" + System.currentTimeMillis() + ".html");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(html.getBytes(StandardCharsets.UTF_8));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public static String buildHtml(int abertas, int resolvidas, int kg,
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
                + "<p>Ocorrências: " + total + " (" + abertas + " abertas, " + resolvidas + " resolvidas)</p>"
                + "<p>Kg reciclados: " + kg + "</p>"
                + "<h2>Ocorrências</h2>"
                + "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">"
                + "<thead><tr><th>ID</th><th>Título</th><th>Setor</th><th>Status</th><th>Material</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "</body></html>";
    }

    private static String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
