package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aula.volta.data.api.ApiClient;
import com.aula.volta.data.api.ReportAPI;
import com.aula.volta.data.local.AuditLog;
import com.aula.volta.data.local.PrefsHelper;
import com.aula.volta.data.report.PgrsReport;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Relatórios — Figma 790:3322 (PGRS, período, mês, meta, por material).
 * GET /reports/summary (mock → API-ready), mesmo cache da Home.
 * Meta de 1.500 kg (canônica no app). Período: mock tem um dataset —
 * os chips alternam seleção e recarregam (API filtra no futuro).
 */
public class ReportsFragment extends Fragment {

    private static final String PREFS_REPORTS = "cache_reports";
    private static final String KEY_SUMMARY = "summary";
    /** Meta mensal canônica (sino n4: "1.147 kg de 1.500 kg"). */
    private static final int GOAL_KG = 1500;

    private TextView tvOccurrences;
    private TextView tvKg;
    private TextView tvRate;
    private TextView tvGoalPercent;
    private TextView tvGoalCurrent;
    private TextView tvGoalTotal;
    private View goalProgress;
    private LinearLayout materialContainer;

    private TextView chipWeek;
    private TextView chipMonth;
    private TextView chipQuarter;

    public ReportsFragment() {
        // Construtor vazio obrigatório
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOccurrences = view.findViewById(R.id.tvReportOccurrences);
        tvKg = view.findViewById(R.id.tvReportKg);
        tvRate = view.findViewById(R.id.tvReportRate);
        tvGoalPercent = view.findViewById(R.id.tvGoalPercent);
        tvGoalCurrent = view.findViewById(R.id.tvGoalCurrent);
        tvGoalTotal = view.findViewById(R.id.tvGoalTotal);
        goalProgress = view.findViewById(R.id.viewGoalProgress);
        materialContainer = view.findViewById(R.id.materialContainer);

        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipQuarter = view.findViewById(R.id.chipQuarter);
        chipWeek.setOnClickListener(v -> selectPeriod(chipWeek));
        chipMonth.setOnClickListener(v -> selectPeriod(chipMonth));
        chipQuarter.setOnClickListener(v -> selectPeriod(chipQuarter));

        View btnPgrs = view.findViewById(R.id.btnGeneratePgrs);
        btnPgrs.setOnClickListener(v -> {
            java.io.File report = PgrsReport.generate(requireContext());
            if (report == null) {
                Toast.makeText(requireContext(), R.string.reports_nothing,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            AuditLog.append(requireContext(), "Breno Gomes", "GERAR",
                    "relatorio", report.getName(), null, null);
            PgrsReport.share(requireContext(), report);
        });

        View btnExportAll = view.findViewById(R.id.btnExportAllData);
        if (btnExportAll != null) {
            btnExportAll.setOnClickListener(v -> {
                java.io.File report = PgrsReport.generate(requireContext());
                if (report == null) {
                    Toast.makeText(requireContext(), R.string.reports_nothing,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                AuditLog.append(requireContext(), "Breno Gomes", "EXPORTAR_DADOS",
                        "relatorio", report.getName(), null, null);
                PgrsReport.share(requireContext(), report);
            });
        }

        loadSummary();
    }

    private void selectPeriod(TextView selected) {
        for (TextView chip : new TextView[]{chipWeek, chipMonth, chipQuarter}) {
            boolean on = chip == selected;
            chip.setBackgroundResource(on
                    ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(on
                    ? R.color.white : R.color.volta_text_secondary_light));
        }
        loadSummary();
    }

    private void loadSummary() {
        ReportAPI api = ApiClient.get(requireContext()).create(ReportAPI.class);
        api.getSummary().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    PrefsHelper.putJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY,
                            response.body().toString());
                    applySummary(response.body());
                } else {
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                loadFromCache();
                Toast.makeText(requireContext(), R.string.offline_cache,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFromCache() {
        String json = PrefsHelper.getJson(requireContext(), PREFS_REPORTS, KEY_SUMMARY);
        if (json != null) {
            try {
                applySummary(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
                // mantém zeros do layout
            }
        }
    }

    private void applySummary(JsonObject summary) {
        int abertas = intOpt(summary, "abertas");
        int resolvidas = intOpt(summary, "resolvidas");
        int kg = intOpt(summary, "kg_reciclados");
        int total = abertas + resolvidas;

        tvOccurrences.setText(String.valueOf(total));
        tvKg.setText(String.format(new Locale("pt", "BR"), "%,d", kg));
        int rate = total == 0 ? 0 : Math.round(resolvidas * 100f / total);
        tvRate.setText(rate + "%");

        int percent = Math.min(100, Math.round(kg * 100f / GOAL_KG));
        tvGoalPercent.setText(percent + "%");
        tvGoalCurrent.setText(String.format(new Locale("pt", "BR"), "%,d kg", kg));
        tvGoalTotal.setText(getString(R.string.reports_goal_of,
                String.format(new Locale("pt", "BR"), "%,d kg", GOAL_KG)));
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) goalProgress.getLayoutParams();
        params.weight = Math.max(percent, 2);
        goalProgress.setLayoutParams(params);

        renderMaterials(summary);
    }

    private int intOpt(JsonObject obj, String member) {
        try {
            return obj.has(member) && !obj.get(member).isJsonNull()
                    ? obj.get(member).getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Barras por material a partir de "por_tipo": {nome: qtd}. */
    private void renderMaterials(JsonObject summary) {
        materialContainer.removeAllViews();
        List<Map.Entry<String, Integer>> items = new ArrayList<>();
        int max = 1;
        try {
            if (summary.has("por_tipo") && summary.get("por_tipo").isJsonObject()) {
                for (Map.Entry<String, com.google.gson.JsonElement> e
                        : summary.getAsJsonObject("por_tipo").entrySet()) {
                    int v = e.getValue().getAsInt();
                    items.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), v));
                    max = Math.max(max, v);
                }
            }
        } catch (Exception ignored) {
            // lista vazia
        }
        float density = getResources().getDisplayMetrics().density;
        for (Map.Entry<String, Integer> item : items) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            int padV = (int) (6 * density);
            row.setPadding(0, padV, 0, padV);

            LinearLayout top = new LinearLayout(requireContext());
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView name = new TextView(requireContext());
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            name.setText(item.getKey());
            name.setTextColor(requireContext().getColor(R.color.volta_text_primary_light));
            name.setTextSize(15);
            name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);

            TextView count = new TextView(requireContext());
            count.setText(getString(R.string.reports_material_kg, item.getValue()));
            count.setTextColor(requireContext().getColor(R.color.volta_text_secondary_light));
            count.setTextSize(13);
            count.setTypeface(count.getTypeface(), android.graphics.Typeface.BOLD);

            top.addView(name);
            top.addView(count);

            LinearLayout track = new LinearLayout(requireContext());
            track.setOrientation(LinearLayout.HORIZONTAL);
            track.setBackgroundResource(R.drawable.bg_progress_track);
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (8 * density));
            trackParams.topMargin = (int) (6 * density);
            track.setLayoutParams(trackParams);

            View fill = new View(requireContext());
            int pct = Math.max(Math.round(item.getValue() * 100f / max), 2);
            fill.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, pct));
            fill.setBackgroundResource(R.drawable.bg_progress_filled);
            track.addView(fill);

            row.addView(top);
            row.addView(track);
            materialContainer.addView(row);
        }
    }
}
